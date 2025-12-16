package web.kplay.studentmanagement.domain.course;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "course_schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CourseSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private LocalDate scheduleDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(length = 20)
    private String dayOfWeek; // 요일

    @Column(nullable = false)
    private Integer currentStudents = 0; // 현재 등록된 학생 수

    @Column(nullable = false)
    private Boolean isCancelled = false; // 수업 취소 여부

    @Column(length = 200)
    private String cancelReason; // 취소 사유

    @Column(length = 500)
    private String memo;

    // 학생 등록
    public void addStudent() {
        this.currentStudents++;
    }

    // 학생 취소
    public void removeStudent() {
        if (this.currentStudents > 0) {
            this.currentStudents--;
        }
    }

    // 수업 취소
    public void cancel(String reason) {
        this.isCancelled = true;
        this.cancelReason = reason;
    }

    // 수업 취소 복구
    public void restore() {
        this.isCancelled = false;
        this.cancelReason = null;
    }

    // 메모 업데이트
    public void updateMemo(String memo) {
        this.memo = memo;
    }

    // 스케줄 시간 변경
    public void updateTime(LocalTime startTime, LocalTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
