package web.kplay.studentmanagement.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Slf4j
@Service
public class AdditionalClassExcelService {

    private Map<String, Set<String>> classAssignments = new HashMap<>();
    private static final String EXCEL_DIR = System.getProperty("user.home") + "/student-management/";
    private static final String EXCEL_FILENAME = "추가수업 리스트.xlsx";

    @PostConstruct
    public void init() {
        new java.io.File(EXCEL_DIR).mkdirs();
        loadExcelData();
    }

    /**
     * 엑셀 파일 업로드 및 데이터 갱신
     */
    public int uploadAndReload(MultipartFile file) throws Exception {
        // 헤더 검증
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            
            if (headerRow == null) {
                throw new IllegalArgumentException("엑셀 파일에 헤더가 없습니다.");
            }
            
            String col0 = getCellValue(headerRow.getCell(0));
            String col1 = getCellValue(headerRow.getCell(1));
            String col2 = getCellValue(headerRow.getCell(2));
            String col3 = getCellValue(headerRow.getCell(3));
            
            boolean validHeader = 
                (col0 != null && col0.contains("Vocabulary")) ||
                (col1 != null && col1.contains("Sightword")) ||
                (col2 != null && col2.contains("Grammar")) ||
                (col3 != null && col3.contains("Phonics"));
            
            if (!validHeader) {
                throw new IllegalArgumentException("추가수업 리스트 엑셀 양식이 아닙니다. (Vocabulary, Sightword, Grammar, Phonics 헤더 필요)");
            }
        }
        
        // 파일 저장
        Path targetPath = Paths.get(EXCEL_DIR + EXCEL_FILENAME);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("추가수업 엑셀 파일 업로드: {}", targetPath);
        
        loadExcelData();
        
        int totalCount = classAssignments.values().stream()
                .mapToInt(Set::size)
                .sum();
        return totalCount;
    }

    public void loadExcelData() {
        classAssignments.clear();
        classAssignments.put("V", new HashSet<>());
        classAssignments.put("S", new HashSet<>());
        classAssignments.put("G", new HashSet<>());
        classAssignments.put("P", new HashSet<>());

        Path filePath = Paths.get(EXCEL_DIR + EXCEL_FILENAME);
        if (!Files.exists(filePath)) {
            log.warn("추가수업 엑셀 파일 없음: {}", filePath);
            return;
        }

        try (InputStream is = Files.newInputStream(filePath);
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
        } catch (Exception e) {
            log.error("추가수업 엑셀 로드 실패", e);
        }
    }

    private void addStudentToClass(String classType, Cell cell) {
        String name = getCellValue(cell);
        if (name != null && !name.isEmpty()) {
            // 한글만 추출 (영어, 숫자, 공백 제거)
            String koreanOnly = name.replaceAll("[^가-힣]", "").trim();
            if (!koreanOnly.isEmpty()) {
                classAssignments.get(classType).add(koreanOnly);
            }
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
        // 입력된 이름에서도 한글만 추출
        String koreanName = studentName.replaceAll("[^가-힣]", "").trim();
        if (koreanName.isEmpty()) return null;
        
        StringBuilder sb = new StringBuilder();
        if (containsStudent("V", koreanName)) sb.append("V");
        if (containsStudent("S", koreanName)) sb.append("S");
        if (containsStudent("G", koreanName)) sb.append("G");
        if (containsStudent("P", koreanName)) sb.append("P");
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

    /**
     * 엑셀에서 읽은 모든 학생 목록 (추가수업 관리 페이지 전용)
     */
    public List<Map<String, Object>> getExcelStudentList() {
        Set<String> allNames = new HashSet<>();
        for (Set<String> names : classAssignments.values()) {
            allNames.addAll(names);
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (String name : allNames) {
            Map<String, Object> student = new HashMap<>();
            student.put("studentName", name);
            student.put("assignedVocabulary", classAssignments.get("V").contains(name));
            student.put("assignedSightword", classAssignments.get("S").contains(name));
            student.put("assignedGrammar", classAssignments.get("G").contains(name));
            student.put("assignedPhonics", classAssignments.get("P").contains(name));
            student.put("assignedClassInitials", getAssignedClassInitials(name));
            result.add(student);
        }
        return result;
    }
}
