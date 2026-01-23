package web.kplay.studentmanagement.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import web.kplay.studentmanagement.service.StudentCourseExcelService;

import java.util.Map;

@RestController
@RequestMapping("/api/student-course")
@RequiredArgsConstructor
public class StudentCourseController {

    private final StudentCourseExcelService studentCourseExcelService;

    /**
     * 학생 목록 엑셀 파일 업로드
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadExcel(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "파일을 선택해주세요."));
            }
            
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.endsWith(".xlsx")) {
                return ResponseEntity.badRequest().body(Map.of("message", "xlsx 파일만 업로드 가능합니다."));
            }
            
            int count = studentCourseExcelService.uploadAndReload(file);
            return ResponseEntity.ok(Map.of(
                "message", "학생 목록이 업데이트되었습니다.",
                "studentCount", count
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "업로드 실패: " + e.getMessage()));
        }
    }

    /**
     * 현재 로드된 학생 수 조회
     */
    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStudentCount() {
        return ResponseEntity.ok(Map.of("studentCount", studentCourseExcelService.getStudentCount()));
    }
}
