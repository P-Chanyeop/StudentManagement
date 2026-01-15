package web.kplay.studentmanagement.service.sms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class AligoSmsService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${aligo.api.key}")
    private String apiKey;

    @Value("${aligo.api.user-id}")
    private String userId;

    @Value("${aligo.api.sender}")
    private String sender;

    private static final String BASE_URL = "https://apis.aligo.in";

    /**
     * 단일 문자 발송 (동일 내용)
     */
    public SmsResponse sendSms(String receiver, String message) {
        return sendSms(receiver, message, null, null);
    }

    /**
     * 단일 문자 발송 (제목 포함)
     */
    public SmsResponse sendSms(String receiver, String message, String title, String msgType) {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("key", apiKey);
            params.add("user_id", userId);
            params.add("sender", sender);
            params.add("receiver", receiver);
            params.add("msg", message);
            
            if (title != null) {
                params.add("title", title);
            }
            if (msgType != null) {
                params.add("msg_type", msgType);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                BASE_URL + "/send/", request, String.class);

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            return SmsResponse.builder()
                .resultCode(jsonNode.get("result_code").asInt())
                .message(jsonNode.has("message") ? jsonNode.get("message").asText() : "")
                .msgId(jsonNode.has("msg_id") ? jsonNode.get("msg_id").asLong() : null)
                .successCnt(jsonNode.has("success_cnt") ? jsonNode.get("success_cnt").asInt() : 0)
                .errorCnt(jsonNode.has("error_cnt") ? jsonNode.get("error_cnt").asInt() : 0)
                .msgType(jsonNode.has("msg_type") ? jsonNode.get("msg_type").asText() : null)
                .build();

        } catch (Exception e) {
            log.error("SMS 발송 실패", e);
            return SmsResponse.builder()
                .resultCode(-999)
                .message("SMS 발송 중 오류 발생: " + e.getMessage())
                .build();
        }
    }

    /**
     * 발송 가능 건수 조회
     */
    public RemainResponse getRemainCount() {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("key", apiKey);
            params.add("user_id", userId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                BASE_URL + "/remain/", request, String.class);

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            
            return RemainResponse.builder()
                .resultCode(jsonNode.get("result_code").asInt())
                .message(jsonNode.has("message") ? jsonNode.get("message").asText() : "")
                .smsCnt(jsonNode.has("SMS_CNT") ? jsonNode.get("SMS_CNT").asInt() : 0)
                .lmsCnt(jsonNode.has("LMS_CNT") ? jsonNode.get("LMS_CNT").asInt() : 0)
                .mmsCnt(jsonNode.has("MMS_CNT") ? jsonNode.get("MMS_CNT").asInt() : 0)
                .build();

        } catch (Exception e) {
            log.error("잔여 건수 조회 실패", e);
            return RemainResponse.builder()
                .resultCode(-999)
                .message("조회 중 오류 발생: " + e.getMessage())
                .build();
        }
    }

    @lombok.Builder
    @lombok.Getter
    public static class SmsResponse {
        private int resultCode;
        private String message;
        private Long msgId;
        private int successCnt;
        private int errorCnt;
        private String msgType;
    }

    @lombok.Builder
    @lombok.Getter
    public static class RemainResponse {
        private int resultCode;
        private String message;
        private int smsCnt;
        private int lmsCnt;
        private int mmsCnt;
    }
}
