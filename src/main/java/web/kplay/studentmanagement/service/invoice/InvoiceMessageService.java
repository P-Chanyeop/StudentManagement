package web.kplay.studentmanagement.service.invoice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.invoice.Invoice;
import web.kplay.studentmanagement.domain.message.Message;
import web.kplay.studentmanagement.domain.message.MessageType;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.InvoiceRepository;
import web.kplay.studentmanagement.repository.MessageRepository;
import web.kplay.studentmanagement.service.message.sms.SmsService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 청구서 문자 발송 서비스
 * 청구서 발급, 납부 안내, 연체 알림 등의 문자를 발송합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceMessageService {

    private final InvoiceRepository invoiceRepository;
    private final MessageRepository messageRepository;
    private final SmsService smsService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일");

    /**
     * 청구서 발급 알림 문자 발송
     */
    @Transactional
    public void sendInvoiceIssuedNotification(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("청구서를 찾을 수 없습니다"));

        String content = buildInvoiceIssuedMessage(invoice);

        sendMessage(invoice, content, MessageType.INVOICE_ISSUED);

        log.info("청구서 발급 알림 발송: invoiceId={}, 학생={}",
                invoiceId, invoice.getStudent().getStudentName());
    }

    /**
     * 납부 안내 문자 발송
     */
    @Transactional
    public void sendPaymentReminderNotification(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("청구서를 찾을 수 없습니다"));

        String content = buildPaymentReminderMessage(invoice);

        sendMessage(invoice, content, MessageType.PAYMENT_REMINDER);

        log.info("납부 안내 문자 발송: invoiceId={}, 학생={}",
                invoiceId, invoice.getStudent().getStudentName());
    }

    /**
     * 연체 알림 문자 발송
     */
    @Transactional
    public void sendOverdueNotification(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("청구서를 찾을 수 없습니다"));

        String content = buildOverdueMessage(invoice);

        sendMessage(invoice, content, MessageType.PAYMENT_OVERDUE);

        log.info("연체 알림 문자 발송: invoiceId={}, 학생={}",
                invoiceId, invoice.getStudent().getStudentName());
    }

    /**
     * 납부 완료 확인 문자 발송
     */
    @Transactional
    public void sendPaymentConfirmation(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("청구서를 찾을 수 없습니다"));

        String content = buildPaymentConfirmationMessage(invoice);

        sendMessage(invoice, content, MessageType.PAYMENT_CONFIRMED);

        log.info("납부 완료 확인 문자 발송: invoiceId={}, 학생={}",
                invoiceId, invoice.getStudent().getStudentName());
    }

    /**
     * 청구서 발급 메시지 템플릿
     */
    private String buildInvoiceIssuedMessage(Invoice invoice) {
        return String.format(
                "[K-PLAY 학원] 청구서 발급 안내\n\n" +
                        "학생: %s\n" +
                        "청구서번호: %s\n" +
                        "항목: %s\n" +
                        "금액: %s원\n" +
                        "발급일: %s\n" +
                        "납부기한: %s\n\n" +
                        "기한 내 납부 부탁드립니다.\n" +
                        "문의: K-PLAY 학원",
                invoice.getStudent().getStudentName(),
                invoice.getInvoiceNumber(),
                invoice.getTitle(),
                formatAmount(invoice.getAmount()),
                invoice.getIssueDate().format(DATE_FORMATTER),
                invoice.getDueDate().format(DATE_FORMATTER)
        );
    }

    /**
     * 납부 안내 메시지 템플릿
     */
    private String buildPaymentReminderMessage(Invoice invoice) {
        long daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDate.now(),
                invoice.getDueDate()
        );

        return String.format(
                "[K-PLAY 학원] 납부 안내\n\n" +
                        "학생: %s\n" +
                        "청구서번호: %s\n" +
                        "항목: %s\n" +
                        "금액: %s원\n" +
                        "납부기한: %s (%d일 남음)\n\n" +
                        "기한 내 납부 부탁드립니다.",
                invoice.getStudent().getStudentName(),
                invoice.getInvoiceNumber(),
                invoice.getTitle(),
                formatAmount(invoice.getAmount()),
                invoice.getDueDate().format(DATE_FORMATTER),
                daysUntilDue
        );
    }

    /**
     * 연체 알림 메시지 템플릿
     */
    private String buildOverdueMessage(Invoice invoice) {
        long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(
                invoice.getDueDate(),
                java.time.LocalDate.now()
        );

        return String.format(
                "[K-PLAY 학원] 납부 기한 경과 안내\n\n" +
                        "학생: %s\n" +
                        "청구서번호: %s\n" +
                        "항목: %s\n" +
                        "금액: %s원\n" +
                        "납부기한: %s (경과 %d일)\n\n" +
                        "빠른 시일 내 납부 부탁드립니다.\n" +
                        "문의: K-PLAY 학원",
                invoice.getStudent().getStudentName(),
                invoice.getInvoiceNumber(),
                invoice.getTitle(),
                formatAmount(invoice.getAmount()),
                invoice.getDueDate().format(DATE_FORMATTER),
                daysOverdue
        );
    }

    /**
     * 납부 완료 확인 메시지 템플릿
     */
    private String buildPaymentConfirmationMessage(Invoice invoice) {
        return String.format(
                "[K-PLAY 학원] 납부 완료 확인\n\n" +
                        "학생: %s\n" +
                        "청구서번호: %s\n" +
                        "항목: %s\n" +
                        "금액: %s원\n" +
                        "납부일시: %s\n" +
                        "결제수단: %s\n\n" +
                        "납부해 주셔서 감사합니다.",
                invoice.getStudent().getStudentName(),
                invoice.getInvoiceNumber(),
                invoice.getTitle(),
                formatAmount(invoice.getAmount()),
                invoice.getPaidAt().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분")),
                invoice.getPaymentMethod()
        );
    }

    /**
     * 실제 문자 발송
     */
    private void sendMessage(Invoice invoice, String content, MessageType messageType) {
        Message message = Message.builder()
                .student(invoice.getStudent())
                .recipientPhone(invoice.getStudent().getParentPhone())
                .recipientName(invoice.getStudent().getParentName())
                .messageType(messageType)
                .content(content)
                .sendStatus("PENDING")
                .build();

        Message savedMessage = messageRepository.save(message);

        try {
            String externalMessageId = smsService.sendSms(
                    invoice.getStudent().getParentPhone(),
                    content
            );

            savedMessage.markAsSent(LocalDateTime.now(), externalMessageId);
        } catch (Exception e) {
            log.error("청구서 문자 발송 실패: {}", e.getMessage(), e);
            savedMessage.markAsFailed("SMS 발송 실패: " + e.getMessage());
        }
    }

    /**
     * 금액 포맷팅 (천 단위 콤마)
     */
    private String formatAmount(Integer amount) {
        return String.format("%,d", amount);
    }
}
