package web.kplay.studentmanagement.domain.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageType {
    // 자동 발송
    ENROLLMENT_REGISTER("첫등록 안내", "수강권 등록 후 기간 안내"),
    LEVEL_TEST_REMINDER("레벨테스트 리마인드", "레벨테스트 전날 오후 7시"),
    ENROLLMENT_COUNT_EXPIRED("횟수 소진", "수강 횟수 모두 소진"),
    ENROLLMENT_PERIOD_EXPIRED("기간 만료", "수강 기간 완료"),
    CHECK_IN("등원 알림", "출석체크 완료 후"),
    CHECK_OUT("하원 알림", "하원 체크 완료 후"),
    RECORDING_UPLOAD("레코딩 업로드", "레코딩 파일 업로드 후"),
    ABSENT_WARNING("결석 경고", "등원 예정 15분 후 미등원"),
    HOLDING("홀딩 안내", "수강권 홀딩 적용 후"),
    NOTICE("공지 알림", "공지 등록 후"),
    RESERVATION_OPEN("예약 시작 알림", "격주 일요일 오전 8시 30분"),
    
    // 수동 발송 (템플릿)
    TEXTBOOK("교재 안내", "단어책/교재 안내"),
    
    // 기존 타입
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
