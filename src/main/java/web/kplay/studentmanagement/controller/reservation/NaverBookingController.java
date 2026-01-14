package web.kplay.studentmanagement.controller.reservation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import web.kplay.studentmanagement.service.NaverBookingCrawlerService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/naver-booking")
@RequiredArgsConstructor
public class NaverBookingController {

    private final NaverBookingCrawlerService crawlerService;

    @PostMapping("/sync")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<?> syncNaverBookings() {
        try {
            log.info("네이버 예약 동기화 요청");
            crawlerService.crawlNaverBookings();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "네이버 예약 동기화 완료"
            ));
        } catch (Exception e) {
            log.error("네이버 예약 동기화 실패", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "동기화 실패: " + e.getMessage()
            ));
        }
    }
}
