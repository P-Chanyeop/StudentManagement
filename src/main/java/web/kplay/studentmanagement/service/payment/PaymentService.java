package web.kplay.studentmanagement.service.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.invoice.Invoice;
import web.kplay.studentmanagement.domain.payment.Payment;
import web.kplay.studentmanagement.domain.payment.PaymentStatus;
import web.kplay.studentmanagement.dto.payment.*;
import web.kplay.studentmanagement.exception.BusinessException;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.InvoiceRepository;
import web.kplay.studentmanagement.repository.PaymentRepository;
import web.kplay.studentmanagement.service.invoice.InvoiceService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 결제 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final PortOneService portOneService;
    private final InvoiceService invoiceService;

    @Value("${portone.pg-provider:nice}")
    private String defaultPgProvider;

    /**
     * 결제 준비
     * 클라이언트가 결제창을 띄우기 전에 호출
     */
    @Transactional
    public PaymentPrepareResponse preparePayment(PaymentPrepareRequest request) {
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("청구서를 찾을 수 없습니다"));

        // 청구서 검증
        if (invoice.getStatus() == web.kplay.studentmanagement.domain.invoice.InvoiceStatus.PAID) {
            throw new BusinessException("이미 납부된 청구서입니다");
        }

        if (invoice.getStatus() == web.kplay.studentmanagement.domain.invoice.InvoiceStatus.CANCELLED) {
            throw new BusinessException("취소된 청구서는 납부할 수 없습니다");
        }

        // 결제 금액 검증
        if (!request.getAmount().equals(invoice.getAmount())) {
            throw new BusinessException("결제 금액이 청구서 금액과 일치하지 않습니다");
        }

        // 가맹점 주문번호 생성
        String merchantUid = "ORDER_" + invoice.getInvoiceNumber() + "_" + UUID.randomUUID().toString().substring(0, 8);

        // PG사 설정
        String pgProvider = request.getPgProvider() != null ? request.getPgProvider() : defaultPgProvider;

        log.info("결제 준비: invoiceId={}, merchantUid={}, amount={}원",
                request.getInvoiceId(), merchantUid, request.getAmount());

        return PaymentPrepareResponse.builder()
                .merchantUid(merchantUid)
                .amount(request.getAmount())
                .name(invoice.getTitle())
                .buyerName(request.getBuyerName())
                .buyerTel(request.getBuyerTel())
                .buyerEmail(request.getBuyerEmail())
                .paymentMethod(request.getPaymentMethod())
                .pgProvider(pgProvider)
                .build();
    }

    /**
     * 결제 완료 검증 및 저장
     * 클라이언트에서 결제 완료 후 호출
     */
    @Transactional
    public PaymentResponse completePayment(PaymentCompleteRequest request) {
        // 중복 결제 확인
        if (paymentRepository.findByImpUid(request.getImpUid()).isPresent()) {
            throw new BusinessException("이미 처리된 결제입니다");
        }

        // 포트원 API로 결제 정보 조회
        Map<String, Object> paymentInfo = portOneService.getPaymentInfo(request.getImpUid());

        // 결제 정보 파싱
        String merchantUid = (String) paymentInfo.get("merchant_uid");
        String status = (String) paymentInfo.get("status");
        Integer amount = (Integer) paymentInfo.get("amount");
        String paymentMethod = (String) paymentInfo.get("pay_method");
        String pgProvider = (String) paymentInfo.get("pg_provider");
        String buyerName = (String) paymentInfo.get("buyer_name");
        String buyerTel = (String) paymentInfo.get("buyer_tel");
        String buyerEmail = (String) paymentInfo.get("buyer_email");
        String receiptUrl = (String) paymentInfo.get("receipt_url");

        // 결제 상태 확인
        if (!"paid".equals(status)) {
            throw new BusinessException("결제가 완료되지 않았습니다. 상태: " + status);
        }

        // 가맹점 주문번호에서 Invoice Number 추출
        String invoiceNumber = extractInvoiceNumber(merchantUid);
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("청구서를 찾을 수 없습니다: " + invoiceNumber));

        // 결제 금액 검증
        if (!portOneService.verifyPayment(request.getImpUid(), invoice.getAmount())) {
            throw new BusinessException("결제 금액 검증 실패");
        }

        // 결제 정보 저장
        Payment payment = Payment.builder()
                .invoice(invoice)
                .impUid(request.getImpUid())
                .merchantUid(merchantUid)
                .amount(amount)
                .status(PaymentStatus.PAID)
                .paymentMethod(paymentMethod)
                .pgProvider(pgProvider)
                .buyerName(buyerName)
                .buyerTel(buyerTel)
                .buyerEmail(buyerEmail)
                .receiptUrl(receiptUrl)
                .build();

        // 결제 시각 파싱
        if (paymentInfo.get("paid_at") != null) {
            long paidAtTimestamp = ((Number) paymentInfo.get("paid_at")).longValue();
            LocalDateTime paidAt = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(paidAtTimestamp), ZoneId.systemDefault());
            payment.markAsPaid(paidAt, receiptUrl);
        }

        // 카드 정보 저장
        if ("card".equals(paymentMethod) && paymentInfo.get("card_name") != null) {
            String cardName = (String) paymentInfo.get("card_name");
            String cardNumber = (String) paymentInfo.get("card_number");
            payment.updateCardInfo(cardName, cardNumber);
        }

        // 가상계좌 정보 저장
        if ("vbank".equals(paymentMethod) && paymentInfo.get("vbank_name") != null) {
            String vbankName = (String) paymentInfo.get("vbank_name");
            String vbankNum = (String) paymentInfo.get("vbank_num");
            String vbankHolder = (String) paymentInfo.get("vbank_holder");
            Long vbankDateTimestamp = ((Number) paymentInfo.get("vbank_date")).longValue();
            LocalDateTime vbankDate = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(vbankDateTimestamp), ZoneId.systemDefault());
            payment.updateVbankInfo(vbankName, vbankNum, vbankDate, vbankHolder);
        }

        Payment savedPayment = paymentRepository.save(payment);

        // Invoice를 납부 완료 처리
        invoice.markAsPaid(paymentMethod);

        log.info("결제 완료: paymentId={}, invoiceId={}, impUid={}, amount={}원",
                savedPayment.getId(), invoice.getId(), request.getImpUid(), amount);

        return toResponse(savedPayment);
    }

    /**
     * 결제 취소/환불
     */
    @Transactional
    public PaymentResponse cancelPayment(PaymentCancelRequest request) {
        Payment payment = paymentRepository.findByImpUid(request.getImpUid())
                .orElseThrow(() -> new ResourceNotFoundException("결제 정보를 찾을 수 없습니다"));

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new BusinessException("완료된 결제만 취소할 수 있습니다");
        }

        // 포트원 API로 결제 취소
        Map<String, Object> cancelInfo = portOneService.cancelPayment(
                request.getImpUid(),
                request.getCancelAmount(),
                request.getReason(),
                request.getRefundHolder(),
                request.getRefundBank(),
                request.getRefundAccount()
        );

        // 전액 취소인지 부분 취소인지 확인
        boolean isFullCancel = request.getCancelAmount() == null ||
                request.getCancelAmount().equals(payment.getAmount());

        if (isFullCancel) {
            payment.markAsCancelled(request.getReason());
        } else {
            payment.markAsRefunded(request.getReason());
        }

        // Invoice도 함께 취소
        if (isFullCancel) {
            payment.getInvoice().cancel(request.getReason());
        }

        log.info("결제 취소: paymentId={}, impUid={}, amount={}, reason={}",
                payment.getId(), request.getImpUid(), request.getCancelAmount(), request.getReason());

        return toResponse(payment);
    }

    /**
     * 결제 정보 조회
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("결제 정보를 찾을 수 없습니다"));
        return toResponse(payment);
    }

    /**
     * impUid로 결제 조회
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByImpUid(String impUid) {
        Payment payment = paymentRepository.findByImpUid(impUid)
                .orElseThrow(() -> new ResourceNotFoundException("결제 정보를 찾을 수 없습니다"));
        return toResponse(payment);
    }

    /**
     * 청구서별 결제 내역 조회
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByInvoice(Long invoiceId) {
        return paymentRepository.findByInvoiceId(invoiceId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 학생별 결제 내역 조회
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByStudent(Long studentId) {
        return paymentRepository.findByStudentId(studentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 전체 결제 내역 조회
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 가맹점 주문번호에서 Invoice Number 추출
     */
    private String extractInvoiceNumber(String merchantUid) {
        // FORMAT: ORDER_INV-20251218-00001_abc123
        String[] parts = merchantUid.split("_");
        if (parts.length >= 2) {
            return parts[1]; // INV-20251218-00001
        }
        throw new BusinessException("유효하지 않은 주문번호 형식입니다: " + merchantUid);
    }

    /**
     * Entity -> DTO 변환
     */
    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .invoiceId(payment.getInvoice().getId())
                .invoiceNumber(payment.getInvoice().getInvoiceNumber())
                .impUid(payment.getImpUid())
                .merchantUid(payment.getMerchantUid())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .pgProvider(payment.getPgProvider())
                .paidAt(payment.getPaidAt())
                .failedReason(payment.getFailedReason())
                .receiptUrl(payment.getReceiptUrl())
                .cardName(payment.getCardName())
                .cardNumber(payment.getCardNumber())
                .buyerName(payment.getBuyerName())
                .buyerTel(payment.getBuyerTel())
                .buyerEmail(payment.getBuyerEmail())
                .vbankName(payment.getVbankName())
                .vbankNum(payment.getVbankNum())
                .vbankDate(payment.getVbankDate())
                .vbankHolder(payment.getVbankHolder())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
