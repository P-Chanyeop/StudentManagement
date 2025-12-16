package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.reservation.Reservation;
import web.kplay.studentmanagement.domain.reservation.ReservationStatus;

import java.time.LocalDate;
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
}
