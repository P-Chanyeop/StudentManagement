package web.kplay.studentmanagement.controller.invoice;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.domain.invoice.InvoiceStatus;
import web.kplay.studentmanagement.dto.invoice.*;
import web.kplay.studentmanagement.security.CustomUserDetails;
import web.kplay.studentmanagement.service.invoice.InvoiceService;

import java.time.LocalDate;
import java.util.List;

/**
 * 청구서 관리 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * 청구서 생성
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<InvoiceResponse> createInvoice(
            @Valid @RequestBody InvoiceCreateRequest request,
            Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        InvoiceResponse response = invoiceService.createInvoice(request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 청구서 단건 조회
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable Long id) {
        InvoiceResponse response = invoiceService.getInvoice(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 청구서 조회
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<InvoiceResponse>> getAllInvoices() {
        List<InvoiceResponse> responses = invoiceService.getAllInvoices();
        return ResponseEntity.ok(responses);
    }

    /**
     * 학생별 청구서 조회
     */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByStudent(@PathVariable Long studentId) {
        List<InvoiceResponse> responses = invoiceService.getInvoicesByStudent(studentId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 상태별 청구서 조회
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByStatus(@PathVariable InvoiceStatus status) {
        List<InvoiceResponse> responses = invoiceService.getInvoicesByStatus(status);
        return ResponseEntity.ok(responses);
    }

    /**
     * 기간별 청구서 조회
     */
    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<InvoiceResponse> responses = invoiceService.getInvoicesByDateRange(startDate, endDate);
        return ResponseEntity.ok(responses);
    }

    /**
     * 학생의 미납 청구서 조회
     */
    @GetMapping("/student/{studentId}/unpaid")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<List<InvoiceResponse>> getUnpaidInvoicesByStudent(@PathVariable Long studentId) {
        List<InvoiceResponse> responses = invoiceService.getUnpaidInvoicesByStudent(studentId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 연체된 청구서 조회
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<InvoiceResponse>> getOverdueInvoices() {
        List<InvoiceResponse> responses = invoiceService.getOverdueInvoices();
        return ResponseEntity.ok(responses);
    }

    /**
     * 청구서 수정
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<InvoiceResponse> updateInvoice(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceUpdateRequest request) {
        InvoiceResponse response = invoiceService.updateInvoice(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 청구서 납부 처리
     */
    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<InvoiceResponse> markAsPaid(
            @PathVariable Long id,
            @Valid @RequestBody InvoicePaymentRequest request) {
        InvoiceResponse response = invoiceService.markAsPaid(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 청구서 취소
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<InvoiceResponse> cancelInvoice(
            @PathVariable Long id,
            @RequestParam String reason) {
        InvoiceResponse response = invoiceService.cancelInvoice(id, reason);
        return ResponseEntity.ok(response);
    }

    /**
     * 청구서 삭제
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 청구서 메모 업데이트
     */
    @PatchMapping("/{id}/memo")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<InvoiceResponse> updateMemo(
            @PathVariable Long id,
            @RequestParam String memo) {
        InvoiceResponse response = invoiceService.updateMemo(id, memo);
        return ResponseEntity.ok(response);
    }

    /**
     * 청구서 통계
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<InvoiceStatistics> getStatistics() {
        InvoiceStatistics statistics = invoiceService.getStatistics();
        return ResponseEntity.ok(statistics);
    }
}
