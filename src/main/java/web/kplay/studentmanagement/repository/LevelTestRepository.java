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
}
