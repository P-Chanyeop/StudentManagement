package web.kplay.studentmanagement.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 준비 응답 DTO
 * 클라이언트에서 포트원 결제창을 띄우기 위한 정보
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentPrepareResponse {

    private String merchantUid; // 가맹점 주문번호
    private Integer amount; // 결제 금액
    private String name; // 상품명
    private String buyerName; // 구매자명
    private String buyerTel; // 구매자 연락처
    private String buyerEmail; // 구매자 이메일
    private String paymentMethod; // 결제 수단
    private String pgProvider; // PG사
}
