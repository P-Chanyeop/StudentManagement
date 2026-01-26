package web.kplay.studentmanagement.domain.teacher;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;
import web.kplay.studentmanagement.domain.user.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "teacher_attendances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TeacherAttendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User teacher;

    @Column(nullable = false)
    private LocalDate attendanceDate;

    private LocalDateTime checkInTime;

    private LocalDateTime checkOutTime;

    private String memo;

    public void checkIn(LocalDateTime time) {
        this.checkInTime = time;
    }

    public void checkOut(LocalDateTime time) {
        this.checkOutTime = time;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }
}
