package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.course.Course;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByIsActive(Boolean isActive);

    List<Course> findByTeacherId(Long teacherId);

    List<Course> findByLevel(String level);

    /**
     * 수업명으로 수업 조회
     * 
     * @param courseName 조회할 수업명
     * @return Optional<Course> 해당 수업명의 수업 정보 (없으면 empty)
     */
    Optional<Course> findByCourseName(String courseName);
}
