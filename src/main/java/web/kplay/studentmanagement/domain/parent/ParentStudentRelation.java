package web.kplay.studentmanagement.domain.parent;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.domain.user.User;

/**
 * 보호자-학생 관계 엔티티
 * 한 보호자가 여러 학생(자녀)을 관리할 수 있습니다.
 */
@Entity
@Table(name = "parent_student_relations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"parent_id", "student_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ParentStudentRelation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private User parent; // 보호자 User (ROLE_PARENT)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student; // 학생

    @Column(length = 20)
    private String relationship; // 관계 (부, 모, 조부모 등)

    @Column(nullable = false)
    private Boolean isActive = true; // 활성화 여부

    @Column(nullable = false)
    private Boolean canViewAttendance = true; // 출석 조회 권한

    @Column(nullable = false)
    private Boolean canViewGrades = true; // 성적 조회 권한

    @Column(nullable = false)
    private Boolean canViewInvoices = true; // 청구서 조회 권한

    @Column(nullable = false)
    private Boolean canMakeReservations = true; // 예약 생성 권한

    @Column(nullable = false)
    private Boolean canReceiveMessages = true; // 문자 수신 권한

    // 관계 비활성화
    public void deactivate() {
        this.isActive = false;
    }

    // 관계 활성화
    public void activate() {
        this.isActive = true;
    }

    // 권한 업데이트
    public void updatePermissions(Boolean canViewAttendance, Boolean canViewGrades,
                                   Boolean canViewInvoices, Boolean canMakeReservations,
                                   Boolean canReceiveMessages) {
        if (canViewAttendance != null) this.canViewAttendance = canViewAttendance;
        if (canViewGrades != null) this.canViewGrades = canViewGrades;
        if (canViewInvoices != null) this.canViewInvoices = canViewInvoices;
        if (canMakeReservations != null) this.canMakeReservations = canMakeReservations;
        if (canReceiveMessages != null) this.canReceiveMessages = canReceiveMessages;
    }

    // 모든 권한 부여
    public void grantAllPermissions() {
        this.canViewAttendance = true;
        this.canViewGrades = true;
        this.canViewInvoices = true;
        this.canMakeReservations = true;
        this.canReceiveMessages = true;
    }

    // 모든 권한 제거
    public void revokeAllPermissions() {
        this.canViewAttendance = false;
        this.canViewGrades = false;
        this.canViewInvoices = false;
        this.canMakeReservations = false;
        this.canReceiveMessages = false;
    }
}
