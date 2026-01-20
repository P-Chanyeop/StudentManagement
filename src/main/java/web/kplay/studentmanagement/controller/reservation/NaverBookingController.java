package web.kplay.studentmanagement.controller.reservation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.domain.reservation.NaverBooking;
import web.kplay.studentmanagement.dto.NaverBookingDTO;
import web.kplay.studentmanagement.service.NaverBookingCrawlerService;

import java.util.List;
import java.util.Map;

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
    
    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<NaverBookingDTO>> getBookingsByDate(@PathVariable String date) {
        try {
            List<NaverBookingDTO> bookings = crawlerService.getBookingsByDate(date);
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            log.error("네이버 예약 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/check-duplicates")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, Object>> checkDuplicates() {
        try {
            List<NaverBooking> allBookings = crawlerService.getAllBookings();
            long totalCount = allBookings.size();
            long uniqueCount = allBookings.stream()
                .map(NaverBooking::getBookingNumber)
                .distinct()
                .count();
            
            Map<String, Long> duplicates = allBookings.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    NaverBooking::getBookingNumber,
                    java.util.stream.Collectors.counting()
                ))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
                ));
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("totalCount", totalCount);
            result.put("uniqueCount", uniqueCount);
            result.put("hasDuplicates", totalCount != uniqueCount);
            result.put("duplicates", duplicates);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("중복 확인 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
