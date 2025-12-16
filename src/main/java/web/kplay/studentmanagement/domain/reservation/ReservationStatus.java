package web.kplay.studentmanagement.domain.reservation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReservationStatus {
    PENDING("예약대기", "예약 대기중"),
    CONFIRMED("예약확정", "예약 확정"),
    CANCELLED("예약취소", "예약 취소"),
    COMPLETED("수업완료", "수업 완료"),
    NO_SHOW("노쇼", "무단결석");

    private final String name;
    private final String description;
}
