package web.kplay.studentmanagement.domain.course;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import web.kplay.studentmanagement.domain.student.Student;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Enrollment 도메인 엔티티 단위 테스트
 * - 수강권 횟수 관리 로직 검증
 * - Race condition 방지 로직 검증
 * - 유효성 검증 로직 검증
 */
@DisplayName("Enrollment 도메인 테스트")
class EnrollmentTest {

    private Student student;
    private Course course;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        student = Student.builder()
                .id(1L)
                .studentName("홍길동")
                .build();

        course = Course.builder()
                .id(1L)
                .courseName("영어 초급반")
                .build();

        enrollment = Enrollment.builder()
                .id(1L)
                .student(student)
                .course(course)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .totalCount(10)
                .usedCount(0)
                .remainingCount(10)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("useCount() - 횟수를 사용하면 usedCount는 증가하고 remainingCount는 감소한다")
    void useCount_ShouldIncreaseUsedAndDecreaseRemaining() {
        // when
        enrollment.useCount();

        // then
        assertThat(enrollment.getUsedCount()).isEqualTo(1);
        assertThat(enrollment.getRemainingCount()).isEqualTo(9);
        assertThat(enrollment.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("useCount() - 마지막 횟수를 사용하면 수강권이 비활성화된다")
    void useCount_WhenLastCount_ShouldDeactivateEnrollment() {
        // given - 마지막 1회 남은 상태
        enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .totalCount(10)
                .usedCount(9)
                .remainingCount(1)
                .isActive(true)
                .build();

        // when
        enrollment.useCount();

        // then
        assertThat(enrollment.getUsedCount()).isEqualTo(10);
        assertThat(enrollment.getRemainingCount()).isEqualTo(0);
        assertThat(enrollment.getIsActive()).isFalse(); // 비활성화됨
    }

    @Test
    @DisplayName("useCount() - 남은 횟수가 0이면 횟수를 사용할 수 없다")
    void useCount_WhenNoRemainingCount_ShouldNotUseCount() {
        // given - 횟수 모두 소진
        enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .totalCount(10)
                .usedCount(10)
                .remainingCount(0)
                .isActive(false)
                .build();

        // when
        enrollment.useCount();

        // then - 변경 없음
        assertThat(enrollment.getUsedCount()).isEqualTo(10);
        assertThat(enrollment.getRemainingCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("restoreCount() - 횟수를 복원하면 usedCount는 감소하고 remainingCount는 증가한다")
    void restoreCount_ShouldDecreaseUsedAndIncreaseRemaining() {
        // given - 횟수 사용 후
        enrollment.useCount();
        enrollment.useCount();

        // when
        enrollment.restoreCount();

        // then
        assertThat(enrollment.getUsedCount()).isEqualTo(1);
        assertThat(enrollment.getRemainingCount()).isEqualTo(9);
    }

    @Test
    @DisplayName("restoreCount() - 비활성화된 수강권도 횟수 복원 시 재활성화된다 (기간 유효 시)")
    void restoreCount_ShouldReactivateIfValid() {
        // given - 횟수 모두 소진하여 비활성화됨
        enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .totalCount(10)
                .usedCount(10)
                .remainingCount(0)
                .isActive(false)
                .build();

        // when
        enrollment.restoreCount();

        // then
        assertThat(enrollment.getRemainingCount()).isEqualTo(1);
        assertThat(enrollment.getIsActive()).isTrue(); // 재활성화됨
    }

    @Test
    @DisplayName("restoreCount() - usedCount가 0이면 복원할 수 없다")
    void restoreCount_WhenNoUsedCount_ShouldNotRestore() {
        // given - 한 번도 사용 안함
        assertThat(enrollment.getUsedCount()).isEqualTo(0);

        // when
        enrollment.restoreCount();

        // then - 변경 없음
        assertThat(enrollment.getUsedCount()).isEqualTo(0);
        assertThat(enrollment.getRemainingCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("isValid() - 유효한 수강권은 true를 반환한다")
    void isValid_WhenValid_ShouldReturnTrue() {
        // when & then
        assertThat(enrollment.isValid()).isTrue();
    }

    @Test
    @DisplayName("isValid() - 시작일 이전에는 false를 반환한다")
    void isValid_BeforeStartDate_ShouldReturnFalse() {
        // given
        enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .startDate(LocalDate.now().plusDays(1)) // 내일부터 시작
                .endDate(LocalDate.now().plusDays(30))
                .totalCount(10)
                .remainingCount(10)
                .isActive(true)
                .build();

        // when & then
        assertThat(enrollment.isValid()).isFalse();
    }

    @Test
    @DisplayName("isValid() - 종료일 이후에는 false를 반환한다")
    void isValid_AfterEndDate_ShouldReturnFalse() {
        // given
        enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .startDate(LocalDate.now().minusDays(30))
                .endDate(LocalDate.now().minusDays(1)) // 어제 만료
                .totalCount(10)
                .remainingCount(5)
                .isActive(true)
                .build();

        // when & then
        assertThat(enrollment.isValid()).isFalse();
    }

    @Test
    @DisplayName("isValid() - 남은 횟수가 0이면 false를 반환한다")
    void isValid_WhenNoRemainingCount_ShouldReturnFalse() {
        // given
        enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .totalCount(10)
                .remainingCount(0) // 횟수 소진
                .isActive(true)
                .build();

        // when & then
        assertThat(enrollment.isValid()).isFalse();
    }

    @Test
    @DisplayName("addCount() - 횟수를 추가하면 totalCount와 remainingCount가 증가한다")
    void addCount_ShouldIncreaseTotalAndRemaining() {
        // when
        enrollment.addCount(5);

        // then
        assertThat(enrollment.getTotalCount()).isEqualTo(15);
        assertThat(enrollment.getRemainingCount()).isEqualTo(15);
        assertThat(enrollment.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("addCount() - 비활성화된 수강권도 횟수 추가 시 재활성화된다 (기간 유효 시)")
    void addCount_ShouldReactivateIfPeriodValid() {
        // given - 횟수 소진으로 비활성화
        enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .totalCount(10)
                .usedCount(10)
                .remainingCount(0)
                .isActive(false)
                .build();

        // when
        enrollment.addCount(3);

        // then
        assertThat(enrollment.getRemainingCount()).isEqualTo(3);
        assertThat(enrollment.getIsActive()).isTrue(); // 재활성화됨
    }

    @Test
    @DisplayName("addCount() - null을 입력하면 예외가 발생한다")
    void addCount_WithNull_ShouldThrowException() {
        // when & then
        assertThatThrownBy(() -> enrollment.addCount(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("추가할 횟수는 필수입니다");
    }

    @Test
    @DisplayName("addCount() - 0 이하의 횟수를 입력하면 예외가 발생한다")
    void addCount_WithZeroOrNegative_ShouldThrowException() {
        // when & then
        assertThatThrownBy(() -> enrollment.addCount(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 이상이어야 합니다");

        assertThatThrownBy(() -> enrollment.addCount(-5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 이상이어야 합니다");
    }

    @Test
    @DisplayName("extendPeriod() - 기간을 연장하면 종료일이 변경된다")
    void extendPeriod_ShouldUpdateEndDate() {
        // given
        LocalDate newEndDate = LocalDate.now().plusDays(60);

        // when
        enrollment.extendPeriod(newEndDate);

        // then
        assertThat(enrollment.getEndDate()).isEqualTo(newEndDate);
    }

    @Test
    @DisplayName("extendPeriod() - 기간 연장 시 횟수가 남아있으면 재활성화된다")
    void extendPeriod_ShouldReactivateIfRemainingCount() {
        // given - 기간 만료로 비활성화
        enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .startDate(LocalDate.now().minusDays(30))
                .endDate(LocalDate.now().minusDays(1))
                .totalCount(10)
                .remainingCount(5)
                .isActive(false)
                .build();

        // when
        enrollment.extendPeriod(LocalDate.now().plusDays(30));

        // then
        assertThat(enrollment.getIsActive()).isTrue(); // 재활성화됨
    }

    @Test
    @DisplayName("extendPeriod() - null을 입력하면 예외가 발생한다")
    void extendPeriod_WithNull_ShouldThrowException() {
        // when & then
        assertThatThrownBy(() -> enrollment.extendPeriod(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("새 종료일은 필수입니다");
    }

    @Test
    @DisplayName("extendPeriod() - 시작일보다 이전 날짜로 연장하면 예외가 발생한다")
    void extendPeriod_BeforeStartDate_ShouldThrowException() {
        // given
        LocalDate invalidDate = enrollment.getStartDate().minusDays(1);

        // when & then
        assertThatThrownBy(() -> enrollment.extendPeriod(invalidDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("시작일")
                .hasMessageContaining("이후여야 합니다");
    }

    @Test
    @DisplayName("manualAdjustCount() - 양수 조절 시 횟수가 증가한다")
    void manualAdjustCount_WithPositive_ShouldIncreaseCount() {
        // when
        enrollment.manualAdjustCount(3);

        // then
        assertThat(enrollment.getTotalCount()).isEqualTo(13);
        assertThat(enrollment.getRemainingCount()).isEqualTo(13);
    }

    @Test
    @DisplayName("manualAdjustCount() - 음수 조절 시 횟수가 감소한다")
    void manualAdjustCount_WithNegative_ShouldDecreaseCount() {
        // when
        enrollment.manualAdjustCount(-3);

        // then
        assertThat(enrollment.getTotalCount()).isEqualTo(7);
        assertThat(enrollment.getRemainingCount()).isEqualTo(7);
    }

    @Test
    @DisplayName("manualAdjustCount() - 0으로 조절하면 변경이 없다")
    void manualAdjustCount_WithZero_ShouldNotChange() {
        // when
        enrollment.manualAdjustCount(0);

        // then
        assertThat(enrollment.getTotalCount()).isEqualTo(10);
        assertThat(enrollment.getRemainingCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("manualAdjustCount() - 음수가 되도록 조절하면 예외가 발생한다")
    void manualAdjustCount_CausingNegative_ShouldThrowException() {
        // when & then
        assertThatThrownBy(() -> enrollment.manualAdjustCount(-15))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("음수가 될 수 없습니다");
    }

    @Test
    @DisplayName("manualAdjustCount() - null을 입력하면 예외가 발생한다")
    void manualAdjustCount_WithNull_ShouldThrowException() {
        // when & then
        assertThatThrownBy(() -> enrollment.manualAdjustCount(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("조절할 횟수는 필수입니다");
    }
}
