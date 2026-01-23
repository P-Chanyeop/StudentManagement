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

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverBookingApiCrawlerService {

    private final NaverBookingRepository naverBookingRepository;
    private final web.kplay.studentmanagement.repository.AttendanceRepository attendanceRepository;
    private final StudentCourseExcelService studentCourseExcelService;

    private static final String API_BASE_URL = "https://partner.booking.naver.com/api/businesses/1047988/bookings";
    // 실제 쿠키는 환경변수나 설정 파일에서 가져와야 함
    private static final String COOKIE_NID_AUT = "gC8Fo10+3VVLsJm0MNdFACCqeuNvgnz1aUdsk+gTUYmMe+HeBTEEn+IjRAJCACYd";
    private static final String COOKIE_NID_SES = "AAABkV5ysLPB07gWsahp2arIw4570dBQUioLi/OeC1EB4wW5Zn6QzwNBnx5sBNbeAkkcdE79wh5uzIPwfCx7hd3YLjX4efi4KCWbyQPkfURZX0zBjkc+cNrQ/w0YDFxCLjOgNC0Y4Nx52SGZWlZUsQhwe/z2wu15+tDo2zQnkq6Y/FTFrkiHxNhwE+wEONscqDpHI2IVCdLJ2cDCGrd9ruXRUK7bV9nLDMp0sOCldYqEUJyw4mB2sEfDAdmdYTnxu5OLFyJoKXWxwTLLCKGUA/Go4f9d0hycxUT3wP3cTePle6aVzPZOjs9VgBctvbFF1RfSQk3LuNcE4G+tiTwn/OSqO19HhbNgz8ycbD6mDWZXqznTPQSJ7zkttPM3u2mZbT52euzVoe9W5Tn23pxcF13YCQ1hz5gXqH2vkBA4SEw9TbflImgcFZvY0eg71wSJKXqvZOSwFZKGw+Min5J33UDmsc4ytYbCGiIxSHg3LFvy42YmVC/tmFiXpir0X2U8lJn1ygnfjTSdGsU48O4qpDPYg7LRG3a6Gn6bs6R9Im8FSBMn";

    @Transactional
    public List<NaverBookingDTO> crawlNaverBookings() {
        List<NaverBookingDTO> allBookings = new ArrayList<>();
        
        try {
            log.info("네이버 예약 API 크롤링 시작");
            
            // 오늘 날짜 (한국 시간)
            LocalDate today = LocalDate.now();
            String todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String timestamp = todayStr + "T00:00:00.000Z";
            
            log.info("조회 날짜: {}", todayStr);
            
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
            request.setHeader("Cookie", String.format("NID_AUT=%s; NID_SES=%s", COOKIE_NID_AUT, COOKIE_NID_SES));
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootArray = mapper.readTree(jsonResponse);
                
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
