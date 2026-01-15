package web.kplay.studentmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NaverBookingDTO {
    private String status;           // 상태 (확정, 취소 등)
    private String name;             // 예약자
    private String phone;            // 전화번호
    private String bookingNumber;    // 예약번호
    private String bookingTime;      // 이용일시
    private String product;          // 상품
    private String quantity;         // 인원
    private String option;           // 옵션
    private String comment;          // 요청사항
    private String deposit;          // 예약금
    private String totalPrice;       // 결제금액
    private String orderDate;        // 신청일시
    private String confirmDate;      // 확정일시
    private String cancelDate;       // 취소일시
}
