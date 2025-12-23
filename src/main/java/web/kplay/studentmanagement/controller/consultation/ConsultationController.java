package web.kplay.studentmanagement.controller.consultation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    /**
     * 상담 기록 생성
     * @param request 상담 정보 (학생ID, 제목, 내용, 파일 등)
     * @return 생성된 상담 기록 정보
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ConsultationResponse> createConsultation(@Valid @RequestBody ConsultationRequest request) {
        ConsultationResponse response = consultationService.createConsultation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 특정 학생의 상담 이력 조회
     * @param studentId 학생 ID
     * @return 해당 학생의 상담 이력 목록
     */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<List<ConsultationResponse>> getConsultationsByStudent(@PathVariable Long studentId) {
        List<ConsultationResponse> responses = consultationService.getConsultationsByStudent(studentId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 부모님의 자녀들 상담 이력 조회
     * @param authentication 인증된 부모님 정보
     * @return 자녀들의 모든 상담 이력 목록
     */
    @GetMapping("/my-children")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<List<ConsultationResponse>> getMyChildrenConsultations(Authentication authentication) {
        String username = authentication.getName();
        List<ConsultationResponse> responses = consultationService.getConsultationsByParent(username);
        return ResponseEntity.ok(responses);
    }

    /**
     * 상담 기록 수정
     * @param id 상담 기록 ID
     * @param request 수정할 상담 정보
     * @return 수정된 상담 기록 정보
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ConsultationResponse> updateConsultation(
            @PathVariable Long id, 
            @Valid @RequestBody ConsultationRequest request) {
        ConsultationResponse response = consultationService.updateConsultation(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 상담 기록 삭제
     * @param id 삭제할 상담 기록 ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Void> deleteConsultation(@PathVariable Long id) {
        consultationService.deleteConsultation(id);
        return ResponseEntity.noContent().build();
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
