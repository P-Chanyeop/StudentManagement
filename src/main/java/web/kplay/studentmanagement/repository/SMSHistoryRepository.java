package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.sms.SMSHistory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SMSHistoryRepository extends JpaRepository<SMSHistory, Long> {

    /**
     * 기간별 발송 내역 조회
     */
    @Query("SELECT s FROM SMSHistory s WHERE s.createdAt BETWEEN :startDate AND :endDate ORDER BY s.createdAt DESC")
    List<SMSHistory> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 오늘 발송 건수 조회
     */
    @Query("SELECT COUNT(s) FROM SMSHistory s WHERE DATE(s.createdAt) = :date AND s.status = 'SENT'")
    long countSentToday(@Param("date") LocalDate date);

    /**
     * 이번 달 발송 건수 조회
     */
    @Query("SELECT COUNT(s) FROM SMSHistory s WHERE YEAR(s.createdAt) = :year AND MONTH(s.createdAt) = :month AND s.status = 'SENT'")
    long countSentThisMonth(@Param("year") int year, @Param("month") int month);

    /**
     * 최근 발송 내역 조회
     */
    List<SMSHistory> findTop100ByOrderByCreatedAtDesc();

    /**
     * 수신자별 발송 내역 조회
     */
    List<SMSHistory> findByReceiverNumberOrderByCreatedAtDesc(String receiverNumber);

    /**
     * 상태별 발송 내역 조회
     */
    List<SMSHistory> findByStatusOrderByCreatedAtDesc(SMSHistory.SMSStatus status);
}
