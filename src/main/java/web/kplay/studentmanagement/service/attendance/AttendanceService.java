package web.kplay.studentmanagement.service.attendance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.attendance.Attendance;
import web.kplay.studentmanagement.domain.attendance.AttendanceStatus;
import web.kplay.studentmanagement.domain.course.CourseSchedule;
import web.kplay.studentmanagement.domain.course.Enrollment;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.dto.attendance.AttendanceCheckInRequest;
import web.kplay.studentmanagement.dto.attendance.AttendanceResponse;
import web.kplay.studentmanagement.exception.BusinessException;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.AttendanceRepository;
import web.kplay.studentmanagement.repository.CourseScheduleRepository;
import web.kplay.studentmanagement.repository.EnrollmentRepository;
import web.kplay.studentmanagement.repository.StudentRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final CourseScheduleRepository scheduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final web.kplay.studentmanagement.service.message.AutomatedMessageService automatedMessageService;

    @Transactional
    public AttendanceResponse checkIn(AttendanceCheckInRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));

        CourseSchedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("수업 스케줄을 찾을 수 없습니다"));

        LocalDateTime now = LocalDateTime.now();

        // 예상 하원 시간 자동 계산
        // 1. 요청에 명시적으로 지정된 경우 사용
        // 2. 그렇지 않으면 등원 시간 + 코스 수업 시간(분)으로 자동 계산
        LocalTime expectedLeave;
        if (request.getExpectedLeaveTime() != null) {
            expectedLeave = request.getExpectedLeaveTime();
        } else {
            // 등원 시간 + 수업 시간으로 자동 계산
            Integer courseDuration = schedule.getCourse().getDurationMinutes();
            expectedLeave = now.toLocalTime().plusMinutes(courseDuration);
            log.info("예상 하원 시간 자동 계산: 등원={}, 수업시간={}분, 예상하원={}",
                    now.toLocalTime(), courseDuration, expectedLeave);
        }

        Attendance attendance = Attendance.builder()
                .student(student)
                .schedule(schedule)
                .status(AttendanceStatus.PRESENT)
                .build();

        // 체크인 처리 (자동 지각 판단 포함)
        attendance.checkIn(now, expectedLeave);

        Attendance savedAttendance = attendanceRepository.save(attendance);
        log.info("출석 체크인: 학생={}, 수업={}, 상태={}, 예상하원={}",
                student.getStudentName(),
                schedule.getCourse().getCourseName(),
                savedAttendance.getStatus(),
                expectedLeave);

        // 지각인 경우 자동으로 문자 알림 발송
        if (savedAttendance.getStatus() == AttendanceStatus.LATE) {
            LocalDateTime scheduledStartTime = LocalDateTime.of(
                    now.toLocalDate(),
                    schedule.getStartTime()
            );
            automatedMessageService.sendLateNotification(student, now, scheduledStartTime);
        }

        return toResponse(savedAttendance);
    }

    @Transactional
    public AttendanceResponse checkOut(Long attendanceId) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("출석 기록을 찾을 수 없습니다"));

        LocalDateTime now = LocalDateTime.now();
        attendance.checkOut(now);

        log.info("하원 체크아웃: 학생={}, 시각={}",
                attendance.getStudent().getStudentName(), now);

        return toResponse(attendance);
    }

    @Transactional
    public AttendanceResponse updateStatus(Long attendanceId, AttendanceStatus status, String reason) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("출석 기록을 찾을 수 없습니다"));

        AttendanceStatus previousStatus = attendance.getStatus();
        attendance.updateStatus(status, reason);
        log.info("출석 상태 변경: 학생={}, 이전상태={}, 새상태={}",
                attendance.getStudent().getStudentName(), previousStatus, status);

        // 결석 처리 시 수강권 횟수 자동 차감
        if (status == AttendanceStatus.ABSENT && previousStatus != AttendanceStatus.ABSENT) {
            deductEnrollmentCount(attendance);
        }

        return toResponse(attendance);
    }

    /**
     * 결석 시 수강권 횟수 자동 차감
     */
    private void deductEnrollmentCount(Attendance attendance) {
        Long studentId = attendance.getStudent().getId();
        Long courseId = attendance.getSchedule().getCourse().getId();

        // 해당 학생의 해당 수업에 대한 활성 수강권 조회
        List<Enrollment> activeEnrollments = enrollmentRepository
                .findActiveEnrollmentByStudentAndCourse(studentId, courseId);

        if (activeEnrollments.isEmpty()) {
            log.warn("결석 처리: 활성 수강권이 없음 - 학생ID={}, 수업ID={}", studentId, courseId);
            throw new BusinessException("활성화된 수강권이 없어 결석 처리할 수 없습니다.");
        }

        // 가장 최근 수강권 사용 (첫 번째 항목)
        Enrollment enrollment = activeEnrollments.get(0);
        enrollment.useCount();

        log.info("결석으로 수강권 횟수 차감: 수강권ID={}, 남은횟수={}/{}",
                enrollment.getId(),
                enrollment.getRemainingCount(),
                enrollment.getTotalCount());
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByDate(LocalDate date) {
        return attendanceRepository.findByDate(date).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByStudent(Long studentId) {
        return attendanceRepository.findByStudentId(studentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceBySchedule(Long scheduleId) {
        return attendanceRepository.findByScheduleId(scheduleId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByStudentAndDateRange(
            Long studentId, LocalDate startDate, LocalDate endDate) {
        return attendanceRepository.findByStudentAndDateRange(studentId, startDate, endDate).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .studentId(attendance.getStudent().getId())
                .studentName(attendance.getStudent().getStudentName())
                .scheduleId(attendance.getSchedule().getId())
                .courseName(attendance.getSchedule().getCourse().getCourseName())
                .status(attendance.getStatus())
                .checkInTime(attendance.getCheckInTime())
                .checkOutTime(attendance.getCheckOutTime())
                .expectedLeaveTime(attendance.getExpectedLeaveTime())
                .memo(attendance.getMemo())
                .reason(attendance.getReason())
                .build();
    }
}
