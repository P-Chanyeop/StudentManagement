package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.teacher.TeacherAttendance;
import web.kplay.studentmanagement.domain.user.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherAttendanceRepository extends JpaRepository<TeacherAttendance, Long> {
    @Query("SELECT ta FROM TeacherAttendance ta JOIN FETCH ta.teacher WHERE ta.attendanceDate = :date")
    List<TeacherAttendance> findByAttendanceDate(LocalDate date);
    Optional<TeacherAttendance> findByTeacherAndAttendanceDate(User teacher, LocalDate date);
    List<TeacherAttendance> findByTeacherIdAndAttendanceDateBetween(Long teacherId, LocalDate start, LocalDate end);
    @Query("SELECT ta FROM TeacherAttendance ta JOIN FETCH ta.teacher WHERE ta.attendanceDate BETWEEN :start AND :end")
    List<TeacherAttendance> findByAttendanceDateBetween(LocalDate start, LocalDate end);
}
