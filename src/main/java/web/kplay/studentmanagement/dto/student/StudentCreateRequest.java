package web.kplay.studentmanagement.dto.student;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StudentCreateRequest {

    @NotBlank(message = "학생 이름은 필수입니다")
    private String studentName;

    private String studentPhone;
    private LocalDate birthDate;
    private String gender;
    private String address;
    private String school;
    private String grade;
    private String englishLevel;
    private String memo;

    @NotBlank(message = "학부모 이름은 필수입니다")
    private String parentName;

    @NotBlank(message = "학부모 전화번호는 필수입니다")
    private String parentPhone;

    private String parentEmail;
}
