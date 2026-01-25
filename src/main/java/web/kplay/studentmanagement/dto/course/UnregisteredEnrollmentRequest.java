package web.kplay.studentmanagement.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UnregisteredEnrollmentRequest {
    // 학부모 정보
    @NotBlank(message = "학부모 이름은 필수입니다")
    private String parentName;
    
    @NotBlank(message = "학부모 전화번호는 필수입니다")
    private String parentPhone;
    
    // 학생 정보
    @NotBlank(message = "학생 이름은 필수입니다")
    private String studentName;
    
    private String studentPhone;
    private String school;
    private String grade;
    private String memo;
    
    // 수강권 정보
    @NotNull(message = "시작일은 필수입니다")
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    @NotNull(message = "총 횟수는 필수입니다")
    private Integer totalCount;
    
    private Long courseId;
    private String enrollmentMemo;
}
