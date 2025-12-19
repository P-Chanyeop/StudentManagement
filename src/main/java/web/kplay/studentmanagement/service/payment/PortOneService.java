package web.kplay.studentmanagement.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import web.kplay.studentmanagement.exception.BusinessException;

import java.util.HashMap;
import java.util.Map;

/**
 * 포트원(PortOne) API 서비스
 * 결제 검증, 취소 등의 API 호출 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortOneService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${portone.api-key}")
    private String apiKey;

    @Value("${portone.api-secret}")
    private String apiSecret;

    @Value("${portone.api-url}")
    private String apiUrl;

    @Value("${portone.test-mode:true}")
    private boolean testMode;

    /**
     * 포트원 액세스 토큰 발급
     */
    public String getAccessToken() {
        if (testMode) {
            log.info("포트원 테스트 모드: 토큰 발급 시뮬레이션");
            return "test_access_token";
        }

        try {
            String url = apiUrl + "/users/getToken";

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("imp_key", apiKey);
            requestBody.put("imp_secret", apiSecret);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode responseBody = objectMapper.readTree(response.getBody());
                if (responseBody.get("code").asInt() == 0) {
                    String accessToken = responseBody.get("response").get("access_token").asText();
                    log.info("포트원 액세스 토큰 발급 성공");
                    return accessToken;
                } else {
                    throw new BusinessException("포트원 액세스 토큰 발급 실패: " + responseBody.get("message").asText());
                }
            } else {
                throw new BusinessException("포트원 API 호출 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("포트원 액세스 토큰 발급 중 오류 발생", e);
            throw new BusinessException("포트원 액세스 토큰 발급 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 결제 정보 조회 및 검증
     */
    public Map<String, Object> getPaymentInfo(String impUid) {
        if (testMode) {
            log.info("포트원 테스트 모드: 결제 정보 조회 시뮬레이션 - impUid={}", impUid);
            Map<String, Object> mockData = new HashMap<>();
            mockData.put("imp_uid", impUid);
            mockData.put("merchant_uid", "ORDER_" + System.currentTimeMillis());
            mockData.put("status", "paid");
            mockData.put("amount", 10000);
            mockData.put("pay_method", "card");
            mockData.put("pg_provider", "nice");
            mockData.put("buyer_name", "테스트 사용자");
            mockData.put("buyer_tel", "010-1234-5678");
            mockData.put("buyer_email", "test@example.com");
            mockData.put("paid_at", System.currentTimeMillis() / 1000);
            mockData.put("receipt_url", "https://example.com/receipt");
            return mockData;
        }

        try {
            String accessToken = getAccessToken();
            String url = apiUrl + "/payments/" + impUid;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode responseBody = objectMapper.readTree(response.getBody());
                if (responseBody.get("code").asInt() == 0) {
                    JsonNode paymentData = responseBody.get("response");
                    Map<String, Object> paymentInfo = objectMapper.convertValue(paymentData, Map.class);
                    log.info("포트원 결제 정보 조회 성공: impUid={}", impUid);
                    return paymentInfo;
                } else {
                    throw new BusinessException("결제 정보 조회 실패: " + responseBody.get("message").asText());
                }
            } else {
                throw new BusinessException("포트원 API 호출 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("결제 정보 조회 중 오류 발생: impUid={}", impUid, e);
            throw new BusinessException("결제 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 결제 취소/환불
     */
    public Map<String, Object> cancelPayment(String impUid, Integer cancelAmount, String reason,
                                               String refundHolder, String refundBank, String refundAccount) {
        if (testMode) {
            log.info("포트원 테스트 모드: 결제 취소 시뮬레이션 - impUid={}, amount={}, reason={}",
                    impUid, cancelAmount, reason);
            Map<String, Object> mockData = new HashMap<>();
            mockData.put("imp_uid", impUid);
            mockData.put("merchant_uid", "ORDER_" + System.currentTimeMillis());
            mockData.put("status", "cancelled");
            mockData.put("cancel_amount", cancelAmount);
            mockData.put("cancel_reason", reason);
            return mockData;
        }

        try {
            String accessToken = getAccessToken();
            String url = apiUrl + "/payments/cancel";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("imp_uid", impUid);
            requestBody.put("reason", reason);

            if (cancelAmount != null) {
                requestBody.put("amount", cancelAmount);
            }

            if (refundHolder != null && refundBank != null && refundAccount != null) {
                requestBody.put("refund_holder", refundHolder);
                requestBody.put("refund_bank", refundBank);
                requestBody.put("refund_account", refundAccount);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode responseBody = objectMapper.readTree(response.getBody());
                if (responseBody.get("code").asInt() == 0) {
                    JsonNode cancelData = responseBody.get("response");
                    Map<String, Object> cancelInfo = objectMapper.convertValue(cancelData, Map.class);
                    log.info("포트원 결제 취소 성공: impUid={}, amount={}", impUid, cancelAmount);
                    return cancelInfo;
                } else {
                    throw new BusinessException("결제 취소 실패: " + responseBody.get("message").asText());
                }
            } else {
                throw new BusinessException("포트원 API 호출 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("결제 취소 중 오류 발생: impUid={}", impUid, e);
            throw new BusinessException("결제 취소 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 결제 금액 검증
     * 실제 결제 금액과 주문 금액이 일치하는지 확인
     */
    public boolean verifyPayment(String impUid, Integer expectedAmount) {
        Map<String, Object> paymentInfo = getPaymentInfo(impUid);

        Integer actualAmount = (Integer) paymentInfo.get("amount");
        String status = (String) paymentInfo.get("status");

        if (!"paid".equals(status)) {
            log.warn("결제 상태가 완료되지 않음: impUid={}, status={}", impUid, status);
            return false;
        }

        if (!actualAmount.equals(expectedAmount)) {
            log.error("결제 금액 불일치: impUid={}, expected={}, actual={}", impUid, expectedAmount, actualAmount);
            return false;
        }

        log.info("결제 검증 성공: impUid={}, amount={}", impUid, actualAmount);
        return true;
    }
}
