package web.kplay.studentmanagement.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 자동 차감 스케줄러 (비활성화)
 * 
 * 차감 로직 변경:
 * - 체크인 시 → 1회 차감 (AttendanceService.checkIn)
 * - 결석 처리 시 → 1회 차감 (AttendanceService.updateStatus)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AutoDeductionScheduler {
    // 비활성화 - 체크인/결석 시 차감으로 변경
}
