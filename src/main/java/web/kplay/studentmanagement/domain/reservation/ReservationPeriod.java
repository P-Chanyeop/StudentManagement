package web.kplay.studentmanagement.domain.reservation;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservation_periods")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ReservationPeriod extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime openTime; // 예약 시작 시간

    @Column(nullable = false)
    private LocalDateTime closeTime; // 예약 종료 시간

    @Column(nullable = false)
    private LocalDateTime reservationStartDate; // 예약 가능한 수업 시작일

    @Column(nullable = false)
    private LocalDateTime reservationEndDate; // 예약 가능한 수업 종료일

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public boolean isReservationOpen() {
        LocalDateTime now = LocalDateTime.now();
        return isActive && now.isAfter(openTime) && now.isBefore(closeTime);
    }
}
