package web.kplay.studentmanagement.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import web.kplay.studentmanagement.service.StudentCourseImportService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentCourseImportController {

    private final StudentCourseImportService importService;

    @PostMapping("/import-courses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> importStudentCourses(
            @RequestParam("file") MultipartFile file) {
        try {
            log.info("학생 반 정보 임포트 시작: {}", file.getOriginalFilename());
            Map<String, Object> result = importService.importFromExcel(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("임포트 실패", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
