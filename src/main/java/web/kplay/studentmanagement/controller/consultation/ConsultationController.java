package web.kplay.studentmanagement.controller.consultation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.dto.consultation.ConsultationRequest;
import web.kplay.studentmanagement.dto.consultation.ConsultationResponse;
import web.kplay.studentmanagement.service.consultation.ConsultationService;
import web.kplay.studentmanagement.service.excel.ConsultationExcelService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;
    private final ConsultationExcelService consultationExcelService;

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

    /**
     * 학생별 상담 이력 Excel 내보내기
     */
    @GetMapping("/export/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<byte[]> exportConsultationsByStudent(@PathVariable Long studentId) throws IOException {
        byte[] excelData = consultationExcelService.exportConsultationsByStudent(studentId);

        String filename = String.format("consultations_student_%d_%s.xlsx",
                studentId,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    /**
     * 전체 상담 이력 Excel 내보내기
     */
    @GetMapping("/export/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<byte[]> exportAllConsultations() throws IOException {
        byte[] excelData = consultationExcelService.exportAllConsultations();

        String filename = String.format("consultations_all_%s.xlsx",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    /**
     * 기간별 상담 이력 Excel 내보내기
     */
    @GetMapping("/export/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<byte[]> exportConsultationsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {
        byte[] excelData = consultationExcelService.exportConsultationsByDateRange(startDate, endDate);

        String filename = String.format("consultations_range_%s_to_%s.xlsx",
                startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }
}
