package web.kplay.studentmanagement.domain.makeup;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import web.kplay.studentmanagement.domain.course.Course;
import web.kplay.studentmanagement.domain.student.Student;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 보강 수업 엔티티
 * 학생이 결석하거나 사전에 못 들은 수업에 대한 보강 수업 기록
 */
@Entity
@Table(name = "makeup_classes")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MakeupClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // 원래 수업 날짜 (결석한 날짜)
    @Column(nullable = false)
    private LocalDate originalDate;

    // 보강 수업 날짜
    @Column(nullable = false)
    private LocalDate makeupDate;

    // 보강 수업 시작 시간
    @Column(nullable = false)
    private LocalTime makeupTime;

    // 보강 사유
    @Column(length = 500)
    private String reason;

    // 보강 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MakeupStatus status;

    // 메모
    @Column(length = 1000)
    private String memo;

    /**
     * 보강 상태 변경
     */
    public void updateStatus(MakeupStatus newStatus) {
        this.status = newStatus;
    }

    /**
     * 보강 일정 변경
     */
    public void reschedule(LocalDate newDate, LocalTime newTime) {
        this.makeupDate = newDate;
        this.makeupTime = newTime;
    }

    /**
     * 메모 업데이트
     */
    public void updateMemo(String newMemo) {
        this.memo = newMemo;
    }
}
