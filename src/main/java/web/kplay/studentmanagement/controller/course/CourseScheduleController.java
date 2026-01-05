package web.kplay.studentmanagement.controller.course;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.dto.course.CourseScheduleCreateRequest;
import web.kplay.studentmanagement.dto.course.CourseScheduleResponse;
import web.kplay.studentmanagement.service.course.CourseScheduleService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class CourseScheduleController {

    private final CourseScheduleService scheduleService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<CourseScheduleResponse> createSchedule(@Valid @RequestBody CourseScheduleCreateRequest request) {
        CourseScheduleResponse response = scheduleService.createSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<CourseScheduleResponse> getSchedule(@PathVariable Long id) {
        CourseScheduleResponse response = scheduleService.getSchedule(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<List<CourseScheduleResponse>> getSchedulesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<CourseScheduleResponse> responses = scheduleService.getSchedulesByDate(date);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/available/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT', 'STUDENT')")
    public ResponseEntity<List<CourseScheduleResponse>> getAvailableSchedulesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<CourseScheduleResponse> responses = scheduleService.getAvailableSchedulesByDate(date);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<CourseScheduleResponse>> getSchedulesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<CourseScheduleResponse> responses = scheduleService.getSchedulesByDateRange(startDate, endDate);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<CourseScheduleResponse>> getSchedulesByCourse(@PathVariable Long courseId) {
        List<CourseScheduleResponse> responses = scheduleService.getSchedulesByCourse(courseId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<CourseScheduleResponse> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody CourseScheduleCreateRequest request) {
        CourseScheduleResponse response = scheduleService.updateSchedule(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, String>> cancelSchedule(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        scheduleService.cancelSchedule(id, reason);
        return ResponseEntity.ok(Map.of("message", "스케줄이 취소되었습니다"));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, String>> restoreSchedule(@PathVariable Long id) {
        scheduleService.restoreSchedule(id);
        return ResponseEntity.ok(Map.of("message", "스케줄이 복구되었습니다"));
    }

    // 선생님 본인 스케줄 조회
    @GetMapping("/my/{date}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<CourseScheduleResponse>> getMySchedules(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        List<CourseScheduleResponse> responses = scheduleService.getMySchedules(date, authentication);
        return ResponseEntity.ok(responses);
    }

    // 관리자 전체 스케줄 조회
    @GetMapping("/all/{date}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CourseScheduleResponse>> getAllSchedules(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<CourseScheduleResponse> responses = scheduleService.getAllSchedules(date);
        return ResponseEntity.ok(responses);
    }

    // 선생님 본인 월별 스케줄 조회
    @GetMapping("/my/monthly")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<List<CourseScheduleResponse>> getMyMonthlySchedules(
            @RequestParam int year,
            @RequestParam int month,
            Authentication authentication) {
        List<CourseScheduleResponse> responses = scheduleService.getMyMonthlySchedules(year, month, authentication);
        return ResponseEntity.ok(responses);
    }

    // 관리자 전체 월별 스케줄 조회
    @GetMapping("/all/monthly")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CourseScheduleResponse>> getAllMonthlySchedules(
            @RequestParam int year,
            @RequestParam int month) {
        List<CourseScheduleResponse> responses = scheduleService.getAllMonthlySchedules(year, month);
        return ResponseEntity.ok(responses);
    }
}
