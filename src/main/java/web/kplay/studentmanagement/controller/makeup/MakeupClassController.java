package web.kplay.studentmanagement.controller.makeup;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.domain.makeup.MakeupStatus;
import web.kplay.studentmanagement.dto.makeup.MakeupClassCreateRequest;
import web.kplay.studentmanagement.dto.makeup.MakeupClassResponse;
import web.kplay.studentmanagement.dto.makeup.MakeupClassUpdateRequest;
import web.kplay.studentmanagement.service.makeup.MakeupClassService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/makeup-classes")
@RequiredArgsConstructor
public class MakeupClassController {

    private final MakeupClassService makeupClassService;

    /**
     * 보강 수업 등록
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<MakeupClassResponse> createMakeupClass(
            @Valid @RequestBody MakeupClassCreateRequest request) {
        MakeupClassResponse response = makeupClassService.createMakeupClass(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 보강 수업 단건 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<MakeupClassResponse> getMakeupClass(@PathVariable Long id) {
        MakeupClassResponse response = makeupClassService.getMakeupClass(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 보강 수업 조회
     */
    @GetMapping
    public ResponseEntity<List<MakeupClassResponse>> getAllMakeupClasses() {
        List<MakeupClassResponse> responses = makeupClassService.getAllMakeupClasses();
        return ResponseEntity.ok(responses);
    }

    /**
     * 학생별 보강 수업 조회
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<MakeupClassResponse>> getMakeupClassesByStudent(
            @PathVariable Long studentId) {
        List<MakeupClassResponse> responses = makeupClassService.getMakeupClassesByStudent(studentId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 수업별 보강 수업 조회
     */
    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<MakeupClassResponse>> getMakeupClassesByCourse(
            @PathVariable Long courseId) {
        List<MakeupClassResponse> responses = makeupClassService.getMakeupClassesByCourse(courseId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 기간별 보강 수업 조회
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<MakeupClassResponse>> getMakeupClassesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<MakeupClassResponse> responses = makeupClassService.getMakeupClassesByDateRange(startDate, endDate);
        return ResponseEntity.ok(responses);
    }

    /**
     * 특정 날짜 보강 수업 조회
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<List<MakeupClassResponse>> getMakeupClassesByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<MakeupClassResponse> responses = makeupClassService.getMakeupClassesByDate(date);
        return ResponseEntity.ok(responses);
    }

    /**
     * 상태별 보강 수업 조회
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<MakeupClassResponse>> getMakeupClassesByStatus(
            @PathVariable MakeupStatus status) {
        List<MakeupClassResponse> responses = makeupClassService.getMakeupClassesByStatus(status);
        return ResponseEntity.ok(responses);
    }

    /**
     * 학생의 예정된 보강 수업 조회
     */
    @GetMapping("/upcoming/student/{studentId}")
    public ResponseEntity<List<MakeupClassResponse>> getUpcomingMakeupsByStudent(
            @PathVariable Long studentId) {
        List<MakeupClassResponse> responses = makeupClassService.getUpcomingMakeupsByStudent(studentId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 전체 예정된 보강 수업 조회
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<MakeupClassResponse>> getAllUpcomingMakeups() {
        List<MakeupClassResponse> responses = makeupClassService.getAllUpcomingMakeups();
        return ResponseEntity.ok(responses);
    }

    /**
     * 보강 수업 수정
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<MakeupClassResponse> updateMakeupClass(
            @PathVariable Long id,
            @Valid @RequestBody MakeupClassUpdateRequest request) {
        MakeupClassResponse response = makeupClassService.updateMakeupClass(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 보강 수업 삭제
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Void> deleteMakeupClass(@PathVariable Long id) {
        makeupClassService.deleteMakeupClass(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 보강 수업 완료 처리
     */
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<MakeupClassResponse> completeMakeupClass(@PathVariable Long id) {
        MakeupClassResponse response = makeupClassService.completeMakeupClass(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 보강 수업 취소 처리
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<MakeupClassResponse> cancelMakeupClass(@PathVariable Long id) {
        MakeupClassResponse response = makeupClassService.cancelMakeupClass(id);
        return ResponseEntity.ok(response);
    }
}
