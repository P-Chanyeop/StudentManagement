package web.kplay.studentmanagement.controller.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.dto.auth.JwtResponse;
import web.kplay.studentmanagement.dto.auth.LoginRequest;
import web.kplay.studentmanagement.dto.auth.RefreshTokenRequest;
import web.kplay.studentmanagement.dto.auth.RegisterRequest;
import web.kplay.studentmanagement.dto.auth.SignupRequest;
import web.kplay.studentmanagement.dto.user.UserProfileResponse;
import web.kplay.studentmanagement.security.UserDetailsImpl;
import web.kplay.studentmanagement.service.auth.AuthService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        JwtResponse jwtResponse = authService.login(loginRequest);
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@Valid @RequestBody SignupRequest signupRequest) {
        authService.signup(signupRequest);
        Map<String, String> response = new HashMap<>();
        response.put("message", "회원가입이 완료되었습니다");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.register(registerRequest);
        Map<String, String> response = new HashMap<>();
        response.put("message", "회원가입이 완료되었습니다");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        JwtResponse jwtResponse = authService.refreshToken(request);
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        authService.logout(userDetails.getUsername());
        Map<String, String> response = new HashMap<>();
        response.put("message", "로그아웃되었습니다");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", userDetails.getId());
        response.put("username", userDetails.getUsername());
        response.put("role", userDetails.getRole());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        UserProfileResponse profile = authService.getUserProfile(userDetails.getId());
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsername(@RequestParam String username) {
        boolean available = authService.isUsernameAvailable(username);
        Map<String, Object> response = new HashMap<>();
        response.put("available", available);
        response.put("message", available ? "사용 가능한 아이디입니다" : "이미 사용 중인 아이디입니다");
        return ResponseEntity.ok(response);
    }
}
