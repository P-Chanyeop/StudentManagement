package web.kplay.studentmanagement.service.mypage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.attendance.AttendanceStatus;
import web.kplay.studentmanagement.domain.reservation.ReservationStatus;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.dto.attendance.AttendanceResponse;
import web.kplay.studentmanagement.dto.consultation.ConsultationResponse;
import web.kplay.studentmanagement.dto.course.EnrollmentResponse;
import web.kplay.studentmanagement.dto.leveltest.LevelTestResponse;
import web.kplay.studentmanagement.dto.message.MessageResponse;
import web.kplay.studentmanagement.dto.mypage.MyPageResponse;
import web.kplay.studentmanagement.dto.reservation.ReservationResponse;
import web.kplay.studentmanagement.dto.student.StudentResponse;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final ReservationRepository reservationRepository;
    private final LevelTestRepository levelTestRepository;
    private final MessageRepository messageRepository;
    private final ConsultationRepository consultationRepository;

    /**
     * 학생 ID로 마이페이지 정보 조회
     */
    public MyPageResponse getMyPageByStudentId(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));

        return buildMyPageResponse(student);
    }

    /**
     * User ID로 마이페이지 정보 조회 (로그인한 사용자)
     */
    public MyPageResponse getMyPageByUserId(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("학생 정보를 찾을 수 없습니다"));

        return buildMyPageResponse(student);
    }

    /**
     * MyPageResponse 생성
     */
    private MyPageResponse buildMyPageResponse(Student student) {
        // 학생 기본 정보
        StudentResponse studentInfo = toStudentResponse(student);

        // 활성화된 수강권 목록
        List<EnrollmentResponse> activeEnrollments = enrollmentRepository
                .findByStudentIdAndIsActiveTrue(student.getId())
                .stream()
                .map(this::toEnrollmentResponse)
                .collect(Collectors.toList());

        // 최근 출석 기록 (최근 10개)
        List<AttendanceResponse> recentAttendances = attendanceRepository
                .findTop10ByStudentIdOrderByCheckInTimeDesc(student.getId())
                .stream()
                .map(this::toAttendanceResponse)
                .collect(Collectors.toList());

        // 예정된 예약 (오늘 이후)
        List<ReservationResponse> upcomingReservations = reservationRepository
                .findByStudentIdAndScheduleDateAfter(student.getId(), LocalDate.now())
                .stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                             r.getStatus() == ReservationStatus.PENDING)
                .map(this::toReservationResponse)
                .collect(Collectors.toList());

        // 예정된 레벨테스트 (오늘 이후)
        List<LevelTestResponse> upcomingLevelTests = levelTestRepository
                .findByStudentIdAndTestDateAfter(student.getId(), LocalDate.now())
                .stream()
                .map(this::toLevelTestResponse)
                .collect(Collectors.toList());

        // 최근 받은 문자 메시지 (최근 20개)
        List<MessageResponse> recentMessages = messageRepository
                .findTop20ByStudentIdOrderByCreatedAtDesc(student.getId())
                .stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());

        // 최근 상담 기록 (최근 5개)
        List<ConsultationResponse> recentConsultations = consultationRepository
                .findTop5ByStudentIdOrderByConsultationDateDesc(student.getId())
                .stream()
                .map(this::toConsultationResponse)
                .collect(Collectors.toList());

        // 통계 정보 생성
        MyPageResponse.MyPageStats stats = buildStats(student.getId());

        return MyPageResponse.builder()
                .studentInfo(studentInfo)
                .activeEnrollments(activeEnrollments)
                .recentAttendances(recentAttendances)
                .upcomingReservations(upcomingReservations)
                .upcomingLevelTests(upcomingLevelTests)
                .recentMessages(recentMessages)
                .recentConsultations(recentConsultations)
                .stats(stats)
                .build();
    }

    /**
     * 통계 정보 생성
     */
    private MyPageResponse.MyPageStats buildStats(Long studentId) {
        // 총 출석 횟수
        Long totalAttendanceCount = attendanceRepository
                .countByStudentIdAndStatus(studentId, AttendanceStatus.PRESENT);

        // 이번 달 출석 횟수
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        Long monthlyAttendanceCount = attendanceRepository
                .countByStudentIdAndCheckInTimeBetween(studentId, monthStart, monthEnd);

        // 총 지각 횟수
        Long totalLateCount = attendanceRepository
                .countByStudentIdAndStatus(studentId, AttendanceStatus.LATE);

        // 총 결석 횟수
        Long totalAbsentCount = attendanceRepository
                .countByStudentIdAndStatus(studentId, AttendanceStatus.ABSENT);

        // 활성 수강권 개수
        Integer activeEnrollmentCount = enrollmentRepository
                .countByStudentIdAndIsActiveTrue(studentId);

        // 예정된 예약 개수
        Integer upcomingReservationCount = (int) reservationRepository
                .findByStudentIdAndScheduleDateAfter(studentId, LocalDate.now())
                .stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                             r.getStatus() == ReservationStatus.PENDING)
                .count();

        return MyPageResponse.MyPageStats.builder()
                .totalAttendanceCount(totalAttendanceCount)
                .monthlyAttendanceCount(monthlyAttendanceCount)
                .totalLateCount(totalLateCount)
                .totalAbsentCount(totalAbsentCount)
                .activeEnrollmentCount(activeEnrollmentCount)
                .upcomingReservationCount(upcomingReservationCount)
                .build();
    }

    // ===== DTO 변환 메서드 =====

    private StudentResponse toStudentResponse(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .studentName(student.getStudentName())
                .studentPhone(student.getStudentPhone())
                .birthDate(student.getBirthDate())
                .gender(student.getGender())
                .address(student.getAddress())
                .school(student.getSchool())
                .grade(student.getGrade())
                .englishLevel(student.getEnglishLevel())
                .memo(student.getMemo())
                .parentName(student.getParentName())
                .parentPhone(student.getParentPhone())
                .parentEmail(student.getParentEmail())
                .isActive(student.getIsActive())
                .build();
    }

    private EnrollmentResponse toEnrollmentResponse(web.kplay.studentmanagement.domain.course.Enrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudent().getId())
                .studentName(enrollment.getStudent().getStudentName())
                .courseId(enrollment.getCourse().getId())
                .courseName(enrollment.getCourse().getCourseName())
                .startDate(enrollment.getStartDate())
                .endDate(enrollment.getEndDate())
                .totalCount(enrollment.getTotalCount())
                .usedCount(enrollment.getUsedCount())
                .remainingCount(enrollment.getRemainingCount())
                .isActive(enrollment.getIsActive())
                .memo(enrollment.getMemo())
                .build();
    }

    private AttendanceResponse toAttendanceResponse(web.kplay.studentmanagement.domain.attendance.Attendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .studentId(attendance.getStudent().getId())
                .studentName(attendance.getStudent().getStudentName())
                .scheduleId(attendance.getSchedule() != null ? attendance.getSchedule().getId() : null)
                .checkInTime(attendance.getCheckInTime())
                .checkOutTime(attendance.getCheckOutTime())
                .expectedLeaveTime(attendance.getExpectedLeaveTime())
                .status(attendance.getStatus().name())
                .reason(attendance.getReason())
                .memo(attendance.getMemo())
                .build();
    }

    private ReservationResponse toReservationResponse(web.kplay.studentmanagement.domain.reservation.Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .studentId(reservation.getStudent().getId())
                .studentName(reservation.getStudent().getStudentName())
                .scheduleId(reservation.getSchedule().getId())
                .scheduleDate(reservation.getSchedule().getScheduleDate())
                .scheduleTime(reservation.getSchedule().getStartTime() + " - " + reservation.getSchedule().getEndTime())
                .enrollmentId(reservation.getEnrollment() != null ? reservation.getEnrollment().getId() : null)
                .status(reservation.getStatus().name())
                .memo(reservation.getMemo())
                .cancelReason(reservation.getCancelReason())
                .cancelledAt(reservation.getCancelledAt())
                .reservationSource(reservation.getReservationSource())
                .build();
    }

    private LevelTestResponse toLevelTestResponse(web.kplay.studentmanagement.domain.leveltest.LevelTest levelTest) {
        return LevelTestResponse.builder()
                .id(levelTest.getId())
                .studentId(levelTest.getStudent().getId())
                .studentName(levelTest.getStudent().getStudentName())
                .testDate(levelTest.getTestDate())
                .testTime(levelTest.getTestTime())
                .currentLevel(levelTest.getCurrentLevel())
                .testResult(levelTest.getTestResult())
                .recommendedLevel(levelTest.getRecommendedLevel())
                .memo(levelTest.getMemo())
                .testStatus(levelTest.getTestStatus())
                .build();
    }

    private MessageResponse toMessageResponse(web.kplay.studentmanagement.domain.message.Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .studentId(message.getStudent() != null ? message.getStudent().getId() : null)
                .studentName(message.getStudent() != null ? message.getStudent().getStudentName() : null)
                .recipientName(message.getRecipientName())
                .recipientPhone(message.getRecipientPhone())
                .messageType(message.getMessageType().name())
                .content(message.getContent())
                .status(message.getStatus().name())
                .sentAt(message.getSentAt())
                .failReason(message.getFailReason())
                .build();
    }

    private ConsultationResponse toConsultationResponse(web.kplay.studentmanagement.domain.consultation.Consultation consultation) {
        return ConsultationResponse.builder()
                .id(consultation.getId())
                .studentId(consultation.getStudent().getId())
                .studentName(consultation.getStudent().getStudentName())
                .consultantId(consultation.getConsultant().getId())
                .consultantName(consultation.getConsultant().getName())
                .consultationDate(consultation.getConsultationDate())
                .title(consultation.getTitle())
                .content(consultation.getContent())
                .consultationType(consultation.getConsultationType())
                .recordingFileUrl(consultation.getRecordingFileUrl())
                .attachmentFileUrl(consultation.getAttachmentFileUrl())
                .actionItems(consultation.getActionItems())
                .nextConsultationDate(consultation.getNextConsultationDate())
                .build();
    }
}
