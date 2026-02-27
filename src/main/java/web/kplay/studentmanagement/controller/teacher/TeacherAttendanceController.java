package web.kplay.studentmanagement.controller.teacher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    // 관리자: 선생님 계정 생성
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registerTeacher(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        String name = request.get("name");
        String phoneNumber = request.get("phoneNumber");

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "이미 존재하는 아이디입니다"));
        }

        User teacher = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .name(name)
                .nickname(name)
                .phoneNumber(phoneNumber)
                .role(UserRole.TEACHER)
                .termsAgreed(true)
                .privacyAgreed(true)
                .build();
        userRepository.save(teacher);
        log.info("선생님 계정 생성: {}", name);
        return ResponseEntity.ok(Map.of("message", name + " 선생님 계정이 생성되었습니다"));
    }

    // 관리자: 선생님 목록 조회
    @GetMapping("/teachers")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<?> getTeachers() {
        return ResponseEntity.ok(userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.TEACHER && u.getIsActive())
                .map(t -> Map.of("id", t.getId(), "name", t.getName(),
                        "phoneNumber", (Object)(t.getPhoneNumber() != null ? t.getPhoneNumber() : ""),
                        "username", (Object)(t.getUsername() != null ? t.getUsername() : "")))
                .toList());
    }

    // 관리자: 선생님 정보 수정
    @PutMapping("/teachers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateTeacher(@PathVariable Long id, @RequestBody Map<String, String> request) {
        User teacher = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("선생님을 찾을 수 없습니다"));
        if (teacher.getRole() != UserRole.TEACHER) {
            return ResponseEntity.badRequest().body(Map.of("message", "선생님 계정이 아닙니다"));
        }
        String name = request.get("name");
        String phoneNumber = request.get("phoneNumber");
        String password = request.get("password");
        if (name != null && !name.isEmpty()) teacher.setName(name);
        if (phoneNumber != null) teacher.setPhoneNumber(phoneNumber);
        if (password != null && !password.isEmpty()) teacher.changePassword(passwordEncoder.encode(password));
        userRepository.save(teacher);
        log.info("선생님 정보 수정: {}", teacher.getName());
        return ResponseEntity.ok(Map.of("message", teacher.getName() + " 선생님 정보가 수정되었습니다"));
    }

    // 관리자: 선생님 삭제 (비활성화)
    @DeleteMapping("/teachers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTeacher(@PathVariable Long id) {
        User teacher = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("선생님을 찾을 수 없습니다"));
        if (teacher.getRole() != UserRole.TEACHER) {
            return ResponseEntity.badRequest().body(Map.of("message", "선생님 계정이 아닙니다"));
        }
        teacher.deactivate();
        userRepository.save(teacher);
        log.info("선생님 삭제: {}", teacher.getName());
        return ResponseEntity.ok(Map.of("message", teacher.getName() + " 선생님이 삭제되었습니다"));
    }

    // 전화번호 뒷자리로 선생님 검색
    @PostMapping("/search")
    public ResponseEntity<?> searchByPhone(@RequestBody Map<String, String> request) {
        String phoneLast4 = request.get("phoneLast4");
        
        List<User> teachers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.TEACHER || u.getRole() == UserRole.ADMIN)
                .filter(u -> u.getPhoneNumber() != null && u.getPhoneNumber().endsWith(phoneLast4))
                .toList();

        LocalDate today = LocalDate.now();
        return ResponseEntity.ok(teachers.stream().map(t -> {
            var attendance = teacherAttendanceRepository.findByTeacherAndAttendanceDate(t, today).orElse(null);
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", t.getId());
            map.put("name", t.getName());
            map.put("phoneNumber", t.getPhoneNumber());
            map.put("checkInTime", attendance != null ? attendance.getCheckInTime() : null);
            map.put("checkOutTime", attendance != null ? attendance.getCheckOutTime() : null);
            return map;
        }).toList());
    }

    // 전화번호 뒷자리로 출근 체크
    @PostMapping("/check-in")
    public ResponseEntity<?> checkIn(@RequestBody Map<String, Object> request) {
        Long teacherId = Long.valueOf(request.get("teacherId").toString());
        
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("선생님을 찾을 수 없습니다"));

        LocalDate today = LocalDate.now();
        
        // 이미 출근했는지 확인
        if (teacherAttendanceRepository.findByTeacherAndAttendanceDate(teacher, today).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "이미 출근 체크되었습니다"));
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
                "message", teacher.getName() + " 선생님 출근 완료",
                "time", attendance.getCheckInTime()
        ));
    }

    // 전화번호 뒷자리로 퇴근 체크
    @PostMapping("/check-out")
    public ResponseEntity<?> checkOut(@RequestBody Map<String, Object> request) {
        Long teacherId = Long.valueOf(request.get("teacherId").toString());
        
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("선생님을 찾을 수 없습니다"));

        LocalDate today = LocalDate.now();
        
        TeacherAttendance attendance = teacherAttendanceRepository.findByTeacherAndAttendanceDate(teacher, today)
                .orElseThrow(() -> new RuntimeException("출근 기록이 없습니다. 먼저 출근 체크해주세요."));

        if (attendance.getCheckOutTime() != null) {
            return ResponseEntity.badRequest().body(Map.of("message", "이미 퇴근 체크되었습니다"));
        }

        attendance.checkOut(LocalDateTime.now());
        teacherAttendanceRepository.save(attendance);
        log.info("선생님 퇴근: {}", teacher.getName());

        return ResponseEntity.ok(Map.of(
                "message", teacher.getName() + " 선생님 퇴근 완료",
                "time", attendance.getCheckOutTime()
        ));
    }

    // 오늘 선생님 출퇴근 현황
    @GetMapping("/today")
    public ResponseEntity<List<TeacherAttendance>> getTodayAttendances() {
        return ResponseEntity.ok(teacherAttendanceRepository.findByAttendanceDate(LocalDate.now()));
    }

    // 특정 날짜 선생님 출퇴근 현황
    @GetMapping("/date/{date}")
    public ResponseEntity<?> getByDate(@PathVariable @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(teacherAttendanceRepository.findByAttendanceDate(date).stream().map(a -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", a.getId());
            map.put("teacherName", a.getTeacher().getName());
            map.put("teacherPhone", a.getTeacher().getPhoneNumber());
            map.put("attendanceDate", a.getAttendanceDate());
            map.put("checkInTime", a.getCheckInTime());
            map.put("checkOutTime", a.getCheckOutTime());
            map.put("memo", a.getMemo());
            return map;
        }).toList());
    }

    // 날짜 범위 선생님 출퇴근 현황
    @GetMapping("/range")
    public ResponseEntity<?> getByRange(
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(teacherAttendanceRepository.findByAttendanceDateBetween(startDate, endDate).stream().map(a -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", a.getId());
            map.put("teacherName", a.getTeacher().getName());
            map.put("teacherPhone", a.getTeacher().getPhoneNumber());
            map.put("attendanceDate", a.getAttendanceDate());
            map.put("checkInTime", a.getCheckInTime());
            map.put("checkOutTime", a.getCheckOutTime());
            map.put("memo", a.getMemo());
            return map;
        }).toList());
    }
}
