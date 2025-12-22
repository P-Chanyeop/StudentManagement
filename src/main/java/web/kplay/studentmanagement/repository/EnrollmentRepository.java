package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.course.Enrollment;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findByCourseId(Long courseId);

    List<Enrollment> findByCourseAndIsActiveTrue(web.kplay.studentmanagement.domain.course.Course course);

    List<Enrollment> findByStudentIdAndIsActive(Long studentId, Boolean isActive);

    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.course.id = :courseId AND e.isActive = true")
    List<Enrollment> findActiveEnrollmentByStudentAndCourse(@Param("studentId") Long studentId,
                                                              @Param("courseId") Long courseId);

    // 만료 예정 수강권 (모든 수강권은 기간을 가짐)
    @Query("SELECT e FROM Enrollment e WHERE e.endDate BETWEEN :startDate AND :endDate AND e.isActive = true")
    List<Enrollment> findExpiringEnrollments(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    // 만료된 수강권
    @Query("SELECT e FROM Enrollment e WHERE e.endDate < :today AND e.isActive = false")
    List<Enrollment> findExpiredEnrollments(@Param("today") LocalDate today);

    // 만료된 수강권 (날짜 파라미터 없음)
    @Query("SELECT e FROM Enrollment e WHERE e.endDate < CURRENT_DATE AND e.isActive = false")
    List<Enrollment> findExpiredEnrollments();

    // 횟수 부족 수강권 (모든 수강권은 횟수를 가짐)
    @Query("SELECT e FROM Enrollment e WHERE e.remainingCount <= :threshold AND e.isActive = true")
    List<Enrollment> findLowCountEnrollments(@Param("threshold") Integer threshold);

    // 마이페이지용 메서드
    List<Enrollment> findByStudentIdAndIsActiveTrue(Long studentId);

    Integer countByStudentIdAndIsActiveTrue(Long studentId);
}
