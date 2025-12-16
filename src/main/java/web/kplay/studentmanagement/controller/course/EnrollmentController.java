package web.kplay.studentmanagement.controller.course;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.dto.course.EnrollmentCreateRequest;
import web.kplay.studentmanagement.dto.course.EnrollmentResponse;
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

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<EnrollmentResponse> getEnrollment(@PathVariable Long id) {
        EnrollmentResponse response = enrollmentService.getEnrollment(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
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

    @PatchMapping("/{id}/add-count")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<EnrollmentResponse> addCount(
            @PathVariable Long id,
            @RequestParam Integer additionalCount) {
        EnrollmentResponse response = enrollmentService.addCount(id, additionalCount);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateEnrollment(@PathVariable Long id) {
        enrollmentService.deactivateEnrollment(id);
        return ResponseEntity.noContent().build();
    }
}
