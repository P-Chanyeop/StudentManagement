package web.kplay.studentmanagement.service.course;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.course.Course;
import web.kplay.studentmanagement.domain.course.CourseSchedule;
import web.kplay.studentmanagement.domain.user.User;
import web.kplay.studentmanagement.dto.course.CourseScheduleCreateRequest;
import web.kplay.studentmanagement.dto.course.CourseScheduleResponse;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.CourseRepository;
import web.kplay.studentmanagement.repository.CourseScheduleRepository;
import web.kplay.studentmanagement.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseScheduleService {

    private final CourseScheduleRepository scheduleRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

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

    // 선생님 본인 스케줄 조회
    @Transactional(readOnly = true)
    public List<CourseScheduleResponse> getMySchedules(LocalDate date, Authentication authentication) {
        String username = authentication.getName();
        User teacher = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

        List<CourseSchedule> schedules = scheduleRepository.findByScheduleDateAndCourse_Teacher(date, teacher);
        return schedules.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // 관리자 전체 스케줄 조회
    @Transactional(readOnly = true)
    public List<CourseScheduleResponse> getAllSchedules(LocalDate date) {
        List<CourseSchedule> schedules = scheduleRepository.findByScheduleDate(date);
        return schedules.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // 선생님 본인 월별 스케줄 조회
    @Transactional(readOnly = true)
    public List<CourseScheduleResponse> getMyMonthlySchedules(int year, int month, Authentication authentication) {
        String username = authentication.getName();
        User teacher = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<CourseSchedule> schedules = scheduleRepository.findByScheduleDateBetweenAndCourse_Teacher(startDate, endDate, teacher);
        return schedules.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // 관리자 전체 월별 스케줄 조회
    @Transactional(readOnly = true)
    public List<CourseScheduleResponse> getAllMonthlySchedules(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<CourseSchedule> schedules = scheduleRepository.findByScheduleDateBetween(startDate, endDate);
        return schedules.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
