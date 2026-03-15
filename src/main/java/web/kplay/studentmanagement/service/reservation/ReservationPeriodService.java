package web.kplay.studentmanagement.service.reservation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.reservation.ReservationPeriod;
import web.kplay.studentmanagement.repository.ReservationPeriodRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationPeriodService {

    private final ReservationPeriodRepository reservationPeriodRepository;

    private void deactivateAllActivePeriods() {
        reservationPeriodRepository.findAllActivePeriods().forEach(p -> {
            ReservationPeriod closed = ReservationPeriod.builder()
                    .id(p.getId())
                    .openTime(p.getOpenTime())
                    .closeTime(p.getCloseTime())
                    .reservationStartDate(p.getReservationStartDate())
                    .reservationEndDate(p.getReservationEndDate())
                    .isActive(false)
                    .build();
            reservationPeriodRepository.save(closed);
        });
    }

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
        deactivateAllActivePeriods();

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
     * 현재 예약 가능한지 확인 (없으면 자동 생성 시도)
     */
    @Transactional
    public boolean isReservationOpen() {
        ReservationPeriod period = getCurrentReservationPeriod();
        return period != null && period.isReservationOpen();
    }

    /**
     * 현재 활성 예약 기간 조회 (없으면 격주 기준으로 자동 생성)
     */
    @Transactional
    public ReservationPeriod getCurrentReservationPeriod() {
        LocalDateTime now = LocalDateTime.now();
        
        // active 기간 중 유효한 것 찾기
        List<ReservationPeriod> activePeriods = reservationPeriodRepository.findAllActivePeriods();
        
        // 만료된 기간 정리
        for (ReservationPeriod p : activePeriods) {
            if (now.isAfter(p.getReservationEndDate())) {
                ReservationPeriod closed = ReservationPeriod.builder()
                        .id(p.getId()).openTime(p.getOpenTime()).closeTime(p.getCloseTime())
                        .reservationStartDate(p.getReservationStartDate()).reservationEndDate(p.getReservationEndDate())
                        .isActive(false).build();
                reservationPeriodRepository.save(closed);
                log.info("만료된 예약 기간 비활성화: id={}", p.getId());
            } else {
                return p;
            }
        }
        
        // 격주 일요일 기준으로 새 기간 자동 생성
        LocalDateTime lastSunday = findLastBiweeklySunday(now);
        LocalDateTime openTime = lastSunday;
        LocalDateTime closeTime = openTime.plusDays(14);
        
        // 현재 시간이 예약창 기간 내인 경우만 생성
        if (!now.isBefore(openTime) && now.isBefore(closeTime)) {
            LocalDateTime reservationStartDate = lastSunday.plusDays(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime reservationEndDate = reservationStartDate.plusDays(13).withHour(23).withMinute(59).withSecond(59);
            
            ReservationPeriod newPeriod = ReservationPeriod.builder()
                    .openTime(openTime)
                    .closeTime(closeTime)
                    .reservationStartDate(reservationStartDate)
                    .reservationEndDate(reservationEndDate)
                    .isActive(true)
                    .build();
            
            reservationPeriodRepository.save(newPeriod);
            log.info("예약 기간 자동 생성: 예약창 {} ~ {}, 수업 {} ~ {}", 
                    openTime, closeTime, reservationStartDate.toLocalDate(), reservationEndDate.toLocalDate());
            return newPeriod;
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

    /**
     * 관리자 수동 예약 기간 열기 (특정 날짜 범위)
     */
    @Transactional
    public ReservationPeriod openManualPeriod(LocalDate startDate, LocalDate endDate) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime openStart = startDate.atStartOfDay();
        LocalDateTime openEnd = endDate.atTime(23, 59, 59);
        
        // 겹치거나 인접한 기존 기간과 병합
        List<ReservationPeriod> activePeriods = reservationPeriodRepository.findAllActivePeriods();
        for (ReservationPeriod p : activePeriods) {
            LocalDateTime pStart = p.getReservationStartDate();
            LocalDateTime pEnd = p.getReservationEndDate();
            
            // 겹치거나 인접하면 병합
            if (!openEnd.plusSeconds(1).isBefore(pStart) && !openStart.minusSeconds(1).isAfter(pEnd)) {
                if (pStart.isBefore(openStart)) openStart = pStart;
                if (pEnd.isAfter(openEnd)) openEnd = pEnd;
                // 기존 기간 비활성화
                ReservationPeriod closed = ReservationPeriod.builder()
                        .id(p.getId()).openTime(p.getOpenTime()).closeTime(p.getCloseTime())
                        .reservationStartDate(pStart).reservationEndDate(pEnd)
                        .isActive(false).build();
                reservationPeriodRepository.save(closed);
            }
        }

        ReservationPeriod period = ReservationPeriod.builder()
                .openTime(now)
                .closeTime(openEnd.toLocalDate().plusDays(1).atStartOfDay())
                .reservationStartDate(openStart)
                .reservationEndDate(openEnd)
                .isActive(true)
                .build();

        reservationPeriodRepository.save(period);
        log.info("수동 예약 기간 열림: {} ~ {}", openStart.toLocalDate(), openEnd.toLocalDate());
        return period;
    }

    @Transactional
    public void closePeriod(LocalDate startDate, LocalDate endDate) {
        LocalDateTime closeStart = startDate.atStartOfDay();
        LocalDateTime closeEnd = endDate.atTime(23, 59, 59);
        
        List<ReservationPeriod> activePeriods = reservationPeriodRepository.findAllActivePeriods();
        for (ReservationPeriod p : activePeriods) {
            LocalDateTime pStart = p.getReservationStartDate();
            LocalDateTime pEnd = p.getReservationEndDate();
            
            // 겹치지 않으면 스킵
            if (closeEnd.isBefore(pStart) || closeStart.isAfter(pEnd)) continue;
            
            // 기존 기간 비활성화
            ReservationPeriod closed = ReservationPeriod.builder()
                    .id(p.getId()).openTime(p.getOpenTime()).closeTime(p.getCloseTime())
                    .reservationStartDate(pStart).reservationEndDate(pEnd)
                    .isActive(false).build();
            reservationPeriodRepository.save(closed);
            
            // 앞쪽 남은 기간 (기존 시작 ~ 닫기 시작 전날)
            if (pStart.isBefore(closeStart)) {
                ReservationPeriod before = ReservationPeriod.builder()
                        .openTime(p.getOpenTime()).closeTime(p.getCloseTime())
                        .reservationStartDate(pStart).reservationEndDate(closeStart.minusSeconds(1))
                        .isActive(true).build();
                reservationPeriodRepository.save(before);
            }
            
            // 뒤쪽 남은 기간 (닫기 종료 다음날 ~ 기존 종료)
            if (pEnd.isAfter(closeEnd)) {
                ReservationPeriod after = ReservationPeriod.builder()
                        .openTime(p.getOpenTime()).closeTime(p.getCloseTime())
                        .reservationStartDate(closeEnd.plusSeconds(1)).reservationEndDate(pEnd)
                        .isActive(true).build();
                reservationPeriodRepository.save(after);
            }
        }
        log.info("예약 기간 닫힘: {} ~ {}", startDate, endDate);
    }
}
