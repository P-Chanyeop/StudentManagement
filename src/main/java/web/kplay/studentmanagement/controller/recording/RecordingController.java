package web.kplay.studentmanagement.controller.recording;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import web.kplay.studentmanagement.domain.recording.Recording;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.RecordingRepository;
import web.kplay.studentmanagement.repository.StudentRepository;
import web.kplay.studentmanagement.service.file.FileStorageService;
import web.kplay.studentmanagement.service.message.AutomatedMessageService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/recordings")
@RequiredArgsConstructor
public class RecordingController {

    private final RecordingRepository recordingRepository;
    private final StudentRepository studentRepository;
    private final FileStorageService fileStorageService;
    private final AutomatedMessageService automatedMessageService;

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<List<Recording>> getByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(recordingRepository.findByStudentIdOrderBySessionNumberDesc(studentId));
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<?> uploadRecording(
            @RequestParam("studentId") Long studentId,
            @RequestParam("sessionNumber") Integer sessionNumber,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "memo", required = false) String memo) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));

        String fileUrl = fileStorageService.storeFile(file, "recording");

        Recording recording = Recording.builder()
                .student(student)
                .sessionNumber(sessionNumber)
                .fileUrl(fileUrl)
                .fileName(file.getOriginalFilename())
                .memo(memo)
                .build();

        recordingRepository.save(recording);

        // 레코딩 업로드 완료 문자 발송
        automatedMessageService.sendRecordingUploadNotification(student, sessionNumber);

        log.info("레코딩 업로드 완료: 학생={}, 회차={}", student.getStudentName(), sessionNumber);

        return ResponseEntity.ok(Map.of(
                "message", "레코딩 업로드 완료",
                "id", recording.getId()
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        recordingRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "삭제 완료"));
    }
}
