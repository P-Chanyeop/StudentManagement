package web.kplay.studentmanagement.service.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 알리고 SMS API 클라이언트
 * 알리고 API 문서: https://smartsms.aligo.in/admin/api/spec.html
 */
@Component
@Slf4j
public class AligoSMSClient {

    private static final String ALIGO_API_URL = "https://apis.aligo.in/send/";
    private static final String ALIGO_REMAINING_URL = "https://apis.aligo.in/remain/";

    private final RestTemplate restTemplate;

    public AligoSMSClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * SMS 발송
     *
     * @param apiKey   알리고 API 키
     * @param userId   알리고 사용자 ID
     * @param sender   발신번호
     * @param receiver 수신번호
     * @param message  메시지 내용
     * @param testMode 테스트 모드 여부
     * @return 발송 결과
     */
    public SMSSendResponse sendSMS(String apiKey, String userId, String sender, String receiver,
                                    String message, boolean testMode) {
        try {
            // 파라미터 설정
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("key", apiKey);
            params.add("user_id", userId);
            params.add("sender", sender);
            params.add("receiver", receiver);
            params.add("msg", message);
            params.add("testmode_yn", testMode ? "Y" : "N");

            // 메시지 타입 설정 (90자 이하: SMS, 초과: LMS)
            String msgType = message.length() <= 90 ? "SMS" : "LMS";
            params.add("msg_type", msgType);

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            // API 호출
            ResponseEntity<Map> response = restTemplate.postForEntity(ALIGO_API_URL, request, Map.class);

            log.info("알리고 SMS 발송 응답: {}", response.getBody());

            // 응답 처리
            Map<String, Object> body = response.getBody();
            if (body != null) {
                int resultCode = Integer.parseInt(body.get("result_code").toString());
                String message_result = body.get("message").toString();

                if (resultCode == 1) {
                    // 발송 성공
                    String msgId = body.getOrDefault("msg_id", "").toString();
                    int successCount = Integer.parseInt(body.getOrDefault("success_cnt", "0").toString());
                    int errorCount = Integer.parseInt(body.getOrDefault("error_cnt", "0").toString());

                    return SMSSendResponse.builder()
                            .success(true)
                            .msgId(msgId)
                            .message(message_result)
                            .successCount(successCount)
                            .errorCount(errorCount)
                            .smsType(msgType)
                            .cost(calculateCost(msgType, successCount))
                            .build();
                } else {
                    // 발송 실패
                    return SMSSendResponse.builder()
                            .success(false)
                            .message(message_result)
                            .errorMessage(message_result)
                            .build();
                }
            }

            throw new RuntimeException("알리고 API 응답이 올바르지 않습니다.");

        } catch (Exception e) {
            log.error("알리고 SMS 발송 실패", e);
            return SMSSendResponse.builder()
                    .success(false)
                    .errorMessage("SMS 발송 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 대량 SMS 발송
     *
     * @param apiKey    알리고 API 키
     * @param userId    알리고 사용자 ID
     * @param sender    발신번호
     * @param receivers 수신번호 목록 (,로 구분)
     * @param message   메시지 내용
     * @param testMode  테스트 모드 여부
     * @return 발송 결과
     */
    public SMSSendResponse sendBulkSMS(String apiKey, String userId, String sender, String receivers,
                                        String message, boolean testMode) {
        // 단일 발송과 동일한 API 사용 (receiver에 ,로 구분된 여러 번호 전달)
        return sendSMS(apiKey, userId, sender, receivers, message, testMode);
    }

    /**
     * 잔여 SMS 건수 조회
     *
     * @param apiKey 알리고 API 키
     * @param userId 알리고 사용자 ID
     * @return 잔여 건수
     */
    public int getRemainingCount(String apiKey, String userId) {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("key", apiKey);
            params.add("user_id", userId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(ALIGO_REMAINING_URL, request, Map.class);

            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("SMS_CNT")) {
                return Integer.parseInt(body.get("SMS_CNT").toString());
            }

            return 0;
        } catch (Exception e) {
            log.error("알리고 잔여 건수 조회 실패", e);
            return 0;
        }
    }

    /**
     * SMS 발송 비용 계산
     *
     * @param msgType      메시지 타입 (SMS, LMS, MMS)
     * @param successCount 발송 성공 건수
     * @return 총 비용 (원)
     */
    private int calculateCost(String msgType, int successCount) {
        int unitCost = switch (msgType) {
            case "SMS" -> 20; // SMS: 20원
            case "LMS" -> 50; // LMS: 50원
            case "MMS" -> 200; // MMS: 200원
            default -> 20;
        };

        return unitCost * successCount;
    }

    /**
     * SMS 발송 응답 DTO
     */
    @lombok.Builder
    @lombok.Getter
    @lombok.ToString
    public static class SMSSendResponse {
        private boolean success;
        private String msgId;
        private String message;
        private String errorMessage;
        private int successCount;
        private int errorCount;
        private String smsType;
        private int cost;
    }
}
