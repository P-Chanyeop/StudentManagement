package web.kplay.studentmanagement.domain.payment;

/**
 * 결제 상태
 */
public enum PaymentStatus {
    PENDING("결제 대기"),
    READY("결제 준비"),
    PAID("결제 완료"),
    FAILED("결제 실패"),
    CANCELLED("결제 취소"),
    REFUNDED("환불 완료"),
    PARTIAL_REFUNDED("부분 환불");

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
