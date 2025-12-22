package web.kplay.studentmanagement.domain.leveltest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.domain.user.User;
import web.kplay.studentmanagement.domain.user.UserRole;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LevelTest 도메인 엔티티 단위 테스트
 * - 비즈니스 메서드 동작 검증
 * - 캡슐화 및 불변성 검증
 */
@DisplayName("LevelTest 도메인 테스트")
class LevelTestTest {

    private Student student;
    private User teacher;
    private LevelTest levelTest;

    @BeforeEach
    void setUp() {
        student = Student.builder()
                .id(1L)
                .studentName("홍길동")
                .birthDate(LocalDate.of(2010, 3, 15))
                .studentPhone("010-1234-5678")
                .build();

        teacher = User.builder()
                .id(1L)
                .username("teacher1")
                .name("김선생")
                .role(UserRole.TEACHER)
                .build();

        levelTest = LevelTest.builder()
                .id(1L)
                .student(student)
                .teacher(teacher)
                .testDate(LocalDate.now())
                .testTime(LocalTime.of(14, 0))
                .testStatus("SCHEDULED")
                .build();
    }

    @Test
    @DisplayName("complete() - 모든 필드를 제공하면 완료 처리되고 결과가 저장된다")
    void complete_WithAllFields_ShouldUpdateAllFieldsAndMarkAsCompleted() {
        // when
        levelTest.complete("Intermediate", 85, "Great job!", "Speaking", "Grammar", "Advanced");

        // then
        assertThat(levelTest.getTestStatus()).isEqualTo("COMPLETED");
        assertThat(levelTest.getTestResult()).isEqualTo("Intermediate");
        assertThat(levelTest.getTestScore()).isEqualTo(85);
        assertThat(levelTest.getFeedback()).isEqualTo("Great job!");
        assertThat(levelTest.getStrengths()).isEqualTo("Speaking");
        assertThat(levelTest.getImprovements()).isEqualTo("Grammar");
        assertThat(levelTest.getRecommendedLevel()).isEqualTo("Advanced");
    }

    @Test
    @DisplayName("complete() - null 필드는 기존 값을 유지한다 (데이터 손실 방지)")
    void complete_WithNullFields_ShouldKeepExistingValues() {
        // given - 기존 데이터 설정
        levelTest.complete("Beginner", 70, "Initial feedback", "Listening", "Vocabulary", "Intermediate");

        // when - 일부 필드만 업데이트 (나머지는 null)
        levelTest.complete("Elementary", 75, null, null, null, null);

        // then - null이 아닌 필드만 업데이트, 나머지는 기존 값 유지
        assertThat(levelTest.getTestResult()).isEqualTo("Elementary");
        assertThat(levelTest.getTestScore()).isEqualTo(75);
        assertThat(levelTest.getFeedback()).isEqualTo("Initial feedback"); // 유지됨
        assertThat(levelTest.getStrengths()).isEqualTo("Listening"); // 유지됨
        assertThat(levelTest.getImprovements()).isEqualTo("Vocabulary"); // 유지됨
        assertThat(levelTest.getRecommendedLevel()).isEqualTo("Intermediate"); // 유지됨
    }

    @Test
    @DisplayName("complete() - 모든 필드가 null이어도 상태는 COMPLETED로 변경된다")
    void complete_WithAllNullFields_ShouldMarkAsCompleted() {
        // when
        levelTest.complete(null, null, null, null, null, null);

        // then
        assertThat(levelTest.getTestStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("cancel() - 테스트를 취소하면 상태가 CANCELLED로 변경된다")
    void cancel_ShouldChangeStatusToCancelled() {
        // when
        levelTest.cancel();

        // then
        assertThat(levelTest.getTestStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("assignTeacher() - 선생님을 배정할 수 있다")
    void assignTeacher_ShouldUpdateTeacher() {
        // given
        User newTeacher = User.builder()
                .id(2L)
                .username("teacher2")
                .name("이선생")
                .role(UserRole.TEACHER)
                .build();

        // when
        levelTest.assignTeacher(newTeacher);

        // then
        assertThat(levelTest.getTeacher()).isEqualTo(newTeacher);
        assertThat(levelTest.getTeacher().getName()).isEqualTo("이선생");
    }

    @Test
    @DisplayName("reschedule() - 날짜와 시간을 변경할 수 있다")
    void reschedule_ShouldUpdateDateAndTime() {
        // given
        LocalDate newDate = LocalDate.now().plusDays(3);
        LocalTime newTime = LocalTime.of(16, 30);

        // when
        levelTest.reschedule(newDate, newTime);

        // then
        assertThat(levelTest.getTestDate()).isEqualTo(newDate);
        assertThat(levelTest.getTestTime()).isEqualTo(newTime);
    }

    @Test
    @DisplayName("updateDetails() - 여러 필드를 한 번에 업데이트할 수 있다")
    void updateDetails_ShouldUpdateMultipleFields() {
        // given
        User newTeacher = User.builder()
                .id(3L)
                .username("teacher3")
                .name("박선생")
                .role(UserRole.TEACHER)
                .build();
        LocalDate newDate = LocalDate.now().plusDays(5);
        LocalTime newTime = LocalTime.of(10, 0);
        String newMemo = "레벨 재평가 필요";

        // when
        levelTest.updateDetails(newTeacher, newDate, newTime, newMemo);

        // then
        assertThat(levelTest.getTeacher()).isEqualTo(newTeacher);
        assertThat(levelTest.getTestDate()).isEqualTo(newDate);
        assertThat(levelTest.getTestTime()).isEqualTo(newTime);
        assertThat(levelTest.getMemo()).isEqualTo(newMemo);
    }

    @Test
    @DisplayName("updateDetails() - null 파라미터는 기존 값을 유지한다")
    void updateDetails_WithNullParameters_ShouldKeepExistingValues() {
        // given
        String originalMemo = "Original memo";
        levelTest = LevelTest.builder()
                .student(student)
                .teacher(teacher)
                .testDate(LocalDate.now())
                .testTime(LocalTime.of(14, 0))
                .memo(originalMemo)
                .build();

        // when - teacher만 null로 업데이트 시도
        levelTest.updateDetails(null, LocalDate.now().plusDays(1), null, null);

        // then
        assertThat(levelTest.getTeacher()).isEqualTo(teacher); // 유지됨
        assertThat(levelTest.getTestDate()).isEqualTo(LocalDate.now().plusDays(1)); // 변경됨
        assertThat(levelTest.getTestTime()).isEqualTo(LocalTime.of(14, 0)); // 유지됨
        assertThat(levelTest.getMemo()).isEqualTo(originalMemo); // 유지됨
    }

    @Test
    @DisplayName("updateMemo() - 메모를 업데이트할 수 있다")
    void updateMemo_ShouldUpdateMemo() {
        // given
        String newMemo = "추가 상담 필요";

        // when
        levelTest.updateMemo(newMemo);

        // then
        assertThat(levelTest.getMemo()).isEqualTo(newMemo);
    }

    @Test
    @DisplayName("markNotificationSent() - 알림 발송 완료 표시를 할 수 있다")
    void markNotificationSent_ShouldSetNotificationSentToTrue() {
        // given
        assertThat(levelTest.getMessageNotificationSent()).isFalse();

        // when
        levelTest.markNotificationSent();

        // then
        assertThat(levelTest.getMessageNotificationSent()).isTrue();
    }
}
