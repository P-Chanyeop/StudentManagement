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
     * 녹음 파일 업로드 (상담용)
     * @param file 업로드할 녹음 파일 (MultipartFile)
     * @return 업로드된 파일 정보 (파일명, 경로, 크기)
     */
    @PostMapping("/upload/audio")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, String>> uploadAudioFile(
            @RequestParam("file") MultipartFile file) {

        log.info("Recording file upload request: {}", file.getOriginalFilename());

        String filePath = fileStorageService.storeFile(file, "audio");

        Map<String, String> response = new HashMap<>();
        response.put("fileName", file.getOriginalFilename());
        response.put("filePath", filePath);
        response.put("fileSize", String.valueOf(file.getSize()));
        response.put("message", "녹음 파일 업로드 성공");

        return ResponseEntity.ok(response);
    }

    /**
     * 다중 녹음 파일 업로드 (상담용)
     * @param files 업로드할 녹음 파일들 (MultipartFile[])
     * @return 업로드된 파일들 정보
     */
    @PostMapping("/upload/audio/multiple")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, Object>> uploadMultipleAudioFiles(
            @RequestParam("files") MultipartFile[] files) {

        log.info("Multiple recording files upload request: {} files", files.length);

        java.util.List<Map<String, String>> uploadedFiles = new java.util.ArrayList<>();
        
        for (MultipartFile file : files) {
            String filePath = fileStorageService.storeFile(file, "audio");
            
            Map<String, String> fileInfo = new HashMap<>();
            fileInfo.put("fileName", file.getOriginalFilename());
            fileInfo.put("filePath", filePath);
            fileInfo.put("fileSize", String.valueOf(file.getSize()));
            
            uploadedFiles.add(fileInfo);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("files", uploadedFiles);
        response.put("count", files.length);
        response.put("message", files.length + "개 녹음 파일 업로드 성공");

        return ResponseEntity.ok(response);
    }

    /**
     * 첨부 파일 업로드 (상담용 문서)
     * @param file 업로드할 문서 파일 (MultipartFile)
     * @return 업로드된 파일 정보 (파일명, 경로, 크기)
     */
    @PostMapping("/upload/document")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, String>> uploadDocumentFile(
            @RequestParam("file") MultipartFile file) {

        log.info("Attachment file upload request: {}", file.getOriginalFilename());

        String filePath = fileStorageService.storeFile(file, "document");

        Map<String, String> response = new HashMap<>();
        response.put("fileName", file.getOriginalFilename());
        response.put("filePath", filePath);
        response.put("fileSize", String.valueOf(file.getSize()));
        response.put("message", "첨부 파일 업로드 성공");

        return ResponseEntity.ok(response);
    }

    /**
     * 다중 첨부 파일 업로드 (상담용 문서)
     * @param files 업로드할 문서 파일들 (MultipartFile[])
     * @return 업로드된 파일들 정보
     */
    @PostMapping("/upload/document/multiple")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, Object>> uploadMultipleDocumentFiles(
            @RequestParam("files") MultipartFile[] files) {

        log.info("Multiple attachment files upload request: {} files", files.length);

        java.util.List<Map<String, String>> uploadedFiles = new java.util.ArrayList<>();
        
        for (MultipartFile file : files) {
            String filePath = fileStorageService.storeFile(file, "document");
            
            Map<String, String> fileInfo = new HashMap<>();
            fileInfo.put("fileName", file.getOriginalFilename());
            fileInfo.put("filePath", filePath);
            fileInfo.put("fileSize", String.valueOf(file.getSize()));
            
            uploadedFiles.add(fileInfo);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("files", uploadedFiles);
        response.put("count", files.length);
        response.put("message", files.length + "개 첨부 파일 업로드 성공");

        return ResponseEntity.ok(response);
    }

    /**
     * 파일 다운로드 (보안 검증 포함)
     * @param filePath 다운로드할 파일 경로
     * @return 파일 리소스 (바이너리 데이터)
     * Path Traversal 방지를 위한 보안 검증 추가
     */
    @GetMapping("/download/**")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<Resource> downloadFile(@RequestParam("filePath") String filePath) {

        try {
            // 입력 검증: null 또는 빈 문자열 체크
            if (filePath == null || filePath.trim().isEmpty()) {
                throw new IllegalArgumentException("파일 경로가 제공되지 않았습니다.");
            }

            // 보안 검증: ".." 경로 조작 문자 차단
            if (filePath.contains("..")) {
                log.warn("Path Traversal attempt detected: {}", filePath);
                throw new SecurityException("부적절한 파일 경로입니다.");
            }

            // 파일 경로 정규화 (앞의 "/" 제거)
            String normalizedPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;

            Path fileStorageLocation = fileStorageService.getFileStorageLocation();
            Path file = fileStorageLocation.resolve(normalizedPath).normalize();

            // 보안 검증: 요청된 파일이 허용된 디렉토리 내에 있는지 확인
            if (!file.startsWith(fileStorageLocation)) {
                log.warn("Attempt to access file outside directory: {}", filePath);
                throw new SecurityException("파일 경로가 유효하지 않습니다.");
            }

            Resource resource = new UrlResource(file.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("파일을 찾을 수 없거나 읽을 수 없습니다: " + filePath);
            }

            // 파일명 추출 및 안전한 파일명 생성 (XSS 방지)
            String fileName = file.getFileName().toString();
            String safeFileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");

            log.info("File download: {}", safeFileName);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeFileName + "\"")
                    .body(resource);

        } catch (SecurityException | IllegalArgumentException ex) {
            log.error("Security validation failed: {}", ex.getMessage());
            throw ex;
        } catch (MalformedURLException ex) {
            log.error("File path error: {}", filePath, ex);
            throw new RuntimeException("파일을 읽을 수 없습니다: " + filePath, ex);
        }
    }

    /**
     * 파일 삭제 (관리자/선생님만 가능)
     * @param filePath 삭제할 파일 경로
     * @return 삭제 결과 메시지
     */
    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, String>> deleteFile(@RequestParam("filePath") String filePath) {

        log.info("File deletion request: {}", filePath);

        fileStorageService.deleteFile(filePath);

        Map<String, String> response = new HashMap<>();
        response.put("message", "파일 삭제 성공");
        response.put("filePath", filePath);

        return ResponseEntity.ok(response);
    }
}
