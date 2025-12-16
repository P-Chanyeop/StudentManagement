package web.kplay.studentmanagement.domain.course;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;
import web.kplay.studentmanagement.domain.user.User;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Course extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String courseName;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @Column(nullable = false)
    private Integer maxStudents; // 최대 수강 인원

    @Column(nullable = false)
    private Integer durationMinutes; // 수업 시간(분)

    @Column(length = 50)
    private String level; // 수업 레벨

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(length = 10)
    private String color; // 캘린더 표시용 색상 코드

    // 수업 정보 업데이트
    public void updateInfo(String courseName, String description, Integer maxStudents,
                           Integer durationMinutes, String level, String color) {
        this.courseName = courseName;
        this.description = description;
        this.maxStudents = maxStudents;
        this.durationMinutes = durationMinutes;
        this.level = level;
        this.color = color;
    }

    // 선생님 배정
    public void assignTeacher(User teacher) {
        this.teacher = teacher;
    }

    // 수업 비활성화
    public void deactivate() {
        this.isActive = false;
    }

    // 수업 활성화
    public void activate() {
        this.isActive = true;
    }
}
