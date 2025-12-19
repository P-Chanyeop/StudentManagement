package web.kplay.studentmanagement.domain.invoice;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.domain.user.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 청구서 엔티티
 * 수강권 갱신, 추가 결제 등의 청구 내역 관리
 */
@Entity
@Table(name = "invoices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Invoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by")
    private User issuedBy; // 발급자 (관리자/선생님)

    @Column(nullable = false, length = 100)
    private String title; // 청구서 제목

    @Column(nullable = false)
    private Integer amount; // 청구 금액

    @Column(nullable = false)
    private LocalDate issueDate; // 발급일

    @Column(nullable = false)
    private LocalDate dueDate; // 납부 기한

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status; // 청구 상태

    @Column(length = 1000)
    private String description; // 청구 상세 설명

    @Column(length = 50)
    private String invoiceNumber; // 청구서 번호

    @Column
    private LocalDateTime paidAt; // 납부 완료 일시

    @Column(length = 100)
    private String paymentMethod; // 결제 수단 (카드, 현금, 계좌이체 등)

    @Column(length = 500)
    private String memo; // 관리자 메모

    // 청구서 번호 생성
    public void generateInvoiceNumber() {
        this.invoiceNumber = String.format("INV-%s-%05d",
                LocalDate.now().toString().replace("-", ""),
                this.id);
    }

    // 납부 완료 처리
    public void markAsPaid(String paymentMethod) {
        this.status = InvoiceStatus.PAID;
        this.paidAt = LocalDateTime.now();
        this.paymentMethod = paymentMethod;
    }

    // 납부 취소 (환불 등)
    public void cancel(String reason) {
        this.status = InvoiceStatus.CANCELLED;
        this.memo = (this.memo != null ? this.memo + "\n" : "") + "취소 사유: " + reason;
    }

    // 연체 처리
    public void markAsOverdue() {
        if (this.status == InvoiceStatus.PENDING && LocalDate.now().isAfter(this.dueDate)) {
            this.status = InvoiceStatus.OVERDUE;
        }
    }

    // 청구서 수정
    public void updateInvoice(String title, Integer amount, LocalDate dueDate, String description) {
        if (this.status != InvoiceStatus.PENDING) {
            throw new IllegalStateException("대기 상태의 청구서만 수정할 수 있습니다.");
        }
        this.title = title;
        this.amount = amount;
        this.dueDate = dueDate;
        this.description = description;
    }

    // 메모 업데이트
    public void updateMemo(String memo) {
        this.memo = memo;
    }
}
