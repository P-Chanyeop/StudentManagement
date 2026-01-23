package web.kplay.studentmanagement.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import web.kplay.studentmanagement.controller.reservation.NaverBookingSseController;
import web.kplay.studentmanagement.service.NaverBookingCrawlerService;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverBookingScheduler {

    private final NaverBookingCrawlerService crawlerService;

    // 매일 오전 11시 6분에 실행
    @Scheduled(cron = "0 6 11 * * *")
    public void scheduledCrawling() {
        log.info("=== 네이버 예약 자동 크롤링 시작 (매일 오전 11시 6분) ===");
        try {
            crawlerService.crawlNaverBookings();
            log.info("=== 네이버 예약 자동 크롤링 완료 ===");
            
            // 프론트엔드에 알림 전송
            NaverBookingSseController.notifyCrawlingComplete();
        } catch (Exception e) {
            log.error("=== 네이버 예약 자동 크롤링 실패 ===", e);
        }
    }
}
