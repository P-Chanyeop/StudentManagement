package web.kplay.studentmanagement.controller.consultation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.dto.consultation.ConsultationRequest;
import web.kplay.studentmanagement.dto.consultation.ConsultationResponse;
import web.kplay.studentmanagement.service.consultation.ConsultationService;

import java.util.List;

@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ConsultationResponse> createConsultation(@Valid @RequestBody ConsultationRequest request) {
        ConsultationResponse response = consultationService.createConsultation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<List<ConsultationResponse>> getConsultationsByStudent(@PathVariable Long studentId) {
        List<ConsultationResponse> responses = consultationService.getConsultationsByStudent(studentId);
        return ResponseEntity.ok(responses);
    }
}
