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
import web.kplay.studentmanagement.repository.UserRepository;
import web.kplay.studentmanagement.domain.user.User;

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
    private final UserRepository userRepository;
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
                .classCompleted(false)
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

    @Transactional
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
                    Attendance attendance = existingAttendance.get();
                    // 자동 결석 처리 (20분 경과 시)
                    attendance.markAsAbsentIfLate();
                    attendanceRepository.save(attendance);
                    
                    // 결석자는 출석현황에서 제외
                    if (!attendance.isAbsent()) {
                        responses.add(toResponse(attendance));
                    }
                } else {
                    // 출석 데이터가 없으면 자동 결석 처리 확인
                    LocalTime scheduleStartTime = schedule.getStartTime();
                    LocalTime cutoffTime = scheduleStartTime.plusMinutes(20);
                    LocalTime currentTime = LocalDateTime.now().toLocalTime();
                    
                    // 20분 경과 시 결석자는 제외, 아직 시간이 안 된 경우만 미출석으로 표시
                    if (currentTime.isBefore(cutoffTime)) {
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
    public void cancelAttendance(Long attendanceId) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("출석 기록을 찾을 수 없습니다"));
        
        attendanceRepository.delete(attendance);
        log.info("Attendance cancelled: student={}, course={}", 
                attendance.getStudent().getStudentName(),
                attendance.getSchedule().getCourse().getCourseName());
    }

    /**
     * 수업 완료 상태 토글
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

    /**
     * 학부모용 자녀 출석 조회
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getMyChildAttendances(String username, LocalDate date) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 학부모의 자녀 목록 조회 (부모 전화번호로 매칭)
        List<Student> myStudents = studentRepository.findByParentPhoneAndIsActive(user.getPhoneNumber(), true);
        
        if (myStudents.isEmpty()) {
            return List.of();
        }
        
        // 자녀들의 출석 기록 조회
        List<Long> studentIds = myStudents.stream()
                .map(Student::getId)
                .collect(Collectors.toList());
        
        List<Attendance> attendances = attendanceRepository.findByStudentIdInAndScheduleScheduleDate(studentIds, date);
        
        return attendances.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 학부모 자녀 월별 출석 조회
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getMyChildMonthlyAttendances(String username, int year, int month) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 학부모의 자녀 목록 조회 (부모 전화번호로 매칭)
        List<Student> myStudents = studentRepository.findByParentPhoneAndIsActive(user.getPhoneNumber(), true);
        
        if (myStudents.isEmpty()) {
            return List.of();
        }
        
        // 월의 시작일과 마지막일 계산
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        // 자녀들의 출석 기록 조회
        List<Long> studentIds = myStudents.stream()
                .map(Student::getId)
                .collect(Collectors.toList());
        
        List<Attendance> attendances = attendanceRepository.findByStudentIdInAndScheduleScheduleDateBetween(studentIds, startDate, endDate);
        
        return attendances.stream()
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
                .startTime(attendance.getSchedule().getStartTime().toString())
                .endTime(attendance.getSchedule().getEndTime().toString())
                .status(attendance.getStatus())
                .checkInTime(attendance.getCheckInTime())
                .checkOutTime(attendance.getCheckOutTime())
                .expectedLeaveTime(attendance.getExpectedLeaveTime())
                .originalExpectedLeaveTime(attendance.getOriginalExpectedLeaveTime())
                .memo(attendance.getMemo())
                .reason(attendance.getReason())
                .classCompleted(attendance.getClassCompleted())
                .teacherName(attendance.getSchedule().getCourse().getTeacher() != null ? 
                    attendance.getSchedule().getCourse().getTeacher().getName() : null)
                .dcCheck(attendance.getDcCheck())
                .wrCheck(attendance.getWrCheck())
                .vocabularyClass(attendance.getVocabularyClass())
                .grammarClass(attendance.getGrammarClass())
                .phonicsClass(attendance.getPhonicsClass())
                .speakingClass(attendance.getSpeakingClass())
                .additionalClassEndTime(attendance.getAdditionalClassEndTime())
                .build();
    }

    /**
     * 학부모 자녀 수업 정보 조회 (스케줄 기반)
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getMyChildSchedules(String username, LocalDate date) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 학부모의 자녀 목록 조회
        List<Student> myStudents = studentRepository.findByParentPhoneAndIsActive(user.getPhoneNumber(), true);
        
        if (myStudents.isEmpty()) {
            return List.of();
        }
        
        // 자녀들의 수업 스케줄 조회 (출석 여부 상관없이)
        List<Long> studentIds = myStudents.stream()
                .map(Student::getId)
                .collect(Collectors.toList());
        
        // 해당 날짜의 스케줄에서 자녀가 등록된 수업 조회
        List<Attendance> attendances = attendanceRepository.findByStudentIdInAndScheduleScheduleDate(studentIds, date);
        
        return attendances.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 학부모 자녀 월별 수업 정보 조회
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getMyChildMonthlySchedules(String username, int year, int month) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 학부모의 자녀 목록 조회
        List<Student> myStudents = studentRepository.findByParentPhoneAndIsActive(user.getPhoneNumber(), true);
        
        if (myStudents.isEmpty()) {
            return List.of();
        }
        
        // 월의 시작일과 마지막일 계산
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        // 자녀들의 수업 스케줄 조회
        List<Long> studentIds = myStudents.stream()
                .map(Student::getId)
                .collect(Collectors.toList());
        
        List<Attendance> attendances = attendanceRepository.findByStudentIdInAndScheduleScheduleDateBetween(studentIds, startDate, endDate);
        
        return attendances.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * D/C 체크 업데이트
     */
    @Transactional
    public AttendanceResponse updateDcCheck(Long attendanceId, String dcCheck) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("출석 기록을 찾을 수 없습니다."));

        attendance.updateDcCheck(dcCheck);
        log.info("D/C check updated: student={}, dcCheck={}", attendance.getStudent().getStudentName(), dcCheck);

        return toResponse(attendance);
    }

    /**
     * WR 체크 업데이트
     */
    @Transactional
    public AttendanceResponse updateWrCheck(Long attendanceId, String wrCheck) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("출석 기록을 찾을 수 없습니다."));

        attendance.updateWrCheck(wrCheck);
        log.info("WR check updated: student={}, wrCheck={}", attendance.getStudent().getStudentName(), wrCheck);

        return toResponse(attendance);
    }

    /**
     * Vocabulary 수업 토글
     */
    @Transactional
    public AttendanceResponse toggleVocabularyClass(Long attendanceId) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("출석 기록을 찾을 수 없습니다."));

        attendance.toggleVocabularyClass();
        log.info("Vocabulary class toggled: student={}, enabled={}", 
                attendance.getStudent().getStudentName(), attendance.getVocabularyClass());

        return toResponse(attendance);
    }

    /**
     * Grammar 수업 토글
     */
    @Transactional
    public AttendanceResponse toggleGrammarClass(Long attendanceId) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("출석 기록을 찾을 수 없습니다."));

        attendance.toggleGrammarClass();
        log.info("Grammar class toggled: student={}, enabled={}", 
                attendance.getStudent().getStudentName(), attendance.getGrammarClass());

        return toResponse(attendance);
    }

    /**
     * Phonics 수업 토글
     */
    @Transactional
    public AttendanceResponse togglePhonicsClass(Long attendanceId) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("출석 기록을 찾을 수 없습니다."));

        attendance.togglePhonicsClass();
        log.info("Phonics class toggled: student={}, enabled={}", 
                attendance.getStudent().getStudentName(), attendance.getPhonicsClass());

        return toResponse(attendance);
    }

    /**
     * Speaking 수업 토글
     */
    @Transactional
    public AttendanceResponse toggleSpeakingClass(Long attendanceId) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("출석 기록을 찾을 수 없습니다."));

        attendance.toggleSpeakingClass();
        log.info("Speaking class toggled: student={}, enabled={}", 
                attendance.getStudent().getStudentName(), attendance.getSpeakingClass());

        return toResponse(attendance);
    }
}
