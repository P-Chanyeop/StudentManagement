package web.kplay.studentmanagement.dto.payment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 준비 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentPrepareRequest {

    @NotNull(message = "청구서 ID는 필수입니다")
    private Long invoiceId;

    @NotNull(message = "결제 금액은 필수입니다")
    @Min(value = 100, message = "결제 금액은 최소 100원 이상이어야 합니다")
    private Integer amount;

    @NotBlank(message = "구매자명은 필수입니다")
    private String buyerName;

    @NotBlank(message = "구매자 연락처는 필수입니다")
    private String buyerTel;

    private String buyerEmail;

    @NotBlank(message = "결제 수단은 필수입니다")
    private String paymentMethod; // card, vbank, trans, phone 등

    private String pgProvider; // nice, kcp, inicis, kakaopay 등 (기본값은 서버에서 설정)
}
