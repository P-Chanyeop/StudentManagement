package web.kplay.studentmanagement.repository.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import web.kplay.studentmanagement.domain.enrollment.EnrollmentAdjustment;

import java.util.List;

public interface EnrollmentAdjustmentRepository extends JpaRepository<EnrollmentAdjustment, Long> {
    
    @Query("SELECT ea FROM EnrollmentAdjustment ea " +
           "WHERE ea.enrollment.id = :enrollmentId " +
           "ORDER BY ea.createdAt DESC")
    List<EnrollmentAdjustment> findByEnrollmentIdOrderByCreatedAtDesc(@Param("enrollmentId") Long enrollmentId);
    
    @Query("SELECT ea FROM EnrollmentAdjustment ea " +
           "JOIN FETCH ea.enrollment e " +
           "JOIN FETCH ea.admin a " +
           "WHERE e.student.id = :studentId " +
           "ORDER BY ea.createdAt DESC")
    List<EnrollmentAdjustment> findByStudentIdOrderByCreatedAtDesc(@Param("studentId") Long studentId);
}
