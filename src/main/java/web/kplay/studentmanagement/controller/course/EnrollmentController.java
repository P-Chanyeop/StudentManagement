package web.kplay.studentmanagement.controller.course;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.dto.course.EnrollmentAdjustRequest;
import web.kplay.studentmanagement.dto.course.EnrollmentCreateRequest;
import web.kplay.studentmanagement.dto.course.EnrollmentHoldRequest;
import web.kplay.studentmanagement.dto.course.EnrollmentResponse;
import web.kplay.studentmanagement.dto.course.UnregisteredEnrollmentRequest;
import web.kplay.studentmanagement.security.UserDetailsImpl;
import web.kplay.studentmanagement.service.course.EnrollmentService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<EnrollmentResponse> createEnrollment(@Valid @RequestBody EnrollmentCreateRequest request) {
        EnrollmentResponse response = enrollmentService.createEnrollment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/unregistered")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<EnrollmentResponse> createUnregisteredEnrollment(@Valid @RequestBody UnregisteredEnrollmentRequest request) {
        EnrollmentResponse response = enrollmentService.createUnregisteredEnrollment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<EnrollmentResponse>> getMyEnrollments(Authentication authentication) {
        List<EnrollmentResponse> enrollments = enrollmentService.getEnrollmentsByUser(authentication.getName());
        return ResponseEntity.ok(enrollments);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<List<EnrollmentResponse>> getAllEnrollments() {
        List<EnrollmentResponse> responses = enrollmentService.getAllEnrollments();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<EnrollmentResponse> getEnrollment(@PathVariable Long id) {
        EnrollmentResponse response = enrollmentService.getEnrollment(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<List<EnrollmentResponse>> getEnrollmentsByStudent(@PathVariable Long studentId) {
        List<EnrollmentResponse> responses = enrollmentService.getEnrollmentsByStudent(studentId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/student/{studentId}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<EnrollmentResponse>> getActiveEnrollmentsByStudent(@PathVariable Long studentId) {
        List<EnrollmentResponse> responses = enrollmentService.getActiveEnrollmentsByStudent(studentId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<EnrollmentResponse>> getExpiringEnrollments(
            @RequestParam(defaultValue = "7") int days) {
        List<EnrollmentResponse> responses = enrollmentService.getExpiringEnrollments(days);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/low-count")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<EnrollmentResponse>> getLowCountEnrollments(
            @RequestParam(defaultValue = "3") int threshold) {
        List<EnrollmentResponse> responses = enrollmentService.getLowCountEnrollments(threshold);
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{id}/extend")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<EnrollmentResponse> extendPeriod(
            @PathVariable Long id,
            @RequestParam LocalDate newEndDate) {
        EnrollmentResponse response = enrollmentService.extendPeriod(id, newEndDate);
        return ResponseEntity.ok(response);
    }

    /**
     * 공휴일을 제외한 N일로 수강권 기간 연장
     */
    @PatchMapping("/{id}/extend-business-days")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<EnrollmentResponse> extendPeriodWithHolidays(
            @PathVariable Long id,
            @RequestParam int businessDays) {
        EnrollmentResponse response = enrollmentService.extendPeriodWithHolidays(id, businessDays);
        return ResponseEntity.ok(response);
    }

    /**
     * 공휴일을 고려한 수강권 생성
     */
    @PostMapping("/with-holidays")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<EnrollmentResponse> createEnrollmentWithHolidays(
            @Valid @RequestBody EnrollmentCreateRequest request) {
        EnrollmentResponse response = enrollmentService.createEnrollmentWithHolidays(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 수동 횟수 조절 (관리자용)
     * 결석/보강/연기 등으로 인한 수동 조절
     * @param id 수강권 ID
     * @param request 조정 요청 정보 (adjustment: 양수=추가, 음수=차감, reason: 조정 사유)
     * @param authentication 현재 로그인한 사용자 인증 정보
     * @return 조정 완료 메시지
     */
    @PatchMapping("/{id}/manual-adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<String> manualAdjustCount(
            @PathVariable Long id,
            @Valid @RequestBody EnrollmentAdjustRequest request,
            Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        web.kplay.studentmanagement.domain.enrollment.EnrollmentAdjustment.AdjustmentType adjustmentType;
        if (request.getAdjustment() > 0) {
            adjustmentType = web.kplay.studentmanagement.domain.enrollment.EnrollmentAdjustment.AdjustmentType.ADD;
        } else {
            adjustmentType = web.kplay.studentmanagement.domain.enrollment.EnrollmentAdjustment.AdjustmentType.DEDUCT;
        }
        
        enrollmentService.adjustEnrollmentCount(id, adjustmentType, 
            Math.abs(request.getAdjustment()), request.getReason(), currentUserId);
        return ResponseEntity.ok("횟수가 성공적으로 조정되었습니다.");
    }

    /**
     * 현재 로그인한 사용자 ID 조회
     * @param authentication Spring Security 인증 객체
     * @return 현재 사용자 ID
     * @throws RuntimeException 인증된 사용자를 찾을 수 없는 경우
     */
    private Long getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getId();
        }
        throw new RuntimeException("인증된 사용자를 찾을 수 없습니다.");
    }

    /**
     * 개별 수업 시간 설정
     */
    @PatchMapping("/{id}/duration")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<EnrollmentResponse> setCustomDuration(
            @PathVariable Long id,
            @RequestParam Integer durationMinutes) {
        EnrollmentResponse response = enrollmentService.setCustomDuration(id, durationMinutes);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<EnrollmentResponse> updateEnrollment(
            @PathVariable Long id,
            @Valid @RequestBody EnrollmentCreateRequest request) {
        EnrollmentResponse response = enrollmentService.updateEnrollment(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateEnrollment(@PathVariable Long id) {
        enrollmentService.deactivateEnrollment(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateEnrollment(@PathVariable Long id) {
        enrollmentService.activateEnrollment(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/expire")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> expireEnrollment(@PathVariable Long id) {
        enrollmentService.expireEnrollment(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/force")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> forceDeleteEnrollment(@PathVariable Long id) {
        enrollmentService.forceDeleteEnrollment(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/hold")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EnrollmentResponse> startHold(
            @PathVariable Long id,
            @RequestBody EnrollmentHoldRequest request) {
        EnrollmentResponse response = enrollmentService.startHold(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/hold")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EnrollmentResponse> endHold(@PathVariable Long id) {
        EnrollmentResponse response = enrollmentService.endHold(id);
        return ResponseEntity.ok(response);
    }
}
