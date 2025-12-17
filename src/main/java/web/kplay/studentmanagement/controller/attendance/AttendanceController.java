package web.kplay.studentmanagement.controller.attendance;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.domain.attendance.AttendanceStatus;
import web.kplay.studentmanagement.dto.attendance.AttendanceCheckInRequest;
import web.kplay.studentmanagement.dto.attendance.AttendanceResponse;
import web.kplay.studentmanagement.service.attendance.AttendanceService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
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
}
