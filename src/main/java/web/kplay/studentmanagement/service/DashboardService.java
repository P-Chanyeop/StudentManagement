package web.kplay.studentmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.user.User;
import web.kplay.studentmanagement.domain.user.UserRole;
import web.kplay.studentmanagement.dto.DashboardStatsResponse;
import web.kplay.studentmanagement.repository.AttendanceRepository;
import web.kplay.studentmanagement.repository.CourseScheduleRepository;
import web.kplay.studentmanagement.repository.EnrollmentRepository;
import web.kplay.studentmanagement.repository.StudentRepository;
import web.kplay.studentmanagement.repository.UserRepository;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DashboardService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final CourseScheduleRepository scheduleRepository;
    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;

    /**
     * 사용자별 대시보드 통계 조회
     * 
     * @param username 현재 로그인한 사용자명
     * @return DashboardStatsResponse 대시보드 통계 정보
     *         - ADMIN: 전체 통계
     *         - TEACHER: 담당 수업의 학생들만 통계
     *         - PARENT: 본인 자녀만 통계 (향후 구현)
     */
    public DashboardStatsResponse getDashboardStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate today = LocalDate.now();
        
        log.info("Dashboard stats requested by user: {}, role: {}", username, user.getRole());

        if (user.getRole() == UserRole.ADMIN) {
            log.info("Calling getAdminStats for ADMIN user");
            return getAdminStats(today);
        } else if (user.getRole() == UserRole.TEACHER) {
            log.info("Calling getTeacherStats for TEACHER user");
            return getTeacherStats(user.getId(), today);
        } else {
            log.info("User role {} not supported, returning empty stats", user.getRole());
            // PARENT 역할은 향후 구현
            return new DashboardStatsResponse(0, 0, 0, 0.0, 0);
        }
    }

    /**
     * 관리자용 전체 통계 조회
     * 
     * @param today 오늘 날짜
     * @return DashboardStatsResponse 전체 통계
     */
    private DashboardStatsResponse getAdminStats(LocalDate today) {
        // 전체 학생 수 (활성 학생)
        int totalStudents = (int) studentRepository.count();
        
        // 오늘 스케줄 수
        int todaySchedules = scheduleRepository.countByScheduleDate(today);
        
        // 오늘 총 예약된 학생 수 (출석 예정 학생 수)
        int totalExpectedStudents = attendanceRepository.countByScheduleDate(today);
        
        // 오늘 실제 출석 수 (체크인한 학생)
        int todayAttendance = attendanceRepository.countByScheduleDateAndCheckInTimeIsNotNull(today);
        
        // 출석률 계산 (예약된 학생 대비 실제 출석)
        double attendanceRate = totalExpectedStudents > 0 ? 
                (double) todayAttendance / totalExpectedStudents * 100 : 0.0;
        
        // 디버깅용 로그
        log.info("=== Admin Dashboard Stats Debug ===");
        log.info("Today: {}", today);
        log.info("Total Students (from Student table): {}", totalStudents);
        log.info("Today Schedules: {}", todaySchedules);
        log.info("Total Expected Students: {}", totalExpectedStudents);
        log.info("Today Attendance: {}", todayAttendance);
        log.info("Attendance Rate: {}%", attendanceRate);
        log.info("===================================");
        
        // 만료 임박 수강권 (7일 이내)
        LocalDate weekLater = today.plusDays(7);
        int expiringEnrollments = enrollmentRepository.countByEndDateBetweenAndIsActive(
                today, weekLater, true);

        return new DashboardStatsResponse(
                totalStudents, 
                todaySchedules, 
                todayAttendance, 
                Math.round(attendanceRate * 100.0) / 100.0, 
                expiringEnrollments
        );
    }

    /**
     * 선생님용 담당 수업 통계 조회
     * 
     * @param teacherId 선생님 ID
     * @param today 오늘 날짜
     * @return DashboardStatsResponse 담당 수업 통계
     */
    private DashboardStatsResponse getTeacherStats(Long teacherId, LocalDate today) {
        log.info("=== getTeacherStats method called ===");
        log.info("Teacher ID: {}, Today: {}", teacherId, today);
        
        // 담당 수업의 학생 수
        int totalStudents = enrollmentRepository.countActiveStudentsByTeacherId(teacherId);
        
        // 오늘 담당 스케줄 수 (schedule 제거로 인해 0으로 설정)
        int todaySchedules = 0;
        
        // 오늘 총 예약된 학생 수
        int totalExpectedStudents = attendanceRepository.countByScheduleDate(today);
        
        // 오늘 실제 출석 수
        int todayAttendance = attendanceRepository.countByScheduleDateAndCheckInTimeIsNotNull(today);
        
        // 출석률 계산 (예약된 학생 대비 실제 출석)
        double attendanceRate = totalExpectedStudents > 0 ? 
                (double) todayAttendance / totalExpectedStudents * 100 : 0.0;
        
        // 디버깅용 로그
        log.info("=== Teacher Dashboard Stats Debug ===");
        log.info("Teacher ID: {}", teacherId);
        log.info("Today: {}", today);
        log.info("Today Schedules: {}", todaySchedules);
        log.info("Total Expected Students: {}", totalExpectedStudents);
        log.info("Today Attendance: {}", todayAttendance);
        log.info("Attendance Rate: {}%", attendanceRate);
        log.info("====================================");
        
        // 담당 수업의 만료 임박 수강권
        LocalDate weekLater = today.plusDays(7);
        int expiringEnrollments = enrollmentRepository.countByEndDateBetweenAndIsActiveAndCourseTeacherId(
                today, weekLater, true, teacherId);

        return new DashboardStatsResponse(
                totalStudents, 
                todaySchedules, 
                todayAttendance, 
                Math.round(attendanceRate * 100.0) / 100.0, 
                expiringEnrollments
        );
    }
}
