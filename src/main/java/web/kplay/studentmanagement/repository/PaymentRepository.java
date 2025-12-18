package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.payment.Payment;
import web.kplay.studentmanagement.domain.payment.PaymentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 포트원 결제 고유번호로 조회
    Optional<Payment> findByImpUid(String impUid);

    // 가맹점 주문번호로 조회
    Optional<Payment> findByMerchantUid(String merchantUid);

    // 청구서별 결제 내역 조회
    List<Payment> findByInvoiceId(Long invoiceId);

    // 청구서별 결제 상태로 조회
    List<Payment> findByInvoiceIdAndStatus(Long invoiceId, PaymentStatus status);

    // 특정 기간 결제 내역 조회
    @Query("SELECT p FROM Payment p WHERE p.paidAt BETWEEN :startDate AND :endDate ORDER BY p.paidAt DESC")
    List<Payment> findByPaidAtBetween(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    // 특정 결제 상태 조회
    List<Payment> findByStatus(PaymentStatus status);

    // 학생별 결제 내역 조회
    @Query("SELECT p FROM Payment p WHERE p.invoice.student.id = :studentId ORDER BY p.paidAt DESC")
    List<Payment> findByStudentId(@Param("studentId") Long studentId);

    // 결제 완료된 총 금액 조회
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'PAID' AND p.paidAt BETWEEN :startDate AND :endDate")
    Long sumPaidAmountByDateRange(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    // PG사별 결제 통계
    @Query("SELECT p.pgProvider, COUNT(p) FROM Payment p WHERE p.status = 'PAID' GROUP BY p.pgProvider")
    List<Object[]> countByPgProvider();

    // 결제 수단별 결제 통계
    @Query("SELECT p.paymentMethod, COUNT(p) FROM Payment p WHERE p.status = 'PAID' GROUP BY p.paymentMethod")
    List<Object[]> countByPaymentMethod();
}
