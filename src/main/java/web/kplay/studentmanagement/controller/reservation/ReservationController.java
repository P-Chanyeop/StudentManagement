package web.kplay.studentmanagement.controller.reservation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.dto.reservation.ReservationCreateRequest;
import web.kplay.studentmanagement.dto.reservation.ReservationResponse;
import web.kplay.studentmanagement.service.reservation.ReservationService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT', 'STUDENT')")
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody ReservationCreateRequest request) {
        ReservationResponse response = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable Long id) {
        ReservationResponse response = reservationService.getReservation(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<List<ReservationResponse>> getReservationsByStudent(@PathVariable Long studentId) {
        List<ReservationResponse> responses = reservationService.getReservationsByStudent(studentId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<ReservationResponse>> getReservationsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<ReservationResponse> responses = reservationService.getReservationsByDate(date);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/schedule/{scheduleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<ReservationResponse>> getReservationsBySchedule(@PathVariable Long scheduleId) {
        List<ReservationResponse> responses = reservationService.getReservationsBySchedule(scheduleId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ReservationResponse> confirmReservation(@PathVariable Long id) {
        ReservationResponse response = reservationService.confirmReservation(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<Map<String, String>> cancelReservation(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        reservationService.cancelReservation(id, reason);
        return ResponseEntity.ok(Map.of("message", "예약이 취소되었습니다"));
    }

    @PostMapping("/{id}/force-cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> forceCancelReservation(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        reservationService.forceCancelReservation(id, reason);
        return ResponseEntity.ok(Map.of("message", "예약이 강제 취소되었습니다"));
    }
}
