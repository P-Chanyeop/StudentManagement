package web.kplay.studentmanagement.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import web.kplay.studentmanagement.domain.payment.PaymentStatus;

import java.time.LocalDateTime;

/**
 * 결제 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private Long id;
    private Long invoiceId;
    private String invoiceNumber;
    private String impUid;
    private String merchantUid;
    private Integer amount;
    private PaymentStatus status;
    private String paymentMethod;
    private String pgProvider;
    private LocalDateTime paidAt;
    private String failedReason;
    private String receiptUrl;
    private String cardName;
    private String cardNumber;
    private String buyerName;
    private String buyerTel;
    private String buyerEmail;
    private String vbankName;
    private String vbankNum;
    private LocalDateTime vbankDate;
    private String vbankHolder;
    private LocalDateTime createdAt;
}
