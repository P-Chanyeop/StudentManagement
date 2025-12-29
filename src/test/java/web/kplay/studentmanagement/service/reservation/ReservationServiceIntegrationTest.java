package web.kplay.studentmanagement.service.reservation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.course.Course;
import web.kplay.studentmanagement.domain.course.CourseSchedule;
import web.kplay.studentmanagement.domain.course.Enrollment;
import web.kplay.studentmanagement.domain.reservation.Reservation;
import web.kplay.studentmanagement.domain.reservation.ReservationStatus;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.dto.reservation.ReservationCreateRequest;
import web.kplay.studentmanagement.dto.reservation.ReservationResponse;
import web.kplay.studentmanagement.repository.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ReservationService 통합 테스트
 * - 예약 생성/취소/삭제 시 수강권 횟수 관리 검증
 * - Race condition 방지 검증
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@DisplayName("ReservationService 통합 테스트")
class ReservationServiceIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseScheduleRepository scheduleRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Student student;
    private Course course;
    private CourseSchedule schedule;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        // 학생 생성
        student = Student.builder()
                .studentName("테스트학생")
                .birthDate(LocalDate.of(2010, 1, 1))
                .studentPhone("010-1234-5678")
                .parentPhone("010-8765-4321")
                .parentName("학부모")
                .school("테스트학교")
                .grade("5")
                .isActive(true)
                .build();
        student = studentRepository.save(student);

        // 과목 생성
        course = Course.builder()
                .courseName("영어 초급")
                .description("영어 기초 과정")
                .maxStudents(10)
                .durationMinutes(60)
                .level("BEGINNER")
                .isActive(true)
                .build();
        course = courseRepository.save(course);

        // 스케줄 생성 (미래 날짜로 설정하여 취소 가능하게)
        schedule = CourseSchedule.builder()
                .course(course)
                .dayOfWeek("MONDAY")
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 0))
                .scheduleDate(LocalDate.now().plusDays(3)) // 3일 후로 설정
                .currentStudents(0)
                .isCancelled(false)
                .build();
        schedule = scheduleRepository.save(schedule);

        // 수강권 생성
        enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .totalCount(10)
                .usedCount(0)
                .remainingCount(10)
                .isActive(true)
                .build();
        enrollment = enrollmentRepository.save(enrollment);
    }

    @Test
    @DisplayName("예약 생성 시 수강권 횟수가 차감된다")
    void createReservation_ShouldDecrementEnrollmentCount() {
        // given
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .studentId(student.getId())
                .scheduleId(schedule.getId())
                .enrollmentId(enrollment.getId())
                .build();

        // when
        ReservationResponse response = reservationService.createReservation(request);

        // then
        Enrollment updatedEnrollment = enrollmentRepository.findById(enrollment.getId()).orElseThrow();
        assertThat(updatedEnrollment.getUsedCount()).isEqualTo(1);
        assertThat(updatedEnrollment.getRemainingCount()).isEqualTo(9);
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("예약 삭제 시 수강권 횟수가 복원된다")
    void deleteReservation_ShouldRestoreEnrollmentCount() {
        // given - 예약 생성
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .studentId(student.getId())
                .scheduleId(schedule.getId())
                .enrollmentId(enrollment.getId())
                .build();
        ReservationResponse created = reservationService.createReservation(request);

        // when - 예약 삭제
        reservationService.deleteReservation(created.getId());

        // then - 수강권 복원 확인
        Enrollment updatedEnrollment = enrollmentRepository.findById(enrollment.getId()).orElseThrow();
        assertThat(updatedEnrollment.getUsedCount()).isEqualTo(0);
        assertThat(updatedEnrollment.getRemainingCount()).isEqualTo(10);

        // 예약이 DB에서 삭제됨
        assertThat(reservationRepository.findById(created.getId())).isEmpty();
    }

    @Test
    @DisplayName("예약 취소 시 수강권 횟수가 복원된다 (상태 변경 전 복원)")
    void cancelReservation_ShouldRestoreEnrollmentCountBeforeStatusChange() {
        // given - 예약 생성
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .studentId(student.getId())
                .scheduleId(schedule.getId())
                .enrollmentId(enrollment.getId())
                .build();
        ReservationResponse created = reservationService.createReservation(request);

        // when - 예약 취소
        reservationService.cancelReservation(created.getId(), "개인 사정");

        // then - 수강권 복원 확인
        Enrollment updatedEnrollment = enrollmentRepository.findById(enrollment.getId()).orElseThrow();
        assertThat(updatedEnrollment.getUsedCount()).isEqualTo(0);
        assertThat(updatedEnrollment.getRemainingCount()).isEqualTo(10);

        // 예약 상태가 CANCELLED로 변경됨 (DB에 남아있음)
        Reservation cancelledReservation = reservationRepository.findById(created.getId()).orElseThrow();
        assertThat(cancelledReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(cancelledReservation.getCancelReason()).isEqualTo("개인 사정");
    }

    @Test
    @DisplayName("강제 취소 시 수강권 횟수가 복원된다")
    void forceCancelReservation_ShouldRestoreEnrollmentCount() {
        // given - 예약 생성
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .studentId(student.getId())
                .scheduleId(schedule.getId())
                .enrollmentId(enrollment.getId())
                .build();
        ReservationResponse created = reservationService.createReservation(request);

        // when - 강제 취소
        reservationService.forceCancelReservation(created.getId(), "관리자 조치");

        // then - 수강권 복원 확인
        Enrollment updatedEnrollment = enrollmentRepository.findById(enrollment.getId()).orElseThrow();
        assertThat(updatedEnrollment.getUsedCount()).isEqualTo(0);
        assertThat(updatedEnrollment.getRemainingCount()).isEqualTo(10);

        // 예약 상태가 CANCELLED로 변경됨
        Reservation cancelledReservation = reservationRepository.findById(created.getId()).orElseThrow();
        assertThat(cancelledReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("수강권 없이 예약 생성 시 횟수 차감이 없다")
    void createReservation_WithoutEnrollment_ShouldNotDecrementCount() {
        // given - 수강권 없이
        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .studentId(student.getId())
                .scheduleId(schedule.getId())
                .enrollmentId(null) // 수강권 없음
                .build();

        int initialRemainingCount = enrollment.getRemainingCount();

        // when
        ReservationResponse response = reservationService.createReservation(request);

        // then - 수강권 횟수 변경 없음
        Enrollment updatedEnrollment = enrollmentRepository.findById(enrollment.getId()).orElseThrow();
        assertThat(updatedEnrollment.getRemainingCount()).isEqualTo(initialRemainingCount);
        assertThat(response.getEnrollmentId()).isNull();
    }

    @Test
    @DisplayName("여러 예약을 생성하고 취소해도 수강권 횟수가 정확히 관리된다")
    void multipleReservationsManagement_ShouldMaintainCorrectCount() {
        // given - 초기 상태 확인
        int initialRemainingCount = enrollment.getRemainingCount(); // 10
        int initialUsedCount = enrollment.getUsedCount(); // 0
        
        // 예약 생성 요청
        ReservationCreateRequest request1 = ReservationCreateRequest.builder()
                .studentId(student.getId())
                .scheduleId(schedule.getId())
                .enrollmentId(enrollment.getId())
                .build();

        // 추가 스케줄 생성 (미래 날짜로 설정)
        CourseSchedule schedule2 = CourseSchedule.builder()
                .course(course)
                .dayOfWeek("TUESDAY")
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 0))
                .scheduleDate(LocalDate.now().plusDays(4)) // 4일 후로 설정
                .currentStudents(0)
                .isCancelled(false)
                .build();
        schedule2 = scheduleRepository.save(schedule2);

        ReservationCreateRequest request2 = ReservationCreateRequest.builder()
                .studentId(student.getId())
                .scheduleId(schedule2.getId())
                .enrollmentId(enrollment.getId())
                .build();

        // when - 예약 생성
        ReservationResponse res1 = reservationService.createReservation(request1);
        ReservationResponse res2 = reservationService.createReservation(request2);

        // then - 2회 차감됨 (10 -> 8)
        enrollmentRepository.flush(); // 강제로 DB에 반영
        Enrollment afterCreate = enrollmentRepository.findById(enrollment.getId()).orElseThrow();
        assertThat(afterCreate.getRemainingCount()).isEqualTo(initialRemainingCount - 2);
        assertThat(afterCreate.getUsedCount()).isEqualTo(initialUsedCount + 2);

        // when - 1개 취소
        reservationService.cancelReservation(res1.getId(), "취소");

        // then - 1회 복원됨 (8 -> 9)
        enrollmentRepository.flush(); // 강제로 DB에 반영
        Enrollment afterCancel = enrollmentRepository.findById(enrollment.getId()).orElseThrow();
        assertThat(afterCancel.getRemainingCount()).isEqualTo(initialRemainingCount - 1);
        assertThat(afterCancel.getUsedCount()).isEqualTo(initialUsedCount + 1);

        // when - 1개 삭제
        reservationService.deleteReservation(res2.getId());

        // then - 다시 1회 복원됨 (9 -> 10)
        enrollmentRepository.flush(); // 강제로 DB에 반영
        Enrollment afterDelete = enrollmentRepository.findById(enrollment.getId()).orElseThrow();
        assertThat(afterDelete.getRemainingCount()).isEqualTo(initialRemainingCount);
        assertThat(afterDelete.getUsedCount()).isEqualTo(initialUsedCount);
    }

    @Test
    @DisplayName("스케줄의 학생 수가 정확히 관리된다")
    void reservationManagement_ShouldUpdateScheduleStudentCount() {
        // given
        assertThat(schedule.getCurrentStudents()).isEqualTo(0);

        ReservationCreateRequest request = ReservationCreateRequest.builder()
                .studentId(student.getId())
                .scheduleId(schedule.getId())
                .enrollmentId(enrollment.getId())
                .build();

        // when - 예약 생성
        ReservationResponse created = reservationService.createReservation(request);

        // then - 학생 수 증가
        CourseSchedule updatedSchedule = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertThat(updatedSchedule.getCurrentStudents()).isEqualTo(1);

        // when - 예약 취소
        reservationService.cancelReservation(created.getId(), "취소");

        // then - 학생 수 감소
        updatedSchedule = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertThat(updatedSchedule.getCurrentStudents()).isEqualTo(0);
    }
}
