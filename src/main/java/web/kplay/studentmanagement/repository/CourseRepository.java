package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.course.Course;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByIsActive(Boolean isActive);

    List<Course> findByTeacherId(Long teacherId);

    List<Course> findByLevel(String level);
}
