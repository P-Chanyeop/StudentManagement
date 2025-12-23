package web.kplay.studentmanagement.domain.student;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;
import web.kplay.studentmanagement.domain.user.User;

import java.time.LocalDate;

@Entity
@Table(name = "students")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Student extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_user_id")
    private User parentUser; // 학부모 계정과의 연결

    @Column(nullable = false, length = 50)
    private String studentName;

    @Column(length = 20)
    private String studentPhone;

    @Column
    private LocalDate birthDate;

    @Column(length = 10)
    private String gender; // MALE, FEMALE

    @Column(length = 200)
    private String address;

    @Column(length = 50)
    private String school;

    @Column(length = 20)
    private String grade; // 학년

    @Column(length = 50)
    private String englishLevel; // 영어 레벨

    @Column(length = 500)
    private String memo;

    @Column(length = 100)
    private String parentName;

    @Column(length = 20)
    private String parentPhone;

    @Column(length = 100)
    private String parentEmail;

    @Column(nullable = false)
    private Boolean isActive = true;

    // 학생 정보 업데이트
    public void updateInfo(String studentName, String studentPhone, LocalDate birthDate,
                           String gender, String address, String school, String grade) {
        this.studentName = studentName;
        this.studentPhone = studentPhone;
        this.birthDate = birthDate;
        this.gender = gender;
        this.address = address;
        this.school = school;
        this.grade = grade;
    }

    // 학부모 정보 업데이트
    public void updateParentInfo(String parentName, String parentPhone, String parentEmail) {
        this.parentName = parentName;
        this.parentPhone = parentPhone;
        this.parentEmail = parentEmail;
    }

    // 학부모 계정 연결
    public void setParentUser(User parentUser) {
        this.parentUser = parentUser;
    }

    // 영어 레벨 업데이트
    public void updateEnglishLevel(String englishLevel) {
        this.englishLevel = englishLevel;
    }

    // 메모 업데이트
    public void updateMemo(String memo) {
        this.memo = memo;
    }

    // 학생 비활성화
    public void deactivate() {
        this.isActive = false;
    }

    // 학생 활성화
    public void activate() {
        this.isActive = true;
    }
}
