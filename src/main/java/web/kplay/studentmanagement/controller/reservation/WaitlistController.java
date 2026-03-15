package web.kplay.studentmanagement.controller.reservation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.domain.reservation.Waitlist;
import web.kplay.studentmanagement.repository.WaitlistRepository;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.repository.StudentRepository;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/waitlist")
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitlistRepository waitlistRepository;
    private final StudentRepository studentRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('PARENT','ADMIN')")
    public ResponseEntity<?> addWaitlist(@RequestBody Map<String, String> request) {
        Long studentId = Long.parseLong(request.get("studentId"));
        LocalDate date = LocalDate.parse(request.get("date"));
        LocalTime time = LocalTime.parse(request.get("time"));
        String type = request.get("consultationType");

        if (waitlistRepository.existsByStudentIdAndWaitDateAndWaitTimeAndConsultationTypeAndActiveTrue(studentId, date, time, type)) {
            return ResponseEntity.badRequest().body(Map.of("message", "이미 대기 신청되어 있습니다."));
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));

        Waitlist waitlist = Waitlist.builder()
                .student(student).waitDate(date).waitTime(time).consultationType(type).build();
        waitlistRepository.save(waitlist);

        return ResponseEntity.ok(Map.of("message", "대기 신청이 완료되었습니다."));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PARENT','ADMIN')")
    public ResponseEntity<?> cancelWaitlist(@PathVariable Long id) {
        Waitlist waitlist = waitlistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("대기를 찾을 수 없습니다"));
        waitlist.deactivate();
        waitlistRepository.save(waitlist);
        return ResponseEntity.ok(Map.of("message", "대기가 취소되었습니다."));
    }

    @GetMapping("/my/{studentId}")
    @PreAuthorize("hasAnyRole('PARENT','ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getMyWaitlist(@PathVariable Long studentId) {
        List<Waitlist> list = waitlistRepository.findByStudentIdAndActiveTrue(studentId);
        List<Map<String, Object>> result = list.stream().map(w -> Map.<String, Object>of(
                "id", w.getId(),
                "date", w.getWaitDate().toString(),
                "time", w.getWaitTime().toString().substring(0, 5),
                "consultationType", w.getConsultationType()
        )).toList();
        return ResponseEntity.ok(result);
    }
}
