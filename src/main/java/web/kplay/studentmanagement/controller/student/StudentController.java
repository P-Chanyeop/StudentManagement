package web.kplay.studentmanagement.controller.student;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.dto.student.StudentCreateRequest;
import web.kplay.studentmanagement.dto.student.StudentResponse;
import web.kplay.studentmanagement.service.student.StudentService;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody StudentCreateRequest request) {
        StudentResponse response = studentService.createStudent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<StudentResponse> getStudent(@PathVariable Long id) {
        StudentResponse response = studentService.getStudent(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<StudentResponse>> getAllStudents() {
        List<StudentResponse> responses = studentService.getAllStudents();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<StudentResponse>> getActiveStudents() {
        List<StudentResponse> responses = studentService.getActiveStudents();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/my-students")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<List<StudentResponse>> getMyStudents(Authentication authentication) {
        String username = authentication.getName();
        List<StudentResponse> responses = studentService.getMyStudents(username);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<StudentResponse>> searchStudents(@RequestParam String keyword) {
        List<StudentResponse> responses = studentService.searchStudents(keyword);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentCreateRequest request) {
        StudentResponse response = studentService.updateStudent(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateStudent(@PathVariable Long id) {
        studentService.deactivateStudent(id);
        return ResponseEntity.noContent().build();
    }
}
