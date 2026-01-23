package web.kplay.studentmanagement.service.attendance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.attendance.Attendance;
import web.kplay.studentmanagement.domain.attendance.AttendanceStatus;
import web.kplay.studentmanagement.domain.course.Course;
import web.kplay.studentmanagement.domain.course.CourseSchedule;
import web.kplay.studentmanagement.domain.course.Enrollment;
import web.kplay.studentmanagement.domain.reservation.NaverBooking;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.dto.attendance.AttendanceCheckInRequest;
import web.kplay.studentmanagement.dto.attendance.AttendanceResponse;
import web.kplay.studentmanagement.dto.attendance.StudentSearchResponse;
import web.kplay.studentmanagement.exception.BusinessException;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.AttendanceRepository;
import web.kplay.studentmanagement.repository.CourseScheduleRepository;
import web.kplay.studentmanagement.repository.EnrollmentRepository;
import web.kplay.studentmanagement.repository.StudentRepository;
import web.kplay.studentmanagement.repository.UserRepository;
import web.kplay.studentmanagement.domain.user.User;
import web.kplay.studentmanagement.service.StudentCourseExcelService;

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
    private final web.kplay.studentmanagement.repository.NaverBookingRepository naverBookingRepository;
    private final StudentCourseExcelService studentCourseExcelService;

    /**
     * 출석 체크인 (부모님 핸드폰 뒷자리 4자리 검증 포함)
     * 
     * @param request 출석 체크인 요청 (학생ID, 부모님 핸드폰 뒷자리)
     * @return 출석 응답 정보
     * @throws ResourceNotFoundException 학생을 찾을 수 없는 경우
     * @throws IllegalArgumentException 부모님 핸드폰 번호가 일치하지 않는 경우
     */
    @Transactional
    public AttendanceResponse checkIn(AttendanceCheckInRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));

        // 부모님 핸드폰 번호 뒷자리 4자리 검증
        validateParentPhone(student, request.getParentPhoneLast4());

        LocalDateTime now = LocalDateTime.now();
        
        // 학생의 활성 수강권에서 Course 정보 가져오기
        web.kplay.studentmanagement.domain.course.Course course = null;
        List<Enrollment> activeEnrollments = enrollmentRepository.findByStudentAndIsActiveTrue(student);
        if (!activeEnrollments.isEmpty()) {
            course = activeEnrollments.get(0).getCourse();
        }

        // Expected leave time auto calculated
        LocalTime expectedLeave;
        if (request.getExpectedLeaveTime() != null) {
            expectedLeave = request.getExpectedLeaveTime();
        } else if (course != null) {
            Integer courseDuration = course.getDurationMinutes();
            expectedLeave = now.toLocalTime().plusMinutes(courseDuration);
        } else {
            expectedLeave = now.toLocalTime().plusHours(2); // 기본 2시간
        }

        Attendance attendance = Attendance.builder()
                .student(student)
                .course(course)
                .attendanceDate(now.toLocalDate())
                .attendanceTime(now.toLocalTime())
                .durationMinutes(course != null ? course.getDurationMinutes() : 120)
                .status(AttendanceStatus.PRESENT)
                .classCompleted(false)
                .build();

        attendance.checkIn(now, expectedLeave);

        Attendance savedAttendance = attendanceRepository.save(attendance);
        log.info("Attendance check-in: student={}, course={}, status={}, expected leave={}",
                student.getStudentName(),
                course != null ? course.getCourseName() : "없음",
                savedAttendance.getStatus(),
                expectedLeave);

        // 학부모에게 등원 알림 문자 발송
        try {
            automatedMessageService.sendCheckInNotification(student, now, expectedLeave);
        } catch (Exception e) {
            log.error("등원 알림 문자 발송 실패: {}", e.getMessage());
        }

        return toResponse(savedAttendance);
    }

    /**
     * 전화번호 뒷 4자리로 출석 체크인 (회원 학생 + 네이버 예약 통합)
     */
    /**
     * 전화번호 뒷자리로 학생 검색 (출석 체크 전 확인용)
     */
    @Transactional(readOnly = true)
    public List<StudentSearchResponse> searchStudentByPhone(String phoneLast4) {
        // 입력값 검증
        if (phoneLast4 == null || phoneLast4.length() != 4 || !phoneLast4.matches("\\d{4}")) {
            throw new IllegalArgumentException("전화번호 뒷자리 4자리를 정확히 입력해주세요");
        }
        
        List<StudentSearchResponse> results = new ArrayList<>();
        
        // 1. 회원 학생 찾기
        List<Student> students = studentRepository.findAll().stream()
                .filter(s -> {
                    if (s.getParentPhone() == null) return false;
                    String cleanPhone = s.getParentPhone().replaceAll("[^0-9]", "");
                    return cleanPhone.length() >= 4 && cleanPhone.substring(cleanPhone.length() - 4).equals(phoneLast4);
                })
                .collect(Collectors.toList());
        
        for (Student student : students) {
            String courseName = null;
            List<Enrollment> activeEnrollments = enrollmentRepository.findByStudentAndIsActiveTrue(student);
            if (!activeEnrollments.isEmpty()) {
                courseName = activeEnrollments.get(0).getCourse().getCourseName();
            }
            
            results.add(StudentSearchResponse.builder()
                    .studentId(student.getId())
                    .studentName(student.getStudentName())
                    .parentName(student.getParentName())
                    .parentPhone(student.getParentPhone())
                    .school(student.getSchool())
                    .courseName(courseName)
                    .isNaverBooking(false)
                    .build());
        }
        
        // 2. 네이버 예약 찾기
        var naverBookings = naverBookingRepository.findAll().stream()
                .filter(nb -> {
                    if (nb.getPhone() == null) return false;
                    String cleanPhone = nb.getPhone().replaceAll("[^0-9]", "");
                    return cleanPhone.length() >= 4 && cleanPhone.substring(cleanPhone.length() - 4).equals(phoneLast4);
                })
                .collect(Collectors.toList());
        
        for (var booking : naverBookings) {
            // 엑셀에서 반 정보 조회
            String courseName = null;
            if (booking.getStudentName() != null) {
                String cleanName = booking.getStudentName().trim().replaceAll("\\s+", "");
                courseName = studentCourseExcelService.getCourseName(cleanName);
            }
            
            results.add(StudentSearchResponse.builder()
                    .naverBookingId(booking.getId())
                    .studentName(booking.getStudentName() != null ? booking.getStudentName() : booking.getName())
                    .parentName(booking.getName())
                    .parentPhone(booking.getPhone())
                    .school(booking.getSchool())
                    .courseName(courseName)
                    .isNaverBooking(true)
                    .build());
        }
        
        if (results.isEmpty()) {
            throw new ResourceNotFoundException("전화번호 뒷자리 " + phoneLast4 + "로 등록된 학생이나 예약을 찾을 수 없습니다");
        }
        
        return results;
    }
    
    /**
     * 출석 체크인 (학생 확인 후)
     */
    @Transactional
    public AttendanceResponse checkInByPhone(String phoneLast4, LocalTime expectedLeaveTime) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        
        // 입력값 검증
        if (phoneLast4 == null || phoneLast4.length() != 4 || !phoneLast4.matches("\\d{4}")) {
            throw new IllegalArgumentException("전화번호 뒷자리 4자리를 정확히 입력해주세요");
        }
        
        // 1. 먼저 회원 학생 찾기 (부모 전화번호로)
        List<Student> students = studentRepository.findAll().stream()
                .filter(s -> s.getParentPhone() != null && 
                            s.getParentPhone().replaceAll("[^0-9]", "").endsWith(phoneLast4))
                .collect(Collectors.toList());
        
        if (students.isEmpty()) {
            // 2. 네이버 예약 찾기
            var naverBookings = naverBookingRepository.findAll().stream()
                    .filter(nb -> nb.getPhone() != null && 
                                 nb.getPhone().replaceAll("[^0-9]", "").endsWith(phoneLast4))
                    .collect(Collectors.toList());
            
            if (naverBookings.isEmpty()) {
                throw new ResourceNotFoundException("전화번호 뒷자리 " + phoneLast4 + "로 등록된 학생이나 예약을 찾을 수 없습니다");
            }
            
            // 네이버 예약 처리
            return processNaverBookingCheckIn(naverBookings.get(0), now, expectedLeaveTime);
        }
        
        // 회원 학생 처리
        Student student = students.get(0);
        
        // 학생의 활성 수강권에서 Course 정보 가져오기
        web.kplay.studentmanagement.domain.course.Course course = null;
        List<Enrollment> activeEnrollments = enrollmentRepository.findByStudentAndIsActiveTrue(student);
        if (!activeEnrollments.isEmpty()) {
            course = activeEnrollments.get(0).getCourse();
        }
        
        // 예상 하원 시간 계산: 원래 수업 시작 시간 + 수업 시간
        LocalTime expectedLeave;
        if (expectedLeaveTime != null) {
            expectedLeave = expectedLeaveTime;
        } else {
            // 오늘 날짜에 이미 출석 레코드가 있으면 그 레코드의 원래 수업 시간 사용
            List<Attendance> existingAttendances = attendanceRepository.findByDate(today).stream()
                    .filter(a -> a.getStudent() != null && a.getStudent().getId().equals(student.getId()))
                    .collect(Collectors.toList());
            
            if (!existingAttendances.isEmpty() && existingAttendances.get(0).getAttendanceTime() != null) {
                // 기존 레코드의 원래 수업 시작 시간 기준으로 계산
                LocalTime originalStartTime = existingAttendances.get(0).getAttendanceTime();
                int duration = course != null ? course.getDurationMinutes() : 120;
                expectedLeave = originalStartTime.plusMinutes(duration);
            } else {
                // 새 레코드면 현재 시간 기준
                int duration = course != null ? course.getDurationMinutes() : 120;
                expectedLeave = now.toLocalTime().plusMinutes(duration);
            }
        }
        
        // 오늘 날짜에 이미 출석 레코드가 있는지 확인
        List<Attendance> existingAttendances = attendanceRepository.findByDate(today).stream()
                .filter(a -> a.getStudent() != null && a.getStudent().getId().equals(student.getId()))
                .collect(Collectors.toList());
        
        Attendance attendance;
        if (!existingAttendances.isEmpty()) {
            // 기존 레코드 업데이트
            attendance = existingAttendances.get(0);
            attendance.checkIn(now, expectedLeave);
            log.info("Existing attendance updated: name={}, expectedLeave={}", student.getStudentName(), expectedLeave);
        } else {
            // 새 레코드 생성
            attendance = Attendance.builder()
                    .student(student)
                    .course(course)
                    .attendanceDate(today)
                    .attendanceTime(now.toLocalTime())
                    .durationMinutes(course != null ? course.getDurationMinutes() : 120)
                    .status(AttendanceStatus.PRESENT)
                    .classCompleted(false)
                    .build();
            attendance.checkIn(now, expectedLeave);
            log.info("New attendance created: name={}, expectedLeave={}", student.getStudentName(), expectedLeave);
        }
        
        Attendance saved = attendanceRepository.save(attendance);
        log.info("Student attendance check-in: name={}, phone={}", student.getStudentName(), student.getParentPhone());
        
        // 학부모에게 등원 알림 문자 발송
        try {
            automatedMessageService.sendCheckInNotification(student, now, expectedLeave);
        } catch (Exception e) {
            log.error("등원 알림 문자 발송 실패: {}", e.getMessage());
        }
        
        return toResponse(saved);
    }
    
    private AttendanceResponse processNaverBookingCheckIn(web.kplay.studentmanagement.domain.reservation.NaverBooking naverBooking, 
                                                           LocalDateTime now, LocalTime expectedLeaveTime) {
        LocalDate today = now.toLocalDate();
        
        // 예상 하원 시간 계산
        LocalTime expectedLeave;
        if (expectedLeaveTime != null) {
            expectedLeave = expectedLeaveTime;
        } else {
            List<Attendance> existingAttendances = attendanceRepository.findByDate(today).stream()
                    .filter(a -> a.getNaverBooking() != null && a.getNaverBooking().getId().equals(naverBooking.getId()))
                    .collect(Collectors.toList());
            
            if (!existingAttendances.isEmpty() && existingAttendances.get(0).getAttendanceTime() != null) {
                LocalTime originalStartTime = existingAttendances.get(0).getAttendanceTime();
                expectedLeave = originalStartTime.plusHours(2);
            } else {
                expectedLeave = now.toLocalTime().plusHours(2);
            }
        }
        
        // 오늘 날짜에 이미 출석 레코드가 있는지 확인
        List<Attendance> existingAttendances = attendanceRepository.findByDate(today).stream()
                .filter(a -> a.getNaverBooking() != null && a.getNaverBooking().getId().equals(naverBooking.getId()))
                .collect(Collectors.toList());
        
        Attendance attendance;
        if (!existingAttendances.isEmpty()) {
            attendance = existingAttendances.get(0);
            attendance.checkIn(now, expectedLeave);
            log.info("Existing naver booking attendance updated: name={}", naverBooking.getStudentName());
        } else {
            attendance = Attendance.builder()
                    .naverBooking(naverBooking)
                    .attendanceDate(today)
                    .attendanceTime(now.toLocalTime())
                    .durationMinutes(120)
                    .status(AttendanceStatus.PRESENT)
                    .classCompleted(false)
                    .build();
            attendance.checkIn(now, expectedLeave);
            log.info("New naver booking attendance created: name={}", naverBooking.getStudentName());
        }
        
        Attendance saved = attendanceRepository.save(attendance);
        log.info("Naver booking attendance check-in: name={}, phone={}", naverBooking.getStudentName(), naverBooking.getPhone());
        return toResponse(saved);
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
        Long courseId = attendance.getCourse() != null ? attendance.getCourse().getId() : null;

        if (courseId == null) {
            log.warn("Absence processing: No course info - studentId={}", studentId);
            return;
        }

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
        // 해당 날짜의 모든 출석 데이터 조회
        List<Attendance> attendances = attendanceRepository.findByDate(date);
        
        return attendances.stream()
                .map(this::toResponse).filter(response -> response != null)
                .filter(response -> response != null) // null 필터링
                .collect(Collectors.toList());
    }
    
    private String parseNaverBookingTime(String bookingTime) {
        // "26. 1. 20.(화) 오전 10:00" -> "10:00"
        // "26. 1. 20.(화) 오후 4:00" -> "16:00"
        try {
            if (bookingTime.contains("오전")) {
                String time = bookingTime.split("오전")[1].trim();
                String[] timeParts = time.split(":");
                int hour = Integer.parseInt(timeParts[0].trim());
                if (hour == 12) hour = 0; // 오전 12시는 00시
                return String.format("%02d:%s", hour, timeParts[1]);
            } else if (bookingTime.contains("오후")) {
                String time = bookingTime.split("오후")[1].trim();
                String[] timeParts = time.split(":");
                int hour = Integer.parseInt(timeParts[0].trim());
                if (hour != 12) hour += 12; // 오후는 +12 (단, 12시는 그대로)
                return String.format("%02d:%s", hour, timeParts[1]);
            }
        } catch (Exception e) {
            log.warn("Failed to parse booking time: {}", bookingTime);
        }
        return bookingTime;
    }

    /**
     * 부모님 핸드폰 번호 뒷자리 4자리 검증
     * 
     * @param student 학생 정보
     * @param parentPhoneLast4 입력된 부모님 핸드폰 뒷자리 4자리
     * @throws IllegalArgumentException 핸드폰 번호가 일치하지 않는 경우
     */
    private void validateParentPhone(Student student, String parentPhoneLast4) {
        String parentPhone = student.getParentPhone();
        if (parentPhone == null || parentPhone.length() < 4) {
            throw new IllegalArgumentException("등록된 부모님 핸드폰 번호가 없습니다");
        }
        
        // 핸드폰 번호에서 숫자만 추출
        String phoneNumbers = parentPhone.replaceAll("[^0-9]", "");
        if (phoneNumbers.length() < 4) {
            throw new IllegalArgumentException("등록된 부모님 핸드폰 번호가 올바르지 않습니다");
        }
        
        // 뒷자리 4자리 추출
        String actualLast4 = phoneNumbers.substring(phoneNumbers.length() - 4);
        
        if (!actualLast4.equals(parentPhoneLast4)) {
            log.warn("Parent phone validation failed: student={}, expected={}, actual={}", 
                    student.getStudentName(), actualLast4, parentPhoneLast4);
            throw new IllegalArgumentException("부모님 핸드폰 번호가 일치하지 않습니다");
        }
        
        log.info("Parent phone validation success: student={}", student.getStudentName());
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByStudent(Long studentId) {
        return attendanceRepository.findByStudentId(studentId).stream()
                .map(this::toResponse).filter(response -> response != null)
                .collect(Collectors.toList());
    }

    // getAttendanceBySchedule 메서드 삭제 (schedule 제거로 인해 사용 안 함)

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByStudentAndDateRange(
            Long studentId, LocalDate startDate, LocalDate endDate) {
        return attendanceRepository.findByStudentAndDateRange(studentId, startDate, endDate).stream()
                .map(this::toResponse).filter(response -> response != null)
                .collect(Collectors.toList());
    }

    /**
     * 출석한 순서대로 조회 (등원 시간 오름차순)
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByCheckInOrder(LocalDate date) {
        return attendanceRepository.findByDateOrderByCheckInTime(date).stream()
                .map(this::toResponse).filter(response -> response != null)
                .collect(Collectors.toList());
    }

    /**
     * 하원 예정 순서대로 조회 (예상 하원 시간 오름차순)
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByLeaveOrder(LocalDate date) {
        return attendanceRepository.findByDateOrderByExpectedLeaveTime(date).stream()
                .map(this::toResponse).filter(response -> response != null)
                .collect(Collectors.toList());
    }

    /**
     * 오늘 출석한 학생만 조회
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendedStudents(LocalDate date) {
        return attendanceRepository.findAttendedByDate(date).stream()
                .map(this::toResponse).filter(response -> response != null)
                .collect(Collectors.toList());
    }

    /**
     * 오늘 출석하지 않은 학생 조회
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getNotAttendedStudents(LocalDate date) {
        return attendanceRepository.findNotAttendedByDate(date).stream()
                .map(this::toResponse).filter(response -> response != null)
                .collect(Collectors.toList());
    }

    /**
     * 하원 완료된 학생만 조회
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getCheckedOutStudents(LocalDate date) {
        return attendanceRepository.findCheckedOutByDate(date).stream()
                .map(this::toResponse).filter(response -> response != null)
                .collect(Collectors.toList());
    }

    /**
     * 아직 하원하지 않은 학생 조회
     */
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getNotCheckedOutStudents(LocalDate date) {
        return attendanceRepository.findNotCheckedOutByDate(date).stream()
                .map(this::toResponse).filter(response -> response != null)
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
                attendance.getCourse() != null ? attendance.getCourse().getCourseName() : "없음");
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
                .map(this::toResponse).filter(response -> response != null)
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
                .map(this::toResponse).filter(response -> response != null)
                .collect(Collectors.toList());
    }

    private AttendanceResponse toResponse(Attendance attendance) {
        boolean isNaver = attendance.getNaverBooking() != null;
        Student student = attendance.getStudent();
        
        // student가 null이면 null 반환 (필터링됨)
        if (!isNaver && student == null) {
            return null;
        }
        
        // 네이버 예약인 경우 학생 이름과 반 정보 가져오기
        String studentName;
        String courseName = "없음";
        
        if (isNaver) {
            NaverBooking naverBooking = attendance.getNaverBooking();
            studentName = naverBooking.getStudentName() != null ? naverBooking.getStudentName() : naverBooking.getName();
            
            // 엑셀에서 반 정보 조회
            if (naverBooking.getStudentName() != null) {
                String cleanName = naverBooking.getStudentName().trim().replaceAll("\\s+", "");
                String excelCourseName = studentCourseExcelService.getCourseName(cleanName);
                if (excelCourseName != null) {
                    courseName = excelCourseName;
                }
            }
        } else {
            studentName = student.getStudentName();
            courseName = attendance.getCourse() != null ? attendance.getCourse().getCourseName() : "없음";
        }
        
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .studentId(isNaver ? null : student.getId())
                .studentName(studentName)
                .studentPhone(isNaver ? attendance.getNaverBooking().getPhone() : student.getParentPhone())
                .className(isNaver ? "네이버 예약" : (attendance.getCourse() != null ? attendance.getCourse().getCourseName() : "없음"))
                .isNaverBooking(isNaver)
                .courseName(courseName)
                .startTime(attendance.getAttendanceTime().toString())
                .endTime(attendance.getExpectedLeaveTime() != null ? attendance.getExpectedLeaveTime().toString() : null)
                .status(attendance.getStatus())
                .checkInTime(attendance.getCheckInTime())
                .checkOutTime(attendance.getCheckOutTime())
                .expectedLeaveTime(attendance.getExpectedLeaveTime())
                .originalExpectedLeaveTime(attendance.getOriginalExpectedLeaveTime())
                .memo(attendance.getMemo())
                .reason(attendance.getReason())
                .classCompleted(attendance.getClassCompleted())
                .teacherName(attendance.getCourse() != null && attendance.getCourse().getTeacher() != null ? 
                    attendance.getCourse().getTeacher().getName() : null)
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
                .map(this::toResponse).filter(response -> response != null)
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
                .map(this::toResponse).filter(response -> response != null)
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

    /**
     * 학생 ID로 출석 체크인
     */
    @Transactional
    public AttendanceResponse checkInByStudentId(Long studentId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));

        // 오늘 이미 출석한 기록이 있는지 확인
        List<Attendance> existingAttendances = attendanceRepository.findByStudentAndDate(student, today);
        for (Attendance att : existingAttendances) {
            if (att.getCheckInTime() != null) {
                throw new IllegalStateException("이미 출석 체크가 완료되었습니다");
            }
        }

        // 기존 출석 기록이 있으면 체크인 처리
        if (!existingAttendances.isEmpty()) {
            Attendance attendance = existingAttendances.get(0);
            attendance.checkIn(now, attendance.getExpectedLeaveTime());
            log.info("Student check-in: name={}", student.getStudentName());
            return toResponse(attendance);
        }

        // 새 출석 기록 생성
        Course course = null;
        List<Enrollment> activeEnrollments = enrollmentRepository.findByStudentAndIsActiveTrue(student);
        if (!activeEnrollments.isEmpty()) {
            course = activeEnrollments.get(0).getCourse();
        }

        int durationMinutes = course != null ? course.getDurationMinutes() : 120;
        LocalTime expectedLeaveTime = now.toLocalTime().plusMinutes(durationMinutes);

        Attendance attendance = Attendance.builder()
                .student(student)
                .course(course)
                .attendanceDate(today)
                .attendanceTime(now.toLocalTime())
                .durationMinutes(durationMinutes)
                .status(AttendanceStatus.PRESENT)
                .classCompleted(false)
                .build();
        attendance.checkIn(now, expectedLeaveTime);

        Attendance saved = attendanceRepository.save(attendance);
        log.info("New attendance created and checked in: student={}", student.getStudentName());
        return toResponse(saved);
    }

    /**
     * 네이버 예약 ID로 출석 체크인
     */
    @Transactional
    public AttendanceResponse checkInByNaverBookingId(Long naverBookingId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        NaverBooking naverBooking = naverBookingRepository.findById(naverBookingId)
                .orElseThrow(() -> new ResourceNotFoundException("네이버 예약을 찾을 수 없습니다"));

        // 오늘 이미 출석한 기록이 있는지 확인
        List<Attendance> existingAttendances = attendanceRepository.findByNaverBookingAndDate(naverBooking, today);
        for (Attendance att : existingAttendances) {
            if (att.getCheckInTime() != null) {
                throw new IllegalStateException("이미 출석 체크가 완료되었습니다");
            }
        }

        // 기존 출석 기록이 있으면 체크인 처리
        if (!existingAttendances.isEmpty()) {
            Attendance attendance = existingAttendances.get(0);
            attendance.checkIn(now, attendance.getExpectedLeaveTime());
            log.info("Naver booking check-in: name={}", naverBooking.getStudentName());
            return toResponse(attendance);
        }

        // 엑셀에서 수업 시간 조회
        int durationMinutes = 120;
        if (naverBooking.getStudentName() != null) {
            String cleanName = naverBooking.getStudentName().trim().replaceAll("\\s+", "");
            Integer duration = studentCourseExcelService.getDurationMinutes(cleanName);
            if (duration != null) {
                durationMinutes = duration;
            }
        }

        LocalTime expectedLeaveTime = now.toLocalTime().plusMinutes(durationMinutes);

        Attendance attendance = Attendance.builder()
                .naverBooking(naverBooking)
                .attendanceDate(today)
                .attendanceTime(now.toLocalTime())
                .durationMinutes(durationMinutes)
                .status(AttendanceStatus.PRESENT)
                .classCompleted(false)
                .build();
        attendance.checkIn(now, expectedLeaveTime);

        Attendance saved = attendanceRepository.save(attendance);
        log.info("New naver booking attendance created and checked in: name={}", naverBooking.getStudentName());
        return toResponse(saved);
    }
}
