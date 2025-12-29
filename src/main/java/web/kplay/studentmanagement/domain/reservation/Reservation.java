package web.kplay.studentmanagement.domain.reservation;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;
import web.kplay.studentmanagement.domain.course.CourseSchedule;
import web.kplay.studentmanagement.domain.course.Enrollment;
import web.kplay.studentmanagement.domain.student.Student;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private CourseSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    private Enrollment enrollment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(length = 500)
    private String memo;

    @Column(length = 50)
    private String consultationType; // 상담 유형

    @Column(length = 200)
    private String cancelReason;

    @Column
    private LocalDateTime cancelledAt;

    @Column(length = 50)
    private String reservationSource; // 예약 출처 (WEB, NAVER, PHONE 등)

    // 예약 확정
    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    // 예약 취소 가능 여부 체크
    public boolean canCancel() {
        LocalDate scheduleDate = schedule.getScheduleDate();
        LocalDateTime cancelDeadline = scheduleDate.minusDays(1).atTime(18, 0); // 전날 오후 6시
        return LocalDateTime.now().isBefore(cancelDeadline) && status == ReservationStatus.CONFIRMED;
    }

    // 예약 취소
    public void cancel(String reason) {
        if (!canCancel()) {
            throw new IllegalStateException("예약 취소 기한이 지났습니다. (전날 오후 6시까지만 취소 가능)");
        }
        this.status = ReservationStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();

        // 수강권 횟수 복구는 서비스 레이어에서 처리
    }

    // 관리자 권한으로 강제 취소
    public void forceCancel(String reason) {
        this.status = ReservationStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();

        // 수강권 횟수 복구는 서비스 레이어에서 처리
    }

    // 수업 완료 처리
    public void complete() {
        this.status = ReservationStatus.COMPLETED;
    }

    // 노쇼 처리
    public void markAsNoShow() {
        this.status = ReservationStatus.NO_SHOW;
    }

    // 상태 업데이트 (자동 차감용)
    public void updateStatus(ReservationStatus status) {
        this.status = status;
    }

    // 메모 업데이트
    public void updateMemo(String memo) {
        this.memo = memo;
    }
}
