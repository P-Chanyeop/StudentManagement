package web.kplay.studentmanagement.service.mypage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.attendance.AttendanceStatus;
import web.kplay.studentmanagement.domain.course.Course;
import web.kplay.studentmanagement.domain.reservation.ReservationStatus;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.domain.user.User;
import web.kplay.studentmanagement.domain.user.UserRole;
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
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final ReservationRepository reservationRepository;
    private final LevelTestRepository levelTestRepository;
    private final MessageRepository messageRepository;
    private final ConsultationRepository consultationRepository;
    private final CourseRepository courseRepository;

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
        log.info("마이페이지 서비스 시작: userId={}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));
        
        log.info("사용자 조회 완료: userId={}, role={}", userId, user.getRole());
        
        if (user.getRole() == UserRole.PARENT) {
            log.info("부모 계정 처리 시작");
            // 부모 계정: 자녀 정보 조회
            List<Student> children = studentRepository.findByParentUser(user);
            if (!children.isEmpty()) {
                log.info("자녀 정보 찾음: 자녀 수={}", children.size());
                return buildMyPageResponse(children.get(0)); // 첫 번째 자녀 정보
            }
            throw new ResourceNotFoundException("학생 정보를 찾을 수 없습니다");
        } else {
            log.info("관리자/선생님 계정 처리 시작: role={}", user.getRole());
            // 관리자/선생님 계정: 사용자 정보 반환
            return buildUserMyPageResponse(user);
        }
    }

    /**
     * 관리자/선생님용 MyPageResponse 생성
     * @param user 사용자 정보 (관리자 또는 선생님)
     * @return 사용자 역할에 따른 마이페이지 응답 데이터
     */
    private MyPageResponse buildUserMyPageResponse(User user) {
        log.info("buildUserMyPageResponse 시작: userId={}, role={}", user.getId(), user.getRole());
        
        // 사용자 기본 정보
        StudentResponse userInfo = StudentResponse.builder()
                .id(user.getId())
                .studentName(user.getName())
                .studentPhone(user.getPhoneNumber())
                .parentEmail(user.getEmail())
                .isActive(true)
                .build();

        if (user.getRole() == UserRole.TEACHER) {
            log.info("선생님 마이페이지 데이터 생성");
            // 선생님인 경우: 본인 수업 관련 데이터
            return buildTeacherMyPageResponse(user, userInfo);
        } else {
            log.info("관리자 마이페이지 데이터 생성");
            // 관리자인 경우: 전체 시스템 통계
            return buildAdminMyPageResponse(user, userInfo);
        }
    }

    /**
     * 선생님용 마이페이지 데이터 생성
     * @param teacher 선생님 사용자 정보
     * @param userInfo 사용자 기본 정보
     * @return 선생님용 마이페이지 응답 데이터
     */
    private MyPageResponse buildTeacherMyPageResponse(User teacher, StudentResponse userInfo) {
        // 선생님의 수업 관련 통계
        MyPageResponse.MyPageStats stats = buildTeacherStats(teacher.getId());

        // 선생님이 진행한 최근 상담 이력 5개
        List<ConsultationResponse> teacherConsultations = consultationRepository
                .findTop5ByConsultantIdOrderByConsultationDateDesc(teacher.getId())
                .stream()
                .map(this::toConsultationResponse)
                .collect(Collectors.toList());
        
        log.info("선생님 ID {} 의 상담 이력 개수: {}", teacher.getId(), teacherConsultations.size());

        return MyPageResponse.builder()
                .studentInfo(userInfo)
                .activeEnrollments(List.of()) // 선생님은 수강권 없음
                .recentAttendances(List.of()) // 선생님은 출석 기록 없음
                .upcomingReservations(List.of()) // 선생님은 예약 없음
                .upcomingLevelTests(List.of()) // 선생님은 레벨테스트 없음
                .recentMessages(List.of()) // 선생님은 개인 메시지 없음
                .recentConsultations(teacherConsultations) // 선생님의 상담 이력 표시
                .stats(stats)
                .build();
    }

    /**
     * 관리자용 마이페이지 데이터 생성
     * @param admin 관리자 사용자 정보
     * @param userInfo 사용자 기본 정보
     * @return 관리자용 마이페이지 응답 데이터 (전체 시스템 통계 포함)
     */
    private MyPageResponse buildAdminMyPageResponse(User admin, StudentResponse userInfo) {
        log.info("관리자 통계 데이터 조회 시작");
        
        try {
            // 관리자는 전체 시스템 통계 표시
            MyPageResponse.MyPageStats stats = buildAdminStats();
            log.info("관리자 통계 데이터 조회 완료");

            return MyPageResponse.builder()
                    .studentInfo(userInfo)
                    .activeEnrollments(List.of())
                    .recentAttendances(List.of())
                    .upcomingReservations(List.of())
                    .upcomingLevelTests(List.of())
                    .recentMessages(List.of())
                    .recentConsultations(List.of())
                    .stats(stats)
                    .build();
        } catch (Exception e) {
            log.error("관리자 마이페이지 데이터 생성 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 선생님 통계 정보 생성
     * @param teacherId 선생님 ID
     * @return 선생님의 수업 관련 통계 정보
     */
    private MyPageResponse.MyPageStats buildTeacherStats(Long teacherId) {
        // 선생님이 진행한 총 상담 개수
        Long totalConsultations = consultationRepository.countByConsultantId(teacherId);
        log.info("선생님 ID {} 의 총 상담 개수: {}", teacherId, totalConsultations);
        
        // 선생님이 담당하는 수업들의 출석 통계
        Long teacherClassAttendance = attendanceRepository.countByTeacherIdAndStatus(teacherId, AttendanceStatus.PRESENT);
        Long teacherClassLate = attendanceRepository.countByTeacherIdAndStatus(teacherId, AttendanceStatus.LATE);
        Long teacherClassAbsent = attendanceRepository.countByTeacherIdAndStatus(teacherId, AttendanceStatus.ABSENT);
        
        // 선생님 담당 수업의 활성 수강권 수
        List<Course> teacherCourses = courseRepository.findByTeacherId(teacherId);
        Integer teacherActiveEnrollments = teacherCourses.stream()
                .mapToInt(course -> enrollmentRepository.countActiveByCourseId(course.getId()))
                .sum();
        
        Double teacherAttendanceRate = calculateAttendanceRate(teacherClassAttendance, teacherClassLate, teacherClassAbsent);
        
        log.info("선생님 ID {} 담당 수업 출석 통계 - 출석: {}, 지각: {}, 결석: {}, 출석률: {}%", 
                teacherId, teacherClassAttendance, teacherClassLate, teacherClassAbsent, teacherAttendanceRate);
        
        return MyPageResponse.MyPageStats.builder()
                .totalAttendanceCount(teacherClassAttendance) // 선생님 담당 수업 총 출석 수
                .monthlyAttendanceCount(0L) // TODO: 이번 달 선생님 수업 출석 수
                .totalLateCount(teacherClassLate) // 선생님 담당 수업 지각 수
                .totalAbsentCount(teacherClassAbsent) // 선생님 담당 수업 결석 수
                .activeEnrollmentCount(teacherActiveEnrollments) // 선생님 담당 수업의 활성 수강권 수
                .upcomingReservationCount(0) // 선생님은 예약이 없음
                .consultationCount(totalConsultations.intValue()) // 상담 개수
                .attendanceRate(teacherAttendanceRate) // 선생님 담당 수업 출석률
                .build();
    }

    /**
     * 관리자 통계 정보 생성 (전체 시스템)
     * @return 전체 시스템의 통계 정보 (총 출석, 지각, 결석, 활성 수강권, 예정된 예약 수)
     */
    private MyPageResponse.MyPageStats buildAdminStats() {
        log.info("관리자 통계 각 항목 조회 시작");
        
        try {
            // 전체 시스템 통계
            log.info("총 출석 수 조회 중...");
            Long totalAttendance = attendanceRepository.countByStatus(AttendanceStatus.PRESENT);
            log.info("총 출석 수: {}", totalAttendance);
            
            log.info("총 지각 수 조회 중...");
            Long totalLate = attendanceRepository.countByStatus(AttendanceStatus.LATE);
            log.info("총 지각 수: {}", totalLate);
            
            log.info("총 결석 수 조회 중...");
            Long totalAbsent = attendanceRepository.countByStatus(AttendanceStatus.ABSENT);
            log.info("총 결석 수: {}", totalAbsent);
            
            log.info("활성 수강권 수 조회 중...");
            Integer activeEnrollments = enrollmentRepository.countByIsActiveTrue();
            log.info("활성 수강권 수: {}", activeEnrollments);
            
            log.info("예정된 예약 수 조회 중...");
            Integer upcomingReservations = (int) reservationRepository
                    .findByScheduleDateAfter(LocalDate.now())
                    .stream()
                    .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                                 r.getStatus() == ReservationStatus.PENDING)
                    .count();
            log.info("예정된 예약 수: {}", upcomingReservations);

            return MyPageResponse.MyPageStats.builder()
                    .totalAttendanceCount(totalAttendance)
                    .monthlyAttendanceCount(0L) // 이번 달 전체 출석은 복잡하므로 일단 0
                    .totalLateCount(totalLate)
                    .totalAbsentCount(totalAbsent)
                    .activeEnrollmentCount(activeEnrollments)
                    .upcomingReservationCount(upcomingReservations)
                    .consultationCount(0) // 관리자는 상담 개수 0
                    .attendanceRate(calculateAttendanceRate(totalAttendance, totalLate, totalAbsent))
                    .build();
        } catch (Exception e) {
            log.error("관리자 통계 조회 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 출석률 계산
     * @param present 출석 수
     * @param late 지각 수  
     * @param absent 결석 수
     * @return 출석률 (%)
     */
    private Double calculateAttendanceRate(Long present, Long late, Long absent) {
        Long total = present + late + absent;
        if (total == 0) {
            return 0.0;
        }
        return Math.round((present.doubleValue() / total.doubleValue()) * 100.0 * 10.0) / 10.0;
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

        // 학생이 받은 상담 개수
        Integer consultationCount = consultationRepository.findByStudentId(studentId).size();

        return MyPageResponse.MyPageStats.builder()
                .totalAttendanceCount(totalAttendanceCount)
                .monthlyAttendanceCount(monthlyAttendanceCount)
                .totalLateCount(totalLateCount)
                .totalAbsentCount(totalAbsentCount)
                .activeEnrollmentCount(activeEnrollmentCount)
                .upcomingReservationCount(upcomingReservationCount)
                .consultationCount(consultationCount) // 학생이 받은 상담 개수
                .attendanceRate(calculateAttendanceRate(totalAttendanceCount, totalLateCount, totalAbsentCount))
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
                .status(attendance.getStatus())
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
                .enrollmentId(reservation.getEnrollment() != null ? reservation.getEnrollment().getId() : null)
                .status(reservation.getStatus())
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
                .messageType(message.getMessageType())
                .content(message.getContent())
                .sentAt(message.getSentAt())
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
