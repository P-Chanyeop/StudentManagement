package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.makeup.MakeupClass;
import web.kplay.studentmanagement.domain.makeup.MakeupStatus;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MakeupClassRepository extends JpaRepository<MakeupClass, Long> {

    // 학생별 보강 수업 조회
    List<MakeupClass> findByStudentId(Long studentId);

    // 학생별 + 상태별 보강 수업 조회
    List<MakeupClass> findByStudentIdAndStatus(Long studentId, MakeupStatus status);

    // 수업별 보강 수업 조회
    List<MakeupClass> findByCourseId(Long courseId);

    // 특정 기간 내 보강 수업 조회
    @Query("SELECT m FROM MakeupClass m WHERE m.makeupDate BETWEEN :startDate AND :endDate ORDER BY m.makeupDate, m.makeupTime")
    List<MakeupClass> findByMakeupDateBetween(@Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    // 특정 날짜의 보강 수업 조회
    List<MakeupClass> findByMakeupDateOrderByMakeupTime(LocalDate makeupDate);

    // 상태별 보강 수업 조회
    List<MakeupClass> findByStatusOrderByMakeupDateDesc(MakeupStatus status);

    // 학생의 예정된 보강 수업 조회
    @Query("SELECT m FROM MakeupClass m WHERE m.student.id = :studentId AND m.status = 'SCHEDULED' AND m.makeupDate >= :today ORDER BY m.makeupDate, m.makeupTime")
    List<MakeupClass> findUpcomingMakeupsByStudent(@Param("studentId") Long studentId,
                                                     @Param("today") LocalDate today);

    // 전체 예정된 보강 수업 조회
    @Query("SELECT m FROM MakeupClass m WHERE m.status = 'SCHEDULED' AND m.makeupDate >= :today ORDER BY m.makeupDate, m.makeupTime")
    List<MakeupClass> findAllUpcomingMakeups(@Param("today") LocalDate today);

    // 학생의 보강 수업 개수 조회
    Long countByStudentIdAndStatus(Long studentId, MakeupStatus status);

    // 상태별 보강 수업 개수 조회
    Long countByStatus(MakeupStatus status);
}
