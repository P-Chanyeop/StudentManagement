package web.kplay.studentmanagement.controller.holiday;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.domain.holiday.Holiday;
import web.kplay.studentmanagement.service.holiday.HolidayService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    /**
     * 공휴일 등록
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Holiday> createHoliday(@RequestBody Map<String, Object> request) {
        LocalDate date = LocalDate.parse(request.get("date").toString());
        String name = request.get("name").toString();
        Boolean isRecurring = request.get("isRecurring") != null
                ? Boolean.parseBoolean(request.get("isRecurring").toString())
                : false;
        String description = request.get("description") != null
                ? request.get("description").toString()
                : null;

        Holiday holiday = holidayService.createHoliday(date, name, isRecurring, description);
        return ResponseEntity.status(HttpStatus.CREATED).body(holiday);
    }

    /**
     * 특정 연도의 공휴일 조회
     */
    @GetMapping("/year/{year}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<Holiday>> getHolidaysByYear(@PathVariable int year) {
        List<Holiday> holidays = holidayService.getHolidaysByYear(year);
        return ResponseEntity.ok(holidays);
    }

    /**
     * 특정 날짜가 공휴일인지 확인
     */
    @GetMapping("/check")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, Boolean>> isHoliday(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        boolean isHoliday = holidayService.isHoliday(date);
        return ResponseEntity.ok(Map.of("isHoliday", isHoliday));
    }

    /**
     * 공휴일 삭제
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {
        holidayService.deleteHoliday(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 공휴일을 제외한 실제 수업일 계산
     */
    @GetMapping("/business-days")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, Integer>> calculateBusinessDays(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        int businessDays = holidayService.calculateBusinessDays(startDate, endDate);
        return ResponseEntity.ok(Map.of("businessDays", businessDays));
    }

    /**
     * N일의 수업일 후 날짜 계산
     */
    @GetMapping("/add-business-days")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, LocalDate>> addBusinessDays(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam int days) {
        LocalDate resultDate = holidayService.addBusinessDays(startDate, days);
        return ResponseEntity.ok(Map.of("resultDate", resultDate));
    }
}
