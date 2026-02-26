package web.kplay.studentmanagement.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.auth.PasswordResetCode;
import web.kplay.studentmanagement.domain.user.User;
import web.kplay.studentmanagement.repository.PasswordResetCodeRepository;
import web.kplay.studentmanagement.repository.UserRepository;
import web.kplay.studentmanagement.service.message.sms.SmsService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository resetCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsService smsService;

    private static final int CODE_EXPIRE_MINUTES = 5;
    private static final int RESET_TOKEN_EXPIRE_MINUTES = 10;
    private static final int MAX_FAIL_COUNT = 5;
    private static final int LOCK_MINUTES = 30;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int DAILY_SEND_LIMIT = 5;

    @Transactional
    public Map<String, Object> sendCode(String username, String phoneNumber) {
        String cleanPhone = phoneNumber.replaceAll("[^0-9]", "");

        // 사용자 조회 + 번호 매칭
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getPhoneNumber() == null ||
                !user.getPhoneNumber().replaceAll("[^0-9]", "").equals(cleanPhone)) {
            throw new IllegalArgumentException("아이디 또는 휴대폰번호가 일치하지 않습니다");
        }

        if (!user.getIsActive()) {
            throw new IllegalArgumentException("비활성화된 계정입니다. 관리자에게 문의하세요");
        }

        // 재발송 쿨다운 체크
        resetCodeRepository.findTopByPhoneNumberOrderByCreatedAtDesc(cleanPhone)
                .ifPresent(last -> {
                    long seconds = ChronoUnit.SECONDS.between(last.getCreatedAt(), LocalDateTime.now());
                    if (seconds < RESEND_COOLDOWN_SECONDS) {
                        throw new IllegalArgumentException(
                                String.format("잠시 후 다시 시도해주세요 (%d초 후 가능)", RESEND_COOLDOWN_SECONDS - seconds));
                    }
                });

        // 일일 발송 제한
        long dailyCount = resetCodeRepository.countByPhoneNumberAndCreatedAtAfter(
                cleanPhone, LocalDateTime.now().minusDays(1));
        if (dailyCount >= DAILY_SEND_LIMIT) {
            throw new IllegalArgumentException("일일 발송 한도를 초과했습니다");
        }

        // 인증번호 생성 및 저장
        String code = String.format("%06d", new Random().nextInt(1000000));
        PasswordResetCode resetCode = PasswordResetCode.builder()
                .username(username)
                .phoneNumber(cleanPhone)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(CODE_EXPIRE_MINUTES))
                .createdAt(LocalDateTime.now())
                .build();
        resetCodeRepository.save(resetCode);

        // SMS 발송
        try {
            String content = String.format("[리틀베어 리딩클럽] 인증번호 [%s]를 입력해주세요. (%d분 이내 입력)", code, CODE_EXPIRE_MINUTES);
            smsService.sendSms(cleanPhone, content);
            log.info("비밀번호 재설정 인증번호 발송: username={}", username);
        } catch (Exception e) {
            log.error("인증번호 SMS 발송 실패: {}", e.getMessage());
            throw new RuntimeException("인증번호 발송에 실패했습니다. 잠시 후 다시 시도해주세요");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "인증번호가 발송되었습니다");
        response.put("expiresIn", CODE_EXPIRE_MINUTES * 60);
        return response;
    }

    @Transactional
    public Map<String, Object> verifyCode(String username, String phoneNumber, String code) {
        String cleanPhone = phoneNumber.replaceAll("[^0-9]", "");

        PasswordResetCode resetCode = resetCodeRepository
                .findTopByUsernameAndPhoneNumberAndVerifiedFalseAndUsedFalseOrderByCreatedAtDesc(username, cleanPhone)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청을 찾을 수 없습니다. 인증번호를 다시 발송해주세요"));

        // 잠금 체크
        if (resetCode.isLocked()) {
            long remainMinutes = ChronoUnit.MINUTES.between(LocalDateTime.now(), resetCode.getLockedUntil()) + 1;
            throw new IllegalArgumentException(
                    String.format("인증 시도 횟수를 초과했습니다. %d분 후 다시 시도해주세요", remainMinutes));
        }

        // 만료 체크
        if (resetCode.isExpired()) {
            throw new IllegalArgumentException("인증번호가 만료되었습니다. 다시 발송해주세요");
        }

        // 코드 검증
        if (!resetCode.getCode().equals(code)) {
            resetCode.incrementFailCount();
            if (resetCode.getFailCount() >= MAX_FAIL_COUNT) {
                resetCode.lock(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                throw new IllegalArgumentException(
                        String.format("인증 시도 횟수를 초과했습니다. %d분 후 다시 시도해주세요", LOCK_MINUTES));
            }
            int remaining = MAX_FAIL_COUNT - resetCode.getFailCount();
            throw new IllegalArgumentException(
                    String.format("인증번호가 일치하지 않습니다 (%d회 남음)", remaining));
        }

        // 인증 성공 → resetToken 발급
        String resetToken = UUID.randomUUID().toString();
        resetCode.verify(resetToken);
        log.info("비밀번호 재설정 인증 성공: username={}", username);

        Map<String, Object> response = new HashMap<>();
        response.put("resetToken", resetToken);
        response.put("expiresIn", RESET_TOKEN_EXPIRE_MINUTES * 60);
        return response;
    }

    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        PasswordResetCode resetCode = resetCodeRepository.findByResetTokenAndUsedFalse(resetToken)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 요청입니다"));

        // 토큰 만료 체크 (인증 시점 + 10분)
        if (ChronoUnit.MINUTES.between(resetCode.getCreatedAt(), LocalDateTime.now()) > CODE_EXPIRE_MINUTES + RESET_TOKEN_EXPIRE_MINUTES) {
            throw new IllegalArgumentException("인증이 만료되었습니다. 처음부터 다시 진행해주세요");
        }

        // 비밀번호 규칙 검증
        validatePassword(newPassword);

        User user = userRepository.findByUsername(resetCode.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 기존 비밀번호 동일 체크
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("기존 비밀번호와 다른 비밀번호를 입력해주세요");
        }

        user.changePassword(passwordEncoder.encode(newPassword));
        resetCode.markUsed();
        log.info("비밀번호 재설정 완료: username={}", resetCode.getUsername());
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("비밀번호는 8글자 이상이어야 합니다");
        }
        if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$")) {
            throw new IllegalArgumentException("비밀번호는 영어와 숫자를 포함하여 8글자 이상이어야 합니다");
        }
    }
}
