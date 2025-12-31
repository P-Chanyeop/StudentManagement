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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

        // Expected leave time auto calculated
        // 1. 요청에 명시적으로 지정된 경우 사용
        // 2. 그렇지 않으면 등원 시간 + 코스 수업 시간(분)으로 자동 계산
        LocalTime expectedLeave;
        if (request.getExpectedLeaveTime() != null) {
            expectedLeave = request.getExpectedLeaveTime();
        } else {
            // 등원 시간 + 수업 시간으로 자동 계산
            Integer courseDuration = schedule.getCourse().getDurationMinutes();
            expectedLeave = now.toLocalTime().plusMinutes(courseDuration);
            log.info("Expected leave time auto calculated: arrival={}, class duration={}min, expected leave={}",
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
        log.info("Attendance check-in: student={}, course={}, status={}, expected leave={}",
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

        log.info("Leave check-out: student={}, time={}",
                attendance.getStudent().getStudentName(), now);

        return toResponse(attendance);
    }

    @Transactional
    public AttendanceResponse updateStatus(Long attendanceId, AttendanceStatus status, String reason) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("출석 기록을 찾을 수 없습니다"));

        AttendanceStatus previousStatus = attendance.getStatus();
        attendance.updateStatus(status, reason);
        log.info("Attendance status changed: student={}, previous status={}, new status={}",
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
            log.warn("Absence processing: No active enrollment - studentId={}, courseId={}", studentId, courseId);
            throw new BusinessException("활성화된 수강권이 없어 결석 처리할 수 없습니다.");
        }

        // 가장 최근 수강권 사용 (첫 번째 항목)
        Enrollment enrollment = activeEnrollments.get(0);
        enrollment.useCount();

        log.info("Enrollment count deducted for absence: enrollmentId={}, remaining={}/{}",
                enrollment.getId(),
                enrollment.getRemainingCount(),
                enrollment.getTotalCount());
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByDate(LocalDate date) {
        // 해당 날짜의 모든 스케줄 조회
        List<CourseSchedule> schedules = scheduleRepository.findByScheduleDate(date);
        
        // 각 스케줄에 등록된 모든 학생들의 출석 데이터 생성/조회
        List<AttendanceResponse> responses = new ArrayList<>();
        
        for (CourseSchedule schedule : schedules) {
            // 해당 스케줄에 등록된 학생들 조회
            List<Enrollment> enrollments = enrollmentRepository.findByCourseAndIsActiveTrue(schedule.getCourse());
            
            for (Enrollment enrollment : enrollments) {
                Student student = enrollment.getStudent();
                
                // 기존 출석 데이터 조회
                Optional<Attendance> existingAttendance = attendanceRepository
                    .findByStudentIdAndScheduleId(student.getId(), schedule.getId());
                
                if (existingAttendance.isPresent()) {
                    responses.add(toResponse(existingAttendance.get()));
                } else {
                    // 출석 데이터가 없으면 미출석으로 생성해서 반환
                    AttendanceResponse response = AttendanceResponse.builder()
                        .id(null)
                        .studentId(student.getId())
                        .studentName(student.getStudentName())
                        .scheduleId(schedule.getId())
                        .courseName(schedule.getCourse().getCourseName())
                        .startTime(schedule.getStartTime().toString())
                        .endTime(schedule.getEndTime().toString())
                        .status(AttendanceStatus.ABSENT)
                        .checkInTime(null)
                        .checkOutTime(null)
                        .classCompleted(false)
                        .memo("")
                        .build();
                    responses.add(response);
                }
            }
        }
        
        return responses;
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

    /**
     * 출석한 순서대로 조회 (등원 시간 오름차순)
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByCheckInOrder(LocalDate date) {
        return attendanceRepository.findByDateOrderByCheckInTime(date).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 하원 예정 순서대로 조회 (예상 하원 시간 오름차순)
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByLeaveOrder(LocalDate date) {
        return attendanceRepository.findByDateOrderByExpectedLeaveTime(date).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 오늘 출석한 학생만 조회
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendedStudents(LocalDate date) {
        return attendanceRepository.findAttendedByDate(date).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 오늘 출석하지 않은 학생 조회
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getNotAttendedStudents(LocalDate date) {
        return attendanceRepository.findNotAttendedByDate(date).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 하원 완료된 학생만 조회
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getCheckedOutStudents(LocalDate date) {
        return attendanceRepository.findCheckedOutByDate(date).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 아직 하원하지 않은 학생 조회
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getNotCheckedOutStudents(LocalDate date) {
        return attendanceRepository.findNotCheckedOutByDate(date).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 수업 완료 체크박스 토글
     */
    @Transactional
    public AttendanceResponse toggleClassCompleted(Long attendanceId) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("출석 기록을 찾을 수 없습니다"));

        attendance.toggleClassCompleted();
        log.info("Class completion status toggled: student={}, completed={}",
                attendance.getStudent().getStudentName(), attendance.getClassCompleted());

        return toResponse(attendance);
    }

    /**
     * 수업 완료 처리
     */
    @Transactional
    public AttendanceResponse completeClass(Long attendanceId) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("출석 기록을 찾을 수 없습니다"));

        attendance.completeClass();
        log.info("Class completion processed: student={}", attendance.getStudent().getStudentName());

        return toResponse(attendance);
    }

    /**
     * 수업 완료 취소
     */
    @Transactional
    public AttendanceResponse uncompleteClass(Long attendanceId) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("출석 기록을 찾을 수 없습니다"));

        attendance.uncompleteClass();
        log.info("Class completion cancelled: student={}", attendance.getStudent().getStudentName());

        return toResponse(attendance);
    }

    /**
     * 사유 업데이트
     */
    @Transactional
    public AttendanceResponse updateReason(Long attendanceId, String reason) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("출석 기록을 찾을 수 없습니다"));

        attendance.updateStatus(attendance.getStatus(), reason);
        log.info("Reason updated: student={}, reason={}", attendance.getStudent().getStudentName(), reason);

        return toResponse(attendance);
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .studentId(attendance.getStudent().getId())
                .studentName(attendance.getStudent().getStudentName())
                .scheduleId(attendance.getSchedule().getId())
                .courseName(attendance.getSchedule().getCourse().getCourseName())
                .startTime(attendance.getSchedule().getStartTime().toString())
                .endTime(attendance.getSchedule().getEndTime().toString())
                .status(attendance.getStatus())
                .checkInTime(attendance.getCheckInTime())
                .checkOutTime(attendance.getCheckOutTime())
                .expectedLeaveTime(attendance.getExpectedLeaveTime())
                .memo(attendance.getMemo())
                .reason(attendance.getReason())
                .classCompleted(attendance.getClassCompleted())
                .build();
    }
}
