package web.kplay.studentmanagement.service.invoice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.invoice.Invoice;
import web.kplay.studentmanagement.domain.invoice.InvoiceStatus;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.domain.user.User;
import web.kplay.studentmanagement.dto.invoice.*;
import web.kplay.studentmanagement.exception.BusinessException;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.InvoiceRepository;
import web.kplay.studentmanagement.repository.StudentRepository;
import web.kplay.studentmanagement.repository.UserRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 청구서 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    /**
     * 청구서 생성
     */
    @Transactional
    public InvoiceResponse createInvoice(InvoiceCreateRequest request, Long issuedById) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));

        User issuedBy = userRepository.findById(issuedById)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

        // 납부 기한 검증
        if (request.getDueDate().isBefore(request.getIssueDate())) {
            throw new BusinessException("납부 기한은 발급일 이후여야 합니다");
        }

        Invoice invoice = Invoice.builder()
                .student(student)
                .issuedBy(issuedBy)
                .title(request.getTitle())
                .amount(request.getAmount())
                .issueDate(request.getIssueDate())
                .dueDate(request.getDueDate())
                .status(InvoiceStatus.PENDING)
                .description(request.getDescription())
                .memo(request.getMemo())
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);
        savedInvoice.generateInvoiceNumber();

        log.info("청구서 생성: id={}, 학생={}, 금액={}원, 납부기한={}",
                savedInvoice.getId(), student.getStudentName(),
                savedInvoice.getAmount(), savedInvoice.getDueDate());

        return toResponse(savedInvoice);
    }

    /**
     * 청구서 조회
     */
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("청구서를 찾을 수 없습니다"));
        return toResponse(invoice);
    }

    /**
     * 전체 청구서 조회
     */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoices() {
        return invoiceRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 학생별 청구서 조회
     */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesByStudent(Long studentId) {
        return invoiceRepository.findByStudentId(studentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 상태별 청구서 조회
     */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesByStatus(InvoiceStatus status) {
        return invoiceRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 기간별 청구서 조회
     */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesByDateRange(LocalDate startDate, LocalDate endDate) {
        return invoiceRepository.findByIssueDateBetween(startDate, endDate).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 학생의 미납 청구서 조회
     */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getUnpaidInvoicesByStudent(Long studentId) {
        return invoiceRepository.findByStudentIdAndStatus(studentId, InvoiceStatus.PENDING).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 연체된 청구서 조회
     */
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getOverdueInvoices() {
        LocalDate today = LocalDate.now();
        return invoiceRepository.findOverdueInvoices(today).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 청구서 수정
     */
    @Transactional
    public InvoiceResponse updateInvoice(Long id, InvoiceUpdateRequest request) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("청구서를 찾을 수 없습니다"));

        invoice.updateInvoice(
                request.getTitle(),
                request.getAmount(),
                request.getDueDate(),
                request.getDescription()
        );

        log.info("청구서 수정: id={}, 제목={}", id, request.getTitle());

        return toResponse(invoice);
    }

    /**
     * 청구서 납부 처리
     */
    @Transactional
    public InvoiceResponse markAsPaid(Long id, InvoicePaymentRequest request) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("청구서를 찾을 수 없습니다"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException("이미 납부된 청구서입니다");
        }

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessException("취소된 청구서는 납부할 수 없습니다");
        }

        invoice.markAsPaid(request.getPaymentMethod());

        log.info("청구서 납부 완료: id={}, 학생={}, 금액={}원, 결제수단={}",
                invoice.getId(),
                invoice.getStudent().getStudentName(),
                invoice.getAmount(),
                request.getPaymentMethod());

        return toResponse(invoice);
    }

    /**
     * 청구서 취소
     */
    @Transactional
    public InvoiceResponse cancelInvoice(Long id, String reason) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("청구서를 찾을 수 없습니다"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException("납부된 청구서는 취소할 수 없습니다. 환불 처리가 필요합니다.");
        }

        invoice.cancel(reason);

        log.info("청구서 취소: id={}, 사유={}", id, reason);

        return toResponse(invoice);
    }

    /**
     * 청구서 삭제
     */
    @Transactional
    public void deleteInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("청구서를 찾을 수 없습니다"));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException("납부된 청구서는 삭제할 수 없습니다");
        }

        invoiceRepository.delete(invoice);
        log.info("청구서 삭제: id={}", id);
    }

    /**
     * 메모 업데이트
     */
    @Transactional
    public InvoiceResponse updateMemo(Long id, String memo) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("청구서를 찾을 수 없습니다"));

        invoice.updateMemo(memo);
        log.info("청구서 메모 업데이트: id={}", id);

        return toResponse(invoice);
    }

    /**
     * 연체 청구서 자동 업데이트 (매일 오전 1시 실행)
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void updateOverdueInvoices() {
        LocalDate today = LocalDate.now();
        List<Invoice> overdueInvoices = invoiceRepository.findOverdueInvoices(today);

        for (Invoice invoice : overdueInvoices) {
            invoice.markAsOverdue();
            log.info("연체 처리: 청구서ID={}, 학생={}, 연체일수={}일",
                    invoice.getId(),
                    invoice.getStudent().getStudentName(),
                    ChronoUnit.DAYS.between(invoice.getDueDate(), today));
        }

        log.info("연체 청구서 자동 업데이트 완료: {}건", overdueInvoices.size());
    }

    /**
     * 청구서 통계
     */
    @Transactional(readOnly = true)
    public InvoiceStatistics getStatistics() {
        List<Invoice> allInvoices = invoiceRepository.findAll();

        long totalCount = allInvoices.size();
        long pendingCount = allInvoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PENDING)
                .count();
        long paidCount = allInvoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PAID)
                .count();
        long overdueCount = allInvoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.OVERDUE)
                .count();

        int totalAmount = allInvoices.stream()
                .mapToInt(Invoice::getAmount)
                .sum();
        int paidAmount = allInvoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PAID)
                .mapToInt(Invoice::getAmount)
                .sum();
        int unpaidAmount = allInvoices.stream()
                .filter(i -> i.getStatus() == InvoiceStatus.PENDING || i.getStatus() == InvoiceStatus.OVERDUE)
                .mapToInt(Invoice::getAmount)
                .sum();

        return InvoiceStatistics.builder()
                .totalCount(totalCount)
                .pendingCount(pendingCount)
                .paidCount(paidCount)
                .overdueCount(overdueCount)
                .totalAmount(totalAmount)
                .paidAmount(paidAmount)
                .unpaidAmount(unpaidAmount)
                .build();
    }

    /**
     * Entity -> DTO 변환
     */
    private InvoiceResponse toResponse(Invoice invoice) {
        LocalDate today = LocalDate.now();
        long daysUntilDue = ChronoUnit.DAYS.between(today, invoice.getDueDate());
        boolean isOverdue = invoice.getStatus() == InvoiceStatus.PENDING &&
                today.isAfter(invoice.getDueDate());

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .studentId(invoice.getStudent().getId())
                .studentName(invoice.getStudent().getStudentName())
                .parentName(invoice.getStudent().getParentName())
                .parentPhone(invoice.getStudent().getParentPhone())
                .issuedById(invoice.getIssuedBy() != null ? invoice.getIssuedBy().getId() : null)
                .issuedByName(invoice.getIssuedBy() != null ? invoice.getIssuedBy().getName() : null)
                .title(invoice.getTitle())
                .amount(invoice.getAmount())
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus())
                .description(invoice.getDescription())
                .invoiceNumber(invoice.getInvoiceNumber())
                .paidAt(invoice.getPaidAt())
                .paymentMethod(invoice.getPaymentMethod())
                .memo(invoice.getMemo())
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .daysUntilDue((int) daysUntilDue)
                .isOverdue(isOverdue)
                .build();
    }
}
