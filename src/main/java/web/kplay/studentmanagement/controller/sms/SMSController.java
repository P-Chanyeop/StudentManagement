package web.kplay.studentmanagement.controller.sms;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SMSController {

    /**
     * SMS 통계 조회
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        // TODO: 실제 SMS 통계 구현
        Map<String, Object> stats = Map.of(
                "balance", 10000,
                "sentToday", 0,
                "sentThisMonth", 0,
                "provider", "알리고"
        );
        return ResponseEntity.ok(stats);
    }

    /**
     * 템플릿 조회
     */
    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<Map<String, Object>>> getTemplates() {
        // TODO: 실제 템플릿 DB 조회
        List<Map<String, Object>> templates = List.of(
                Map.of(
                        "id", 1,
                        "name", "출석 리마인더",
                        "category", "attendance",
                        "content", "[K-PLAY 학원]\\n안녕하세요, {학부모명}님.\\n{학생명} 학생의 오늘 수업이 {수업시간}에 예정되어 있습니다.\\n잊지 말고 참석 부탁드립니다!"
                )
        );
        return ResponseEntity.ok(templates);
    }

    /**
     * 템플릿 생성
     */
    @PostMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, Object>> createTemplate(@RequestBody Map<String, String> request) {
        // TODO: 실제 템플릿 DB 저장
        Map<String, Object> template = Map.of(
                "id", 1,
                "name", request.get("name"),
                "category", request.get("category"),
                "content", request.get("content")
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(template);
    }

    /**
     * 템플릿 삭제
     */
    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        // TODO: 실제 템플릿 DB 삭제
        return ResponseEntity.noContent().build();
    }

    /**
     * SMS 발송
     */
    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, Object>> sendSMS(@RequestBody Map<String, Object> request) {
        // TODO: 실제 SMS 발송 로직 (알리고/문자나라 API 연동)
        // 현재는 성공 응답만 반환
        Map<String, Object> response = Map.of(
                "success", true,
                "message", "SMS가 발송되었습니다.",
                "sentCount", request.getOrDefault("recipients", List.of()).toString().split(",").length
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 발송 내역 조회
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        // TODO: 실제 발송 내역 DB 조회
        List<Map<String, Object>> history = List.of();
        return ResponseEntity.ok(history);
    }

    /**
     * SMS 설정 저장
     */
    @PostMapping("/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> saveSettings(@RequestBody Map<String, String> request) {
        // TODO: 실제 설정 DB 저장 (암호화 필요)
        return ResponseEntity.ok(Map.of("message", "설정이 저장되었습니다."));
    }

    /**
     * 연결 테스트
     */
    @PostMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testConnection() {
        // TODO: 실제 SMS 공급사 API 연결 테스트
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "연결 테스트 성공"
        ));
    }

    /**
     * 자동 발송 설정 저장
     */
    @PostMapping("/auto-settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> saveAutoSettings(@RequestBody Map<String, Boolean> request) {
        // TODO: 실제 자동 발송 설정 DB 저장
        return ResponseEntity.ok(Map.of("message", "자동 발송 설정이 저장되었습니다."));
    }
}
