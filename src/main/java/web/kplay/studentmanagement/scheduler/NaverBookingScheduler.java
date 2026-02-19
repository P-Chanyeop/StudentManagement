package web.kplay.studentmanagement.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import web.kplay.studentmanagement.controller.reservation.NaverBookingSseController;
import web.kplay.studentmanagement.service.NaverBookingApiCrawlerService;
import web.kplay.studentmanagement.service.message.sms.SmsService;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverBookingScheduler {

    private final NaverBookingApiCrawlerService crawlerService;
    private final SmsService smsService;

    // 매일 오전 9시 00분에 실행
    @Scheduled(cron = "0 30 8 * * *")
    public void scheduledCrawling() {
        log.info("=== 네이버 예약 자동 크롤링 시작 (매일 오전 9시) ===");
        String result = "FAIL";
        try {
            crawlerService.crawlNaverBookings(null);
            log.info("=== 네이버 예약 자동 크롤링 완료 ===");
            result = "OK";

            // 프론트엔드에 알림 전송
            NaverBookingSseController.notifyCrawlingComplete();
        } catch (Exception e) {
            log.error("=== 네이버 예약 자동 크롤링 실패 ===", e);
        } finally {
            // 크롤링 결과 문자 발송
            sendResultSms(result);
        }
    }

    private void sendResultSms(String result) {
        try {
            String message = "[네이버예약 크롤링] " + result;
            smsService.sendSms("010-4414-7579", message);
            log.info("크롤링 결과 문자 발송 완료: {}", result);
        } catch (Exception e) {
            log.error("크롤링 결과 문자 발송 실패", e);
        }
    }
}
