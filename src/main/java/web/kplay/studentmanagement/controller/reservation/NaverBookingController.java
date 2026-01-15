package web.kplay.studentmanagement.controller.reservation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import web.kplay.studentmanagement.dto.NaverBookingDTO;
import web.kplay.studentmanagement.service.NaverBookingCrawlerService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/naver-booking")
@RequiredArgsConstructor
public class NaverBookingController {

    private final NaverBookingCrawlerService crawlerService;

    @PostMapping("/sync")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<NaverBookingDTO>> syncNaverBookings() {
        try {
            log.info("네이버 예약 동기화 요청");
            List<NaverBookingDTO> bookings = crawlerService.crawlNaverBookings();
            log.info("네이버 예약 동기화 완료: {}건", bookings.size());
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            log.error("네이버 예약 동기화 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<NaverBookingDTO>> getTodayBookings() {
        try {
            List<NaverBookingDTO> bookings = crawlerService.getTodayBookings();
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            log.error("네이버 예약 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
