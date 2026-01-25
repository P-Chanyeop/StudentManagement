package web.kplay.studentmanagement.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
public class AdditionalClassExcelService {

    private Map<String, Set<String>> classAssignments = new HashMap<>();

    @PostConstruct
    public void init() {
        loadExcelData();
    }

    public void loadExcelData() {
        classAssignments.clear();
        classAssignments.put("V", new HashSet<>());
        classAssignments.put("S", new HashSet<>());
        classAssignments.put("G", new HashSet<>());
        classAssignments.put("P", new HashSet<>());

        try {
            ClassPathResource resource = new ClassPathResource("static/images/추가수업 리스트.xlsx");
            try (InputStream is = resource.getInputStream();
                 Workbook workbook = new XSSFWorkbook(is)) {
                
                Sheet sheet = workbook.getSheetAt(0);
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    
                    // 헤더 행 스킵 (Vocabulary, Sightword 등이 있는 행)
                    Cell firstCell = row.getCell(0);
                    if (firstCell != null && "Vocabulary".equals(getCellValue(firstCell))) continue;

                    addStudentToClass("V", row.getCell(0));
                    addStudentToClass("S", row.getCell(1));
                    addStudentToClass("G", row.getCell(2));
                    addStudentToClass("P", row.getCell(3));
                }
                log.info("추가수업 엑셀 로드 완료 - V:{}, S:{}, G:{}, P:{}", 
                    classAssignments.get("V").size(),
                    classAssignments.get("S").size(),
                    classAssignments.get("G").size(),
                    classAssignments.get("P").size());
            }
        } catch (Exception e) {
            log.error("추가수업 엑셀 로드 실패", e);
        }
    }

    private void addStudentToClass(String classType, Cell cell) {
        String name = getCellValue(cell);
        if (name != null && !name.isEmpty()) {
            classAssignments.get(classType).add(name.trim());
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            default -> null;
        };
    }

    public String getAssignedClassInitials(String studentName) {
        if (studentName == null) return null;
        String name = studentName.trim();
        StringBuilder sb = new StringBuilder();
        if (containsStudent("V", name)) sb.append("V");
        if (containsStudent("S", name)) sb.append("S");
        if (containsStudent("G", name)) sb.append("G");
        if (containsStudent("P", name)) sb.append("P");
        return sb.length() > 0 ? sb.toString() : null;
    }

    // 부분 매칭: 엑셀의 이름이 입력된 이름에 포함되어 있는지 확인
    private boolean containsStudent(String classType, String inputName) {
        return classAssignments.get(classType).stream()
                .anyMatch(excelName -> inputName.contains(excelName));
    }

    public boolean hasAnyAssignedClass(String studentName) {
        return getAssignedClassInitials(studentName) != null;
    }
}
