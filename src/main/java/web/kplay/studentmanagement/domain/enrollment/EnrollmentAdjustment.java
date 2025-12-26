package web.kplay.studentmanagement.domain.enrollment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import web.kplay.studentmanagement.domain.BaseEntity;
import web.kplay.studentmanagement.domain.course.Enrollment;
import web.kplay.studentmanagement.domain.user.User;

@Entity
@Table(name = "enrollment_adjustments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnrollmentAdjustment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false)
    private AdjustmentType adjustmentType;

    @Column(name = "count_change", nullable = false)
    private Integer countChange;

    @Column(name = "reason", length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Builder
    public EnrollmentAdjustment(Enrollment enrollment, AdjustmentType adjustmentType, 
                               Integer countChange, String reason, User admin) {
        this.enrollment = enrollment;
        this.adjustmentType = adjustmentType;
        this.countChange = countChange;
        this.reason = reason;
        this.admin = admin;
    }

    public enum AdjustmentType {
        DEDUCT("차감"),
        ADD("추가"),
        RESTORE("복원");

        private final String description;

        AdjustmentType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
