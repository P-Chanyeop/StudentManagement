package web.kplay.studentmanagement.service.course;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.course.Course;
import web.kplay.studentmanagement.domain.user.User;
import web.kplay.studentmanagement.dto.course.CourseCreateRequest;
import web.kplay.studentmanagement.dto.course.CourseResponse;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.CourseRepository;
import web.kplay.studentmanagement.repository.EnrollmentRepository;
import web.kplay.studentmanagement.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public CourseResponse createCourse(CourseCreateRequest request) {
        User teacher = null;
        if (request.getTeacherId() != null) {
            teacher = userRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("선생님을 찾을 수 없습니다"));
        }

        Course course = Course.builder()
                .courseName(request.getCourseName())
                .description(request.getDescription())
                .teacher(teacher)
                .maxStudents(request.getMaxStudents())
                .durationMinutes(request.getDurationMinutes())
                .level(request.getLevel())
                .color(request.getColor())
                .isActive(true)
                .build();

        Course savedCourse = courseRepository.save(course);
        log.info("New course registered: {}", savedCourse.getCourseName());

        return toResponse(savedCourse);
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("수업을 찾을 수 없습니다"));
        return toResponse(course);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getActiveCourses() {
        return courseRepository.findByIsActive(true).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getCoursesByTeacher(Long teacherId) {
        return courseRepository.findByTeacherId(teacherId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CourseResponse updateCourse(Long id, CourseCreateRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("수업을 찾을 수 없습니다"));

        User teacher = null;
        if (request.getTeacherId() != null) {
            teacher = userRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException("선생님을 찾을 수 없습니다"));
        }

        course.updateInfo(
                request.getCourseName(),
                request.getDescription(),
                request.getMaxStudents(),
                request.getDurationMinutes(),
                request.getLevel(),
                request.getColor()
        );

        if (teacher != null) {
            course.assignTeacher(teacher);
        }

        log.info("Course info updated: {}", course.getCourseName());
        return toResponse(course);
    }

    @Transactional
    public void deactivateCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("수업을 찾을 수 없습니다"));
        course.deactivate();
        log.info("Course deactivated: {}", course.getCourseName());
    }

    private CourseResponse toResponse(Course course) {
        // 현재 활성 수강권 수 계산
        Integer currentEnrollments = enrollmentRepository.countActiveByCourseId(course.getId());
        
        return CourseResponse.builder()
                .id(course.getId())
                .courseName(course.getCourseName())
                .description(course.getDescription())
                .teacherId(course.getTeacher() != null ? course.getTeacher().getId() : null)
                .teacherName(course.getTeacher() != null ? course.getTeacher().getName() : null)
                .maxStudents(course.getMaxStudents())
                .durationMinutes(course.getDurationMinutes())
                .level(course.getLevel())
                .color(course.getColor())
                .isActive(course.getIsActive())
                .currentEnrollments(currentEnrollments)
                .build();
    }
}
