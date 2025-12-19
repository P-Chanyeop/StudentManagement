package web.kplay.studentmanagement.domain.invoice;

/**
 * 청구서 상태
 */
public enum InvoiceStatus {
    PENDING,    // 대기 (발급됨, 미납)
    PAID,       // 납부 완료
    OVERDUE,    // 연체
    CANCELLED   // 취소됨
}
