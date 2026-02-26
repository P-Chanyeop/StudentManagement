package web.kplay.studentmanagement.service.reservation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.attendance.Attendance;
import web.kplay.studentmanagement.domain.attendance.AttendanceStatus;
import web.kplay.studentmanagement.domain.course.CourseSchedule;
import web.kplay.studentmanagement.domain.course.Enrollment;
import web.kplay.studentmanagement.domain.reservation.Reservation;
import web.kplay.studentmanagement.domain.reservation.ReservationStatus;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.dto.reservation.ReservationCreateRequest;
import web.kplay.studentmanagement.dto.reservation.ReservationResponse;
import web.kplay.studentmanagement.exception.BusinessException;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.AttendanceRepository;
import web.kplay.studentmanagement.repository.CourseScheduleRepository;
import web.kplay.studentmanagement.repository.EnrollmentRepository;

import java.time.LocalDate;
import java.util.stream.Collectors;
import web.kplay.studentmanagement.repository.ReservationRepository;
import web.kplay.studentmanagement.repository.StudentRepository;
import web.kplay.studentmanagement.repository.UserRepository;
import web.kplay.studentmanagement.domain.user.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final StudentRepository studentRepository;
    private final CourseScheduleRepository scheduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final web.kplay.studentmanagement.service.message.AutomatedMessageService automatedMessageService;
    private final web.kplay.studentmanagement.repository.BlockedTimeSlotRepository blockedTimeSlotRepository;
    private final web.kplay.studentmanagement.service.notification.AdminNotificationService adminNotificationService;

    private static final int MAX_RESERVATIONS_PER_SLOT = 9;

    @Transactional
    public ReservationResponse createReservation(ReservationCreateRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));

        // 날짜/시간 검증
        if ("재원생수업".equals(request.getConsultationType())) {
            validateReservationDateTime(request.getReservationDate());
        }

        Enrollment enrollment = null;
        if (request.getEnrollmentId() != null) {
            enrollment = enrollmentRepository.findById(request.getEnrollmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("수강권을 찾을 수 없습니다"));

            // 수강권 유효성 검증
            if (!enrollment.isValid()) {
                throw new BusinessException("유효하지 않은 수강권입니다");
            }

            // 홀딩 기간 체크
            if (enrollment.getIsOnHold() != null && enrollment.getIsOnHold()) {
                LocalDate holdEnd = enrollment.getHoldEndDate();
                if (holdEnd != null && !request.getReservationDate().isAfter(holdEnd)) {
                    throw new BusinessException("홀딩 기간 중에는 예약할 수 없습니다. " + 
                            (holdEnd.getMonthValue()) + "/" + holdEnd.getDayOfMonth() + " 이후부터 예약 가능합니다.");
                }
            }

            // 수강권 횟수 차감
            enrollment.useCount();
        } else {
            // enrollmentId가 없으면 학생의 활성 수강권 자동 조회
            List<Enrollment> activeEnrollments = enrollmentRepository.findByStudentIdAndIsActive(student.getId(), true);
            if (!activeEnrollments.isEmpty()) {
                enrollment = activeEnrollments.get(0); // 첫 번째 활성 수강권 사용
                log.info("Auto-selected enrollment: {}", enrollment.getId());
                
                // 홀딩 기간 체크
                if (enrollment.getIsOnHold() != null && enrollment.getIsOnHold()) {
                    LocalDate holdEnd = enrollment.getHoldEndDate();
                    if (holdEnd != null && !request.getReservationDate().isAfter(holdEnd)) {
                        throw new BusinessException("홀딩 기간 중에는 예약할 수 없습니다. " + 
                                (holdEnd.getMonthValue()) + "/" + holdEnd.getDayOfMonth() + " 이후부터 예약 가능합니다.");
                    }
                }
                
                // 수강권 횟수 차감
                enrollment.useCount();
            } else if ("재원생수업".equals(request.getConsultationType())) {
                throw new BusinessException("활성 수강권이 없습니다. 수강권 등록 후 예약해주세요.");
            }
        }

        // 차단된 시간 체크
        String timeStr = request.getReservationTime().toString().substring(0, 5);
        List<String> unavailable = getUnavailableTimes(request.getReservationDate(), request.getConsultationType());
        if (unavailable.contains(timeStr)) {
            throw new BusinessException("해당 시간은 예약이 불가능합니다 (차단 또는 만석)");
        }

        // 같은 학생 같은 날짜/시간 중복 체크
        List<Reservation> existingReservations = reservationRepository
            .findByReservationDateAndReservationTime(request.getReservationDate(), request.getReservationTime());
        boolean hasDuplicate = existingReservations.stream()
            .anyMatch(r -> r.getStudent().getId().equals(request.getStudentId()) &&
                    (r.getStatus() == ReservationStatus.CONFIRMED || r.getStatus() == ReservationStatus.PENDING));
        if (hasDuplicate) {
            throw new BusinessException("해당 시간에 이미 예약이 있습니다");
        }

        Reservation reservation = Reservation.builder()
                .student(student)
                .reservationDate(request.getReservationDate())
                .reservationTime(request.getReservationTime())
                .enrollment(enrollment)
                .status(ReservationStatus.CONFIRMED)
                .memo(request.getMemo())
                .consultationType(request.getConsultationType())
                .reservationSource(request.getReservationSource())
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);

        // 예약 생성 시 출석 레코드도 자동 생성
        createAttendanceRecord(savedReservation);

        // 관리자 알림 생성
        try {
            boolean isClass = "재원생수업".equals(request.getConsultationType()) || "레벨테스트".equals(request.getConsultationType());
            String type = isClass ? "RESERVATION" : "CONSULTATION";
            String title = isClass ? "새로운 수업 예약" : "새로운 상담 예약";
            String label = isClass ? "수업" : "상담";
            LocalTime time = request.getReservationTime();
            String content = String.format("%s님이 %d년 %02d월 %02d일 %02d시 %02d분으로 %s 예약하였습니다.",
                    student.getStudentName(),
                    request.getReservationDate().getYear(),
                    request.getReservationDate().getMonthValue(),
                    request.getReservationDate().getDayOfMonth(),
                    time.getHour(), time.getMinute(),
                    label);
            adminNotificationService.createNotification(type, title, content, savedReservation.getId());
        } catch (Exception e) {
            log.error("관리자 알림 생성 실패: {}", e.getMessage());
        }

        log.info("예약 생성: 학생={}, 날짜={}, 시간={}",
                student.getStudentName(),
                request.getReservationDate(),
                request.getReservationTime());

        // 학부모에게 예약 확인 문자 발송
        try {
            automatedMessageService.sendReservationNotification(savedReservation);
        } catch (Exception e) {
            log.error("예약 알림 문자 발송 실패: {}", e.getMessage());
        }

        return toResponse(savedReservation);
    }

    @Transactional
    public ReservationResponse confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("예약을 찾을 수 없습니다"));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException("대기 중인 예약만 확정할 수 있습니다");
        }

        reservation.confirm();
        log.info("예약 확정: 학생={}, 날짜={}, 시간={}",
                reservation.getStudent().getStudentName(),
                reservation.getReservationDate(),
                reservation.getReservationTime());

        return toResponse(reservation);
    }

    @Transactional
    public void deleteReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("예약을 찾을 수 없습니다"));

        // 수강권 횟수 복원
        if (reservation.getEnrollment() != null) {
            reservation.getEnrollment().restoreCount();
        }

        // 실제로 DB에서 삭제
        reservationRepository.delete(reservation);

        log.info("예약 삭제: 학생={}, 날짜={}, 시간={}, 수강권 복원={}",
                reservation.getStudent().getStudentName(),
                reservation.getReservationDate(),
                reservation.getReservationTime(),
                reservation.getEnrollment() != null);
    }

    @Transactional
    public void cancelReservation(Long id, String reason) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("예약을 찾을 수 없습니다"));

        // 수강권 횟수 복원 (상태 변경 전에 먼저 수행 - race condition 방지)
        if (reservation.getEnrollment() != null) {
            reservation.getEnrollment().restoreCount();
        }

        // 예약 취소 (전날 오후 6시까지만 가능)
        reservation.cancel(reason);

        // 해당 예약의 출석 레코드 삭제
        deleteAttendanceRecord(reservation);

        log.info("예약 취소: 학생={}, 날짜={}, 시간={}, 사유={}, 수강권 복원={}",
                reservation.getStudent().getStudentName(),
                reservation.getReservationDate(),
                reservation.getReservationTime(),
                reason,
                reservation.getEnrollment() != null);
    }

    @Transactional
    public void forceCancelReservation(Long id, String reason) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("예약을 찾을 수 없습니다"));

        // 수강권 횟수 복원 (상태 변경 전에 먼저 수행 - race condition 방지)
        if (reservation.getEnrollment() != null) {
            reservation.getEnrollment().restoreCount();
        }

        // 관리자 강제 취소 (시간 제한 없음)
        reservation.forceCancel(reason);

        // 해당 예약의 출석 레코드 삭제
        deleteAttendanceRecord(reservation);

        log.info("예약 강제 취소 (관리자): 학생={}, 날짜={}, 시간={}, 사유={}, 수강권 복원={}",
                reservation.getStudent().getStudentName(),
                reservation.getReservationDate(),
                reservation.getReservationTime(),
                reason,
                reservation.getEnrollment() != null);
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("예약을 찾을 수 없습니다"));
        return toResponse(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByStudent(Long studentId) {
        return reservationRepository.findByStudentId(studentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByDate(LocalDate date) {
        List<ReservationStatus> activeStatuses = List.of(
                ReservationStatus.CONFIRMED,
                ReservationStatus.PENDING
        );
        return reservationRepository.findByDateAndStatuses(date, activeStatuses).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ReservationResponse toResponse(Reservation reservation) {
        String courseName = null;
        if (reservation.getEnrollment() != null && reservation.getEnrollment().getCourse() != null) {
            courseName = reservation.getEnrollment().getCourse().getCourseName();
        }
        
        return ReservationResponse.builder()
                .id(reservation.getId())
                .studentId(reservation.getStudent().getId())
                .studentName(reservation.getStudent().getStudentName())
                .studentEnglishLevel(reservation.getStudent().getEnglishLevel())
                .courseName(courseName)
                .reservationDate(reservation.getReservationDate())
                .reservationTime(reservation.getReservationTime())
                .enrollmentId(reservation.getEnrollment() != null ? reservation.getEnrollment().getId() : null)
                .status(reservation.getStatus())
                .memo(reservation.getMemo())
                .consultationType(reservation.getConsultationType())
                .cancelReason(reservation.getCancelReason())
                .cancelledAt(reservation.getCancelledAt())
                .reservationSource(reservation.getReservationSource())
                .canCancel(reservation.canCancel())
                .build();
    }

    /**
     * 재원생 상담 예약 날짜/시간 검증
     * 
     * @param reservationDate 예약하려는 날짜
     * @throws BusinessException 예약 불가능한 날짜/시간인 경우
     */
    private void validateReservationDateTime(LocalDate reservationDate) {
        LocalDate today = LocalDate.now();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        int currentHour = now.getHour();
        
        // 당일 예약 불가
        if (reservationDate.equals(today)) {
            throw new BusinessException("재원생 상담은 당일 예약이 불가능합니다.");
        }
        
        // 오늘 18시가 지났다면 내일도 예약 불가
        if (currentHour >= 18) {
            LocalDate tomorrow = today.plusDays(1);
            if (reservationDate.equals(tomorrow)) {
                throw new BusinessException("오후 6시 이후에는 다음날 예약이 불가능합니다. 다다음날부터 예약 가능합니다.");
            }
        }
        
        // 과거 날짜 예약 불가
        if (reservationDate.isBefore(today)) {
            throw new BusinessException("과거 날짜로는 예약할 수 없습니다.");
        }
        
        log.info("Reservation date validation passed for date: {}, current time: {}", reservationDate, now);
    }

    /**
     * 특정 날짜의 모든 예약된 시간 목록 조회
     */
    public List<String> getReservedTimesByDate(LocalDate date) {
        return getUnavailableTimes(date, null);
    }

    /**
     * 특정 날짜와 상담 유형의 예약 불가 시간 목록 조회
     * (차단된 시간 + 9명 만석 시간)
     */
    public List<String> getReservedTimesByDateAndType(LocalDate date, String consultationType) {
        return getUnavailableTimes(date, consultationType);
    }

    private List<String> getUnavailableTimes(LocalDate date, String consultationType) {
        // 1. 차단된 시간 (consultationType에 따라 targetType 필터)
        String targetType = "상담".equals(consultationType) ? "CONSULTATION" : "CLASS";
        var blocks = blockedTimeSlotRepository.findActiveBlocksForDateAndType(date, date.getDayOfWeek(), targetType);
        java.util.Set<String> unavailable = blocks.stream()
                .map(b -> b.getBlockTime().toString().substring(0, 5))
                .collect(java.util.stream.Collectors.toCollection(java.util.HashSet::new));

        // 2. 9명 만석 시간
        List<Reservation> reservations = reservationRepository.findByDateAndStatuses(
                date, List.of(web.kplay.studentmanagement.domain.reservation.ReservationStatus.CONFIRMED,
                        web.kplay.studentmanagement.domain.reservation.ReservationStatus.PENDING));
        if (consultationType != null) {
            reservations = reservations.stream()
                    .filter(r -> consultationType.equals(r.getConsultationType()))
                    .collect(java.util.stream.Collectors.toList());
        }
        reservations.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        r -> r.getReservationTime().toString().substring(0, 5),
                        java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() >= MAX_RESERVATIONS_PER_SLOT)
                .forEach(e -> unavailable.add(e.getKey()));

        return new java.util.ArrayList<>(unavailable);
    }

    /**
     * 시간대별 상태 정보 (차단/예약수/잔여석)
     */
    public List<java.util.Map<String, Object>> getTimeSlotStatus(LocalDate date, String consultationType) {
        String[] slots = {"09:00","10:00","11:00","12:00","13:00","14:00","15:00","16:00","17:00","18:00","19:00","20:00"};

        // 차단된 시간 (consultationType에 따라 targetType 필터)
        String targetType = "상담".equals(consultationType) ? "CONSULTATION" : "CLASS";
        var blocks = blockedTimeSlotRepository.findActiveBlocksForDateAndType(date, date.getDayOfWeek(), targetType);
        java.util.Set<String> blockedTimes = blocks.stream()
                .map(b -> b.getBlockTime().toString().substring(0, 5))
                .collect(java.util.stream.Collectors.toSet());

        // 시간별 예약 수
        List<Reservation> reservations = reservationRepository.findByDateAndStatuses(
                date, List.of(ReservationStatus.CONFIRMED, ReservationStatus.PENDING));
        if (consultationType != null) {
            reservations = reservations.stream()
                    .filter(r -> consultationType.equals(r.getConsultationType()))
                    .collect(java.util.stream.Collectors.toList());
        }
        var countMap = reservations.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        r -> r.getReservationTime().toString().substring(0, 5),
                        java.util.stream.Collectors.counting()));

        List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (String slot : slots) {
            java.util.Map<String, Object> info = new java.util.LinkedHashMap<>();
            info.put("time", slot);
            int count = countMap.getOrDefault(slot, 0L).intValue();
            boolean blocked = blockedTimes.contains(slot);
            info.put("reserved", count);
            info.put("remaining", blocked ? 0 : MAX_RESERVATIONS_PER_SLOT - count);
            info.put("status", blocked ? "BLOCKED" : count >= MAX_RESERVATIONS_PER_SLOT ? "FULL" : "AVAILABLE");
            result.add(info);
        }
        return result;
    }

    /**
     * 학부모의 자녀 예약 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyReservations(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        // 학부모의 자녀 목록 조회 (부모 전화번호로 매칭)
        List<Student> myStudents = studentRepository.findByParentPhoneAndIsActive(user.getPhoneNumber(), true);
        
        if (myStudents.isEmpty()) {
            return List.of();
        }
        
        // 자녀들의 예약 목록 조회
        List<Long> studentIds = myStudents.stream()
                .map(Student::getId)
                .collect(Collectors.toList());
        
        List<Reservation> reservations = reservationRepository.findByStudentIdInOrderByReservationDateDesc(studentIds);
        
        return reservations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 특정 시간 이후의 새로운 예약 조회 (관리자용 알림)
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getNewReservationsSince(String since) {
        try {
            // ISO 8601 형식 (Z 포함) 파싱
            java.time.Instant instant = java.time.Instant.parse(since);
            LocalDateTime sinceTime = LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
            log.info("since 시간 이후 예약 조회: {}", sinceTime);
            
            List<Reservation> reservations = reservationRepository.findAll().stream()
                    .filter(r -> r.getCreatedAt().isAfter(sinceTime))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .collect(Collectors.toList());
            
            log.info("since 이후 예약 개수: {}", reservations.size());
                    
            return reservations.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting new reservations: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 예약에 대한 출석 레코드 생성
     */
    private void createAttendanceRecord(Reservation reservation) {
        try {
            // 학생 정보가 없으면 출석 레코드 생성하지 않음
            if (reservation.getStudent() == null) {
                log.warn("Cannot create attendance record: reservation has no student. reservationId={}", reservation.getId());
                return;
            }
            
            // 학생의 활성 수강권에서 Course 정보 가져오기
            web.kplay.studentmanagement.domain.course.Course course = null;
            if (reservation.getEnrollment() != null) {
                course = reservation.getEnrollment().getCourse();
            }
            
            // 예상 하원 시간 계산
            LocalTime expectedLeave = null;
            if (course != null) {
                expectedLeave = reservation.getReservationTime().plusMinutes(course.getDurationMinutes());
            } else {
                expectedLeave = reservation.getReservationTime().plusHours(2); // 기본 2시간
            }
            
            Attendance attendance = Attendance.builder()
                    .student(reservation.getStudent())
                    .course(course)
                    .attendanceDate(reservation.getReservationDate())
                    .attendanceTime(reservation.getReservationTime())
                    .durationMinutes(course != null ? course.getDurationMinutes() : 120) // 기본 2시간
                    .expectedLeaveTime(expectedLeave)
                    .originalExpectedLeaveTime(expectedLeave)
                    .status(AttendanceStatus.NOTYET) // 초기 상태는 미출석
                    .build();
            
            attendanceRepository.save(attendance);
            log.info("Attendance record created for reservation: student={}, date={}, time={}, course={}, expectedLeave={}", 
                    reservation.getStudent().getStudentName(), 
                    reservation.getReservationDate(),
                    reservation.getReservationTime(),
                    course != null ? course.getCourseName() : "없음",
                    expectedLeave);
        } catch (Exception e) {
            log.warn("Failed to create attendance record for reservation: {}", e.getMessage());
        }
    }

    /**
     * 예약 취소 시 출석 레코드 삭제
     */
    private void deleteAttendanceRecord(Reservation reservation) {
        try {
            if (reservation.getStudent() == null) {
                return;
            }

            // 해당 예약의 출석 레코드 찾기 (날짜와 시간으로)
            List<Attendance> attendances = attendanceRepository.findByDate(reservation.getReservationDate());

            // 같은 학생, 같은 시간의 출석 레코드 삭제
            attendances.stream()
                    .filter(a -> a.getStudent() != null && 
                                 a.getStudent().getId().equals(reservation.getStudent().getId()) &&
                                 a.getAttendanceTime().equals(reservation.getReservationTime()))
                    .forEach(a -> {
                        attendanceRepository.delete(a);
                        log.info("Attendance record deleted for cancelled reservation: student={}, date={}, time={}",
                                reservation.getStudent().getStudentName(),
                                reservation.getReservationDate(),
                                reservation.getReservationTime());
                    });
        } catch (Exception e) {
            log.warn("Failed to delete attendance record for reservation: {}", e.getMessage());
        }
    }
}
