package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.reservation.Reservation;
import web.kplay.studentmanagement.domain.reservation.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByStudentId(Long studentId);

    List<Reservation> findByScheduleId(Long scheduleId);

    List<Reservation> findByStatus(ReservationStatus status);

    @Query("SELECT r FROM Reservation r WHERE r.student.id = :studentId AND r.status = :status")
    List<Reservation> findByStudentIdAndStatus(@Param("studentId") Long studentId,
                                                 @Param("status") ReservationStatus status);

    @Query("SELECT r FROM Reservation r WHERE r.schedule.scheduleDate = :date AND r.status IN :statuses")
    List<Reservation> findByDateAndStatuses(@Param("date") LocalDate date,
                                             @Param("statuses") List<ReservationStatus> statuses);

    @Query("SELECT r FROM Reservation r WHERE r.schedule.scheduleDate BETWEEN :startDate AND :endDate")
    List<Reservation> findByDateRange(@Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    // 마이페이지용 메서드
    @Query("SELECT r FROM Reservation r WHERE r.student.id = :studentId AND r.schedule.scheduleDate > :date")
    List<Reservation> findByStudentIdAndScheduleDateAfter(@Param("studentId") Long studentId,
                                                            @Param("date") LocalDate date);

    // 자동 차감용 메서드
    @Query("SELECT r FROM Reservation r " +
           "WHERE r.schedule.scheduleDate = :date " +
           "AND r.status = :status " +
           "AND r.status != 'AUTO_DEDUCTED'")
    List<Reservation> findByScheduleDateAndStatusAndNotDeducted(@Param("date") LocalDate date,
                                                                @Param("status") ReservationStatus status);

    /**
     * 특정 날짜 이후의 모든 예약 조회 (관리자용)
     * @param date 기준 날짜
     * @return 기준 날짜 이후의 모든 예약 목록
     */
    @Query("SELECT r FROM Reservation r WHERE r.schedule.scheduleDate > :date")
    List<Reservation> findByScheduleDateAfter(@Param("date") LocalDate date);

    /**
     * 특정 날짜의 예약 목록 조회 (스케줄 정보 포함)
     * @param date 조회할 날짜
     * @return List<Reservation> 해당 날짜의 예약 목록 (스케줄 정보 포함)
     */
    @Query("SELECT r FROM Reservation r JOIN FETCH r.schedule WHERE r.schedule.scheduleDate = :date")
    List<Reservation> findByScheduleScheduleDate(@Param("date") LocalDate date);

    /**
     * 특정 날짜와 상담 유형의 예약 목록 조회
     * @param date 조회할 날짜
     * @param consultationType 상담 유형
     * @return List<Reservation> 해당 날짜와 유형의 예약 목록
     */
    @Query("SELECT r FROM Reservation r JOIN FETCH r.schedule WHERE r.schedule.scheduleDate = :date AND r.consultationType = :consultationType")
    List<Reservation> findByScheduleDateAndConsultationType(@Param("date") LocalDate date, @Param("consultationType") String consultationType);

    /**
     * 여러 학생의 예약 목록 조회 (학부모용)
     * @param studentIds 학생 ID 목록
     * @return List<Reservation> 해당 학생들의 예약 목록 (최신순)
     */
    @Query("SELECT r FROM Reservation r JOIN FETCH r.schedule s JOIN FETCH r.student st " +
           "WHERE r.student.id IN :studentIds " +
           "ORDER BY s.scheduleDate DESC, s.startTime DESC")
    List<Reservation> findByStudentIdInOrderByScheduleScheduleDateDescScheduleStartTimeDesc(@Param("studentIds") List<Long> studentIds);

    /**
     * 특정 시간 이후 생성된 예약 조회 (관리자 알림용)
     * @param since 기준 시간
     * @return List<Reservation> 해당 시간 이후 생성된 예약 목록 (최신순)
     */
    @Query("SELECT r FROM Reservation r JOIN FETCH r.schedule s JOIN FETCH r.student st " +
           "WHERE r.createdAt > :since " +
           "ORDER BY r.createdAt DESC")
    List<Reservation> findByCreatedAtAfterOrderByCreatedAtDesc(@Param("since") LocalDateTime since);
}
