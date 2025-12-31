package web.kplay.studentmanagement.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.reservation.Reservation;
import web.kplay.studentmanagement.domain.reservation.ReservationStatus;
import web.kplay.studentmanagement.repository.ReservationRepository;
import web.kplay.studentmanagement.service.course.EnrollmentService;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 수업 시작 10분 후 자동 차감 처리 스케줄러
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AutoDeductionScheduler {

    private final ReservationRepository reservationRepository;
    private final EnrollmentService enrollmentService;

    /**
     * 매분마다 실행하여 수업 시작 + 10분 시점에 정확히 자동 차감 처리
     */
    @Scheduled(cron = "0 * * * * *") // 매분 0초에 실행
    @Transactional
    public void processAutoDeduction() {
        LocalDateTime now = LocalDateTime.now();
        
        // 오늘 날짜의 확정된 예약들 중 아직 차감되지 않은 것들 조회
        List<Reservation> confirmedReservations = reservationRepository
            .findByScheduleDateAndStatusAndNotDeducted(now.toLocalDate(), ReservationStatus.CONFIRMED);
        
        for (Reservation reservation : confirmedReservations) {
            try {
                // 수업 시작 시간 + 10분 계산
                LocalTime classStartTime = reservation.getSchedule().getStartTime();
                LocalTime deductionTime = classStartTime.plusMinutes(10);
                
                // 현재 시간이 차감 시간과 일치하는지 확인 (분 단위로)
                if (now.toLocalTime().getHour() == deductionTime.getHour() && 
                    now.toLocalTime().getMinute() == deductionTime.getMinute()) {
                    
                    // 수강권 횟수 차감
                    enrollmentService.deductCount(reservation.getEnrollment().getId(), 
                        "수업 시작 10분 후 자동 차감");
                    
                    // 예약 상태를 AUTO_DEDUCTED로 변경
                    reservation.updateStatus(ReservationStatus.AUTO_DEDUCTED);
                    
                    log.info("Auto deduction completed - Reservation ID: {}, Student: {}, Class time: {}, Deducted time: {}", 
                        reservation.getId(), reservation.getStudent().getStudentName(), 
                        classStartTime, deductionTime);
                }
                    
            } catch (Exception e) {
                log.error("Auto deduction failed - Reservation ID: {}, Error: {}", 
                    reservation.getId(), e.getMessage());
            }
        }
    }
}
