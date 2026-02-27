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
 * 알리고(Aligo) SMS 발송 제공자
 * API 문서: https://smartsms.aligo.in/admin/api/spec.html
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AligoSmsProvider implements SmsProvider {

    private final RestTemplate restTemplate;

    @Value("${sms.aligo.api-key:}")
    private String apiKey;

    @Value("${sms.aligo.user-id:}")
    private String userId;

    @Value("${sms.aligo.sender:}")
    private String sender;

    @Value("${sms.aligo.enabled:false}")
    private boolean enabled;

    private static final String SEND_URL = "https://apis.aligo.in/send/";

    @Override
    public String sendSms(String recipientPhone, String content) {
        if (!enabled) {
            log.warn("알리고 SMS가 비활성화되어 있습니다. 테스트 모드로 실행됩니다.");
            return "TEST-ALIGO-" + System.currentTimeMillis();
        }

        if (apiKey == null || apiKey.isEmpty() || userId == null || userId.isEmpty()) {
            log.error("알리고 SMS API 설정이 누락되었습니다.");
            throw new IllegalStateException("알리고 SMS API 설정이 필요합니다.");
        }

        try {
            // 전화번호 포맷 정리 (하이픈 제거)
            String cleanPhone = recipientPhone.replaceAll("[^0-9]", "");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // 문자 길이에 따라 SMS/LMS 자동 선택
            String msgType = content.length() > 90 ? "LMS" : "SMS";
            
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("key", apiKey);
            params.add("user_id", userId);
            params.add("sender", sender);
            params.add("receiver", cleanPhone);
            params.add("msg", content);
            params.add("msg_type", msgType); // SMS: 단문(90자), LMS: 장문(2000자)
            params.add("title", msgType.equals("LMS") ? "리틀베어 리딩클럽" : ""); // LMS 제목
            params.add("testmode_yn", "N"); // 테스트 모드 비활성화 (실제 발송)

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(SEND_URL, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("알리고 SMS 발송 성공: 수신자={}, 타입={}, 길이={}자, 응답={}", 
                        cleanPhone, msgType, content.length(), response.getBody());
                // 알리고는 응답에서 message_id를 반환합니다
                return "ALIGO-" + System.currentTimeMillis();
            } else {
                log.error("알리고 SMS 발송 실패: 상태={}, 응답={}", response.getStatusCode(), response.getBody());
                throw new RuntimeException("SMS 발송 실패: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("알리고 SMS 발송 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("SMS 발송 중 오류 발생", e);
        }
    }

    @Override
    public String checkStatus(String externalMessageId) {
        // 알리고 API를 통한 상태 조회 구현
        // 현재는 기본 구현으로 SENT 반환
        return "SENT";
    }

    @Override
    public String getProviderName() {
        return "ALIGO";
    }
}
