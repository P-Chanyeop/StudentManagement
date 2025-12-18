package web.kplay.studentmanagement.service.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private final Path fileStorageLocation;
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final String[] ALLOWED_AUDIO_EXTENSIONS = {".mp3", ".m4a", ".wav", ".aac", ".ogg"};
    private static final String[] ALLOWED_FILE_EXTENSIONS = {".pdf", ".jpg", ".jpeg", ".png", ".doc", ".docx", ".txt"};

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("파일 저장 디렉토리 생성: {}", this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("파일 저장 디렉토리를 생성할 수 없습니다.", ex);
        }
    }

    /**
     * 파일 업로드
     */
    public String storeFile(MultipartFile file, String fileType) {
        // 파일명 정규화
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // 파일명에 부적절한 문자가 있는지 체크
            if (originalFileName.contains("..")) {
                throw new RuntimeException("파일명에 부적절한 경로가 포함되어 있습니다: " + originalFileName);
            }

            // 파일 크기 체크
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new RuntimeException("파일 크기가 최대 허용 크기(50MB)를 초과했습니다.");
            }

            // 파일 확장자 체크
            String extension = getFileExtension(originalFileName);
            if (fileType.equals("audio")) {
                validateAudioFile(extension);
            } else {
                validateDocumentFile(extension);
            }

            // 고유 파일명 생성 (날짜 + UUID + 원본 확장자)
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String uniqueFileName = timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

            // 파일 타입별 하위 디렉토리 생성
            Path typeDirectory = this.fileStorageLocation.resolve(fileType);
            Files.createDirectories(typeDirectory);

            // 파일 저장
            Path targetLocation = typeDirectory.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("파일 업로드 완료: {} -> {}", originalFileName, uniqueFileName);

            // 저장된 파일의 상대 경로 반환
            return "/" + fileType + "/" + uniqueFileName;

        } catch (IOException ex) {
            throw new RuntimeException("파일 저장에 실패했습니다: " + originalFileName, ex);
        }
    }

    /**
     * 파일 삭제
     */
    public void deleteFile(String filePath) {
        try {
            if (filePath == null || filePath.isEmpty()) {
                return;
            }

            // 파일 경로 정규화 (앞의 "/" 제거)
            String normalizedPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
            Path fileToDelete = this.fileStorageLocation.resolve(normalizedPath).normalize();

            // 보안: 저장 디렉토리 밖의 파일 삭제 시도 방지
            if (!fileToDelete.startsWith(this.fileStorageLocation)) {
                throw new RuntimeException("파일 경로가 유효하지 않습니다.");
            }

            Files.deleteIfExists(fileToDelete);
            log.info("파일 삭제 완료: {}", filePath);

        } catch (IOException ex) {
            log.error("파일 삭제 실패: {}", filePath, ex);
            throw new RuntimeException("파일 삭제에 실패했습니다: " + filePath, ex);
        }
    }

    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            throw new RuntimeException("파일 확장자가 없습니다: " + fileName);
        }
        return fileName.substring(lastDotIndex).toLowerCase();
    }

    /**
     * 오디오 파일 확장자 검증
     */
    private void validateAudioFile(String extension) {
        for (String allowed : ALLOWED_AUDIO_EXTENSIONS) {
            if (allowed.equals(extension)) {
                return;
            }
        }
        throw new RuntimeException("허용되지 않은 오디오 파일 형식입니다. 허용 형식: mp3, m4a, wav, aac, ogg");
    }

    /**
     * 문서 파일 확장자 검증
     */
    private void validateDocumentFile(String extension) {
        for (String allowed : ALLOWED_FILE_EXTENSIONS) {
            if (allowed.equals(extension)) {
                return;
            }
        }
        throw new RuntimeException("허용되지 않은 파일 형식입니다. 허용 형식: pdf, jpg, jpeg, png, doc, docx, txt");
    }

    /**
     * 파일 저장 위치 반환
     */
    public Path getFileStorageLocation() {
        return fileStorageLocation;
    }
}
