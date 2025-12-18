package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.attendance.Attendance;
import web.kplay.studentmanagement.domain.attendance.AttendanceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByStudentId(Long studentId);

    List<Attendance> findByScheduleId(Long scheduleId);

    @Query("SELECT a FROM Attendance a WHERE a.student.id = :studentId AND a.schedule.scheduleDate BETWEEN :startDate AND :endDate")
    List<Attendance> findByStudentAndDateRange(@Param("studentId") Long studentId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    List<Attendance> findByStatus(AttendanceStatus status);

    @Query("SELECT a FROM Attendance a WHERE a.schedule.scheduleDate = :date")
    List<Attendance> findByDate(@Param("date") LocalDate date);

    // 마이페이지용 메서드
    List<Attendance> findTop10ByStudentIdOrderByCheckInTimeDesc(Long studentId);

    Long countByStudentIdAndStatus(Long studentId, AttendanceStatus status);

    Long countByStudentIdAndCheckInTimeBetween(Long studentId, LocalDateTime start, LocalDateTime end);
}
