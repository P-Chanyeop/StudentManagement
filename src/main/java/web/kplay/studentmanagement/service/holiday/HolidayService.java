package web.kplay.studentmanagement.service.holiday;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.holiday.Holiday;
import web.kplay.studentmanagement.repository.HolidayRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 공휴일 관리 서비스
 * - 공휴일 정보 저장 및 조회
 * - 수강권 기간 계산 시 공휴일 제외
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayRepository holidayRepository;

    /**
     * 특정 날짜가 공휴일인지 확인
     */
    @Transactional(readOnly = true)
    public boolean isHoliday(LocalDate date) {
        return holidayRepository.existsByDate(date);
    }

    /**
     * 특정 기간 내의 공휴일 개수 계산
     */
    @Transactional(readOnly = true)
    public int countHolidaysInRange(LocalDate startDate, LocalDate endDate) {
        List<Holiday> holidays = holidayRepository.findByDateRange(startDate, endDate);
        return holidays.size();
    }

    /**
     * 공휴일을 제외한 실제 수업 일수 계산
     * 주말(토,일)과 공휴일을 제외
     */
    @Transactional(readOnly = true)
    public int calculateBusinessDays(LocalDate startDate, LocalDate endDate) {
        int totalDays = 0;
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            // 주말이 아니고 공휴일이 아닌 경우만 카운트
            if (!isWeekend(current) && !isHoliday(current)) {
                totalDays++;
            }
            current = current.plusDays(1);
        }

        return totalDays;
    }

    /**
     * 공휴일을 제외한 N일 후의 날짜 계산
     * 수강권 종료일 계산 시 사용
     */
    @Transactional(readOnly = true)
    public LocalDate addBusinessDays(LocalDate startDate, int businessDays) {
        LocalDate current = startDate;
        int daysAdded = 0;

        while (daysAdded < businessDays) {
            current = current.plusDays(1);
            // 주말이 아니고 공휴일이 아닌 경우만 카운트
            if (!isWeekend(current) && !isHoliday(current)) {
                daysAdded++;
            }
        }

        return current;
    }

    /**
     * 주말 여부 확인
     */
    private boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * 공휴일 정보 저장
     */
    @Transactional
    public Holiday createHoliday(LocalDate date, String name, Boolean isRecurring, String description) {
        // 이미 존재하는 날짜인 경우 에러
        if (holidayRepository.existsByDate(date)) {
            throw new IllegalArgumentException("이미 등록된 공휴일입니다: " + date);
        }

        Holiday holiday = Holiday.builder()
                .date(date)
                .name(name)
                .isRecurring(isRecurring != null ? isRecurring : false)
                .description(description)
                .build();

        Holiday saved = holidayRepository.save(holiday);
        log.info("공휴일 등록: 날짜={}, 이름={}", date, name);

        return saved;
    }

    /**
     * 특정 연도의 공휴일 조회
     */
    @Transactional(readOnly = true)
    public List<Holiday> getHolidaysByYear(int year) {
        return holidayRepository.findByYear(year);
    }

    /**
     * 공휴일 삭제
     */
    @Transactional
    public void deleteHoliday(Long id) {
        holidayRepository.deleteById(id);
        log.info("공휴일 삭제: id={}", id);
    }

    /**
     * 다음 연도 공휴일 자동 동기화 (매년 1월 1일 오전 1시)
     * TODO: 실제 공공데이터포털 API 연동
     * API 키 발급: https://www.data.go.kr/
     * 특일정보 조회 서비스 활용
     */
    @Scheduled(cron = "0 0 1 1 1 *") // 매년 1월 1일 오전 1시
    @Transactional
    public void syncNextYearHolidays() {
        int nextYear = LocalDate.now().getYear() + 1;
        log.info("{}년 공휴일 동기화 시작", nextYear);

        // TODO: 공공데이터포털 API 호출
        // 1. API 키 설정
        // 2. HTTP 요청으로 공휴일 정보 조회
        // 3. 응답 파싱
        // 4. DB에 저장

        // 현재는 기본 공휴일만 등록 (예시)
        List<LocalDate> fixedHolidays = getFixedHolidays(nextYear);

        for (LocalDate date : fixedHolidays) {
            if (!holidayRepository.existsByDate(date)) {
                Holiday holiday = Holiday.builder()
                        .date(date)
                        .name(getHolidayName(date))
                        .isRecurring(true)
                        .description("자동 등록된 법정 공휴일")
                        .build();
                holidayRepository.save(holiday);
            }
        }

        log.info("{}년 공휴일 동기화 완료: 총 {}건", nextYear, fixedHolidays.size());
    }

    /**
     * 고정 공휴일 목록 반환 (매년 동일한 날짜)
     */
    private List<LocalDate> getFixedHolidays(int year) {
        List<LocalDate> holidays = new ArrayList<>();

        // 신정
        holidays.add(LocalDate.of(year, 1, 1));

        // 삼일절
        holidays.add(LocalDate.of(year, 3, 1));

        // 어린이날
        holidays.add(LocalDate.of(year, 5, 5));

        // 현충일
        holidays.add(LocalDate.of(year, 6, 6));

        // 광복절
        holidays.add(LocalDate.of(year, 8, 15));

        // 개천절
        holidays.add(LocalDate.of(year, 10, 3));

        // 한글날
        holidays.add(LocalDate.of(year, 10, 9));

        // 크리스마스
        holidays.add(LocalDate.of(year, 12, 25));

        // TODO: 음력 공휴일 (설날, 부처님오신날, 추석) 계산 추가
        // 음력 → 양력 변환 로직 필요 또는 API 활용

        return holidays;
    }

    /**
     * 공휴일 이름 반환
     */
    private String getHolidayName(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        if (month == 1 && day == 1) return "신정";
        if (month == 3 && day == 1) return "삼일절";
        if (month == 5 && day == 5) return "어린이날";
        if (month == 6 && day == 6) return "현충일";
        if (month == 8 && day == 15) return "광복절";
        if (month == 10 && day == 3) return "개천절";
        if (month == 10 && day == 9) return "한글날";
        if (month == 12 && day == 25) return "크리스마스";

        return "공휴일";
    }
}
