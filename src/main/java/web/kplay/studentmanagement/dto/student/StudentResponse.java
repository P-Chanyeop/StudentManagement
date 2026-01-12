package web.kplay.studentmanagement.dto.student;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import web.kplay.studentmanagement.dto.course.EnrollmentResponse;

import java.time.LocalDate;
import java.util.List;

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
    private List<EnrollmentResponse> enrollments;
}
