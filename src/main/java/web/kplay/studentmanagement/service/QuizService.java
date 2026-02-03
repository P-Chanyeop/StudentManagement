package web.kplay.studentmanagement.service;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {

    private final StudentRepository studentRepository;

    @Transactional
    public int uploadRenaissanceIds(MultipartFile file) throws IOException {
        List<RenaissanceIdUploadDto> dataList = parseExcel(file);
        int updatedCount = 0;

        for (RenaissanceIdUploadDto dto : dataList) {
            // 학생 이름 + 학부모 전화번호로 매칭
            List<Student> students = studentRepository.findByStudentNameAndParentPhone(
                dto.getStudentName(), 
                dto.getParentPhone()
            );
            
            if (!students.isEmpty()) {
                Student student = students.get(0);
                student.updateRenaissanceUsername(dto.getRenaissanceUsername());
                updatedCount++;
            } else {
                log.warn("학생을 찾을 수 없음: {} (전화번호: {})", 
                    dto.getStudentName(), dto.getParentPhone());
            }
        }

        return updatedCount;
    }

    private List<RenaissanceIdUploadDto> parseExcel(MultipartFile file) throws IOException {
        List<RenaissanceIdUploadDto> result = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell nameCell = row.getCell(0);
                Cell usernameCell = row.getCell(1);
                Cell phoneCell = row.getCell(2);

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
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> "";
        };
    }
}
