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

    @Transactional
    public ReservationResponse createReservation(ReservationCreateRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));

        // 날짜/시간 검증
        if ("재원생상담".equals(request.getConsultationType())) {
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

            // 수강권 횟수 차감
            enrollment.useCount();
        } else {
            // enrollmentId가 없으면 학생의 활성 수강권 자동 조회
            List<Enrollment> activeEnrollments = enrollmentRepository.findByStudentIdAndIsActive(student.getId(), true);
            if (!activeEnrollments.isEmpty()) {
                enrollment = activeEnrollments.get(0); // 첫 번째 활성 수강권 사용
                log.info("Auto-selected enrollment: {}", enrollment.getId());
                
                // 수강권 횟수 차감
                enrollment.useCount();
            }
        }

        // 같은 날짜/시간 중복 체크
        List<Reservation> existingReservations = reservationRepository
            .findByReservationDateAndReservationTime(request.getReservationDate(), request.getReservationTime());
        if (!existingReservations.isEmpty()) {
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
        List<Reservation> reservations = reservationRepository.findByDateAndStatuses(
            date, List.of(ReservationStatus.CONFIRMED, ReservationStatus.PENDING));
        
        return reservations.stream()
                .map(reservation -> reservation.getReservationTime().toString().substring(0, 5))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 특정 날짜와 상담 유형의 예약된 시간 목록 조회
     */
    public List<String> getReservedTimesByDateAndType(LocalDate date, String consultationType) {
        log.info("예약된 시간 조회 - 날짜: {}, 유형: {}", date, consultationType);
        List<Reservation> reservations = reservationRepository.findByDateAndStatuses(
            date, List.of(ReservationStatus.CONFIRMED, ReservationStatus.PENDING));
        
        // 상담 유형 필터링
        reservations = reservations.stream()
            .filter(r -> consultationType.equals(r.getConsultationType()))
            .collect(Collectors.toList());
            
        log.info("조회된 예약 수: {}", reservations.size());
        
        reservations.forEach(r -> log.info("예약 정보 - 시간: {}, 유형: {}", 
            r.getReservationTime(), r.getConsultationType()));
        
        return reservations.stream()
                .map(reservation -> reservation.getReservationTime().toString().substring(0, 5))
                .distinct()
                .collect(Collectors.toList());
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
            // 오늘 생성된 모든 예약을 반환 (간단한 구현)
            LocalDate today = LocalDate.now();
            List<Reservation> reservations = reservationRepository.findAll().stream()
                    .filter(r -> r.getCreatedAt().toLocalDate().equals(today))
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .collect(Collectors.toList());
                    
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
