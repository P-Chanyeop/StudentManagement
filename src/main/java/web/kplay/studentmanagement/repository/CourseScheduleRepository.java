package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.course.CourseSchedule;
import web.kplay.studentmanagement.domain.user.User;

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

    /**
     * 특정 날짜의 전체 스케줄 수 조회
     * @param date 조회할 날짜
     * @return 해당 날짜의 스케줄 수
     */
    int countByScheduleDate(LocalDate date);

    /**
     * 특정 날짜와 선생님의 스케줄 수 조회
     * @param date 조회할 날짜
     * @param teacherId 선생님 ID
     * @return 해당 날짜에 해당 선생님의 스케줄 수
     */
    @Query("SELECT COUNT(cs) FROM CourseSchedule cs WHERE cs.scheduleDate = :date AND cs.course.teacher.id = :teacherId")
    int countByScheduleDateAndCourseTeacherId(@Param("date") LocalDate date, @Param("teacherId") Long teacherId);

    // 선생님별 스케줄 조회
    List<CourseSchedule> findByScheduleDateAndCourse_Teacher(LocalDate date, User teacher);
    
    List<CourseSchedule> findByScheduleDateBetweenAndCourse_Teacher(LocalDate startDate, LocalDate endDate, User teacher);
}
