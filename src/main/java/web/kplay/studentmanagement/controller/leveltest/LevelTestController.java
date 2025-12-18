package web.kplay.studentmanagement.controller.leveltest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.dto.leveltest.LevelTestRequest;
import web.kplay.studentmanagement.dto.leveltest.LevelTestResponse;
import web.kplay.studentmanagement.service.leveltest.LevelTestService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leveltests")
@RequiredArgsConstructor
public class LevelTestController {

    private final LevelTestService levelTestService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<LevelTestResponse> createLevelTest(@Valid @RequestBody LevelTestRequest request) {
        LevelTestResponse response = levelTestService.createLevelTest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<LevelTestResponse> completeLevelTest(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        LevelTestResponse response = levelTestService.completeLevelTest(
                id,
                request.get("testResult"),
                request.get("feedback"),
                request.get("strengths"),
                request.get("improvements"),
                request.get("recommendedLevel")
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<LevelTestResponse>> getLevelTestsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<LevelTestResponse> responses = levelTestService.getLevelTestsByDateRange(startDate, endDate);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<List<LevelTestResponse>> getLevelTestsByStudent(@PathVariable Long studentId) {
        List<LevelTestResponse> responses = levelTestService.getLevelTestsByStudent(studentId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 월별 레벨테스트 조회 (캘린더 뷰)
     */
    @GetMapping("/calendar/month")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<LevelTestResponse>> getLevelTestsByMonth(
            @RequestParam int year,
            @RequestParam int month) {
        List<LevelTestResponse> responses = levelTestService.getLevelTestsByMonth(year, month);
        return ResponseEntity.ok(responses);
    }

    /**
     * 주간 레벨테스트 조회 (주간 뷰)
     */
    @GetMapping("/calendar/week")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<LevelTestResponse>> getLevelTestsByWeek(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekEnd) {
        List<LevelTestResponse> responses = levelTestService.getLevelTestsByWeek(weekStart, weekEnd);
        return ResponseEntity.ok(responses);
    }

    /**
     * 선생님별 레벨테스트 조회
     */
    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<LevelTestResponse>> getLevelTestsByTeacher(@PathVariable Long teacherId) {
        List<LevelTestResponse> responses = levelTestService.getLevelTestsByTeacher(teacherId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 예정된 레벨테스트 조회
     */
    @GetMapping("/upcoming")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<LevelTestResponse>> getUpcomingTests() {
        List<LevelTestResponse> responses = levelTestService.getUpcomingTests();
        return ResponseEntity.ok(responses);
    }

    /**
     * 완료된 레벨테스트 조회
     */
    @GetMapping("/completed")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<LevelTestResponse>> getCompletedTests() {
        List<LevelTestResponse> responses = levelTestService.getCompletedTests();
        return ResponseEntity.ok(responses);
    }

    /**
     * 레벨테스트 통계 조회
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Map<String, Object> statistics = levelTestService.getStatistics(startDate, endDate);
        return ResponseEntity.ok(statistics);
    }
}
