package web.kplay.studentmanagement.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    
    @NotBlank(message = "아이디는 필수입니다")
    @Size(min = 4, message = "아이디는 4글자 이상이어야 합니다")
    private String username;
    
    @NotBlank(message = "비밀번호는 필수입니다")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$", 
             message = "비밀번호는 영어와 숫자를 포함하여 8글자 이상이어야 합니다")
    private String password;
    
    @NotBlank(message = "이름은 필수입니다")
    private String name;
    
    @NotBlank(message = "연락처는 필수입니다")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "올바른 연락처 형식을 입력해주세요")
    private String phoneNumber;
    
    @NotBlank(message = "주소는 필수입니다")
    private String address;
    
    private String role = "PARENT";
    
    // 학생 정보
    private StudentInfo student;
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentInfo {
        @NotBlank(message = "학생 이름은 필수입니다")
        private String studentName;
        
        private String studentPhone;
        
        @NotBlank(message = "생년월일은 필수입니다")
        private String birthDate;
        
        @NotBlank(message = "성별은 필수입니다")
        private String gender;
        
        @NotBlank(message = "학교는 필수입니다")
        private String school;
        
        @NotBlank(message = "학년은 필수입니다")
        private String grade;
        
        private String englishLevel = "1.0";
        
        private String parentName;
        
        private String parentPhone;
    }
}
