package web.kplay.studentmanagement.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentResponse {
    private Long id;
    private String studentName;
    private String studentPhone;
    private LocalDate birthDate;
    private String gender;
    private String address;
    private String school;
    private String grade;
    private String englishLevel;
    private String memo;
    private String parentName;
    private String parentPhone;
    private String parentEmail;
    private Boolean isActive;
}
