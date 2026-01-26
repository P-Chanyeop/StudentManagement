package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.teacher.TeacherAttendance;
import web.kplay.studentmanagement.domain.user.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherAttendanceRepository extends JpaRepository<TeacherAttendance, Long> {
    List<TeacherAttendance> findByAttendanceDate(LocalDate date);
    Optional<TeacherAttendance> findByTeacherAndAttendanceDate(User teacher, LocalDate date);
    List<TeacherAttendance> findByTeacherIdAndAttendanceDateBetween(Long teacherId, LocalDate start, LocalDate end);
}
