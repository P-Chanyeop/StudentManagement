package web.kplay.studentmanagement.domain.course;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;
import web.kplay.studentmanagement.domain.student.Student;

import java.time.LocalDate;

@Entity
@Table(name = "enrollments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Enrollment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    // 모든 수강권은 기간 + 횟수를 모두 가짐
    @Column(nullable = false)
    private LocalDate startDate; // 시작일

    @Column(nullable = false)
    private LocalDate endDate; // 종료일

    @Column(nullable = false)
    private Integer totalCount; // 총 횟수

    @Column(nullable = false)
    @Builder.Default
    private Integer usedCount = 0; // 사용 횟수

    @Column(nullable = false)
    private Integer remainingCount; // 남은 횟수

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column
    private Integer customDurationMinutes; // 학생별 개별 수업 시간 (분)

    @Column(length = 500)
    private String memo;

    // 홀딩 관련 필드
    @Column
    private LocalDate holdStartDate; // 홀딩 시작일

    @Column
    private LocalDate holdEndDate; // 홀딩 종료일

    @Column
    @Builder.Default
    private Boolean isOnHold = false; // 홀딩 중 여부

    @Column
    @Builder.Default
    private Integer totalHoldDays = 0; // 총 홀딩 일수

    // 횟수 사용
    public void useCount() {
        if (remainingCount > 0) {
            this.usedCount++;
            this.remainingCount--;
            // 횟수가 모두 소진되거나 기간이 만료되면 비활성화
            if (remainingCount == 0 || LocalDate.now().isAfter(endDate)) {
                this.isActive = false;
            }
        }
    }

    // 횟수 복구 (출석 취소 시)
    public void restoreCount() {
        if (usedCount > 0) {
            this.usedCount--;
            this.remainingCount++;
            // 횟수가 남아있고 기간이 유효하면 재활성화
            if (!this.isActive && remainingCount > 0 && !LocalDate.now().isAfter(endDate)) {
                this.isActive = true;
            }
        }
    }

    // 수강권 유효성 체크 (기간 + 횟수 모두 체크)
    public boolean isValid() {
        LocalDate now = LocalDate.now();
        return isActive &&
               !now.isBefore(startDate) &&
               !now.isAfter(endDate) &&
               remainingCount > 0;
    }

    // 수강권 비활성화
    public void deactivate() {
        this.isActive = false;
    }

    // 수강권 활성화
    public void activate() {
        this.isActive = true;
    }

    // 수강권 만료 처리
    public void expire() {
        this.isActive = false;
        // 종료일을 현재 날짜로 설정 (횟수는 유지)
        this.endDate = LocalDate.now();
    }

    // 수강권 정보 업데이트
    public void updateEnrollment(Course course, LocalDate startDate, LocalDate endDate, Integer totalCount, Integer remainingCount) {
        if (course != null) {
            this.course = course;
        }
        if (startDate != null) {
            this.startDate = startDate;
        }
        if (endDate != null) {
            this.endDate = endDate;
        }
        if (totalCount != null) {
            this.totalCount = totalCount;
        }
        if (remainingCount != null) {
            this.remainingCount = remainingCount;
            this.usedCount = totalCount - remainingCount;
        }
    }

    // 메모 업데이트
    public void updateMemo(String memo) {
        this.memo = memo;
    }

    // 개별 수업 시간 설정
    public void setCustomDuration(Integer durationMinutes) {
        this.customDurationMinutes = durationMinutes;
    }

    // 실제 수업 시간 반환 (개별 설정이 있으면 개별 시간, 없으면 코스 기본 시간)
    public Integer getActualDurationMinutes() {
        if (customDurationMinutes != null) {
            return customDurationMinutes;
        }
        return course != null ? course.getDurationMinutes() : null;
    }

    // 기간 연장 (입력 검증 추가)
    public void extendPeriod(LocalDate newEndDate) {
        // 입력 검증: null 체크
        if (newEndDate == null) {
            throw new IllegalArgumentException("새 종료일은 필수입니다.");
        }

        // 입력 검증: 새 종료일이 시작일보다 이전인지 체크
        if (newEndDate.isBefore(this.startDate)) {
            throw new IllegalArgumentException(
                String.format("종료일(%s)은 시작일(%s)보다 이후여야 합니다.", newEndDate, this.startDate)
            );
        }

        this.endDate = newEndDate;
        // 기간이 연장되고 횟수가 남아있으면 재활성화
        if (remainingCount > 0) {
            this.isActive = true;
        }
    }

    // 횟수 추가 (입력 검증 및 오버플로우 방지)
    public void addCount(Integer additionalCount) {
        // 입력 검증: null 체크
        if (additionalCount == null) {
            throw new IllegalArgumentException("추가할 횟수는 필수입니다.");
        }

        // 입력 검증: 양수 체크
        if (additionalCount <= 0) {
            throw new IllegalArgumentException("추가할 횟수는 1 이상이어야 합니다: " + additionalCount);
        }

        // 입력 검증: 최대값 체크 (매우 큰 값 방지)
        if (additionalCount > 10000) {
            throw new IllegalArgumentException("추가할 횟수가 너무 큽니다 (최대 10000): " + additionalCount);
        }

        // 오버플로우 방지: 합계가 Integer.MAX_VALUE를 초과하지 않는지 확인
        if ((long) this.totalCount + additionalCount > Integer.MAX_VALUE) {
            throw new ArithmeticException("총 횟수가 최대값을 초과합니다.");
        }
        if ((long) this.remainingCount + additionalCount > Integer.MAX_VALUE) {
            throw new ArithmeticException("남은 횟수가 최대값을 초과합니다.");
        }

        this.totalCount += additionalCount;
        this.remainingCount += additionalCount;
        // 횟수가 추가되고 기간이 유효하면 재활성화
        if (!LocalDate.now().isAfter(endDate)) {
            this.isActive = true;
        }
    }

    /**
     * 수동 횟수 조절 (관리자용)
     * 양수: 횟수 증가, 음수: 횟수 감소
     * 결석/보강/연기 등 수동 조절이 필요한 경우 사용
     */
    public void manualAdjustCount(Integer adjustment) {
        // 입력 검증: null 체크
        if (adjustment == null) {
            throw new IllegalArgumentException("조절할 횟수는 필수입니다.");
        }

        // 0인 경우 변경 없음
        if (adjustment == 0) {
            return;
        }

        // 입력 검증: 최대값 체크 (매우 큰 값 방지)
        if (Math.abs(adjustment) > 10000) {
            throw new IllegalArgumentException("조절할 횟수가 너무 큽니다 (최대 ±10000): " + adjustment);
        }

        // 횟수 감소 시 음수가 되지 않도록 확인
        if (adjustment < 0 && this.remainingCount + adjustment < 0) {
            throw new IllegalArgumentException(
                String.format("남은 횟수가 음수가 될 수 없습니다. 현재=%d, 조절=%d", this.remainingCount, adjustment)
            );
        }

        // 오버플로우 방지
        if (adjustment > 0) {
            if ((long) this.totalCount + adjustment > Integer.MAX_VALUE) {
                throw new ArithmeticException("총 횟수가 최대값을 초과합니다.");
            }
            if ((long) this.remainingCount + adjustment > Integer.MAX_VALUE) {
                throw new ArithmeticException("남은 횟수가 최대값을 초과합니다.");
            }
        }

        // 횟수 조절
        this.totalCount += adjustment;
        this.remainingCount += adjustment;

        // 횟수 조절 후 상태 업데이트
        if (this.remainingCount <= 0) {
            this.isActive = false;
        } else if (!LocalDate.now().isAfter(endDate)) {
            this.isActive = true;
        }
    }

    /**
     * 횟수 복원 (관리자 수동 조정용)
     */
    public void restoreCount(Integer count) {
        if (count <= 0) {
            throw new IllegalArgumentException("복원할 횟수는 0보다 커야 합니다");
        }
        this.remainingCount += count;
        this.usedCount = Math.max(0, this.usedCount - count);
        this.isActive = true;
    }

    /**
     * 홀딩 시작
     */
    public void startHold(LocalDate holdStartDate, LocalDate holdEndDate, LocalDate newEndDate) {
        if (holdStartDate.isAfter(holdEndDate)) {
            throw new IllegalArgumentException("홀딩 시작일은 종료일보다 이전이어야 합니다");
        }
        this.holdStartDate = holdStartDate;
        this.holdEndDate = holdEndDate;
        this.isOnHold = true;
        
        int holdDays = (int) java.time.temporal.ChronoUnit.DAYS.between(holdStartDate, holdEndDate) + 1;
        this.totalHoldDays += holdDays;
        this.endDate = newEndDate != null ? newEndDate : this.endDate.plusDays(holdDays);
    }

    /**
     * 홀딩 종료
     */
    public void endHold() {
        this.isOnHold = false;
        this.holdStartDate = null;
        this.holdEndDate = null;
    }

    /**
     * 홀딩 중인지 확인
     */
    public boolean isCurrentlyOnHold() {
        if (!isOnHold || holdStartDate == null || holdEndDate == null) {
            return false;
        }
        LocalDate now = LocalDate.now();
        return !now.isBefore(holdStartDate) && !now.isAfter(holdEndDate);
    }
}
