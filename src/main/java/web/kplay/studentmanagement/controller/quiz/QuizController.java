package web.kplay.studentmanagement.controller.quiz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import web.kplay.studentmanagement.service.QuizService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
@Slf4j
public class QuizController {

    private final QuizService quizService;

    @PostMapping("/upload-renaissance-ids")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadRenaissanceIds(@RequestParam("file") MultipartFile file) {
        try {
            int updatedCount = quizService.uploadRenaissanceIds(file);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("updatedCount", updatedCount);
            response.put("message", updatedCount + "명의 학생 정보가 업데이트되었습니다.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("르네상스 ID 업로드 실패", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "업로드 실패: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
}
