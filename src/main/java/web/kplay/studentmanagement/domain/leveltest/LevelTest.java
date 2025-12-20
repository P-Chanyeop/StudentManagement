package web.kplay.studentmanagement.domain.leveltest;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.domain.user.User;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "level_tests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LevelTest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @Column(nullable = false)
    private LocalDate testDate;

    @Column(nullable = false)
    private LocalTime testTime;

    @Column(length = 20)
    private String testStatus; // SCHEDULED, COMPLETED, CANCELLED

    @Column(length = 50)
    private String testResult; // 테스트 결과 레벨

    @Column
    private Integer testScore; // 테스트 점수

    @Column(length = 1000)
    private String feedback; // 피드백

    @Column(length = 1000)
    private String strengths; // 강점

    @Column(length = 1000)
    private String improvements; // 개선 필요 사항

    @Column(length = 200)
    private String recommendedLevel; // 권장 레벨

    @Column(length = 500)
    private String memo;

    @Column
    private Boolean messageNotificationSent = false; // 문자 안내 발송 여부

    // 테스트 완료 처리 (null인 경우 기존 값 유지)
    public void complete(String testResult, Integer testScore, String feedback, String strengths,
                         String improvements, String recommendedLevel) {
        this.testStatus = "COMPLETED";
        if (testResult != null) {
            this.testResult = testResult;
        }
        if (testScore != null) {
            this.testScore = testScore;
        }
        if (feedback != null) {
            this.feedback = feedback;
        }
        if (strengths != null) {
            this.strengths = strengths;
        }
        if (improvements != null) {
            this.improvements = improvements;
        }
        if (recommendedLevel != null) {
            this.recommendedLevel = recommendedLevel;
        }
    }

    // 테스트 취소
    public void cancel() {
        this.testStatus = "CANCELLED";
    }

    // 선생님 배정
    public void assignTeacher(User teacher) {
        this.teacher = teacher;
    }

    // 일정 변경
    public void reschedule(LocalDate newDate, LocalTime newTime) {
        this.testDate = newDate;
        this.testTime = newTime;
    }

    // 문자 알림 발송 완료 표시
    public void markNotificationSent() {
        this.messageNotificationSent = true;
    }

    // 메모 업데이트
    public void updateMemo(String memo) {
        this.memo = memo;
    }

    // 레벨테스트 상세 정보 업데이트 (일정, 선생님, 메모)
    public void updateDetails(User teacher, LocalDate testDate, LocalTime testTime, String memo) {
        if (teacher != null) {
            this.teacher = teacher;
        }
        if (testDate != null) {
            this.testDate = testDate;
        }
        if (testTime != null) {
            this.testTime = testTime;
        }
        if (memo != null) {
            this.memo = memo;
        }
    }
}
