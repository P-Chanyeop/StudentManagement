package web.kplay.studentmanagement.controller.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.dto.auth.PasswordResetRequest;
import web.kplay.studentmanagement.dto.auth.PasswordResetSendCodeRequest;
import web.kplay.studentmanagement.dto.auth.PasswordResetVerifyRequest;
import web.kplay.studentmanagement.service.auth.PasswordResetService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/send-code")
    public ResponseEntity<Map<String, Object>> sendCode(@Valid @RequestBody PasswordResetSendCodeRequest request) {
        return ResponseEntity.ok(passwordResetService.sendCode(request.getUsername(), request.getPhoneNumber()));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<Map<String, Object>> verifyCode(@Valid @RequestBody PasswordResetVerifyRequest request) {
        return ResponseEntity.ok(passwordResetService.verifyCode(
                request.getUsername(), request.getPhoneNumber(), request.getCode()));
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.resetPassword(request.getResetToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "비밀번호가 변경되었습니다"));
    }
}
