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

    @Query("SELECT e FROM Enrollment e WHERE e.enrollmentType = 'COUNT' AND e.remainingCount <= :threshold AND e.isActive = true")
    List<Enrollment> findLowCountEnrollments(@Param("threshold") Integer threshold);
}
