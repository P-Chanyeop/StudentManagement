package web.kplay.studentmanagement.service.holiday;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import web.kplay.studentmanagement.domain.holiday.Holiday;
import web.kplay.studentmanagement.repository.HolidayRepository;

import jakarta.annotation.PostConstruct;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 공휴일 관리 서비스
 * - 공휴일 정보 저장 및 조회
 * - 수강권 기간 계산 시 공휴일 제외
 * - 공휴일 데이터 캐싱
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayRepository holidayRepository;
    
    // 공휴일 캐시 (년도별)
    private final Map<Integer, List<Holiday>> holidayCache = new ConcurrentHashMap<>();
    
    @Value("${holiday.api.key:}")
    private String holidayApiKey;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 서버 시작 시 3년치 공휴일 데이터 초기화
     */
    @PostConstruct
    @Transactional
    public void initializeHolidayData() {
        int currentYear = LocalDate.now().getYear();
        
        // 현재 년도부터 3년치만 초기화 (API 데이터 제공 범위 고려)
        for (int year = currentYear; year <= currentYear + 2; year++) {
            // 해당 년도에 데이터가 하나라도 있으면 건너뛰기
            boolean hasData = holidayRepository.existsByYear(year);
            
            if (!hasData) {
                try {
                    List<Holiday> apiHolidays = fetchHolidaysFromApi(year);
                    if (!apiHolidays.isEmpty()) {
                        // 각 공휴일을 개별적으로 중복 체크 후 저장
                        int savedCount = 0;
                        for (Holiday holiday : apiHolidays) {
                            if (!holidayRepository.existsByDate(holiday.getDate())) {
                                holidayRepository.save(holiday);
                                savedCount++;
                            }
                        }
                        log.info("공휴일 API 데이터 저장 완료: {}년 {}개", year, savedCount);
                    } else {
                        // API 실패 시 기본 공휴일 저장
                        List<Holiday> defaultHolidays = createDefaultHolidays(year);
                        int savedCount = 0;
                        for (Holiday holiday : defaultHolidays) {
                            if (!holidayRepository.existsByDate(holiday.getDate())) {
                                holidayRepository.save(holiday);
                                savedCount++;
                            }
                        }
                        log.info("공휴일 기본 데이터 저장 완료: {}년 {}개", year, savedCount);
                    }
                } catch (Exception e) {
                    log.error("공휴일 데이터 초기화 실패: {}년 - {}", year, e.getMessage());
                }
            } else {
                log.info("공휴일 데이터 이미 존재: {}년", year);
            }
        }
        
        log.info("3년치 공휴일 데이터 초기화 완료 ({}-{})", currentYear, currentYear + 2);
    }

    /**
     * 매일 새벽 3시에 공휴일 데이터 동기화 (임시공휴일 등 반영)
     */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void syncHolidayData() {
        int currentYear = LocalDate.now().getYear();
        for (int year = currentYear; year <= currentYear + 2; year++) {
            try {
                List<Holiday> apiHolidays = fetchHolidaysFromApi(year);
                int savedCount = 0;
                for (Holiday holiday : apiHolidays) {
                    if (!holidayRepository.existsByDate(holiday.getDate())) {
                        holidayRepository.save(holiday);
                        savedCount++;
                    }
                }
                if (savedCount > 0) {
                    holidayCache.remove(year);
                    log.info("공휴일 동기화: {}년 {}개 추가", year, savedCount);
                }
            } catch (Exception e) {
                log.error("공휴일 동기화 실패: {}년 - {}", year, e.getMessage());
            }
        }
    }
    
    /**
     * 공공데이터 포탈 API에서 공휴일 데이터 가져오기
     */
    private List<Holiday> fetchHolidaysFromApi(int year) {
        if (holidayApiKey == null || holidayApiKey.isEmpty()) {
            log.warn("공휴일 API 키가 설정되지 않음");
            return new ArrayList<>();
        }
        
        try {
            String url = String.format(
                "http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getHoliDeInfo" +
                "?serviceKey=%s&solYear=%d&numOfRows=100&_type=json",
                holidayApiKey, year
            );
            
            String response = restTemplate.getForObject(url, String.class);
            return parseHolidayResponse(response, year);
            
        } catch (Exception e) {
            log.error("공휴일 API 호출 실패: {}년 - {}", year, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * API 응답 파싱
     */
    private List<Holiday> parseHolidayResponse(String response, int year) {
        List<Holiday> holidays = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode responseNode = root.path("response");
            JsonNode bodyNode = responseNode.path("body");
            JsonNode itemsNode = bodyNode.path("items");
            JsonNode itemNode = itemsNode.path("item");
            
            if (itemNode.isArray()) {
                for (JsonNode holiday : itemNode) {
                    String isHoliday = holiday.path("isHoliday").asText();
                    if ("Y".equals(isHoliday)) {
                        String locdate = holiday.path("locdate").asText();
                        String dateName = holiday.path("dateName").asText();
                        
                        // 날짜 파싱 (yyyyMMdd 형식)
                        LocalDate date = LocalDate.parse(locdate, DateTimeFormatter.ofPattern("yyyyMMdd"));
                        
                        Holiday holidayEntity = Holiday.builder()
                                .date(date)
                                .name(dateName)
                                .isRecurring(false)
                                .build();
                        
                        holidays.add(holidayEntity);
                    }
                }
            } else if (!itemNode.isMissingNode()) {
                // 단일 항목인 경우
                String isHoliday = itemNode.path("isHoliday").asText();
                if ("Y".equals(isHoliday)) {
                    String locdate = itemNode.path("locdate").asText();
                    String dateName = itemNode.path("dateName").asText();
                    
                    LocalDate date = LocalDate.parse(locdate, DateTimeFormatter.ofPattern("yyyyMMdd"));
                    
                    Holiday holidayEntity = Holiday.builder()
                            .date(date)
                            .name(dateName)
                            .isRecurring(false)
                            .build();
                    
                    holidays.add(holidayEntity);
                }
            }
            
            log.info("공휴일 API 파싱 완료: {}년 {}개", year, holidays.size());
            
        } catch (Exception e) {
            log.error("공휴일 API 응답 파싱 실패: {}", e.getMessage());
        }
        
        return holidays;
    }
    
    /**
     * 기본 공휴일 생성 (API 실패 시 대체)
     */
    private List<Holiday> createDefaultHolidays(int year) {
        List<Holiday> holidays = new ArrayList<>();
        
        holidays.add(Holiday.builder().date(LocalDate.of(year, 1, 1)).name("신정").isRecurring(false).build());
        holidays.add(Holiday.builder().date(LocalDate.of(year, 3, 1)).name("삼일절").isRecurring(false).build());
        holidays.add(Holiday.builder().date(LocalDate.of(year, 5, 5)).name("어린이날").isRecurring(false).build());
        holidays.add(Holiday.builder().date(LocalDate.of(year, 6, 6)).name("현충일").isRecurring(false).build());
        holidays.add(Holiday.builder().date(LocalDate.of(year, 8, 15)).name("광복절").isRecurring(false).build());
        holidays.add(Holiday.builder().date(LocalDate.of(year, 10, 3)).name("개천절").isRecurring(false).build());
        holidays.add(Holiday.builder().date(LocalDate.of(year, 10, 9)).name("한글날").isRecurring(false).build());
        holidays.add(Holiday.builder().date(LocalDate.of(year, 12, 25)).name("크리스마스").isRecurring(false).build());
        
        return holidays;
    }

    /**
     * 특정 년도의 공휴일 조회 (캐시 사용)
     */
    public List<Holiday> getHolidaysByYear(int year) {
        return holidayCache.computeIfAbsent(year, y -> {
            LocalDate startOfYear = LocalDate.of(y, 1, 1);
            LocalDate endOfYear = LocalDate.of(y, 12, 31);
            List<Holiday> holidays = holidayRepository.findByDateRange(startOfYear, endOfYear);
            log.info("공휴일 데이터 캐시됨: {}년 {}개", y, holidays.size());
            return holidays;
        });
    }

    /**
     * 캐시된 공휴일 데이터로 특정 날짜가 공휴일인지 확인
     */
    public boolean isHoliday(LocalDate date) {
        List<Holiday> holidays = getHolidaysByYear(date.getYear());
        return holidays.stream().anyMatch(h -> h.getDate().equals(date));
    }

    /**
     * 영업일 계산 (주말, 공휴일 제외)
     */
    public LocalDate calculateEndDate(LocalDate startDate, int businessDays) {
        LocalDate currentDate = startDate;
        int count = 0;
        
        // 시작일이 영업일이면 1로 시작
        if (isBusinessDay(currentDate)) {
            count = 1;
        }
        
        while (count < businessDays) {
            currentDate = currentDate.plusDays(1);
            if (isBusinessDay(currentDate)) {
                count++;
            }
        }
        
        return currentDate;
    }

    /**
     * 영업일인지 확인 (주말, 공휴일 제외)
     */
    public boolean isBusinessDay(LocalDate date) {
        // 주말 체크
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }
        
        // 공휴일 체크
        return !isHoliday(date);
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
            if (isBusinessDay(current)) {
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
            if (isBusinessDay(current)) {
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
