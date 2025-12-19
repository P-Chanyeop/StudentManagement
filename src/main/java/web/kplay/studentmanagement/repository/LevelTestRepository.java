package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.leveltest.LevelTest;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LevelTestRepository extends JpaRepository<LevelTest, Long> {

    List<LevelTest> findByStudentId(Long studentId);

    List<LevelTest> findByTestStatus(String testStatus);

    @Query("SELECT lt FROM LevelTest lt WHERE lt.testDate BETWEEN :startDate AND :endDate")
    List<LevelTest> findByDateRange(@Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    // 특정 날짜의 모든 레벨테스트 조회
    @Query("SELECT lt FROM LevelTest lt WHERE lt.testDate = :date")
    List<LevelTest> findByTestDate(@Param("date") LocalDate date);

    @Query("SELECT lt FROM LevelTest lt WHERE lt.testDate = :date AND lt.testStatus = 'SCHEDULED'")
    List<LevelTest> findScheduledTestsByDate(@Param("date") LocalDate date);

    @Query("SELECT lt FROM LevelTest lt WHERE lt.testDate BETWEEN :startDate AND :endDate AND lt.messageNotificationSent = false")
    List<LevelTest> findPendingNotifications(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    // 마이페이지용 메서드
    @Query("SELECT lt FROM LevelTest lt WHERE lt.student.id = :studentId AND lt.testDate > :date ORDER BY lt.testDate ASC")
    List<LevelTest> findByStudentIdAndTestDateAfter(@Param("studentId") Long studentId,
                                                      @Param("date") LocalDate date);

    // 캘린더용 추가 메서드
    // 특정 월의 모든 레벨테스트 조회 (캘린더 뷰)
    @Query("SELECT lt FROM LevelTest lt WHERE YEAR(lt.testDate) = :year AND MONTH(lt.testDate) = :month ORDER BY lt.testDate ASC, lt.testTime ASC")
    List<LevelTest> findByYearAndMonth(@Param("year") int year, @Param("month") int month);

    // 특정 주의 모든 레벨테스트 조회 (주간 뷰)
    @Query("SELECT lt FROM LevelTest lt WHERE lt.testDate BETWEEN :weekStart AND :weekEnd ORDER BY lt.testDate ASC, lt.testTime ASC")
    List<LevelTest> findByWeek(@Param("weekStart") LocalDate weekStart, @Param("weekEnd") LocalDate weekEnd);

    // 선생님별 레벨테스트 조회
    @Query("SELECT lt FROM LevelTest lt WHERE lt.teacher.id = :teacherId ORDER BY lt.testDate ASC, lt.testTime ASC")
    List<LevelTest> findByTeacherId(@Param("teacherId") Long teacherId);

    // 선생님별 특정 기간 레벨테스트 조회
    @Query("SELECT lt FROM LevelTest lt WHERE lt.teacher.id = :teacherId AND lt.testDate BETWEEN :startDate AND :endDate ORDER BY lt.testDate ASC, lt.testTime ASC")
    List<LevelTest> findByTeacherIdAndDateRange(@Param("teacherId") Long teacherId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    // 예정된 레벨테스트만 조회 (오늘 이후)
    @Query("SELECT lt FROM LevelTest lt WHERE lt.testDate >= :date AND lt.testStatus = 'SCHEDULED' ORDER BY lt.testDate ASC, lt.testTime ASC")
    List<LevelTest> findUpcomingTests(@Param("date") LocalDate date);

    // 완료된 레벨테스트만 조회
    @Query("SELECT lt FROM LevelTest lt WHERE lt.testStatus = 'COMPLETED' ORDER BY lt.testDate DESC")
    List<LevelTest> findCompletedTests();

    // 취소된 레벨테스트만 조회
    @Query("SELECT lt FROM LevelTest lt WHERE lt.testStatus = 'CANCELLED' ORDER BY lt.testDate DESC")
    List<LevelTest> findCancelledTests();

    // 특정 날짜 범위 및 상태별 레벨테스트 조회
    @Query("SELECT lt FROM LevelTest lt WHERE lt.testDate BETWEEN :startDate AND :endDate AND lt.testStatus = :status ORDER BY lt.testDate ASC, lt.testTime ASC")
    List<LevelTest> findByDateRangeAndStatus(@Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate,
                                               @Param("status") String status);

    // 레벨테스트 통계 - 총 개수
    @Query("SELECT COUNT(lt) FROM LevelTest lt WHERE lt.testDate BETWEEN :startDate AND :endDate")
    Long countByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // 레벨테스트 통계 - 상태별 개수
    @Query("SELECT COUNT(lt) FROM LevelTest lt WHERE lt.testDate BETWEEN :startDate AND :endDate AND lt.testStatus = :status")
    Long countByDateRangeAndStatus(@Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate,
                                     @Param("status") String status);
}
