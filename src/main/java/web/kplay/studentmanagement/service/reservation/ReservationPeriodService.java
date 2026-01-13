package web.kplay.studentmanagement.service.reservation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.reservation.ReservationPeriod;
import web.kplay.studentmanagement.repository.ReservationPeriodRepository;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationPeriodService {

    private final ReservationPeriodRepository reservationPeriodRepository;

    /**
     * 격주 일요일 오전 9시에 새로운 예약 기간 생성
     */
    @Scheduled(cron = "0 0 9 * * SUN") // 매주 일요일 오전 9시
    @Transactional
    public void createReservationPeriodIfNeeded() {
        LocalDateTime now = LocalDateTime.now();
        
        // 격주 확인 (2024년 1월 첫째 주를 기준으로 격주 계산)
        LocalDateTime baseDate = LocalDateTime.of(2024, 1, 7, 9, 0); // 2024년 1월 첫째 일요일
        long weeksBetween = java.time.temporal.ChronoUnit.WEEKS.between(baseDate.toLocalDate(), now.toLocalDate());
        
        if (weeksBetween % 2 != 0) {
            log.info("격주가 아니므로 예약 기간을 생성하지 않습니다. 기준일로부터 {}주차", weeksBetween);
            return;
        }

        // 기존 활성 예약 기간 비활성화
        reservationPeriodRepository.findLatestActivePeriod()
                .ifPresent(period -> {
                    ReservationPeriod updatedPeriod = ReservationPeriod.builder()
                            .id(period.getId())
                            .openTime(period.getOpenTime())
                            .closeTime(period.getCloseTime())
                            .reservationStartDate(period.getReservationStartDate())
                            .reservationEndDate(period.getReservationEndDate())
                            .isActive(false)
                            .build();
                    reservationPeriodRepository.save(updatedPeriod);
                });

        // 새로운 예약 기간 생성
        LocalDateTime openTime = now; // 현재 시간 (일요일 오전 9시)
        LocalDateTime closeTime = openTime.plusDays(14); // 2주 후 일요일 오전 9시까지
        
        // 예약 가능한 수업 기간 (다음 날 월요일부터 2주간)
        LocalDateTime reservationStartDate = now.plusDays(1).withHour(0).withMinute(0).withSecond(0); // 다음 날 월요일 00:00
        LocalDateTime reservationEndDate = reservationStartDate.plusDays(13).withHour(23).withMinute(59).withSecond(59); // 2주 후 일요일 23:59

        ReservationPeriod newPeriod = ReservationPeriod.builder()
                .openTime(openTime)
                .closeTime(closeTime)
                .reservationStartDate(reservationStartDate)
                .reservationEndDate(reservationEndDate)
                .isActive(true)
                .build();

        reservationPeriodRepository.save(newPeriod);
        
        log.info("새로운 예약 기간이 생성되었습니다. 예약창 열림: {} ~ {}, 예약 가능한 수업 날짜: {} ~ {}", 
                openTime, closeTime, reservationStartDate.toLocalDate(), reservationEndDate.toLocalDate());
    }

    /**
     * 현재 예약 가능한지 확인
     */
    @Transactional(readOnly = true)
    public boolean isReservationOpen() {
        return reservationPeriodRepository.findActiveReservationPeriod(LocalDateTime.now())
                .map(ReservationPeriod::isReservationOpen)
                .orElse(false);
    }

    /**
     * 현재 활성 예약 기간 조회 (없으면 생성)
     */
    @Transactional
    public ReservationPeriod getCurrentReservationPeriod() {
        LocalDateTime now = LocalDateTime.now();
        log.info("getCurrentReservationPeriod 호출됨. 현재 시간: {}", now);
        
        // 기존 활성 예약 기간 확인
        Optional<ReservationPeriod> existing = reservationPeriodRepository.findActiveReservationPeriod(now);
        if (existing.isPresent()) {
            log.info("기존 예약 기간 발견: {}", existing.get());
            return existing.get();
        }
        
        log.info("기존 예약 기간 없음. 새로 생성 시도");
        
        // 없으면 현재 시점 기준으로 예약 기간 생성
        // 가장 최근 격주 일요일 오전 9시를 찾기
        LocalDateTime lastSunday = findLastBiweeklySunday(now);
        log.info("가장 최근 격주 일요일: {}", lastSunday);
        
        // 예약창 기간 (격주 일요일 오전 9시부터 2주간)
        LocalDateTime openTime = lastSunday;
        LocalDateTime closeTime = openTime.plusDays(14);
        
        // 예약 가능한 수업 기간 (그 다음 월요일부터 2주간 평일)
        LocalDateTime reservationStartDate = lastSunday.plusDays(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime reservationEndDate = reservationStartDate.plusDays(13).withHour(23).withMinute(59).withSecond(59);
        
        log.info("예약창 기간: {} ~ {}", openTime, closeTime);
        log.info("수업 예약 가능 기간: {} ~ {}", reservationStartDate, reservationEndDate);
        
        // 현재 시간이 예약창 기간 내에 있는지 확인
        if (now.isAfter(openTime) && now.isBefore(closeTime)) {
            ReservationPeriod newPeriod = ReservationPeriod.builder()
                    .openTime(openTime)
                    .closeTime(closeTime)
                    .reservationStartDate(reservationStartDate)
                    .reservationEndDate(reservationEndDate)
                    .isActive(true)
                    .build();
            
            reservationPeriodRepository.save(newPeriod);
            log.info("예약 기간 생성 완료: {}", newPeriod);
            return newPeriod;
        } else {
            log.info("현재 시간이 예약창 기간 밖임. 예약 기간 생성하지 않음");
        }
        
        return null;
    }
    
    /**
     * 가장 최근 격주 일요일 오전 9시 찾기
     */
    private LocalDateTime findLastBiweeklySunday(LocalDateTime now) {
        // 2024년 1월 7일을 기준점으로 설정 (첫 번째 격주 일요일)
        LocalDateTime baseDate = LocalDateTime.of(2024, 1, 7, 9, 0);
        
        // 현재 날짜에서 가장 가까운 이전 일요일 찾기
        LocalDateTime currentSunday = now.with(DayOfWeek.SUNDAY).withHour(9).withMinute(0).withSecond(0);
        if (currentSunday.isAfter(now)) {
            currentSunday = currentSunday.minusWeeks(1);
        }
        
        // 기준일로부터 몇 주 차이인지 계산
        long weeksBetween = java.time.temporal.ChronoUnit.WEEKS.between(baseDate.toLocalDate(), currentSunday.toLocalDate());
        
        // 격주가 아니면 이전 주로
        if (weeksBetween % 2 != 0) {
            currentSunday = currentSunday.minusWeeks(1);
        }
        
        return currentSunday;
    }
}
