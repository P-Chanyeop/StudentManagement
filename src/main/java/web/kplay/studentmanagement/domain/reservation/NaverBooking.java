package web.kplay.studentmanagement.domain.reservation;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "naver_bookings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NaverBooking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String bookingNumber;  // 예약번호 (중복 방지용)

    private String status;         // 상태
    private String name;           // 예약자
    private String phone;          // 전화번호
    private String bookingTime;    // 이용일시
    private String product;        // 상품
    private String quantity;       // 인원
    private String option;         // 옵션
    
    @Column(length = 1000)
    private String comment;        // 요청사항
    
    private String deposit;        // 예약금
    private String totalPrice;     // 결제금액
    private String orderDate;      // 신청일시
    private String confirmDate;    // 확정일시
    private String cancelDate;     // 취소일시

    private LocalDateTime syncedAt; // 동기화 시간
}
