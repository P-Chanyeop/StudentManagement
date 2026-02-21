package web.kplay.studentmanagement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.reservation.NaverBooking;
import web.kplay.studentmanagement.dto.NaverBookingDTO;
import web.kplay.studentmanagement.repository.NaverBookingRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverBookingApiCrawlerService {

    private final NaverBookingRepository naverBookingRepository;
    private final web.kplay.studentmanagement.repository.AttendanceRepository attendanceRepository;
    private final StudentCourseExcelService studentCourseExcelService;

    private static final String API_BASE_URL = "https://partner.booking.naver.com/api/businesses/1047988/bookings";
    
    @Value("${naver.id:}")
    private String naverId;
    
    @Value("${naver.pw:}")
    private String naverPw;
    
    // 캐시된 쿠키
    private String cachedNidAut = null;
    private String cachedNidSes = null;
    private long cookieExpireTime = 0;

    /**
     * Selenium으로 네이버 로그인 후 쿠키 획득
     */
    private void refreshCookiesWithSelenium() {
        log.info("Selenium으로 네이버 로그인 시작");
        
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.setBinary("/usr/bin/google-chrome-stable");
        
        WebDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            
            // 네이버 로그인 페이지
            driver.get("https://nid.naver.com/nidlogin.login");
            Thread.sleep(2000);
            
            // 자바스크립트로 아이디/비밀번호 입력 (복사붙여넣기 방식)
            org.openqa.selenium.JavascriptExecutor js = (org.openqa.selenium.JavascriptExecutor) driver;
            js.executeScript("document.getElementById('id').value = arguments[0]", naverId);
            js.executeScript("document.getElementById('pw').value = arguments[0]", naverPw);
            
            Thread.sleep(500);
            
            // 로그인 버튼 클릭
            driver.findElement(org.openqa.selenium.By.id("log.login")).click();
            Thread.sleep(3000);
            
            // 파트너센터로 이동
            driver.get("https://partner.booking.naver.com/");
            Thread.sleep(3000);
            
            // 쿠키 추출
            Set<Cookie> cookies = driver.manage().getCookies();
            for (Cookie cookie : cookies) {
                if ("NID_AUT".equals(cookie.getName())) {
                    cachedNidAut = cookie.getValue();
                } else if ("NID_SES".equals(cookie.getName())) {
                    cachedNidSes = cookie.getValue();
                }
            }
            
            if (cachedNidAut != null && cachedNidSes != null) {
                // 쿠키 유효시간 1시간 설정
                cookieExpireTime = System.currentTimeMillis() + (60 * 60 * 1000);
                log.info("네이버 쿠키 획득 성공");
            } else {
                log.error("네이버 쿠키 획득 실패");
            }
            
        } catch (Exception e) {
            log.error("Selenium 로그인 실패", e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }
    
    private String getCookieString() {
        // 쿠키가 없거나 만료되었으면 갱신
        if (cachedNidAut == null || cachedNidSes == null || System.currentTimeMillis() > cookieExpireTime) {
            refreshCookiesWithSelenium();
        }
        return String.format("NID_AUT=%s; NID_SES=%s", cachedNidAut, cachedNidSes);
    }

    @Transactional
    public List<NaverBookingDTO> crawlNaverBookings(String dateStr) {
        List<NaverBookingDTO> allBookings = new ArrayList<>();
        
        try {
            log.info("네이버 예약 API 크롤링 시작");
            
            // 날짜 파라미터 없으면 오늘
            LocalDate targetDate = (dateStr != null && !dateStr.isEmpty()) 
                ? LocalDate.parse(dateStr) 
                : LocalDate.now();
            String targetDateStr = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String timestamp = targetDateStr + "T00:00:00.000Z";
            
            log.info("조회 날짜: {}", targetDateStr);
            
            int page = 0;
            while (true) {
                String url = String.format(
                    "%s?bizItemTypes=STANDARD&bookingStatusCodes=&dateDropdownType=TODAY&dateFilter=USEDATE" +
                    "&endDateTime=%s&maxDays=31&nPayChargedStatusCodes=&orderBy=&orderByStartDate=ASC" +
                    "&paymentStatusCodes=&searchValue=&startDateTime=%s&page=%d&size=50&noCache=%d",
                    API_BASE_URL, timestamp, timestamp, page, System.currentTimeMillis()
                );
                
                List<NaverBookingDTO> pageBookings = fetchBookingsFromApi(url);
                
                if (pageBookings.isEmpty()) {
                    break;
                }
                
                allBookings.addAll(pageBookings);
                log.info("페이지 {}: {}건 로드 (누적: {}건)", page, pageBookings.size(), allBookings.size());
                
                if (pageBookings.size() < 50) {
                    break;
                }
                
                page++;
            }
            
            log.info("총 {}건 크롤링 완료", allBookings.size());
            
            // DB 저장
            saveBookingsToDatabase(allBookings);
            
            return allBookings;
            
        } catch (Exception e) {
            log.error("네이버 예약 API 크롤링 실패", e);
            throw new RuntimeException("크롤링 실패: " + e.getMessage());
        }
    }

    private List<NaverBookingDTO> fetchBookingsFromApi(String url) throws Exception {
        List<NaverBookingDTO> bookings = new ArrayList<>();
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            request.setHeader("Accept", "application/json");
            request.setHeader("Referer", "https://partner.booking.naver.com/");
            request.setHeader("Cookie", getCookieString());
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                
                // 응답 로그 추가
                log.debug("API 응답: {}", jsonResponse.length() > 500 ? jsonResponse.substring(0, 500) + "..." : jsonResponse);
                
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootArray = mapper.readTree(jsonResponse);
                
                // 응답이 배열이 아닌 경우 (에러 응답 등) - 쿠키 갱신 후 재시도
                if (!rootArray.isArray()) {
                    log.warn("API 응답이 배열이 아님, 쿠키 갱신 시도");
                    refreshCookiesWithSelenium();
                    return bookings;
                }
                
                if (rootArray.isArray()) {
                    for (JsonNode bookingNode : rootArray) {
                        NaverBookingDTO dto = parseBookingNode(bookingNode);
                        if (dto != null) {
                            bookings.add(dto);
                        }
                    }
                }
            }
        }
        
        return bookings;
    }

    private NaverBookingDTO parseBookingNode(JsonNode node) {
        try {
            String bookingNumber = node.get("bookingId").asText();
            String status = node.get("bookingStatusCode").asText();
            String parentName = node.get("name").asText();
            String phone = node.get("phone").asText();
            String startDate = node.get("startDate").asText();
            String bizItemName = node.get("bizItemName").asText();
            
            // customFormInputJson에서 학생 이름과 학교 추출
            String studentName = null;
            String school = null;
            String startDateTime = null;
            String endDateTime = null;
            
            JsonNode snapshot = node.get("snapshotJson");
            if (snapshot != null) {
                // 시간 정보 추출
                if (snapshot.has("startDateTime")) {
                    startDateTime = snapshot.get("startDateTime").asText();
                }
                if (snapshot.has("endDateTime")) {
                    endDateTime = snapshot.get("endDateTime").asText();
                }
                
                JsonNode customForm = snapshot.get("customFormInputJson");
                if (customForm != null && customForm.isArray()) {
                    for (JsonNode field : customForm) {
                        String title = field.get("title").asText();
                        String value = field.has("value") ? field.get("value").asText() : null;
                        
                        if (title.contains("학생이름")) {
                            studentName = value;
                        } else if (title.contains("학교")) {
                            school = value;
                        }
                    }
                }
            }
            
            return NaverBookingDTO.builder()
                    .bookingNumber(bookingNumber)
                    .status(status)
                    .name(parentName)
                    .studentName(studentName)
                    .school(school)
                    .phone(phone)
                    .bookingTime(startDate)
                    .product(bizItemName)
                    .orderDate(startDateTime)  // 시작 시간
                    .confirmDate(endDateTime)  // 종료 시간
                    .build();
                    
        } catch (Exception e) {
            log.error("예약 파싱 실패", e);
            return null;
        }
    }

    private void saveBookingsToDatabase(List<NaverBookingDTO> bookings) {
        LocalDateTime now = LocalDateTime.now();
        int savedCount = 0;
        int updatedCount = 0;

        for (NaverBookingDTO dto : bookings) {
            try {
                NaverBooking existingEntity = naverBookingRepository.findByBookingNumber(dto.getBookingNumber())
                        .orElse(null);

                boolean isNew = (existingEntity == null);

                NaverBooking entity = NaverBooking.builder()
                        .id(isNew ? null : existingEntity.getId())
                        .bookingNumber(dto.getBookingNumber())
                        .status(dto.getStatus())
                        .name(dto.getName())
                        .studentName(dto.getStudentName())
                        .school(dto.getSchool())
                        .phone(dto.getPhone())
                        .bookingTime(dto.getBookingTime())
                        .product(dto.getProduct())
                        .orderDate(dto.getOrderDate())
                        .confirmDate(dto.getConfirmDate())
                        .syncedAt(now)
                        .build();

                naverBookingRepository.save(entity);

                // 신규 예약이고 확정 상태이면 출석 레코드 생성
                if (isNew && "RC03".equals(entity.getStatus())) {
                    createAttendanceForNaverBooking(entity);
                }

                // 기존 예약이 취소된 경우 출석 레코드 삭제
                if (!isNew && !"RC03".equals(entity.getStatus())) {
                    deleteAttendanceForCancelledBooking(entity);
                }

                if (isNew) {
                    savedCount++;
                } else {
                    updatedCount++;
                }
            } catch (Exception e) {
                log.error("예약 저장 실패: {}", dto.getBookingNumber(), e);
            }
        }

        log.info("DB 저장 완료: 신규 {}건, 업데이트 {}건", savedCount, updatedCount);
    }

    private void deleteAttendanceForCancelledBooking(NaverBooking naverBooking) {
        try {
            LocalDate bookingDate = LocalDate.parse(naverBooking.getBookingTime());
            attendanceRepository.findByDate(bookingDate).stream()
                .filter(a -> a.getNaverBooking() != null && a.getNaverBooking().getId().equals(naverBooking.getId()))
                .forEach(a -> {
                    attendanceRepository.delete(a);
                    log.info("취소된 예약 출석 레코드 삭제: 예약번호={}, 학생={}", 
                            naverBooking.getBookingNumber(), naverBooking.getStudentName());
                });
        } catch (Exception e) {
            log.error("취소된 예약 출석 삭제 실패: {}", naverBooking.getBookingNumber(), e);
        }
    }

    private void createAttendanceForNaverBooking(NaverBooking naverBooking) {
        try {
            // bookingTime 파싱: "2026-01-23" -> LocalDate
            LocalDate bookingDate = LocalDate.parse(naverBooking.getBookingTime());
            
            // orderDate에서 시작 시간 파싱: "2026-01-23T00:00:00Z" -> 한국시간으로 변환
            LocalTime bookingTime = LocalTime.of(9, 0); // 기본값
            
            if (naverBooking.getOrderDate() != null) {
                try {
                    // UTC 시간을 한국 시간으로 변환 (+9시간)
                    java.time.Instant startInstant = java.time.Instant.parse(naverBooking.getOrderDate());
                    java.time.ZonedDateTime koreaTime = startInstant.atZone(java.time.ZoneId.of("Asia/Seoul"));
                    bookingTime = koreaTime.toLocalTime();
                } catch (Exception e) {
                    log.warn("시간 파싱 실패, 기본값 사용: {}", e.getMessage());
                }
            }
            
            log.info("네이버 예약 파싱: 날짜={}, 시작시간={}", bookingDate, bookingTime);
            
            // 이미 출석 레코드가 있는지 확인
            boolean exists = attendanceRepository.findByDate(bookingDate).stream()
                .anyMatch(a -> a.getNaverBooking() != null && 
                              a.getNaverBooking().getId().equals(naverBooking.getId()));
            
            if (exists) {
                log.debug("이미 출석 레코드가 존재함: {}", naverBooking.getBookingNumber());
                return;
            }
            
            // 엑셀에서 학생 이름으로 반 정보 조회
            String studentName = naverBooking.getStudentName();
            if (studentName == null || studentName.trim().isEmpty()) {
                log.warn("학생 이름 없음: {}", naverBooking.getBookingNumber());
                studentName = naverBooking.getName(); // 부모님 이름으로 대체
            }
            
            // 공백 제거
            studentName = studentName.trim().replaceAll("\\s+", "");
            
            String courseName = studentCourseExcelService.getCourseName(studentName);
            Integer durationMinutes = studentCourseExcelService.getDurationMinutes(studentName);
            
            LocalTime expectedLeaveTime = null;
            
            // 엑셀 매칭 정보 우선 사용
            if (courseName != null && durationMinutes != null) {
                expectedLeaveTime = bookingTime.plusMinutes(durationMinutes);
                log.info("학생 [{}] 반: {}, 수업 시간: {}분, 시작: {}, 예상 하원: {}", 
                        studentName, courseName, durationMinutes, bookingTime, expectedLeaveTime);
            } else {
                log.warn("학생 [{}]의 반 정보 없음 - 예상 하원 시간 미설정", studentName);
            }
            
            // 출석 레코드 생성
            web.kplay.studentmanagement.domain.attendance.Attendance attendance = 
                web.kplay.studentmanagement.domain.attendance.Attendance.builder()
                    .naverBooking(naverBooking)
                    .attendanceDate(bookingDate)
                    .attendanceTime(bookingTime)
                    .durationMinutes(durationMinutes)
                    .expectedLeaveTime(expectedLeaveTime)
                    .originalExpectedLeaveTime(expectedLeaveTime)
                    .status(web.kplay.studentmanagement.domain.attendance.AttendanceStatus.NOTYET)
                    .classCompleted(false)
                    .build();
            
            attendanceRepository.save(attendance);
            log.info("네이버 예약 출석 레코드 생성: {}, 날짜={}, 시간={}", 
                    studentName, bookingDate, bookingTime);
        } catch (Exception e) {
            log.error("네이버 예약 출석 레코드 생성 실패: {}", e.getMessage(), e);
        }
    }

    public List<NaverBookingDTO> getTodayBookings() {
        return naverBookingRepository.findAll().stream()
            .map(entity -> NaverBookingDTO.builder()
                .status(entity.getStatus())
                .name(entity.getName())
                .studentName(entity.getStudentName())
                .school(entity.getSchool())
                .phone(entity.getPhone())
                .bookingNumber(entity.getBookingNumber())
                .bookingTime(entity.getBookingTime())
                .product(entity.getProduct())
                .build())
            .toList();
    }
}
