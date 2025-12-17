package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.course.Enrollment;
import web.kplay.studentmanagement.domain.course.EnrollmentType;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findByCourseId(Long courseId);

    List<Enrollment> findByStudentIdAndIsActive(Long studentId, Boolean isActive);

    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.course.id = :courseId AND e.isActive = true")
    List<Enrollment> findActiveEnrollmentByStudentAndCourse(@Param("studentId") Long studentId,
                                                              @Param("courseId") Long courseId);

    @Query("SELECT e FROM Enrollment e WHERE e.enrollmentType = :type AND e.endDate BETWEEN :startDate AND :endDate AND e.isActive = true")
    List<Enrollment> findExpiringEnrollments(@Param("type") EnrollmentType type,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    // 기간권 만료 예정 (4주 이내)
    @Query("SELECT e FROM Enrollment e WHERE e.enrollmentType = 'PERIOD' AND e.endDate BETWEEN :startDate AND :endDate AND e.isActive = true")
    List<Enrollment> findExpiringEnrollments(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    // 만료된 수강권
    @Query("SELECT e FROM Enrollment e WHERE e.enrollmentType = 'PERIOD' AND e.endDate < :today AND e.isActive = false")
    List<Enrollment> findExpiredEnrollments(@Param("today") LocalDate today);

    // 만료된 수강권 (날짜 파라미터 없음)
    @Query("SELECT e FROM Enrollment e WHERE e.enrollmentType = 'PERIOD' AND e.endDate < CURRENT_DATE AND e.isActive = false")
    List<Enrollment> findExpiredEnrollments();

    @Query("SELECT e FROM Enrollment e WHERE e.enrollmentType = 'COUNT' AND e.remainingCount <= :threshold AND e.isActive = true")
    List<Enrollment> findLowCountEnrollments(@Param("threshold") Integer threshold);
}
