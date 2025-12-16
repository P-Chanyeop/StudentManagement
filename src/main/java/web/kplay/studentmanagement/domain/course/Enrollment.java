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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentType enrollmentType;

    @Column
    private LocalDate startDate; // 시작일 (기간권)

    @Column
    private LocalDate endDate; // 종료일 (기간권)

    @Column
    private Integer totalCount; // 총 횟수 (횟수권)

    @Column
    private Integer usedCount = 0; // 사용 횟수 (횟수권)

    @Column
    private Integer remainingCount; // 남은 횟수 (횟수권)

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(length = 500)
    private String memo;

    // 횟수 사용
    public void useCount() {
        if (enrollmentType == EnrollmentType.COUNT && remainingCount > 0) {
            this.usedCount++;
            this.remainingCount--;
            if (remainingCount == 0) {
                this.isActive = false;
            }
        }
    }

    // 횟수 복구 (출석 취소 시)
    public void restoreCount() {
        if (enrollmentType == EnrollmentType.COUNT && usedCount > 0) {
            this.usedCount--;
            this.remainingCount++;
            if (!this.isActive && remainingCount > 0) {
                this.isActive = true;
            }
        }
    }

    // 기간권 유효성 체크
    public boolean isValid() {
        if (enrollmentType == EnrollmentType.PERIOD) {
            LocalDate now = LocalDate.now();
            return isActive && !now.isBefore(startDate) && !now.isAfter(endDate);
        } else if (enrollmentType == EnrollmentType.COUNT) {
            return isActive && remainingCount > 0;
        }
        return false;
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
        if (enrollmentType == EnrollmentType.PERIOD) {
            this.endDate = newEndDate;
            this.isActive = true;
        }
    }

    // 횟수 추가
    public void addCount(Integer additionalCount) {
        if (enrollmentType == EnrollmentType.COUNT) {
            this.totalCount += additionalCount;
            this.remainingCount += additionalCount;
            this.isActive = true;
        }
    }
}
