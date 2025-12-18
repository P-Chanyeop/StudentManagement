package web.kplay.studentmanagement.domain.payment;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;
import web.kplay.studentmanagement.domain.invoice.Invoice;

import java.time.LocalDateTime;

/**
 * 결제 엔티티
 * 포트원(PortOne) 결제 정보 저장
 */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice; // 연결된 청구서

    @Column(nullable = false, unique = true, length = 100)
    private String impUid; // 포트원 결제 고유번호

    @Column(nullable = false, unique = true, length = 100)
    private String merchantUid; // 가맹점 주문번호

    @Column(nullable = false)
    private Integer amount; // 결제 금액

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status; // 결제 상태

    @Column(length = 50)
    private String paymentMethod; // 결제 수단 (card, vbank, trans, phone 등)

    @Column(length = 50)
    private String pgProvider; // PG사 (nice, kcp, inicis, kakaopay 등)

    @Column
    private LocalDateTime paidAt; // 결제 완료 시각

    @Column(length = 500)
    private String failedReason; // 결제 실패 사유

    @Column(length = 500)
    private String receiptUrl; // 영수증 URL

    @Column(length = 100)
    private String cardName; // 카드사명

    @Column(length = 50)
    private String cardNumber; // 카드번호 (마스킹)

    @Column(length = 100)
    private String buyerName; // 구매자명

    @Column(length = 20)
    private String buyerTel; // 구매자 연락처

    @Column(length = 100)
    private String buyerEmail; // 구매자 이메일

    @Column(length = 50)
    private String vbankName; // 가상계좌 은행명

    @Column(length = 50)
    private String vbankNum; // 가상계좌 번호

    @Column
    private LocalDateTime vbankDate; // 가상계좌 입금 기한

    @Column(length = 100)
    private String vbankHolder; // 가상계좌 예금주

    @Column(length = 1000)
    private String memo; // 관리자 메모

    // 결제 완료 처리
    public void markAsPaid(LocalDateTime paidAt, String receiptUrl) {
        this.status = PaymentStatus.PAID;
        this.paidAt = paidAt;
        this.receiptUrl = receiptUrl;
    }

    // 결제 실패 처리
    public void markAsFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failedReason = reason;
    }

    // 결제 취소 처리
    public void markAsCancelled(String reason) {
        this.status = PaymentStatus.CANCELLED;
        this.memo = (this.memo != null ? this.memo + "\n" : "") + "취소 사유: " + reason;
    }

    // 환불 처리
    public void markAsRefunded(String reason) {
        this.status = PaymentStatus.REFUNDED;
        this.memo = (this.memo != null ? this.memo + "\n" : "") + "환불 사유: " + reason;
    }

    // 카드 정보 업데이트
    public void updateCardInfo(String cardName, String cardNumber) {
        this.cardName = cardName;
        this.cardNumber = cardNumber;
    }

    // 가상계좌 정보 업데이트
    public void updateVbankInfo(String vbankName, String vbankNum, LocalDateTime vbankDate, String vbankHolder) {
        this.vbankName = vbankName;
        this.vbankNum = vbankNum;
        this.vbankDate = vbankDate;
        this.vbankHolder = vbankHolder;
    }

    // 메모 업데이트
    public void updateMemo(String memo) {
        this.memo = memo;
    }
}
