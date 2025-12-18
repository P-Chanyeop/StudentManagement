package web.kplay.studentmanagement.dto.payment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 취소/환불 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCancelRequest {

    @NotBlank(message = "포트원 결제 고유번호는 필수입니다")
    private String impUid;

    @NotBlank(message = "취소 사유는 필수입니다")
    private String reason;

    @Min(value = 0, message = "취소 금액은 0 이상이어야 합니다")
    private Integer cancelAmount; // 부분 취소 금액 (null이면 전액 취소)

    private String refundHolder; // 환불 계좌 예금주
    private String refundBank; // 환불 계좌 은행명
    private String refundAccount; // 환불 계좌 번호
}
