package web.kplay.studentmanagement.service;

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
import web.kplay.studentmanagement.dto.NaverBookingDTO;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class NaverBookingCrawlerService {

    private static final String NAVER_BOOKING_URL = "https://partner.booking.naver.com/bizes/1047988/booking-list-view?bookingBusinessId=1047988&dateDropdownType=TODAY";
    private static final String NAVER_ID = "littlebearrc";
    private static final String NAVER_PW = "littlebear!";

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
            driver.get(NAVER_BOOKING_URL);
            Thread.sleep(2000);
            
            String currentUrl = driver.getCurrentUrl();
            log.info("현재 URL: {}", currentUrl);
            
            String pageTitle = driver.getTitle();
            log.info("페이지 제목: {}", pageTitle);
            
            // 예약 데이터 크롤링
            log.info("예약 데이터 크롤링 시작");
            List<NaverBookingDTO> bookings = new ArrayList<>();
            
            // 모든 예약 행 찾기 (XPath contains 사용)
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
            return bookings;
            
        } catch (Exception e) {
            log.error("네이버 예약 크롤링 실패", e);
            throw new RuntimeException("크롤링 실패: " + e.getMessage());
        } finally {
            // 개발 중 - 브라우저 창 유지
            // if (driver != null) {
            //     driver.quit();
            //     log.info("WebDriver 종료");
            // }
            log.info("크롤링 완료 - 브라우저 창 유지");
        }
    }
}

