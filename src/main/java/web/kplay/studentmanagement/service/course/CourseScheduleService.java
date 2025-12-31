package web.kplay.studentmanagement.service.course;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.course.Course;
import web.kplay.studentmanagement.domain.course.CourseSchedule;
import web.kplay.studentmanagement.dto.course.CourseScheduleCreateRequest;
import web.kplay.studentmanagement.dto.course.CourseScheduleResponse;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.CourseRepository;
import web.kplay.studentmanagement.repository.CourseScheduleRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseScheduleService {

    private final CourseScheduleRepository scheduleRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public CourseScheduleResponse createSchedule(CourseScheduleCreateRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("수업을 찾을 수 없습니다"));

        CourseSchedule schedule = CourseSchedule.builder()
                .course(course)
                .scheduleDate(request.getScheduleDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .dayOfWeek(request.getDayOfWeek())
                .currentStudents(0)
                .isCancelled(false)
                .memo(request.getMemo())
                .build();

        CourseSchedule savedSchedule = scheduleRepository.save(schedule);
        log.info("Course schedule created: course={}, date={}",
                course.getCourseName(), request.getScheduleDate());

        return toResponse(savedSchedule);
    }

    @Transactional(readOnly = true)
    public CourseScheduleResponse getSchedule(Long id) {
        CourseSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("스케줄을 찾을 수 없습니다"));
        return toResponse(schedule);
    }

    @Transactional(readOnly = true)
    public List<CourseScheduleResponse> getSchedulesByDate(LocalDate date) {
        return scheduleRepository.findActiveSchedulesByDate(date).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourseScheduleResponse> getAvailableSchedulesByDate(LocalDate date) {
        return scheduleRepository.findActiveSchedulesByDate(date).stream()
                .filter(schedule -> schedule.getCurrentStudents() < schedule.getCourse().getMaxStudents())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourseScheduleResponse> getSchedulesByDateRange(LocalDate startDate, LocalDate endDate) {
        return scheduleRepository.findByScheduleDateBetween(startDate, endDate).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourseScheduleResponse> getSchedulesByCourse(Long courseId) {
        return scheduleRepository.findByCourseId(courseId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CourseScheduleResponse updateSchedule(Long id, CourseScheduleCreateRequest request) {
        CourseSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("스케줄을 찾을 수 없습니다"));

        schedule.updateTime(request.getStartTime(), request.getEndTime());
        schedule.updateMemo(request.getMemo());

        log.info("Course schedule updated: ID={}", id);
        return toResponse(schedule);
    }

    @Transactional
    public void cancelSchedule(Long id, String reason) {
        CourseSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("스케줄을 찾을 수 없습니다"));

        schedule.cancel(reason);
        log.info("Course schedule cancelled: ID={}, reason={}", id, reason);
    }

    @Transactional
    public void restoreSchedule(Long id) {
        CourseSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("스케줄을 찾을 수 없습니다"));

        schedule.restore();
        log.info("Course schedule restored: ID={}", id);
    }

    private CourseScheduleResponse toResponse(CourseSchedule schedule) {
        return CourseScheduleResponse.builder()
                .id(schedule.getId())
                .courseId(schedule.getCourse().getId())
                .courseName(schedule.getCourse().getCourseName())
                .courseLevel(schedule.getCourse().getLevel())
                .teacherName(schedule.getCourse().getTeacher() != null ? schedule.getCourse().getTeacher().getName() : "미배정")
                .scheduleDate(schedule.getScheduleDate())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .dayOfWeek(schedule.getDayOfWeek())
                .currentStudents(schedule.getCurrentStudents())
                .maxStudents(schedule.getCourse().getMaxStudents())
                .currentCount(schedule.getCurrentStudents())
                .maxCapacity(schedule.getCourse().getMaxStudents())
                .isCancelled(schedule.getIsCancelled())
                .cancelReason(schedule.getCancelReason())
                .memo(schedule.getMemo())
                .build();
    }
}
