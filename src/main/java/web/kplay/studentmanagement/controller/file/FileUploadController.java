package web.kplay.studentmanagement.controller.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import web.kplay.studentmanagement.service.file.FileStorageService;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;

    /**
     * 녹음 파일 업로드
     */
    @PostMapping("/upload/audio")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, String>> uploadAudioFile(
            @RequestParam("file") MultipartFile file) {

        log.info("녹음 파일 업로드 요청: {}", file.getOriginalFilename());

        String filePath = fileStorageService.storeFile(file, "audio");

        Map<String, String> response = new HashMap<>();
        response.put("fileName", file.getOriginalFilename());
        response.put("filePath", filePath);
        response.put("fileSize", String.valueOf(file.getSize()));
        response.put("message", "녹음 파일 업로드 성공");

        return ResponseEntity.ok(response);
    }

    /**
     * 첨부 파일 업로드
     */
    @PostMapping("/upload/document")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, String>> uploadDocumentFile(
            @RequestParam("file") MultipartFile file) {

        log.info("첨부 파일 업로드 요청: {}", file.getOriginalFilename());

        String filePath = fileStorageService.storeFile(file, "document");

        Map<String, String> response = new HashMap<>();
        response.put("fileName", file.getOriginalFilename());
        response.put("filePath", filePath);
        response.put("fileSize", String.valueOf(file.getSize()));
        response.put("message", "첨부 파일 업로드 성공");

        return ResponseEntity.ok(response);
    }

    /**
     * 파일 다운로드
     */
    @GetMapping("/download/**")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<Resource> downloadFile(@RequestParam("filePath") String filePath) {

        try {
            // 파일 경로 정규화 (앞의 "/" 제거)
            String normalizedPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;

            Path fileStorageLocation = fileStorageService.getFileStorageLocation();
            Path file = fileStorageLocation.resolve(normalizedPath).normalize();
            Resource resource = new UrlResource(file.toUri());

            if (!resource.exists()) {
                throw new RuntimeException("파일을 찾을 수 없습니다: " + filePath);
            }

            // 파일명 추출
            String fileName = file.getFileName().toString();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (MalformedURLException ex) {
            throw new RuntimeException("파일을 읽을 수 없습니다: " + filePath, ex);
        }
    }

    /**
     * 파일 삭제
     */
    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, String>> deleteFile(@RequestParam("filePath") String filePath) {

        log.info("파일 삭제 요청: {}", filePath);

        fileStorageService.deleteFile(filePath);

        Map<String, String> response = new HashMap<>();
        response.put("message", "파일 삭제 성공");
        response.put("filePath", filePath);

        return ResponseEntity.ok(response);
    }
}
