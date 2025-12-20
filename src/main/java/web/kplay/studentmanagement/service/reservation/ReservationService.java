package web.kplay.studentmanagement.service.reservation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.course.CourseSchedule;
import web.kplay.studentmanagement.domain.course.Enrollment;
import web.kplay.studentmanagement.domain.reservation.Reservation;
import web.kplay.studentmanagement.domain.reservation.ReservationStatus;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.dto.reservation.ReservationCreateRequest;
import web.kplay.studentmanagement.dto.reservation.ReservationResponse;
import web.kplay.studentmanagement.exception.BusinessException;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.CourseScheduleRepository;
import web.kplay.studentmanagement.repository.EnrollmentRepository;
import web.kplay.studentmanagement.repository.ReservationRepository;
import web.kplay.studentmanagement.repository.StudentRepository;

import java.time.LocalDate;
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

    @Transactional
    public ReservationResponse createReservation(ReservationCreateRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));

        CourseSchedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("수업 스케줄을 찾을 수 없습니다"));

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
        }

        // 수업 정원 체크
        if (schedule.getCurrentStudents() >= schedule.getCourse().getMaxStudents()) {
            throw new BusinessException("수업 정원이 가득 찼습니다");
        }

        Reservation reservation = Reservation.builder()
                .student(student)
                .schedule(schedule)
                .enrollment(enrollment)
                .status(ReservationStatus.CONFIRMED)
                .memo(request.getMemo())
                .reservationSource(request.getReservationSource())
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);

        // 스케줄에 학생 수 증가
        schedule.addStudent();

        log.info("예약 생성: 학생={}, 수업={}, 날짜={}",
                student.getStudentName(),
                schedule.getCourse().getCourseName(),
                schedule.getScheduleDate());

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
        log.info("예약 확정: 학생={}, 수업={}",
                reservation.getStudent().getStudentName(),
                reservation.getSchedule().getCourse().getCourseName());

        return toResponse(reservation);
    }

    @Transactional
    public void deleteReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("예약을 찾을 수 없습니다"));

        // 스케줄에서 학생 수 감소
        reservation.getSchedule().removeStudent();

        // 실제로 DB에서 삭제
        reservationRepository.delete(reservation);

        log.info("예약 삭제: 학생={}, 수업={}",
                reservation.getStudent().getStudentName(),
                reservation.getSchedule().getCourse().getCourseName());
    }

    @Transactional
    public void cancelReservation(Long id, String reason) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("예약을 찾을 수 없습니다"));

        // 예약 취소 (전날 오후 6시까지만 가능)
        reservation.cancel(reason);

        // 스케줄에서 학생 수 감소
        reservation.getSchedule().removeStudent();

        log.info("예약 취소: 학생={}, 수업={}, 사유={}",
                reservation.getStudent().getStudentName(),
                reservation.getSchedule().getCourse().getCourseName(),
                reason);
    }

    @Transactional
    public void forceCancelReservation(Long id, String reason) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("예약을 찾을 수 없습니다"));

        // 관리자 강제 취소 (시간 제한 없음)
        reservation.forceCancel(reason);

        // 스케줄에서 학생 수 감소
        reservation.getSchedule().removeStudent();

        log.info("예약 강제 취소 (관리자): 학생={}, 수업={}, 사유={}",
                reservation.getStudent().getStudentName(),
                reservation.getSchedule().getCourse().getCourseName(),
                reason);
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

    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsBySchedule(Long scheduleId) {
        return reservationRepository.findByScheduleId(scheduleId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .studentId(reservation.getStudent().getId())
                .studentName(reservation.getStudent().getStudentName())
                .scheduleId(reservation.getSchedule().getId())
                .courseName(reservation.getSchedule().getCourse().getCourseName())
                .scheduleDate(reservation.getSchedule().getScheduleDate())
                .startTime(reservation.getSchedule().getStartTime())
                .endTime(reservation.getSchedule().getEndTime())
                .enrollmentId(reservation.getEnrollment() != null ? reservation.getEnrollment().getId() : null)
                .status(reservation.getStatus())
                .memo(reservation.getMemo())
                .cancelReason(reservation.getCancelReason())
                .cancelledAt(reservation.getCancelledAt())
                .reservationSource(reservation.getReservationSource())
                .canCancel(reservation.canCancel())
                .build();
    }
}
