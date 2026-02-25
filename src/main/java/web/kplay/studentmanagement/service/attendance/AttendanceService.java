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
import web.kplay.studentmanagement.service.AdditionalClassExcelService;

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
    private final AdditionalClassExcelService additionalClassExcelService;
    private final web.kplay.studentmanagement.repository.CourseRepository courseRepository;
    private final web.kplay.studentmanagement.repository.ReservationRepository reservationRepository;

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

        // 오늘 이미 결석 처리된 레코드가 있는지 확인
        List<Attendance> todayRecords = attendanceRepository.findByStudentAndDate(student, now.toLocalDate());
        Attendance existingAbsent = todayRecords.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT && a.getCheckInTime() == null)
                .findFirst().orElse(null);

        // Expected leave time auto calculated
        LocalTime expectedLeave;
        if (request.getExpectedLeaveTime() != null) {
            expectedLeave = request.getExpectedLeaveTime();
        } else if (course != null) {
            Integer courseDuration = course.getDurationMinutes();
            expectedLeave = now.toLocalTime().plusMinutes(courseDuration);
        } else {
            expectedLeave = now.toLocalTime().plusHours(2);
        }

        // 예약 시간 기준 출석/지각/결석 판단 (15분 출석, 30분 지각, 이후 결석)
        AttendanceStatus checkInStatus = AttendanceStatus.PRESENT;
        var reservations = reservationRepository.findByStudentIdAndReservationDate(student.getId(), now.toLocalDate());
        if (!reservations.isEmpty()) {
            LocalTime reservedTime = reservations.get(0).getReservationTime();
            long minutesLate = java.time.Duration.between(reservedTime, now.toLocalTime()).toMinutes();
            if (minutesLate > 30) {
                checkInStatus = AttendanceStatus.ABSENT;
            } else if (minutesLate > 15) {
                checkInStatus = AttendanceStatus.LATE;
            }
        }

        Attendance attendance;
        if (existingAbsent != null) {
            // 자동 결석 처리 후 뒤늦게 온 경우 → 지각으로 변경
            attendance = existingAbsent;
            attendance.checkIn(now, expectedLeave);
            attendance.updateStatus(AttendanceStatus.LATE, null);
            log.info("결석→지각 변경: student={}", student.getStudentName());
        } else {
            // 새 출석 레코드 생성
            attendance = Attendance.builder()
                    .student(student)
                    .course(course)
                    .attendanceDate(now.toLocalDate())
                    .attendanceTime(now.toLocalTime())
                    .durationMinutes(course != null ? course.getDurationMinutes() : 120)
                    .status(checkInStatus)
                    .classCompleted(false)
                    .build();
            attendance.checkIn(now, expectedLeave);
            if (checkInStatus == AttendanceStatus.LATE) {
                attendance.updateStatus(AttendanceStatus.LATE, null);
            }

            // 새 레코드일 때만 수강권 1회 차감
            if (!activeEnrollments.isEmpty()) {
                Enrollment enrollment = activeEnrollments.get(0);
                enrollment.useCount();
                log.info("체크인 횟수 차감: student={}, remaining={}/{}", 
                        student.getStudentName(), enrollment.getRemainingCount(), enrollment.getTotalCount());
            }
        }

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
        
        LocalDate today = LocalDate.now();
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
            
            // 오늘 출석 기록 조회
            List<Attendance> todayAttendances = attendanceRepository.findByStudentAndDate(student, today);
            Attendance todayAttendance = todayAttendances.isEmpty() ? null : todayAttendances.get(0);
            
            results.add(StudentSearchResponse.builder()
                    .studentId(student.getId())
                    .studentName(student.getStudentName())
                    .parentName(student.getParentName())
                    .parentPhone(student.getParentPhone())
                    .school(student.getSchool())
                    .courseName(courseName)
                    .isNaverBooking(false)
                    .attendanceId(todayAttendance != null ? todayAttendance.getId() : null)
                    .checkInTime(todayAttendance != null ? todayAttendance.getCheckInTime() : null)
                    .checkOutTime(todayAttendance != null ? todayAttendance.getCheckOutTime() : null)
                    .build());
        }
        
        // 2. 네이버 예약 찾기 (오늘 예약만, 취소 제외)
        var naverBookings = naverBookingRepository.findAll().stream()
                .filter(nb -> {
                    if (nb.getPhone() == null) return false;
                    // 취소된 예약 제외 (취소, RC04)
                    if (nb.getStatus() != null && (nb.getStatus().contains("취소") || nb.getStatus().equals("RC04"))) return false;
                    // 오늘 예약만 (bookingTime에서 날짜 추출)
                    if (nb.getBookingTime() == null) return false;
                    try {
                        String dateStr = nb.getBookingTime().split(" ")[0]; // "2026-02-06 14:00" -> "2026-02-06"
                        LocalDate bookingDate = LocalDate.parse(dateStr);
                        if (!bookingDate.equals(today)) return false;
                    } catch (Exception e) {
                        return false;
                    }
                    String cleanPhone = nb.getPhone().replaceAll("[^0-9]", "");
                    return cleanPhone.length() >= 4 && cleanPhone.substring(cleanPhone.length() - 4).equals(phoneLast4);
                })
                .collect(Collectors.toList());
        
        for (var booking : naverBookings) {
            // 학생명이 없으면 스킵 (예약자명만 있는 경우)
            if (booking.getStudentName() == null || booking.getStudentName().trim().isEmpty()) {
                continue;
            }
            
            // 엑셀에서 반 정보 조회
            String courseName = null;
            String cleanName = booking.getStudentName().trim().replaceAll("\\s+", "");
            courseName = studentCourseExcelService.getCourseName(cleanName);
            
            // 오늘 출석 기록 조회
            List<Attendance> todayAttendances = attendanceRepository.findByNaverBookingAndDate(booking, today);
            Attendance todayAttendance = todayAttendances.isEmpty() ? null : todayAttendances.get(0);
            
            results.add(StudentSearchResponse.builder()
                    .naverBookingId(booking.getId())
                    .studentName(booking.getStudentName())
                    .parentName(booking.getName())
                    .parentPhone(booking.getPhone())
                    .school(booking.getSchool())
                    .courseName(courseName)
                    .isNaverBooking(true)
                    .attendanceId(todayAttendance != null ? todayAttendance.getId() : null)
                    .checkInTime(todayAttendance != null ? todayAttendance.getCheckInTime() : null)
                    .checkOutTime(todayAttendance != null ? todayAttendance.getCheckOutTime() : null)
                    .build());
        }
        
        // 3. 엑셀 학생 중 수동 추가된 출석 레코드 찾기 (위에서 이미 찾은 학생 제외)
        var excelMatches = studentCourseExcelService.findByPhoneLast4(phoneLast4);
        for (var entry : excelMatches) {
            String excelName = entry.getKey();
            // 이미 시스템 학생이나 네이버 예약으로 찾은 이름이면 스킵
            boolean alreadyFound = results.stream().anyMatch(r -> 
                r.getStudentName() != null && r.getStudentName().replaceAll("\\s+", "").equals(excelName));
            if (alreadyFound) continue;
            
            // 오늘 수동 추가된 출석 레코드 찾기
            List<Attendance> manualAttendances = attendanceRepository.findByDate(today).stream()
                    .filter(a -> a.getStudent() == null && a.getNaverBooking() == null 
                            && a.getManualStudentName() != null
                            && a.getManualStudentName().trim().replaceAll("\\s+", "").equals(excelName))
                    .collect(Collectors.toList());
            Attendance manualAtt = manualAttendances.isEmpty() ? null : manualAttendances.get(0);
            
            String courseName = studentCourseExcelService.getCourseName(excelName);
            results.add(StudentSearchResponse.builder()
                    .studentName(excelName)
                    .parentPhone(entry.getValue())
                    .courseName(courseName)
                    .isNaverBooking(false)
                    .isManualExcel(true)
                    .attendanceId(manualAtt != null ? manualAtt.getId() : null)
                    .checkInTime(manualAtt != null ? manualAtt.getCheckInTime() : null)
                    .checkOutTime(manualAtt != null ? manualAtt.getCheckOutTime() : null)
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
        
        // 예상 하원 시간 계산: 실제 등원 시간 + 수업 시간
        LocalTime expectedLeave;
        if (expectedLeaveTime != null) {
            expectedLeave = expectedLeaveTime;
        } else {
            int duration = course != null ? course.getDurationMinutes() : 120;
            expectedLeave = now.toLocalTime().plusMinutes(duration);
        }
        
        // 오늘 날짜에 이미 출석 레코드가 있는지 확인
        List<Attendance> existingAttendances = attendanceRepository.findByDate(today).stream()
                .filter(a -> a.getStudent() != null && a.getStudent().getId().equals(student.getId()))
                .collect(Collectors.toList());
        
        // 예약 시간 기준 출석/지각/결석 판단 (15분 출석, 30분 지각, 이후 결석)
        AttendanceStatus phoneCheckInStatus = AttendanceStatus.PRESENT;
        var phoneReservations = reservationRepository.findByStudentIdAndReservationDate(student.getId(), today);
        if (!phoneReservations.isEmpty()) {
            LocalTime reservedTime = phoneReservations.get(0).getReservationTime();
            long minutesLate = java.time.Duration.between(reservedTime, now.toLocalTime()).toMinutes();
            if (minutesLate > 30) {
                phoneCheckInStatus = AttendanceStatus.ABSENT;
            } else if (minutesLate > 15) {
                phoneCheckInStatus = AttendanceStatus.LATE;
            }
        }

        Attendance attendance;
        if (!existingAttendances.isEmpty()) {
            // 기존 레코드 업데이트
            attendance = existingAttendances.get(0);
            boolean wasAbsent = attendance.getStatus() == AttendanceStatus.ABSENT && attendance.getCheckInTime() == null;
            attendance.checkIn(now, expectedLeave);
            if (wasAbsent) {
                attendance.updateStatus(AttendanceStatus.LATE, null);
                log.info("결석→지각 변경: name={}", student.getStudentName());
            } else if (phoneCheckInStatus == AttendanceStatus.LATE) {
                attendance.updateStatus(AttendanceStatus.LATE, null);
            }
            log.info("Existing attendance updated: name={}, expectedLeave={}", student.getStudentName(), expectedLeave);
        } else {
            // 새 레코드 생성
            attendance = Attendance.builder()
                    .student(student)
                    .course(course)
                    .attendanceDate(today)
                    .attendanceTime(now.toLocalTime())
                    .durationMinutes(course != null ? course.getDurationMinutes() : 120)
                    .status(phoneCheckInStatus)
                    .classCompleted(false)
                    .build();
            attendance.checkIn(now, expectedLeave);
            if (phoneCheckInStatus == AttendanceStatus.LATE) {
                attendance.updateStatus(AttendanceStatus.LATE, null);
            }
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
        
        // 오늘 날짜에 이미 출석 레코드가 있는지 확인
        List<Attendance> existingAttendances = attendanceRepository.findByDate(today).stream()
                .filter(a -> a.getNaverBooking() != null && a.getNaverBooking().getId().equals(naverBooking.getId()))
                .collect(Collectors.toList());
        
        // 예상 하원 시간: 실제 등원 시간 + 수업 시간으로 계산
        LocalTime expectedLeave;
        if (expectedLeaveTime != null) {
            expectedLeave = expectedLeaveTime;
        } else {
            int duration = 120; // 기본 2시간
            if (!existingAttendances.isEmpty() && existingAttendances.get(0).getDurationMinutes() != null) {
                duration = existingAttendances.get(0).getDurationMinutes();
            }
            expectedLeave = now.toLocalTime().plusMinutes(duration);
        }
        
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
        
        // 학부모에게 등원 알림 문자 발송 (네이버 예약)
        try {
            automatedMessageService.sendNaverCheckInNotification(naverBooking, now, expectedLeave);
        } catch (Exception e) {
            log.error("네이버 예약 등원 알림 문자 발송 실패: {}", e.getMessage());
        }
        
        return toResponse(saved);
    }

    @Transactional
    public AttendanceResponse addManualAttendance(String type, Long studentId, String studentName,
            String dateStr, String startTimeStr, int durationMinutes, String courseName) {
        LocalDate date = LocalDate.parse(dateStr);
        LocalTime startTime = LocalTime.parse(startTimeStr);
        LocalTime endTime = startTime.plusMinutes(durationMinutes);
        
        Student student = null;
        web.kplay.studentmanagement.domain.course.Course course = null;
        
        if ("system".equals(type) && studentId != null) {
            student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));
            List<Enrollment> enrollments = enrollmentRepository.findByStudentAndIsActiveTrue(student);
            if (!enrollments.isEmpty()) {
                course = enrollments.get(0).getCourse();
            }
        } else if ("naver".equals(type) && courseName != null) {
            course = courseRepository.findByCourseName(courseName).orElse(null);
        }
        
        Attendance attendance = Attendance.builder()
                .student(student)
                .course(course)
                .attendanceDate(date)
                .attendanceTime(startTime)
                .durationMinutes(durationMinutes)
                .expectedLeaveTime(endTime)
                .originalExpectedLeaveTime(endTime)
                .manualStudentName("naver".equals(type) ? studentName : null)
                .manualParentPhone("naver".equals(type) ? studentCourseExcelService.getParentPhone(studentName) : null)
                .status(AttendanceStatus.NOTYET)
                .classCompleted(false)
                .build();
        
        Attendance saved = attendanceRepository.save(attendance);
        log.info("수동 출석 추가: type={}, name={}, date={}, time={}", type, studentName, date, startTime);
        return toResponse(saved);
    }

    /**
     * 출석 ID로 체크인 (수동 추가 학생용)
     */
    @Transactional
    public AttendanceResponse checkInByAttendanceId(Long attendanceId) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("출석 기록을 찾을 수 없습니다"));

        if (attendance.getCheckInTime() != null) {
            throw new IllegalStateException("이미 출석 체크가 완료되었습니다");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalTime expectedLeave = null;
        if (attendance.getDurationMinutes() != null && attendance.getDurationMinutes() > 0) {
            expectedLeave = now.toLocalTime().plusMinutes(attendance.getDurationMinutes());
        }

        attendance.checkIn(now, expectedLeave);
        log.info("Manual attendance check-in: name={}", attendance.getManualStudentName());

        // 문자 발송
        if (attendance.getManualParentPhone() != null) {
            try {
                automatedMessageService.sendManualCheckInNotification(
                        attendance.getManualStudentName(), attendance.getManualParentPhone(), now, expectedLeave);
            } catch (Exception e) {
                log.error("수동 추가 학생 등원 알림 문자 발송 실패: {}", e.getMessage());
            }
        }

        return toResponse(attendance);
    }

    @Transactional
    public AttendanceResponse checkOut(Long attendanceId) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("출석 기록을 찾을 수 없습니다"));

        LocalDateTime now = LocalDateTime.now();
        attendance.checkOut(now);

        // 하원 알림 발송
        Student student = attendance.getStudent();
        if (student != null) {
            automatedMessageService.sendCheckOutNotification(student, now);
        } else if (attendance.getNaverBooking() != null) {
            try {
                automatedMessageService.sendNaverCheckOutNotification(attendance.getNaverBooking(), now);
            } catch (Exception e) {
                log.error("네이버 예약 하원 알림 문자 발송 실패: {}", e.getMessage());
            }
        } else if (attendance.getManualParentPhone() != null) {
            try {
                automatedMessageService.sendManualCheckOutNotification(
                        attendance.getManualStudentName(), attendance.getManualParentPhone(), now);
            } catch (Exception e) {
                log.error("수동 추가 학생 하원 알림 문자 발송 실패: {}", e.getMessage());
            }
        }

        log.info("Leave check-out: student={}, time={}",
                getStudentName(attendance), now);

        return toResponse(attendance);
    }

    @Transactional
    public AttendanceResponse updateStatus(Long attendanceId, AttendanceStatus status, String reason) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("출석 기록을 찾을 수 없습니다"));

        AttendanceStatus previousStatus = attendance.getStatus();
        attendance.updateStatus(status, reason);
        log.info("Attendance status changed: student={}, previous status={}, new status={}",
                getStudentName(attendance), previousStatus, status);

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
                getStudentName(attendance),
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
                getStudentName(attendance), attendance.getClassCompleted());

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
        log.info("Class completion processed: student={}", getStudentName(attendance));

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
        log.info("Class completion cancelled: student={}", getStudentName(attendance));

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
        log.info("Reason updated: student={}, reason={}", getStudentName(attendance), reason);

        return toResponse(attendance);
    }

    /**
     * 수업 시작/종료 시간 수정 (관리자용)
     */
    @Transactional
    public AttendanceResponse updateClassTime(Long attendanceId, String startTimeStr, String endTimeStr) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("출석 기록을 찾을 수 없습니다"));

        if (startTimeStr != null && !startTimeStr.isEmpty()) {
            LocalTime startTime = LocalTime.parse(startTimeStr);
            attendance.updateAttendanceTime(startTime);
        }
        if (endTimeStr != null && !endTimeStr.isEmpty()) {
            LocalTime endTime = LocalTime.parse(endTimeStr);
            LocalTime startTime = attendance.getAttendanceTime();
            int duration = (int) java.time.Duration.between(startTime, endTime).toMinutes();
            attendance.updateDurationMinutes(duration);
        }

        log.info("Class time updated: student={}, start={}, end={}",
                getStudentName(attendance), startTimeStr, endTimeStr);
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
        boolean isManual = !isNaver && student == null && (attendance.getCourse() != null || attendance.getManualStudentName() != null);
        
        // student가 null이고 네이버도 아니고 수동추가도 아니면 필터링
        if (!isNaver && student == null && !isManual) {
            return null;
        }
        
        String studentName;
        String courseName = "없음";
        String assignedClassInitials = null;
        
        if (isNaver) {
            NaverBooking naverBooking = attendance.getNaverBooking();
            studentName = naverBooking.getStudentName() != null ? naverBooking.getStudentName() : naverBooking.getName();
            if (naverBooking.getStudentName() != null) {
                String cleanName = naverBooking.getStudentName().trim().replaceAll("\\s+", "");
                String excelCourseName = studentCourseExcelService.getCourseName(cleanName);
                if (excelCourseName != null) courseName = excelCourseName;
                assignedClassInitials = additionalClassExcelService.getAssignedClassInitials(cleanName);
            }
        } else if (student != null) {
            studentName = student.getStudentName();
            courseName = attendance.getCourse() != null ? attendance.getCourse().getCourseName() : "없음";
            assignedClassInitials = student.getAssignedClassInitials();
        } else {
            // 수동 추가 (네이버 엑셀 학생)
            studentName = attendance.getManualStudentName() != null ? attendance.getManualStudentName() : "수동 추가";
            if (attendance.getCourse() != null) {
                courseName = attendance.getCourse().getCourseName();
            } else if (attendance.getManualStudentName() != null) {
                String cleanName = attendance.getManualStudentName().trim().replaceAll("\\s+", "");
                String excelCourseName = studentCourseExcelService.getCourseName(cleanName);
                if (excelCourseName != null) courseName = excelCourseName;
            }
        }
        
        // 추가수업 시간 계산
        LocalTime additionalClassTime = null;
        if (assignedClassInitials != null && attendance.getCheckInTime() != null) {
            additionalClassTime = attendance.getCheckInTime().toLocalTime().plusMinutes(30);
        }
        
        String rawPhone = isNaver ? attendance.getNaverBooking().getPhone() 
            : (student != null ? student.getParentPhone() : attendance.getManualParentPhone());
        String maskedPhone = maskPhone(rawPhone);
        
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .studentId(student != null ? student.getId() : null)
                .studentName(studentName)
                .parentPhone(maskedPhone)
                .className(isNaver ? "네이버 예약" : (isManual ? "네이버 예약" : (attendance.getCourse() != null ? attendance.getCourse().getCourseName() : null)))
                .isNaverBooking(isNaver)
                .courseName(courseName)
                .startTime(attendance.getAttendanceTime().toString())
                .endTime(attendance.getDurationMinutes() != null 
                    ? attendance.getAttendanceTime().plusMinutes(attendance.getDurationMinutes()).toString()
                    : (attendance.getCourse() != null && attendance.getCourse().getDurationMinutes() != null
                        ? attendance.getAttendanceTime().plusMinutes(attendance.getCourse().getDurationMinutes()).toString()
                        : null))
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
                .assignedClassInitials(assignedClassInitials)
                .additionalClassTime(additionalClassTime)
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
        log.info("D/C check updated: student={}, dcCheck={}", getStudentName(attendance), dcCheck);

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
        log.info("WR check updated: student={}, wrCheck={}", getStudentName(attendance), wrCheck);

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
                getStudentName(attendance), attendance.getVocabularyClass());

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
                getStudentName(attendance), attendance.getGrammarClass());

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
                getStudentName(attendance), attendance.getPhonicsClass());

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
                getStudentName(attendance), attendance.getSpeakingClass());

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
            
            // 수업시간을 알면 하원예정시간 계산, 모르면 null
            LocalTime newExpectedLeaveTime = null;
            if (attendance.getDurationMinutes() != null && attendance.getDurationMinutes() > 0) {
                newExpectedLeaveTime = now.toLocalTime().plusMinutes(attendance.getDurationMinutes());
            }
            
            attendance.checkIn(now, newExpectedLeaveTime);
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

        // 오늘 출석 기록이 있는지 확인
        List<Attendance> existingAttendances = attendanceRepository.findByNaverBookingAndDate(naverBooking, today);
        
        // 출석 기록이 없으면 체크인 불가
        if (existingAttendances.isEmpty()) {
            throw new IllegalStateException("오늘 예약된 수업이 없습니다");
        }
        
        // 이미 출석 체크 완료된 경우
        for (Attendance att : existingAttendances) {
            if (att.getCheckInTime() != null) {
                throw new IllegalStateException("이미 출석 체크가 완료되었습니다");
            }
        }

        // 기존 출석 기록에 체크인 처리
        Attendance attendance = existingAttendances.get(0);
        
        // 엑셀에서 반 이름 조회 후 DB Course에서 수업시간 가져오기
        LocalTime newExpectedLeaveTime = null;
        if (naverBooking.getStudentName() != null) {
            String cleanName = naverBooking.getStudentName().trim().replaceAll("\\s+", "");
            String courseName = studentCourseExcelService.getCourseName(cleanName);
            if (courseName != null) {
                var course = courseRepository.findByCourseName(courseName);
                if (course.isPresent() && course.get().getDurationMinutes() != null) {
                    newExpectedLeaveTime = now.toLocalTime().plusMinutes(course.get().getDurationMinutes());
                }
            }
        }
        
        attendance.checkIn(now, newExpectedLeaveTime);
        log.info("Naver booking check-in: name={}", naverBooking.getStudentName());
        
        // 학부모에게 등원 알림 문자 발송
        try {
            automatedMessageService.sendNaverCheckInNotification(naverBooking, now, newExpectedLeaveTime);
        } catch (Exception e) {
            log.error("네이버 예약 등원 알림 문자 발송 실패: {}", e.getMessage());
        }
        
        return toResponse(attendance);
    }

    // 전화번호 마스킹 (01012345678 -> 010****5678)
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) return phone;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 8) return phone;
        return digits.substring(0, 3) + "****" + digits.substring(digits.length() - 4);
    }

    // 학생 이름 조회 (네이버 예약은 NaverBooking에서, 시스템 학생은 Student에서)
    private String getStudentName(Attendance attendance) {
        if (attendance.getStudent() != null) {
            return attendance.getStudent().getStudentName();
        } else if (attendance.getNaverBooking() != null) {
            return attendance.getNaverBooking().getStudentName();
        }
        return "Unknown";
    }
}
