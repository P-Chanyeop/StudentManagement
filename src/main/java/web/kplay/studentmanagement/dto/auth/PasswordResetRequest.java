package web.kplay.studentmanagement.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {

    @NotBlank(message = "인증 토큰은 필수입니다")
    private String resetToken;

    @NotBlank(message = "새 비밀번호는 필수입니다")
    private String newPassword;
}
