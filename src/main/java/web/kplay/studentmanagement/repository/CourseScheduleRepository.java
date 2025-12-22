package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.course.CourseSchedule;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CourseScheduleRepository extends JpaRepository<CourseSchedule, Long> {

    List<CourseSchedule> findByCourseId(Long courseId);

    List<CourseSchedule> findByScheduleDate(LocalDate scheduleDate);

    List<CourseSchedule> findByScheduleDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT cs FROM CourseSchedule cs WHERE cs.scheduleDate = :date AND cs.isCancelled = false")
    List<CourseSchedule> findActiveSchedulesByDate(@Param("date") LocalDate date);

    @Query("SELECT cs FROM CourseSchedule cs WHERE cs.course.id = :courseId AND cs.scheduleDate BETWEEN :startDate AND :endDate")
    List<CourseSchedule> findByCourseIdAndDateRange(@Param("courseId") Long courseId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);
}
