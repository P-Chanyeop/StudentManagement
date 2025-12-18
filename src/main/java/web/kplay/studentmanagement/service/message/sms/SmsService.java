package web.kplay.studentmanagement.service.message.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * SMS 통합 발송 서비스
 * 설정된 제공자를 통해 SMS를 발송합니다.
 */
@Slf4j
@Service
public class SmsService {

    private final Map<String, SmsProvider> providers;
    private final SmsProvider activeProvider;

    @Value("${sms.provider:test}")
    private String providerName;

    public SmsService(List<SmsProvider> smsProviders,
                      @Value("${sms.provider:test}") String providerName) {
        // 모든 SmsProvider를 Map으로 저장
        this.providers = smsProviders.stream()
                .collect(Collectors.toMap(
                        provider -> provider.getProviderName().toLowerCase(),
                        Function.identity()
                ));

        // 활성 제공자 선택
        this.activeProvider = selectProvider(providerName);
        log.info("SMS 제공자 초기화: provider={}, 사용가능제공자={}",
                activeProvider.getProviderName(), providers.keySet());
    }

    /**
     * SMS 발송
     */
    public String sendSms(String recipientPhone, String content) {
        try {
            log.info("SMS 발송 시작: 제공자={}, 수신자={}",
                    activeProvider.getProviderName(), recipientPhone);
            String externalMessageId = activeProvider.sendSms(recipientPhone, content);
            log.info("SMS 발송 완료: externalMessageId={}", externalMessageId);
            return externalMessageId;
        } catch (Exception e) {
            log.error("SMS 발송 실패: {}", e.getMessage(), e);
            throw new RuntimeException("SMS 발송 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 발송 상태 조회
     */
    public String checkStatus(String externalMessageId) {
        return activeProvider.checkStatus(externalMessageId);
    }

    /**
     * 활성 제공자 조회
     */
    public String getActiveProviderName() {
        return activeProvider.getProviderName();
    }

    /**
     * 제공자 선택
     */
    private SmsProvider selectProvider(String providerName) {
        String lowerProviderName = providerName.toLowerCase();

        if ("test".equals(lowerProviderName)) {
            log.info("테스트 모드로 SMS 서비스를 시작합니다.");
            return new TestSmsProvider();
        }

        SmsProvider provider = providers.get(lowerProviderName);
        if (provider == null) {
            log.warn("SMS 제공자를 찾을 수 없습니다: {}. 테스트 모드로 전환합니다.", providerName);
            return new TestSmsProvider();
        }

        return provider;
    }

    /**
     * 테스트용 SMS 제공자
     */
    private static class TestSmsProvider implements SmsProvider {
        @Override
        public String sendSms(String recipientPhone, String content) {
            log.info("[테스트 SMS] 수신자: {}, 내용: {}", recipientPhone, content);
            return "TEST-" + System.currentTimeMillis();
        }

        @Override
        public String checkStatus(String externalMessageId) {
            return "SENT";
        }

        @Override
        public String getProviderName() {
            return "TEST";
        }
    }
}
