package web.kplay.studentmanagement.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetSendCodeRequest {

    @NotBlank(message = "아이디는 필수입니다")
    private String username;

    @NotBlank(message = "휴대폰번호는 필수입니다")
    private String phoneNumber;
}
