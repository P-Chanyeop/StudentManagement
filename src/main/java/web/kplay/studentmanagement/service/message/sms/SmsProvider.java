package web.kplay.studentmanagement.service.message.sms;

/**
 * SMS 발송 제공자 인터페이스
 * 다양한 SMS API 업체를 지원하기 위한 공통 인터페이스
 */
public interface SmsProvider {

    /**
     * SMS 발송
     *
     * @param recipientPhone 수신자 전화번호
     * @param content 발송 내용
     * @return 외부 메시지 ID
     */
    String sendSms(String recipientPhone, String content);

    /**
     * 발송 상태 조회
     *
     * @param externalMessageId 외부 메시지 ID
     * @return 발송 상태 (SENT, FAILED 등)
     */
    String checkStatus(String externalMessageId);

    /**
     * 제공자 이름 반환
     */
    String getProviderName();
}
