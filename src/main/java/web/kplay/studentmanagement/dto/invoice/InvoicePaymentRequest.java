package web.kplay.studentmanagement.dto.invoice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 청구서 납부 처리 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoicePaymentRequest {

    @NotBlank(message = "결제 수단은 필수입니다")
    @Size(max = 100, message = "결제 수단은 100자를 초과할 수 없습니다")
    private String paymentMethod; // 카드, 현금, 계좌이체 등
}
