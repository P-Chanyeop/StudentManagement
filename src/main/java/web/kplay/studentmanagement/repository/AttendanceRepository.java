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

    // 출석한 순서대로 (등원 시간 오름차순)
    @Query("SELECT a FROM Attendance a WHERE a.schedule.scheduleDate = :date ORDER BY a.checkInTime ASC")
    List<Attendance> findByDateOrderByCheckInTime(@Param("date") LocalDate date);

    // 하원 예정 순서대로 (예상 하원 시간 오름차순)
    @Query("SELECT a FROM Attendance a WHERE a.schedule.scheduleDate = :date ORDER BY a.expectedLeaveTime ASC")
    List<Attendance> findByDateOrderByExpectedLeaveTime(@Param("date") LocalDate date);

    // 오늘 출석한 학생만 조회 (등원 시간 순서)
    @Query("SELECT a FROM Attendance a WHERE a.schedule.scheduleDate = :date AND a.checkInTime IS NOT NULL ORDER BY a.checkInTime ASC")
    List<Attendance> findAttendedByDate(@Param("date") LocalDate date);

    // 오늘 출석하지 않은 학생 조회 (예정되었으나 체크인 안 함)
    @Query("SELECT a FROM Attendance a WHERE a.schedule.scheduleDate = :date AND a.checkInTime IS NULL")
    List<Attendance> findNotAttendedByDate(@Param("date") LocalDate date);

    // 하원 완료된 학생만 조회 (하원 시간 순서)
    @Query("SELECT a FROM Attendance a WHERE a.schedule.scheduleDate = :date AND a.checkOutTime IS NOT NULL ORDER BY a.checkOutTime ASC")
    List<Attendance> findCheckedOutByDate(@Param("date") LocalDate date);

    // 아직 하원하지 않은 학생 조회 (등원했으나 하원 안 함, 예상 하원 시간 순서)
    @Query("SELECT a FROM Attendance a WHERE a.schedule.scheduleDate = :date AND a.checkInTime IS NOT NULL AND a.checkOutTime IS NULL ORDER BY a.expectedLeaveTime ASC")
    List<Attendance> findNotCheckedOutByDate(@Param("date") LocalDate date);

    // 마이페이지용 메서드
    List<Attendance> findTop10ByStudentIdOrderByCheckInTimeDesc(Long studentId);

    Long countByStudentIdAndStatus(Long studentId, AttendanceStatus status);

    Long countByStudentIdAndCheckInTimeBetween(Long studentId, LocalDateTime start, LocalDateTime end);
}
