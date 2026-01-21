package web.kplay.studentmanagement.controller.reservation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.dto.reservation.ReservationCreateRequest;
import web.kplay.studentmanagement.dto.reservation.ReservationResponse;
import web.kplay.studentmanagement.domain.reservation.ReservationPeriod;
import web.kplay.studentmanagement.service.reservation.ReservationService;
import web.kplay.studentmanagement.service.reservation.ReservationPeriodService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationPeriodService reservationPeriodService;

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

    @GetMapping("/my-reservations")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(Authentication authentication) {
        String username = authentication.getName();
        List<ReservationResponse> responses = reservationService.getMyReservations(username);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<List<ReservationResponse>> getReservationsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<ReservationResponse> responses = reservationService.getReservationsByDate(date);
        return ResponseEntity.ok(responses);
    }

    /**
     * 특정 날짜와 상담 유형의 예약된 시간 목록 조회
     * 
     * @param date 조회할 날짜
     * @param consultationType 상담 유형 (선택사항)
     * @return ResponseEntity<List<String>> 예약된 시간 목록 (HH:MM 형식)
     */
    @GetMapping("/reserved-times/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<List<String>> getReservedTimesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String consultationType) {
        List<String> reservedTimes = consultationType != null ? 
            reservationService.getReservedTimesByDateAndType(date, consultationType) :
            reservationService.getReservedTimesByDate(date);
        return ResponseEntity.ok(reservedTimes);
    }

    // getReservationsBySchedule 메서드 삭제 (schedule 제거로 인해 사용 안 함)

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

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT', 'STUDENT')")
    public ResponseEntity<List<ReservationResponse>> getAvailableSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<ReservationResponse> responses = reservationService.getReservationsByDate(date);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReservationResponse>> getNewReservations(
            @RequestParam String since) {
        List<ReservationResponse> responses = reservationService.getNewReservationsSince(since);
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 예약 가능 여부 확인
     */
    @GetMapping("/availability")
    public ResponseEntity<Boolean> checkReservationAvailability() {
        boolean isOpen = reservationPeriodService.isReservationOpen();
        return ResponseEntity.ok(isOpen);
    }

    /**
     * 예약 가능한 날짜 범위 조회
     */
    @GetMapping("/available-dates")
    public ResponseEntity<Map<String, String>> getAvailableDates() {
        ReservationPeriod period = reservationPeriodService.getCurrentReservationPeriod();
        Map<String, String> dates = new HashMap<>();
        
        if (period != null) {
            dates.put("startDate", period.getReservationStartDate().toLocalDate().toString());
            dates.put("endDate", period.getReservationEndDate().toLocalDate().toString());
        }
        
        return ResponseEntity.ok(dates);
    }
}
