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
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // 모든 수강권은 기간 + 횟수를 모두 가짐
    @Column(nullable = false)
    private LocalDate startDate; // 시작일

    @Column(nullable = false)
    private LocalDate endDate; // 종료일

    @Column(nullable = false)
    private Integer totalCount; // 총 횟수

    @Column(nullable = false)
    private Integer usedCount = 0; // 사용 횟수

    @Column(nullable = false)
    private Integer remainingCount; // 남은 횟수

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(length = 500)
    private String memo;

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

    // 메모 업데이트
    public void updateMemo(String memo) {
        this.memo = memo;
    }

    // 기간 연장
    public void extendPeriod(LocalDate newEndDate) {
        this.endDate = newEndDate;
        // 기간이 연장되고 횟수가 남아있으면 재활성화
        if (remainingCount > 0) {
            this.isActive = true;
        }
    }

    // 횟수 추가
    public void addCount(Integer additionalCount) {
        this.totalCount += additionalCount;
        this.remainingCount += additionalCount;
        // 횟수가 추가되고 기간이 유효하면 재활성화
        if (!LocalDate.now().isAfter(endDate)) {
            this.isActive = true;
        }
    }
}
