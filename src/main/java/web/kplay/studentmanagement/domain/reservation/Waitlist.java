package web.kplay.studentmanagement.domain.reservation;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;
import web.kplay.studentmanagement.domain.student.Student;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "waitlists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Waitlist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private LocalDate waitDate;

    @Column(nullable = false)
    private LocalTime waitTime;

    @Column(length = 50)
    private String consultationType;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    public void deactivate() {
        this.active = false;
    }
}
