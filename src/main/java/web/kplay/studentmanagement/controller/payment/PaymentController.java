package web.kplay.studentmanagement.controller.payment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.dto.payment.*;
import web.kplay.studentmanagement.service.payment.PaymentService;

import java.util.List;

/**
 * 결제 API 컨트롤러
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 결제 준비
     * 클라이언트가 결제창을 띄우기 전에 호출하여 주문번호 등을 받아옴
     */
    @PostMapping("/prepare")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<PaymentPrepareResponse> preparePayment(@Valid @RequestBody PaymentPrepareRequest request) {
        PaymentPrepareResponse response = paymentService.preparePayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 결제 완료 검증
     * 클라이언트에서 결제 완료 후 서버에서 검증
     */
    @PostMapping("/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<PaymentResponse> completePayment(@Valid @RequestBody PaymentCompleteRequest request) {
        PaymentResponse response = paymentService.completePayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 결제 취소/환불
     */
    @PostMapping("/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<PaymentResponse> cancelPayment(@Valid @RequestBody PaymentCancelRequest request) {
        PaymentResponse response = paymentService.cancelPayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 결제 정보 조회 (ID)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long id) {
        PaymentResponse response = paymentService.getPayment(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 결제 정보 조회 (impUid)
     */
    @GetMapping("/imp/{impUid}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<PaymentResponse> getPaymentByImpUid(@PathVariable String impUid) {
        PaymentResponse response = paymentService.getPaymentByImpUid(impUid);
        return ResponseEntity.ok(response);
    }

    /**
     * 청구서별 결제 내역 조회
     */
    @GetMapping("/invoice/{invoiceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByInvoice(@PathVariable Long invoiceId) {
        List<PaymentResponse> responses = paymentService.getPaymentsByInvoice(invoiceId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 학생별 결제 내역 조회
     */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByStudent(@PathVariable Long studentId) {
        List<PaymentResponse> responses = paymentService.getPaymentsByStudent(studentId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 전체 결제 내역 조회
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        List<PaymentResponse> responses = paymentService.getAllPayments();
        return ResponseEntity.ok(responses);
    }
}
