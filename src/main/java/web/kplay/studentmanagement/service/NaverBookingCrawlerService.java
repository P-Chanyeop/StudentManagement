package web.kplay.studentmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.reservation.NaverBooking;
import web.kplay.studentmanagement.dto.NaverBookingDTO;
import web.kplay.studentmanagement.repository.NaverBookingRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverBookingCrawlerService {

    private final NaverBookingRepository naverBookingRepository;
    private final web.kplay.studentmanagement.repository.AttendanceRepository attendanceRepository;

    private static final String NAVER_BOOKING_BASE_URL = "https://partner.booking.naver.com/bizes/1047988/booking-list-view";
    private static final String NAVER_ID = "littlebearrc";
    private static final String NAVER_PW = "littlebear!";
    
    private String buildNaverBookingUrl() {
        java.time.Instant now = java.time.Instant.now();
        String timestamp = now.toString(); // ISO-8601 형식: 2026-01-20T06:11:08.066Z
        return String.format("%s?dateDropdownTpye=TODAY&startDateTime=%s&endDateTime=%s", 
            NAVER_BOOKING_BASE_URL, timestamp, timestamp);
    }

    @Transactional
    public List<NaverBookingDTO> crawlNaverBookings() {
        WebDriver driver = null;
        
        try {
            log.info("네이버 예약 크롤링 시작");
            
            ChromeOptions options = new ChromeOptions();
            // options.addArguments("--headless"); // 브라우저 창 보이도록 주석 처리
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
            options.setExperimentalOption("useAutomationExtension", false);
            options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            
            // Chrome 사용자 프로필 저장 (로그인 세션 유지)
            String userDataDir = System.getProperty("user.home") + "/.chrome-naver-profile";
            options.addArguments("--user-data-dir=" + userDataDir);
            options.addArguments("--profile-directory=Default");
            
            log.info("Chrome 프로필 경로: {}", userDataDir);
            
            driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            
            // 네이버 접속해서 로그인 확인
            log.info("네이버 접속");
            driver.get("https://www.naver.com/");
            Thread.sleep(500);
            
            // 로그인 여부 확인
            try {
                driver.findElement(By.linkText("로그인"));
                log.info("로그인 필요");
                
                // 로그인 페이지로 이동
                driver.get("https://nid.naver.com/nidlogin.login?mode=form&url=https://www.naver.com/");
                Thread.sleep(2000);
                
                // 로그인 상태 유지 체크박스 클릭
                WebElement keepLoginCheckbox = driver.findElement(By.id("keep"));
                keepLoginCheckbox.click();
                log.info("로그인 상태 유지 체크");
                
                Thread.sleep(300);
                
                // 아이디 입력 (Windows 클립보드 사용)
                WebElement idInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("id")));
                idInput.click();
                Thread.sleep(300);
                
                // PowerShell로 Windows 클립보드에 아이디 복사
                ProcessBuilder pb1 = new ProcessBuilder("powershell.exe", "-Command", 
                    "Set-Clipboard -Value '" + NAVER_ID + "'");
                pb1.start().waitFor();
                Thread.sleep(200);
                
                // Ctrl+V로 붙여넣기
                idInput.sendKeys(Keys.CONTROL + "v");
                log.info("아이디 입력 완료: {}", NAVER_ID);
                
                Thread.sleep(500);
                
                // 비밀번호 입력 (Windows 클립보드 사용)
                WebElement pwInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("pw")));
                pwInput.click();
                Thread.sleep(300);
                
                // PowerShell로 Windows 클립보드에 비밀번호 복사
                ProcessBuilder pb2 = new ProcessBuilder("powershell.exe", "-Command", 
                    "Set-Clipboard -Value '" + NAVER_PW + "'");
                pb2.start().waitFor();
                Thread.sleep(200);
                
                // Ctrl+V로 붙여넣기
                pwInput.sendKeys(Keys.CONTROL + "v");
                log.info("비밀번호 입력 완료");
                
                Thread.sleep(500);
                
                // 로그인 버튼 클릭
                WebElement loginBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("log.login")));
                loginBtn.click();
                log.info("로그인 버튼 클릭");
                
                Thread.sleep(1500);
                
                // 네이버 홈으로 이동
                log.info("네이버 홈으로 이동");
                driver.get("https://www.naver.com/");
                Thread.sleep(500);
            } catch (Exception e) {
                log.info("이미 로그인되어 있습니다!");
            }
            
            // 네이버 예약 관리 페이지로 이동
            log.info("네이버 예약 관리 페이지로 이동");
            String naverBookingUrl = buildNaverBookingUrl();
            log.info("접속 URL: {}", naverBookingUrl);
            driver.get(naverBookingUrl);
            Thread.sleep(3000);
            
            String currentUrl = driver.getCurrentUrl();
            log.info("현재 URL: {}", currentUrl);
            
            String pageTitle = driver.getTitle();
            log.info("페이지 제목: {}", pageTitle);
            
            // 스크롤을 끝까지 내려서 모든 데이터 로드
            log.info("페이지 스크롤 시작");
            int previousCount = 0;
            int currentCount = 0;
            int sameCountRetry = 0;
            
            // 예약 리스트 컨테이너 찾기
            WebElement listContainer = null;
            try {
                listContainer = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.className("BookingListView__list-contents__g037Y")
                ));
                log.info("예약 리스트 컨테이너 찾음");
            } catch (Exception e) {
                log.warn("예약 리스트 컨테이너를 찾을 수 없음");
            }
            
            while (sameCountRetry < 3) {
                // 현재 로드된 예약 개수 확인
                List<WebElement> currentRows = driver.findElements(
                    By.xpath("//a[contains(@class, 'BookingListView__contents-user')]")
                );
                currentCount = currentRows.size();
                log.info("현재 로드된 예약: {}건", currentCount);
                
                // 마지막 예약 항목으로 스크롤
                if (!currentRows.isEmpty()) {
                    WebElement lastRow = currentRows.get(currentRows.size() - 1);
                    ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView(true);", lastRow
                    );
                }
                
                Thread.sleep(2000);
                
                // 개수가 변하지 않으면 카운트 증가
                if (currentCount == previousCount) {
                    sameCountRetry++;
                    log.info("더 이상 로드되지 않음 ({}번째 시도)", sameCountRetry);
                } else {
                    sameCountRetry = 0;
                    previousCount = currentCount;
                }
            }
            
            log.info("스크롤 완료: 총 {}건 로드됨", currentCount);
            
            // 예약 데이터 크롤링
            log.info("예약 데이터 크롤링 시작");
            List<NaverBookingDTO> bookings = new ArrayList<>();
            
            // 모든 예약 행 찾기
            List<WebElement> bookingRows = driver.findElements(
                By.xpath("//a[contains(@class, 'BookingListView__contents-user')]")
            );
            log.info("총 {}건의 예약 발견", bookingRows.size());
            
            for (int i = 0; i < bookingRows.size(); i++) {
                try {
                    WebElement row = bookingRows.get(i);
                    
                    // 상태 추출 (label 텍스트)
                    String status = "";
                    try {
                        status = row.findElement(By.xpath(".//div[contains(@class, 'BookingListView__state')]//span[contains(@class, 'label')]")).getText();
                    } catch (Exception e) {
                        status = "확정"; // 기본값
                    }
                    
                    NaverBookingDTO booking = NaverBookingDTO.builder()
                        .status(status)
                        .name(row.findElement(By.xpath(".//span[contains(@class, 'BookingListView__name-ellipsis')]")).getText())
                        .phone(row.findElement(By.xpath(".//div[contains(@class, 'BookingListView__phone')]/span")).getText())
                        .bookingNumber(row.findElement(By.xpath(".//div[contains(@class, 'BookingListView__book-number')]")).getText())
                        .bookingTime(row.findElement(By.xpath(".//div[contains(@class, 'BookingListView__book-date')]")).getText())
                        .product(row.findElement(By.xpath(".//div[contains(@class, 'BookingListView__host')]")).getText())
                        .quantity(row.findElement(By.xpath(".//div[contains(@class, 'BookingListView__qty')]/span")).getText())
                        .option(row.findElement(By.xpath(".//div[contains(@class, 'BookingListView__option')]")).getText())
                        .comment(row.findElement(By.xpath(".//div[contains(@class, 'BookingListView__comment')]")).getText())
                        .deposit(row.findElement(By.xpath(".//div[contains(@class, 'BookingListView__payment-state')]")).getText())
                        .totalPrice(row.findElement(By.xpath(".//div[contains(@class, 'BookingListView__total-price')]")).getText())
                        .orderDate(row.findElement(By.xpath(".//div[contains(@class, 'BookingListView__order-date')]")).getText())
                        .confirmDate(row.findElement(By.xpath(".//div[contains(@class, 'BookingListView__order-success-date')]")).getText())
                        .cancelDate(row.findElement(By.xpath(".//div[contains(@class, 'BookingListView__order-cancel-date')]")).getText())
                        .build();
                    
                    bookings.add(booking);
                    log.info("예약 {}번 크롤링 완료: {} - {}", i + 1, booking.getName(), booking.getBookingNumber());
                    
                } catch (Exception e) {
                    log.error("예약 {}번 크롤링 실패: {}", i + 1, e.getMessage());
                }
            }
            
            log.info("크롤링 완료: 총 {}건", bookings.size());
            
            // DB에 저장
            saveBookingsToDatabase(bookings);
            
            return bookings;
            
        } catch (Exception e) {
            log.error("네이버 예약 크롤링 실패", e);
            throw new RuntimeException("크롤링 실패: " + e.getMessage());
        } finally {
            if (driver != null) {
                driver.quit();
                log.info("WebDriver 종료");
            }
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
                    .phone(dto.getPhone())
                    .bookingTime(dto.getBookingTime())
                    .product(dto.getProduct())
                    .quantity(dto.getQuantity())
                    .option(dto.getOption())
                    .comment(dto.getComment())
                    .deposit(dto.getDeposit())
                    .totalPrice(dto.getTotalPrice())
                    .orderDate(dto.getOrderDate())
                    .confirmDate(dto.getConfirmDate())
                    .cancelDate(dto.getCancelDate())
                    .syncedAt(now)
                    .build();
                
                naverBookingRepository.save(entity);
                
                // 신규 예약이고 확정 상태이면 출석 레코드 생성
                if (isNew && "예약확정".equals(entity.getStatus())) {
                    createAttendanceForNaverBooking(entity);
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
    
    public List<NaverBookingDTO> getTodayBookings() {
        return naverBookingRepository.findAll().stream()
            .map(entity -> NaverBookingDTO.builder()
                .status(entity.getStatus())
                .name(entity.getName())
                .phone(entity.getPhone())
                .bookingNumber(entity.getBookingNumber())
                .bookingTime(entity.getBookingTime())
                .product(entity.getProduct())
                .quantity(entity.getQuantity())
                .option(entity.getOption())
                .comment(entity.getComment())
                .deposit(entity.getDeposit())
                .totalPrice(entity.getTotalPrice())
                .orderDate(entity.getOrderDate())
                .confirmDate(entity.getConfirmDate())
                .cancelDate(entity.getCancelDate())
                .build())
            .collect(Collectors.toList());
    }
    
    public List<NaverBookingDTO> getBookingsByDate(String date) {
        // 2026-01-20 -> 26. 1. 20. 형식으로 변환
        String[] parts = date.split("-");
        String year = parts[0].substring(2); // 2026 -> 26
        String month = String.valueOf(Integer.parseInt(parts[1])); // 01 -> 1
        String day = String.valueOf(Integer.parseInt(parts[2])); // 20 -> 20
        String naverDateFormat = year + ". " + month + ". " + day + ".";
        
        return naverBookingRepository.findByBookingDate(naverDateFormat).stream()
            .map(entity -> NaverBookingDTO.builder()
                .status(entity.getStatus())
                .name(entity.getName())
                .phone(entity.getPhone())
                .bookingNumber(entity.getBookingNumber())
                .bookingTime(entity.getBookingTime())
                .product(entity.getProduct())
                .quantity(entity.getQuantity())
                .option(entity.getOption())
                .comment(entity.getComment())
                .deposit(entity.getDeposit())
                .totalPrice(entity.getTotalPrice())
                .orderDate(entity.getOrderDate())
                .confirmDate(entity.getConfirmDate())
                .cancelDate(entity.getCancelDate())
                .build())
            .collect(Collectors.toList());
    }
    
    public List<NaverBooking> getAllBookings() {
        return naverBookingRepository.findAll();
    }
    
    /**
     * 네이버 예약에 대한 출석 레코드 생성
     */
    private void createAttendanceForNaverBooking(NaverBooking naverBooking) {
        try {
            // bookingTime 파싱: "26. 1. 20.(화) 오전 10:00" -> LocalDate, LocalTime
            String bookingTimeStr = naverBooking.getBookingTime();
            log.info("네이버 예약 시간 파싱 시작: {}", bookingTimeStr);
            
            // 날짜 부분 추출: "26. 1. 20.(화)" -> "2026-01-20"
            String[] parts = bookingTimeStr.split("\\)");
            if (parts.length < 2) {
                log.warn("잘못된 예약 시간 형식: {}", bookingTimeStr);
                return;
            }
            
            String datePart = parts[0].replace("(", "").trim(); // "26. 1. 20.(화" -> "26. 1. 20."
            String[] dateTokens = datePart.split("\\.");
            if (dateTokens.length < 3) {
                log.warn("날짜 파싱 실패: {}", datePart);
                return;
            }
            
            int year = 2000 + Integer.parseInt(dateTokens[0].trim());
            int month = Integer.parseInt(dateTokens[1].trim());
            int day = Integer.parseInt(dateTokens[2].trim());
            java.time.LocalDate bookingDate = java.time.LocalDate.of(year, month, day);
            
            // 시간 부분 추출: "오전 10:00" or "오후 4:00"
            String timePart = parts[1].trim(); // " 오전 10:00" or " 오후 4:00"
            String[] timeTokens = timePart.split(" ");
            if (timeTokens.length < 2) {
                log.warn("시간 파싱 실패: {}", timePart);
                return;
            }
            
            String ampm = timeTokens[0].trim(); // "오전" or "오후"
            String time = timeTokens[1].trim(); // "10:00" or "4:00"
            String[] hourMin = time.split(":");
            int hour = Integer.parseInt(hourMin[0]);
            int minute = Integer.parseInt(hourMin[1]);
            
            // 오후면 12시간 추가 (12시는 제외)
            if ("오후".equals(ampm) && hour != 12) {
                hour += 12;
            } else if ("오전".equals(ampm) && hour == 12) {
                hour = 0;
            }
            
            java.time.LocalTime bookingTime = java.time.LocalTime.of(hour, minute);
            
            log.info("파싱 결과: 날짜={}, 시간={}", bookingDate, bookingTime);
            
            // 이미 출석 레코드가 있는지 확인
            boolean exists = attendanceRepository.findByDate(bookingDate).stream()
                .anyMatch(a -> a.getNaverBooking() != null && 
                              a.getNaverBooking().getId().equals(naverBooking.getId()));
            
            if (exists) {
                log.debug("이미 출석 레코드가 존재함: {}", naverBooking.getBookingNumber());
                return;
            }
            
            // 출석 레코드 생성
            web.kplay.studentmanagement.domain.attendance.Attendance attendance = 
                web.kplay.studentmanagement.domain.attendance.Attendance.builder()
                    .naverBooking(naverBooking)
                    .attendanceDate(bookingDate)
                    .attendanceTime(bookingTime)
                    .durationMinutes(120) // 기본 2시간
                    .expectedLeaveTime(bookingTime.plusHours(2))
                    .originalExpectedLeaveTime(bookingTime.plusHours(2))
                    .status(web.kplay.studentmanagement.domain.attendance.AttendanceStatus.NOTYET)
                    .classCompleted(false)
                    .build();
            
            attendanceRepository.save(attendance);
            log.info("네이버 예약 출석 레코드 생성: {}, 날짜={}, 시간={}", 
                    naverBooking.getName(), bookingDate, bookingTime);
        } catch (Exception e) {
            log.error("네이버 예약 출석 레코드 생성 실패: {}, 원본 시간: {}", 
                    e.getMessage(), naverBooking.getBookingTime(), e);
        }
    }
}

