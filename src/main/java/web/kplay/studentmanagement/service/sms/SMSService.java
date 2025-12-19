package web.kplay.studentmanagement.service.sms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.sms.SMSConfig;
import web.kplay.studentmanagement.domain.sms.SMSHistory;
import web.kplay.studentmanagement.domain.sms.SMSTemplate;
import web.kplay.studentmanagement.repository.SMSConfigRepository;
import web.kplay.studentmanagement.repository.SMSHistoryRepository;
import web.kplay.studentmanagement.repository.SMSTemplateRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SMSService {

    private final SMSTemplateRepository templateRepository;
    private final SMSHistoryRepository historyRepository;
    private final SMSConfigRepository configRepository;
    private final AligoSMSClient aligoSMSClient;

    // ========== 템플릿 관리 ==========

    /**
     * 전체 템플릿 조회
     */
    public List<Map<String, Object>> getAllTemplates() {
        return templateRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toTemplateMap)
                .collect(Collectors.toList());
    }

    /**
     * 활성 템플릿만 조회
     */
    public List<Map<String, Object>> getActiveTemplates() {
        return templateRepository.findByIsActiveTrue().stream()
                .map(this::toTemplateMap)
                .collect(Collectors.toList());
    }

    /**
     * 카테고리별 템플릿 조회
     */
    public List<Map<String, Object>> getTemplatesByCategory(String category) {
        return templateRepository.findByCategoryAndIsActiveTrue(category).stream()
                .map(this::toTemplateMap)
                .collect(Collectors.toList());
    }

    /**
     * 템플릿 생성
     */
    @Transactional
    public Map<String, Object> createTemplate(String name, String category, String content, String description) {
        SMSTemplate template = SMSTemplate.builder()
                .name(name)
                .category(category)
                .content(content)
                .description(description)
                .isActive(true)
                .build();

        template = templateRepository.save(template);
        log.info("SMS 템플릿 생성: ID={}, 이름={}", template.getId(), template.getName());

        return toTemplateMap(template);
    }

    /**
     * 템플릿 수정
     */
    @Transactional
    public Map<String, Object> updateTemplate(Long id, String name, String category, String content, String description) {
        SMSTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("템플릿을 찾을 수 없습니다"));

        template.update(name, category, content, description);
        log.info("SMS 템플릿 수정: ID={}", id);

        return toTemplateMap(template);
    }

    /**
     * 템플릿 삭제
     */
    @Transactional
    public void deleteTemplate(Long id) {
        templateRepository.deleteById(id);
        log.info("SMS 템플릿 삭제: ID={}", id);
    }

    // ========== SMS 발송 ==========

    /**
     * 단일 SMS 발송
     */
    @Transactional
    public Map<String, Object> sendSMS(String receiverNumber, String receiverName, String message, String category) {
        SMSConfig config = getActiveConfig();

        // SMS 발송
        AligoSMSClient.SMSSendResponse response = aligoSMSClient.sendSMS(
                config.getApiKey(),
                config.getUserId(),
                config.getSenderNumber(),
                receiverNumber,
                message,
                config.getTestMode()
        );

        // 발송 내역 저장
        SMSHistory history = SMSHistory.builder()
                .senderNumber(config.getSenderNumber())
                .receiverNumber(receiverNumber)
                .receiverName(receiverName)
                .message(message)
                .status(response.isSuccess() ? SMSHistory.SMSStatus.SENT : SMSHistory.SMSStatus.FAILED)
                .msgId(response.getMsgId())
                .cost(response.getCost())
                .smsType(response.getSmsType())
                .category(category)
                .errorMessage(response.getErrorMessage())
                .build();

        if (response.isSuccess()) {
            history.markAsSent(response.getMsgId());
        } else {
            history.markAsFailed(response.getErrorMessage());
        }

        historyRepository.save(history);

        log.info("SMS 발송: 수신={}, 성공={}, 메시지ID={}", receiverNumber, response.isSuccess(), response.getMsgId());

        return Map.of(
                "success", response.isSuccess(),
                "message", response.isSuccess() ? "SMS가 발송되었습니다." : "SMS 발송에 실패했습니다.",
                "msgId", response.getMsgId() != null ? response.getMsgId() : "",
                "cost", response.getCost()
        );
    }

    /**
     * 대량 SMS 발송
     */
    @Transactional
    public Map<String, Object> sendBulkSMS(List<String> receiverNumbers, String message, String category) {
        SMSConfig config = getActiveConfig();

        // 수신번호 목록을 ,로 구분된 문자열로 변환
        String receivers = String.join(",", receiverNumbers);

        // SMS 발송
        AligoSMSClient.SMSSendResponse response = aligoSMSClient.sendBulkSMS(
                config.getApiKey(),
                config.getUserId(),
                config.getSenderNumber(),
                receivers,
                message,
                config.getTestMode()
        );

        // 각 수신자별 발송 내역 저장
        for (String receiverNumber : receiverNumbers) {
            SMSHistory history = SMSHistory.builder()
                    .senderNumber(config.getSenderNumber())
                    .receiverNumber(receiverNumber)
                    .message(message)
                    .status(response.isSuccess() ? SMSHistory.SMSStatus.SENT : SMSHistory.SMSStatus.FAILED)
                    .msgId(response.getMsgId())
                    .cost(response.getCost() / receiverNumbers.size())
                    .smsType(response.getSmsType())
                    .category(category)
                    .errorMessage(response.getErrorMessage())
                    .build();

            if (response.isSuccess()) {
                history.markAsSent(response.getMsgId());
            } else {
                history.markAsFailed(response.getErrorMessage());
            }

            historyRepository.save(history);
        }

        log.info("대량 SMS 발송: 건수={}, 성공={}", receiverNumbers.size(), response.isSuccess());

        return Map.of(
                "success", response.isSuccess(),
                "message", response.isSuccess() ? "SMS가 발송되었습니다." : "SMS 발송에 실패했습니다.",
                "sentCount", response.getSuccessCount(),
                "errorCount", response.getErrorCount(),
                "totalCost", response.getCost()
        );
    }

    // ========== 발송 내역 ==========

    /**
     * 기간별 발송 내역 조회
     */
    public List<Map<String, Object>> getHistory(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        return historyRepository.findByDateRange(start, end).stream()
                .map(this::toHistoryMap)
                .collect(Collectors.toList());
    }

    /**
     * 최근 발송 내역 조회
     */
    public List<Map<String, Object>> getRecentHistory() {
        return historyRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(this::toHistoryMap)
                .collect(Collectors.toList());
    }

    // ========== 통계 ==========

    /**
     * SMS 통계 조회
     */
    public Map<String, Object> getStatistics() {
        SMSConfig config = getActiveConfig();

        // 잔여 건수 조회
        int balance = aligoSMSClient.getRemainingCount(config.getApiKey(), config.getUserId());

        // 오늘 발송 건수
        long sentToday = historyRepository.countSentToday(LocalDate.now());

        // 이번 달 발송 건수
        YearMonth thisMonth = YearMonth.now();
        long sentThisMonth = historyRepository.countSentThisMonth(thisMonth.getYear(), thisMonth.getMonthValue());

        return Map.of(
                "balance", balance,
                "sentToday", sentToday,
                "sentThisMonth", sentThisMonth,
                "provider", config.getProvider().name()
        );
    }

    // ========== 설정 관리 ==========

    /**
     * 활성 설정 조회
     */
    private SMSConfig getActiveConfig() {
        return configRepository.findByIsActiveTrue()
                .orElseGet(() -> configRepository.findTopByOrderByIdDesc()
                        .orElseThrow(() -> new RuntimeException("SMS 설정이 없습니다. 설정을 먼저 등록해주세요.")));
    }

    /**
     * 설정 저장
     */
    @Transactional
    public Map<String, String> saveSettings(String provider, String apiKey, String userId, String senderNumber) {
        // 기존 설정 비활성화
        configRepository.findByIsActiveTrue().ifPresent(config -> config.setIsActive(false));

        // 새 설정 생성
        SMSConfig config = SMSConfig.builder()
                .provider(SMSConfig.SMSProvider.valueOf(provider.toUpperCase()))
                .apiKey(apiKey)
                .userId(userId)
                .senderNumber(senderNumber)
                .isActive(true)
                .testMode(false)
                .autoAttendanceReminder(false)
                .autoEnrollmentExpiry(false)
                .autoPaymentReminder(false)
                .build();

        configRepository.save(config);
        log.info("SMS 설정 저장: 공급사={}, 발신번호={}", provider, senderNumber);

        return Map.of("message", "설정이 저장되었습니다.");
    }

    /**
     * 자동 발송 설정 저장
     */
    @Transactional
    public Map<String, String> saveAutoSettings(Boolean attendanceReminder, Boolean enrollmentExpiry, Boolean paymentReminder) {
        SMSConfig config = getActiveConfig();
        config.updateAutoSettings(attendanceReminder, enrollmentExpiry, paymentReminder);

        log.info("자동 발송 설정 저장: 출석={}, 수강권={}, 결제={}",
                attendanceReminder, enrollmentExpiry, paymentReminder);

        return Map.of("message", "자동 발송 설정이 저장되었습니다.");
    }

    /**
     * 연결 테스트
     */
    public Map<String, Object> testConnection() {
        try {
            SMSConfig config = getActiveConfig();

            // 잔여 건수 조회로 연결 테스트
            int balance = aligoSMSClient.getRemainingCount(config.getApiKey(), config.getUserId());

            return Map.of(
                    "success", true,
                    "message", "연결 테스트 성공",
                    "balance", balance
            );
        } catch (Exception e) {
            log.error("SMS 연결 테스트 실패", e);
            return Map.of(
                    "success", false,
                    "message", "연결 테스트 실패: " + e.getMessage()
            );
        }
    }

    // ========== 헬퍼 메서드 ==========

    private Map<String, Object> toTemplateMap(SMSTemplate template) {
        return Map.of(
                "id", template.getId(),
                "name", template.getName(),
                "category", template.getCategory(),
                "content", template.getContent(),
                "description", template.getDescription() != null ? template.getDescription() : "",
                "isActive", template.getIsActive()
        );
    }

    private Map<String, Object> toHistoryMap(SMSHistory history) {
        return Map.of(
                "id", history.getId(),
                "senderNumber", history.getSenderNumber(),
                "receiverNumber", history.getReceiverNumber(),
                "receiverName", history.getReceiverName() != null ? history.getReceiverName() : "",
                "message", history.getMessage(),
                "status", history.getStatus().name(),
                "sentAt", history.getSentAt() != null ? history.getSentAt().toString() : "",
                "cost", history.getCost() != null ? history.getCost() : 0,
                "smsType", history.getSmsType() != null ? history.getSmsType() : "",
                "category", history.getCategory() != null ? history.getCategory() : ""
        );
    }
}
