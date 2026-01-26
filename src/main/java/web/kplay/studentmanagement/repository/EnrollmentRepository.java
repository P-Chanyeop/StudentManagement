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
    
    List<Enrollment> findByStudentAndIsActiveTrue(web.kplay.studentmanagement.domain.student.Student student);

    Integer countByStudentIdAndIsActiveTrue(Long studentId);
    
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id = :courseId AND e.isActive = true")
    Integer countActiveByCourseId(@Param("courseId") Long courseId);

    /**
     * 전체 시스템의 활성 수강권 수 조회 (관리자용)
     * @return 전체 활성 수강권 수
     */
    Integer countByIsActiveTrue();

    /**
     * 만료 임박 수강권 수 조회
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param isActive 활성 상태
     * @return 해당 기간 내 만료 예정인 활성 수강권 수
     */
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.endDate BETWEEN :startDate AND :endDate AND e.isActive = :isActive")
    int countByEndDateBetweenAndIsActive(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("isActive") Boolean isActive);

    /**
     * 특정 선생님 담당 수업의 활성 학생 수 조회
     * @param teacherId 선생님 ID
     * @return 해당 선생님 수업에 등록된 활성 학생 수
     */
    @Query("SELECT COUNT(DISTINCT e.student.id) FROM Enrollment e WHERE e.course.teacher.id = :teacherId AND e.isActive = true")
    int countActiveStudentsByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * 특정 선생님 담당 수업의 만료 임박 수강권 수 조회
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param isActive 활성 상태
     * @param teacherId 선생님 ID
     * @return 해당 선생님 수업의 만료 임박 수강권 수
     */
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.endDate BETWEEN :startDate AND :endDate AND e.isActive = :isActive AND e.course.teacher.id = :teacherId")
    int countByEndDateBetweenAndIsActiveAndCourseTeacherId(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("isActive") Boolean isActive, @Param("teacherId") Long teacherId);

    // 오늘 기간 완료되는 활성 수강권
    List<Enrollment> findByEndDateAndIsActiveTrue(LocalDate endDate);
}
