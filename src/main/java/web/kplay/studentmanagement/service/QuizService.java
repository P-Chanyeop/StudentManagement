package web.kplay.studentmanagement.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.dto.quiz.RenaissanceIdUploadDto;
import web.kplay.studentmanagement.repository.StudentRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {

    private final StudentRepository studentRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public int uploadRenaissanceIds(MultipartFile file) throws IOException {
        List<RenaissanceIdUploadDto> dataList = parseExcel(file);
        int updatedCount = 0;
        int createdCount = 0;

        for (RenaissanceIdUploadDto dto : dataList) {
            // 전화번호 하이픈 제거해서 비교
            String phoneWithoutHyphen = dto.getParentPhone().replaceAll("-", "");
            
            // 학생 이름으로 먼저 조회
            List<Student> students = studentRepository.findByStudentName(dto.getStudentName());
            
            // 전화번호 매칭 (하이픈 제거 후 비교)
            Student matchedStudent = students.stream()
                .filter(s -> s.getParentPhone() != null && 
                            s.getParentPhone().replaceAll("-", "").equals(phoneWithoutHyphen))
                .findFirst()
                .orElse(null);
            
            if (matchedStudent != null) {
                // 기존 학생 업데이트
                matchedStudent.updateRenaissanceUsername(dto.getRenaissanceUsername());
                updatedCount++;
                log.info("업데이트: {} (전화번호: {})", dto.getStudentName(), dto.getParentPhone());
            } else {
                // 새 학생 추가
                Student newStudent = Student.builder()
                    .studentName(dto.getStudentName())
                    .parentPhone(dto.getParentPhone())
                    .renaissanceUsername(dto.getRenaissanceUsername())
                    .isActive(true)
                    .build();
                studentRepository.save(newStudent);
                createdCount++;
                log.info("새 학생 추가: {} (전화번호: {})", dto.getStudentName(), dto.getParentPhone());
            }
        }

        log.info("업데이트: {}명, 신규 추가: {}명", updatedCount, createdCount);
        return updatedCount + createdCount;
    }

    public List<Map<String, Object>> fetchStudentQuizData(Long studentId) throws Exception {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다."));

        if (student.getRenaissanceUsername() == null || student.getRenaissanceUsername().isEmpty()) {
            throw new IllegalStateException("학생의 르네상스 아이디가 등록되지 않았습니다.");
        }

        // Python 스크립트 절대 경로
        String projectRoot = System.getProperty("user.dir");
        String scriptPath = projectRoot + "/src/main/resources/static/images/get_student_quiz.py";
        String password = "0000";

        log.info("Python 스크립트 실행: {} {} {}", scriptPath, student.getRenaissanceUsername(), password);

        ProcessBuilder processBuilder = new ProcessBuilder(
            "python", scriptPath, student.getRenaissanceUsername(), password
        );
        processBuilder.redirectErrorStream(false);
        
        // UTF-8 인코딩 강제
        Map<String, String> env = processBuilder.environment();
        env.put("PYTHONIOENCODING", "utf-8");

        Process process = processBuilder.start();
        
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(120, java.util.concurrent.TimeUnit.SECONDS);
        
        if (!finished) {
            process.destroy();
            throw new RuntimeException("Python 스크립트 타임아웃 (120초 초과)");
        }

        int exitCode = process.exitValue();
        
        log.info("Python 스크립트 종료 코드: {}", exitCode);
        log.info("출력: {}", output.toString());
        log.info("에러: {}", errorOutput.toString());
        
        if (exitCode != 0) {
            throw new RuntimeException("Python 스크립트 실행 실패 (exit code: " + exitCode + ")");
        }

        // JSON 파싱 (마지막 [ 부터)
        String jsonOutput = output.toString();
        int jsonStart = jsonOutput.lastIndexOf('[');
        if (jsonStart != -1) {
            jsonOutput = jsonOutput.substring(jsonStart);
        }
        
        return objectMapper.readValue(jsonOutput, new TypeReference<List<Map<String, Object>>>() {});
    }

    private List<RenaissanceIdUploadDto> parseExcel(MultipartFile file) throws IOException {
        List<RenaissanceIdUploadDto> result = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // 3행이 헤더, 4행부터 데이터 시작
            for (int i = 3; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // 2열: 이름, 3열: 아이디, 4열: 전화번호
                Cell nameCell = row.getCell(1);
                Cell usernameCell = row.getCell(2);
                Cell phoneCell = row.getCell(3);

                if (nameCell != null && usernameCell != null && phoneCell != null) {
                    String name = getCellValueAsString(nameCell);
                    String username = getCellValueAsString(usernameCell);
                    String phone = getCellValueAsString(phoneCell);

                    if (!name.isEmpty() && !username.isEmpty() && !phone.isEmpty()) {
                        result.add(new RenaissanceIdUploadDto(name, username, phone));
                    }
                }
            }
        }

        return result;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                String value = String.valueOf((long) cell.getNumericCellValue());
                // 전화번호가 10자리면 앞에 0 추가
                if (value.length() == 10) {
                    value = "0" + value;
                }
                yield value;
            }
            default -> "";
        };
    }
}
