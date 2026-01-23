package web.kplay.studentmanagement.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.attendance.Attendance;
import web.kplay.studentmanagement.domain.attendance.AttendanceStatus;
import web.kplay.studentmanagement.repository.AttendanceRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceScheduler {

    private final AttendanceRepository attendanceRepository;

    /**
     * 10분마다 실행: 수업 시작 후 30분 지난 미출석자를 결석 처리
     */
    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void checkAbsentStudents() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalTime thirtyMinutesAgo = now.minusMinutes(30);

        // 오늘 날짜의 모든 출석 기록 조회
        List<Attendance> attendances = attendanceRepository.findByDate(today);

        int absentCount = 0;
        for (Attendance attendance : attendances) {
            // 체크인하지 않았고, 수업 시작 시간이 30분 이상 지났으면 결석 처리
            if (attendance.getCheckInTime() == null 
                && attendance.getAttendanceTime() != null
                && attendance.getAttendanceTime().isBefore(thirtyMinutesAgo)
                && attendance.getStatus() != AttendanceStatus.ABSENT
                && attendance.getStatus() != AttendanceStatus.EXCUSED) {
                
                attendance.updateStatus(AttendanceStatus.ABSENT, "자동 결석 처리 (30분 미출석)");
                attendanceRepository.save(attendance);
                absentCount++;
                
                log.info("Auto absent: student={}, time={}", 
                    attendance.getStudent() != null ? attendance.getStudent().getStudentName() : 
                    attendance.getNaverBooking() != null ? attendance.getNaverBooking().getStudentName() : "Unknown",
                    attendance.getAttendanceTime());
            }
        }

        if (absentCount > 0) {
            log.info("Absent check completed: {} students marked as absent", absentCount);
        }
    }
}
