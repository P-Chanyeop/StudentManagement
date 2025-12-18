package web.kplay.studentmanagement.service.message.sms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * 문자나라 SMS 발송 제공자
 * API 문서: http://www.munjanara.co.kr/
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MunjaNaraSmsProvider implements SmsProvider {

    private final RestTemplate restTemplate;

    @Value("${sms.munjanara.user-id:}")
    private String userId;

    @Value("${sms.munjanara.api-key:}")
    private String apiKey;

    @Value("${sms.munjanara.sender:}")
    private String sender;

    @Value("${sms.munjanara.enabled:false}")
    private boolean enabled;

    private static final String SEND_URL = "https://www.munjanara.co.kr/MSG/send/";

    @Override
    public String sendSms(String recipientPhone, String content) {
        if (!enabled) {
            log.warn("문자나라 SMS가 비활성화되어 있습니다. 테스트 모드로 실행됩니다.");
            return "TEST-MUNJANARA-" + System.currentTimeMillis();
        }

        if (apiKey == null || apiKey.isEmpty() || userId == null || userId.isEmpty()) {
            log.error("문자나라 SMS API 설정이 누락되었습니다.");
            throw new IllegalStateException("문자나라 SMS API 설정이 필요합니다.");
        }

        try {
            // 전화번호 포맷 정리 (하이픈 제거)
            String cleanPhone = recipientPhone.replaceAll("[^0-9]", "");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("user_id", userId);
            params.add("key", apiKey);
            params.add("sender", sender);
            params.add("receiver", cleanPhone);
            params.add("msg", content);
            params.add("msg_type", "sms"); // sms, lms, mms

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(SEND_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("문자나라 SMS 발송 성공: 수신자={}, 응답={}", cleanPhone, response.getBody());
                return "MUNJANARA-" + System.currentTimeMillis();
            } else {
                log.error("문자나라 SMS 발송 실패: 상태={}, 응답={}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("SMS 발송 실패: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("문자나라 SMS 발송 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("SMS 발송 중 오류 발생", e);
        }
    }

    @Override
    public String checkStatus(String externalMessageId) {
        // 문자나라 API를 통한 상태 조회 구현
        // 현재는 기본 구현으로 SENT 반환
        return "SENT";
    }

    @Override
    public String getProviderName() {
        return "MUNJANARA";
    }
}
