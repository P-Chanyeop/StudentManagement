package web.kplay.studentmanagement.dto.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 완료 검증 요청 DTO
 * 클라이언트에서 결제 완료 후 서버로 전송
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompleteRequest {

    @NotBlank(message = "포트원 결제 고유번호는 필수입니다")
    private String impUid; // 포트원 결제 고유번호

    @NotBlank(message = "가맹점 주문번호는 필수입니다")
    private String merchantUid; // 가맹점 주문번호
}
