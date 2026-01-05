package web.kplay.studentmanagement.controller.attendance;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.domain.attendance.AttendanceStatus;
import web.kplay.studentmanagement.dto.attendance.AttendanceCheckInRequest;
import web.kplay.studentmanagement.dto.attendance.AttendanceResponse;
import web.kplay.studentmanagement.service.attendance.AttendanceService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/checkin")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<AttendanceResponse> checkIn(@Valid @RequestBody AttendanceCheckInRequest request) {
        AttendanceResponse response = attendanceService.checkIn(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/checkout")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<AttendanceResponse> checkOut(@PathVariable Long id) {
        AttendanceResponse response = attendanceService.checkOut(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<AttendanceResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam AttendanceStatus status,
            @RequestParam(required = false) String reason) {
        AttendanceResponse response = attendanceService.updateStatus(id, status, reason);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AttendanceResponse> responses = attendanceService.getAttendanceByDate(date);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByStudent(@PathVariable Long studentId) {
        List<AttendanceResponse> responses = attendanceService.getAttendanceByStudent(studentId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/schedule/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceBySchedule(@PathVariable Long scheduleId) {
        List<AttendanceResponse> responses = attendanceService.getAttendanceBySchedule(scheduleId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/student/{studentId}/range")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByStudentAndDateRange(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AttendanceResponse> responses = attendanceService.getAttendanceByStudentAndDateRange(
                studentId, startDate, endDate);
        return ResponseEntity.ok(responses);
    }

    /**
     * 출석한 순서대로 조회 (등원 시간 오름차순)
     */
    @GetMapping("/date/{date}/check-in-order")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByCheckInOrder(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AttendanceResponse> responses = attendanceService.getAttendanceByCheckInOrder(date);
        return ResponseEntity.ok(responses);
    }

    /**
     * 하원 예정 순서대로 조회 (예상 하원 시간 오름차순)
     */
    @GetMapping("/date/{date}/leave-order")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByLeaveOrder(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AttendanceResponse> responses = attendanceService.getAttendanceByLeaveOrder(date);
        return ResponseEntity.ok(responses);
    }

    /**
     * 오늘 출석한 학생만 조회
     */
    @GetMapping("/date/{date}/attended")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<AttendanceResponse>> getAttendedStudents(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AttendanceResponse> responses = attendanceService.getAttendedStudents(date);
        return ResponseEntity.ok(responses);
    }

    /**
     * 오늘 출석하지 않은 학생 조회
     */
    @GetMapping("/date/{date}/not-attended")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<AttendanceResponse>> getNotAttendedStudents(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AttendanceResponse> responses = attendanceService.getNotAttendedStudents(date);
        return ResponseEntity.ok(responses);
    }

    /**
     * 하원 완료된 학생만 조회
     */
    @GetMapping("/date/{date}/checked-out")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<AttendanceResponse>> getCheckedOutStudents(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AttendanceResponse> responses = attendanceService.getCheckedOutStudents(date);
        return ResponseEntity.ok(responses);
    }

    /**
     * 아직 하원하지 않은 학생 조회
     */
    @GetMapping("/date/{date}/not-checked-out")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<AttendanceResponse>> getNotCheckedOutStudents(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AttendanceResponse> responses = attendanceService.getNotCheckedOutStudents(date);
        return ResponseEntity.ok(responses);
    }

    /**
     * 출석 취소 (출석 기록 삭제)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Void> cancelAttendance(@PathVariable Long id) {
        attendanceService.cancelAttendance(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 수업 완료 체크박스 토글
     */
    @PatchMapping("/{id}/toggle-completed")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<AttendanceResponse> toggleClassCompleted(@PathVariable Long id) {
        AttendanceResponse response = attendanceService.toggleClassCompleted(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 수업 완료 처리
     */
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<AttendanceResponse> completeClass(@PathVariable Long id) {
        AttendanceResponse response = attendanceService.completeClass(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 수업 완료 취소
     */
    @PatchMapping("/{id}/uncomplete")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<AttendanceResponse> uncompleteClass(@PathVariable Long id) {
        AttendanceResponse response = attendanceService.uncompleteClass(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 사유 업데이트
     */
    @PatchMapping("/{id}/reason")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<AttendanceResponse> updateReason(
            @PathVariable Long id, 
            @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        AttendanceResponse response = attendanceService.updateReason(id, reason);
        return ResponseEntity.ok(response);
    }

    /**
     * 학부모용 자녀 출석 조회
     */
    @GetMapping("/my-child/{date}")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<List<AttendanceResponse>> getMyChildAttendances(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        String username = authentication.getName();
        List<AttendanceResponse> responses = attendanceService.getMyChildAttendances(username, date);
        return ResponseEntity.ok(responses);
    }

    // 학부모 자녀 월별 출석 조회
    @GetMapping("/my-child/monthly")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<List<AttendanceResponse>> getMyChildMonthlyAttendances(
            @RequestParam int year,
            @RequestParam int month,
            Authentication authentication) {
        String username = authentication.getName();
        List<AttendanceResponse> responses = attendanceService.getMyChildMonthlyAttendances(username, year, month);
        return ResponseEntity.ok(responses);
    }

    // 학부모 자녀 수업 정보 조회 (스케줄 기반)
    @GetMapping("/my-child/schedules/{date}")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<List<AttendanceResponse>> getMyChildSchedules(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication) {
        String username = authentication.getName();
        List<AttendanceResponse> responses = attendanceService.getMyChildSchedules(username, date);
        return ResponseEntity.ok(responses);
    }

    // 학부모 자녀 월별 수업 정보 조회
    @GetMapping("/my-child/schedules/monthly")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<List<AttendanceResponse>> getMyChildMonthlySchedules(
            @RequestParam int year,
            @RequestParam int month,
            Authentication authentication) {
        String username = authentication.getName();
        List<AttendanceResponse> responses = attendanceService.getMyChildMonthlySchedules(username, year, month);
        return ResponseEntity.ok(responses);
    }
}
