package web.kplay.studentmanagement.controller.teacher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.domain.teacher.TeacherAttendance;
import web.kplay.studentmanagement.domain.user.User;
import web.kplay.studentmanagement.domain.user.UserRole;
import web.kplay.studentmanagement.repository.TeacherAttendanceRepository;
import web.kplay.studentmanagement.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/teacher-attendance")
@RequiredArgsConstructor
public class TeacherAttendanceController {

    private final TeacherAttendanceRepository teacherAttendanceRepository;
    private final UserRepository userRepository;

    // 전화번호 뒷자리로 선생님 검색
    @PostMapping("/search")
    public ResponseEntity<?> searchByPhone(@RequestBody Map<String, String> request) {
        String phoneLast4 = request.get("phoneLast4");
        
        List<User> teachers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.TEACHER || u.getRole() == UserRole.ADMIN)
                .filter(u -> u.getPhoneNumber() != null && u.getPhoneNumber().endsWith(phoneLast4))
                .toList();

        return ResponseEntity.ok(teachers.stream().map(t -> Map.of(
                "id", t.getId(),
                "name", t.getName(),
                "phoneNumber", t.getPhoneNumber()
        )).toList());
    }

    // 전화번호 뒷자리로 출근 체크
    @PostMapping("/check-in")
    public ResponseEntity<?> checkIn(@RequestBody Map<String, Object> request) {
        Long teacherId = Long.valueOf(request.get("teacherId").toString());
        
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("선생님을 찾을 수 없습니다"));

        LocalDate today = LocalDate.now();
        
        // 이미 출근했으면 퇴근 처리
        var existing = teacherAttendanceRepository.findByTeacherAndAttendanceDate(teacher, today);
        if (existing.isPresent()) {
            TeacherAttendance attendance = existing.get();
            if (attendance.getCheckOutTime() == null) {
                attendance.checkOut(LocalDateTime.now());
                teacherAttendanceRepository.save(attendance);
                log.info("선생님 퇴근: {}", teacher.getName());
                return ResponseEntity.ok(Map.of(
                        "type", "checkout",
                        "message", teacher.getName() + " 선생님 퇴근 완료",
                        "time", attendance.getCheckOutTime()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "이미 퇴근 처리되었습니다"));
            }
        }

        // 출근 처리
        TeacherAttendance attendance = TeacherAttendance.builder()
                .teacher(teacher)
                .attendanceDate(today)
                .checkInTime(LocalDateTime.now())
                .build();

        teacherAttendanceRepository.save(attendance);
        log.info("선생님 출근: {}", teacher.getName());

        return ResponseEntity.ok(Map.of(
                "type", "checkin",
                "message", teacher.getName() + " 선생님 출근 완료",
                "time", attendance.getCheckInTime()
        ));
    }

    // 오늘 선생님 출퇴근 현황
    @GetMapping("/today")
    public ResponseEntity<List<TeacherAttendance>> getTodayAttendances() {
        return ResponseEntity.ok(teacherAttendanceRepository.findByAttendanceDate(LocalDate.now()));
    }
}
