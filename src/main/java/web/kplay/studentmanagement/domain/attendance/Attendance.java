package web.kplay.studentmanagement.domain.attendance;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;
import web.kplay.studentmanagement.domain.course.CourseSchedule;
import web.kplay.studentmanagement.domain.student.Student;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "attendances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Attendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private CourseSchedule schedule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    @Column
    private LocalDateTime checkInTime; // 체크인 시간

    @Column
    private LocalDateTime checkOutTime; // 체크아웃 시간

    @Column
    private LocalTime expectedLeaveTime; // 예상 하원 시간

    @Column(length = 500)
    private String memo;

    @Column(length = 200)
    private String reason; // 결석/지각 사유

    @Column(nullable = false)
    @Builder.Default
    private Boolean classCompleted = false; // 수업 완료 여부 (체크박스)

    // 출석 체크
    public void checkIn(LocalDateTime checkInTime, LocalTime expectedLeaveTime) {
        this.checkInTime = checkInTime;
        this.expectedLeaveTime = expectedLeaveTime;

        // 지각 여부 확인 (수업 시작 시간 10분 이후면 지각)
        LocalTime scheduleStartTime = schedule.getStartTime();
        LocalTime actualCheckInTime = checkInTime.toLocalTime();

        if (actualCheckInTime.isAfter(scheduleStartTime.plusMinutes(10))) {
            this.status = AttendanceStatus.LATE;
        } else {
            this.status = AttendanceStatus.PRESENT;
        }
    }

    // 하원 체크
    public void checkOut(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    // 출석 상태 변경
    public void updateStatus(AttendanceStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    // 자동 결석 처리 (수업 시작 20분 후)
    public void markAsAbsentIfLate() {
        if (this.checkInTime == null) {
            LocalTime scheduleStartTime = schedule.getStartTime();
            LocalTime cutoffTime = scheduleStartTime.plusMinutes(20);
            LocalTime currentTime = LocalDateTime.now().toLocalTime();
            
            if (currentTime.isAfter(cutoffTime)) {
                this.status = AttendanceStatus.ABSENT;
                this.reason = "20분 경과로 인한 자동 결석 처리";
            }
        }
    }

    // 결석 여부 확인
    public boolean isAbsent() {
        return this.status == AttendanceStatus.ABSENT;
    }

    // 메모 업데이트
    public void updateMemo(String memo) {
        this.memo = memo;
    }

    // 예상 하원 시간 업데이트
    public void updateExpectedLeaveTime(LocalTime expectedLeaveTime) {
        this.expectedLeaveTime = expectedLeaveTime;
    }

    // 수업 완료 처리
    public void completeClass() {
        this.classCompleted = true;
    }

    // 수업 완료 취소
    public void uncompleteClass() {
        this.classCompleted = false;
    }

    // 수업 완료 상태 토글
    public void toggleClassCompleted() {
        this.classCompleted = !this.classCompleted;
    }
}
