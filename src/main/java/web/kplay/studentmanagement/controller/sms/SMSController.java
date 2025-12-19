package web.kplay.studentmanagement.controller.sms;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.service.sms.SMSService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SMSController {

    private final SMSService smsService;

    /**
     * SMS 통계 조회
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(smsService.getStatistics());
    }

    /**
     * 템플릿 조회
     */
    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<Map<String, Object>>> getTemplates() {
        return ResponseEntity.ok(smsService.getActiveTemplates());
    }

    /**
     * 템플릿 생성
     */
    @PostMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, Object>> createTemplate(@RequestBody Map<String, String> request) {
        Map<String, Object> template = smsService.createTemplate(
                request.get("name"),
                request.get("category"),
                request.get("content"),
                request.get("description")
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(template);
    }

    /**
     * 템플릿 수정
     */
    @PutMapping("/templates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, Object>> updateTemplate(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        Map<String, Object> template = smsService.updateTemplate(
                id,
                request.get("name"),
                request.get("category"),
                request.get("content"),
                request.get("description")
        );
        return ResponseEntity.ok(template);
    }

    /**
     * 템플릿 삭제
     */
    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        smsService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * SMS 발송
     */
    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, Object>> sendSMS(@RequestBody Map<String, Object> request) {
        String recipientType = (String) request.get("recipientType");
        String message = (String) request.get("message");
        String category = (String) request.getOrDefault("category", "general");

        // 수신자 목록 추출
        @SuppressWarnings("unchecked")
        List<String> recipients = (List<String>) request.getOrDefault("recipients", List.of());

        if (recipients.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "수신자를 선택해주세요."
            ));
        }

        if (message == null || message.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "메시지 내용을 입력해주세요."
            ));
        }

        // 대량 발송
        Map<String, Object> result = smsService.sendBulkSMS(recipients, message, category);
        return ResponseEntity.ok(result);
    }

    /**
     * 단일 SMS 발송
     */
    @PostMapping("/send-single")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, Object>> sendSingleSMS(@RequestBody Map<String, String> request) {
        String receiverNumber = request.get("receiverNumber");
        String receiverName = request.getOrDefault("receiverName", "");
        String message = request.get("message");
        String category = request.getOrDefault("category", "general");

        if (receiverNumber == null || receiverNumber.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "수신 번호를 입력해주세요."
            ));
        }

        if (message == null || message.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "메시지 내용을 입력해주세요."
            ));
        }

        Map<String, Object> result = smsService.sendSMS(receiverNumber, receiverName, message, category);
        return ResponseEntity.ok(result);
    }

    /**
     * 발송 내역 조회
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // 기본값 설정 (최근 30일)
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<Map<String, Object>> history = smsService.getHistory(startDate, endDate);
        return ResponseEntity.ok(history);
    }

    /**
     * 최근 발송 내역 조회
     */
    @GetMapping("/history/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<Map<String, Object>>> getRecentHistory() {
        return ResponseEntity.ok(smsService.getRecentHistory());
    }

    /**
     * SMS 설정 저장
     */
    @PostMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> saveSettings(@RequestBody Map<String, String> request) {
        String provider = request.getOrDefault("provider", "ALIGO");
        String apiKey = request.get("apiKey");
        String userId = request.get("userId");
        String senderNumber = request.get("senderNumber");

        if (apiKey == null || apiKey.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "API Key를 입력해주세요."));
        }

        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "사용자 ID를 입력해주세요."));
        }

        if (senderNumber == null || senderNumber.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "발신번호를 입력해주세요."));
        }

        return ResponseEntity.ok(smsService.saveSettings(provider, apiKey, userId, senderNumber));
    }

    /**
     * 연결 테스트
     */
    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testConnection() {
        return ResponseEntity.ok(smsService.testConnection());
    }

    /**
     * 자동 발송 설정 저장
     */
    @PostMapping("/auto-settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> saveAutoSettings(@RequestBody Map<String, Boolean> request) {
        Boolean attendanceReminder = request.get("attendanceReminder");
        Boolean enrollmentExpiry = request.get("enrollmentExpiry");
        Boolean paymentReminder = request.get("paymentReminder");

        return ResponseEntity.ok(smsService.saveAutoSettings(attendanceReminder, enrollmentExpiry, paymentReminder));
    }
}
