package web.kplay.studentmanagement.domain.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageType {
    LATE_NOTIFICATION("지각 안내", "수업 지각 안내 문자"),
    ENROLLMENT_EXPIRY("수강 기한 안내", "수강권 만료 임박 안내"),
    LEVEL_TEST("레벨테스트 안내", "레벨테스트 일정 안내"),
    RESERVATION_CONFIRM("예약 확인", "수업 예약 확인"),
    RESERVATION_CANCEL("예약 취소", "수업 예약 취소 확인"),
    INVOICE_ISSUED("청구서 발급", "청구서 발급 안내"),
    PAYMENT_REMINDER("납부 안내", "청구서 납부 안내"),
    PAYMENT_OVERDUE("연체 알림", "청구서 연체 알림"),
    PAYMENT_CONFIRMED("납부 확인", "청구서 납부 완료 확인"),
    GENERAL("일반 안내", "일반 안내 문자");

    private final String name;
    private final String description;
}
