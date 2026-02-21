package web.kplay.studentmanagement.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.attendance.Attendance;
import web.kplay.studentmanagement.domain.attendance.AttendanceStatus;
import web.kplay.studentmanagement.domain.course.Enrollment;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.repository.AttendanceRepository;
import web.kplay.studentmanagement.repository.EnrollmentRepository;
import web.kplay.studentmanagement.service.message.AutomatedMessageService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceScheduler {

    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AutomatedMessageService automatedMessageService;

    /**
     * 5분마다 실행: 수업 시작 후 15분 지난 미출석자에게 알림 발송 및 횟수 차감
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void checkLateStudents() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalTime fifteenMinutesAgo = now.minusMinutes(15);

        List<Attendance> attendances = attendanceRepository.findByDate(today);

        for (Attendance attendance : attendances) {
            // 체크인하지 않았고, 수업 시작 시간이 15분 이상 지났고, 아직 알림 안 보낸 경우
            if (attendance.getCheckInTime() == null 
                && attendance.getAttendanceTime() != null
                && attendance.getAttendanceTime().isBefore(fifteenMinutesAgo)
                && attendance.getStatus() == AttendanceStatus.NOTYET
                && !attendance.isLateNotificationSent()) {
                
                Student student = attendance.getStudent();
                if (student != null) {
                    // 미출석 알림 발송
                    automatedMessageService.sendNoShowNotification(student, attendance.getAttendanceTime());
                    
                    // 횟수 차감
                    List<Enrollment> enrollments = enrollmentRepository.findByStudentAndIsActiveTrue(student);
                    for (Enrollment enrollment : enrollments) {
                        if (enrollment.getRemainingCount() > 0) {
                            enrollment.useCount();
                            log.info("Auto deduct: student={}, remaining={}", 
                                student.getStudentName(), enrollment.getRemainingCount());
                            break;
                        }
                    }
                    
                    // 알림 발송 표시
                    attendance.markLateNotificationSent();
                    
                    log.info("No-show notification sent: student={}, time={}", 
                        student.getStudentName(), attendance.getAttendanceTime());
                } else if (attendance.getNaverBooking() != null) {
                    // 네이버 예약 학생 미출석 알림 발송
                    automatedMessageService.sendNaverNoShowNotification(attendance.getNaverBooking(), attendance.getAttendanceTime());
                    attendance.markLateNotificationSent();
                    log.info("No-show notification sent (Naver): student={}, time={}", 
                        attendance.getNaverBooking().getStudentName(), attendance.getAttendanceTime());
                }
            }
        }
    }

    /**
     * 5분마다 실행: 수업 시작 후 15분 지난 미출석자를 결석 처리 + 횟수 차감
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void checkAbsentStudents() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // 현재 시간 기준 15분 전까지만 결석 대상
        // 예: 지금 11:35이면 11:20 이전 수업만 결석 처리
        LocalTime cutoff = now.minusMinutes(15);

        List<Attendance> attendances = attendanceRepository.findByDate(today);

        int absentCount = 0;
        for (Attendance attendance : attendances) {
            if (attendance.getCheckInTime() == null 
                && attendance.getAttendanceTime() != null
                && attendance.getAttendanceTime().isBefore(cutoff)
                && attendance.getAttendanceTime().isBefore(now) // 미래 수업 제외
                && attendance.getStatus() != AttendanceStatus.ABSENT
                && attendance.getStatus() != AttendanceStatus.EXCUSED) {
                
                attendance.updateStatus(AttendanceStatus.ABSENT, null);
                attendanceRepository.save(attendance);
                absentCount++;
                
                // 결석 시 수강권 1회 차감
                Student student = attendance.getStudent();
                if (student != null) {
                    List<Enrollment> activeEnrollments = enrollmentRepository.findByStudentAndIsActiveTrue(student);
                    if (!activeEnrollments.isEmpty()) {
                        Enrollment enrollment = activeEnrollments.get(0);
                        enrollment.useCount();
                        log.info("결석 횟수 차감: student={}, remaining={}/{}", 
                                student.getStudentName(), enrollment.getRemainingCount(), enrollment.getTotalCount());
                    }
                }
                
                log.info("Auto absent: student={}, time={}", 
                    student != null ? student.getStudentName() : 
                    attendance.getNaverBooking() != null ? attendance.getNaverBooking().getStudentName() : "Unknown",
                    attendance.getAttendanceTime());
            }
        }

        if (absentCount > 0) {
            log.info("Absent check completed: {} students marked as absent", absentCount);
        }
    }
}
