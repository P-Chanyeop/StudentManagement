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

    @Query("SELECT a FROM Attendance a WHERE a.student.id = :studentId AND a.attendanceDate BETWEEN :startDate AND :endDate")
    List<Attendance> findByStudentAndDateRange(@Param("studentId") Long studentId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    List<Attendance> findByStatus(AttendanceStatus status);

    @Query("SELECT a FROM Attendance a LEFT JOIN FETCH a.student LEFT JOIN FETCH a.course WHERE a.attendanceDate = :date")
    List<Attendance> findByDate(@Param("date") LocalDate date);

    // 출석한 순서대로 (등원 시간 오름차순)
    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate = :date ORDER BY a.checkInTime ASC")
    List<Attendance> findByDateOrderByCheckInTime(@Param("date") LocalDate date);

    // 하원 예정 순서대로 (예상 하원 시간 오름차순)
    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate = :date ORDER BY a.expectedLeaveTime ASC")
    List<Attendance> findByDateOrderByExpectedLeaveTime(@Param("date") LocalDate date);

    // 오늘 출석한 학생만 조회 (등원 시간 순서)
    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate = :date AND a.checkInTime IS NOT NULL ORDER BY a.checkInTime ASC")
    List<Attendance> findAttendedByDate(@Param("date") LocalDate date);

    // 오늘 출석하지 않은 학생 조회 (예정되었으나 체크인 안 함)
    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate = :date AND a.checkInTime IS NULL")
    List<Attendance> findNotAttendedByDate(@Param("date") LocalDate date);

    // 하원 완료된 학생만 조회 (하원 시간 순서)
    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate = :date AND a.checkOutTime IS NOT NULL ORDER BY a.checkOutTime ASC")
    List<Attendance> findCheckedOutByDate(@Param("date") LocalDate date);

    // 아직 하원하지 않은 학생 조회 (등원했으나 하원 안 함, 예상 하원 시간 순서)
    @Query("SELECT a FROM Attendance a WHERE a.attendanceDate = :date AND a.checkInTime IS NOT NULL AND a.checkOutTime IS NULL ORDER BY a.expectedLeaveTime ASC")
    List<Attendance> findNotCheckedOutByDate(@Param("date") LocalDate date);

    // 마이페이지용 메서드
    List<Attendance> findTop10ByStudentIdOrderByCheckInTimeDesc(Long studentId);

    Long countByStudentIdAndStatus(Long studentId, AttendanceStatus status);

    Long countByStudentIdAndCheckInTimeBetween(Long studentId, LocalDateTime start, LocalDateTime end);

    /**
     * 전체 시스템의 특정 상태별 출석 수 조회 (관리자용)
     * @param status 출석 상태 (PRESENT, LATE, ABSENT 등)
     * @return 해당 상태의 총 출석 수
     */
    Long countByStatus(AttendanceStatus status);

    /**
     * 특정 날짜의 실제 출석 수 조회 (체크인한 학생만)
     * @param date 조회할 날짜
     * @return 해당 날짜에 체크인한 학생 수
     */
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.attendanceDate = :date AND a.checkInTime IS NOT NULL")
    int countByScheduleDateAndCheckInTimeIsNotNull(@Param("date") LocalDate date);

    /**
     * 특정 날짜의 총 출석 예정 학생 수 조회 (예약된 모든 학생)
     * @param date 조회할 날짜
     * @return 해당 날짜에 예약된 총 학생 수
     */
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.attendanceDate = :date")
    int countByScheduleDate(@Param("date") LocalDate date);

    /**
     * 여러 학생의 특정 날짜 출석 기록 조회 (학부모용)
     * @param studentIds 학생 ID 목록
     * @param date 조회할 날짜
     * @return List<Attendance> 해당 학생들의 출석 기록
     */
    @Query("SELECT a FROM Attendance a JOIN FETCH a.student st LEFT JOIN FETCH a.course c LEFT JOIN FETCH c.teacher t " +
           "WHERE a.student.id IN :studentIds AND a.attendanceDate = :date " +
           "ORDER BY a.attendanceTime ASC")
    List<Attendance> findByStudentIdInAndScheduleScheduleDate(@Param("studentIds") List<Long> studentIds, @Param("date") LocalDate date);

    // 월별 출석 조회
    @Query("SELECT a FROM Attendance a JOIN FETCH a.student st LEFT JOIN FETCH a.course c LEFT JOIN FETCH c.teacher t " +
           "WHERE a.student.id IN :studentIds AND a.attendanceDate BETWEEN :startDate AND :endDate " +
           "ORDER BY a.attendanceDate ASC, a.attendanceTime ASC")
    List<Attendance> findByStudentIdInAndScheduleScheduleDateBetween(@Param("studentIds") List<Long> studentIds, 
                                                                     @Param("startDate") LocalDate startDate, 
                                                                     @Param("endDate") LocalDate endDate);

    // 학생과 날짜로 출석 조회
    @Query("SELECT a FROM Attendance a WHERE a.student = :student AND a.attendanceDate = :date")
    List<Attendance> findByStudentAndDate(@Param("student") web.kplay.studentmanagement.domain.student.Student student, 
                                          @Param("date") LocalDate date);

    // 네이버 예약과 날짜로 출석 조회
    @Query("SELECT a FROM Attendance a WHERE a.naverBooking = :naverBooking AND a.attendanceDate = :date")
    List<Attendance> findByNaverBookingAndDate(@Param("naverBooking") web.kplay.studentmanagement.domain.reservation.NaverBooking naverBooking, 
                                               @Param("date") LocalDate date);
}
