package web.kplay.studentmanagement.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class StudentCourseExcelService {

    private static final String EXCEL_DIR = System.getProperty("user.home") + "/student-management/";
    private static final String EXCEL_FILENAME = "student-list.xlsx";
    
    private Map<String, String> studentCourseMap = new HashMap<>();
    private Map<String, Integer> courseDurationMap = new HashMap<>();

    public StudentCourseExcelService() {
        // 반별 수업 시간 설정
        courseDurationMap.put("Able", 60);
        courseDurationMap.put("Basic", 90);
        courseDurationMap.put("Core", 120);
        courseDurationMap.put("Development", 150);

        // 디렉토리 생성
        new File(EXCEL_DIR).mkdirs();
        
        // 엑셀 파일 로드
        loadExcelData();
    }

    /**
     * 엑셀 파일 업로드 및 데이터 갱신
     */
    public int uploadAndReload(MultipartFile file) throws Exception {
        // 파일 저장
        Path path = Paths.get(EXCEL_DIR + EXCEL_FILENAME);
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        log.info("엑셀 파일 저장 완료: {}", path);
        
        // 데이터 다시 로드
        studentCourseMap.clear();
        loadExcelData();
        
        return studentCourseMap.size();
    }

    private void loadExcelData() {
        // 1. 외부 경로 먼저 확인
        File excelFile = new File(EXCEL_DIR + EXCEL_FILENAME);

        // 2. 없으면 개발용 경로 확인
        if (!excelFile.exists()) {
            excelFile = new File("src/main/resources/static/images/리틀베어_리딩클럽_학생목록 (16).xlsx");
        }
        
        if (!excelFile.exists()) {
            log.warn("엑셀 파일 없음. 업로드가 필요합니다.");
            return;
        }

        try {
            FileInputStream fis = new FileInputStream(excelFile);
            loadFromInputStream(fis);
            fis.close();
        } catch (Exception e) {
            log.error("엑셀 로드 실패", e);
        }
    }

    private void loadFromInputStream(InputStream is) throws Exception {
        Workbook workbook = WorkbookFactory.create(is);
        Sheet sheet = workbook.getSheetAt(0);

        int loadCount = 0;

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Cell nameCell = row.getCell(0);
            Cell classCell = row.getCell(1);

            if (nameCell == null || classCell == null) continue;

            String studentName = nameCell.getStringCellValue().trim().replaceAll("\\s+", "");
            String classInfo = classCell.getStringCellValue().trim();

            // 선생님 제외
            if (classInfo.contains("선생님") || studentName.endsWith("T")) {
                continue;
            }

            // 클래스명 추출
            String courseName = null;
            if (classInfo.contains("Able")) courseName = "Able";
            else if (classInfo.contains("Basic")) courseName = "Basic";
            else if (classInfo.contains("Core")) courseName = "Core";
            else if (classInfo.contains("Development")) courseName = "Development";

            if (courseName != null) {
                studentCourseMap.put(studentName, courseName);
                loadCount++;
            }
        }

        workbook.close();
        log.info("엑셀에서 학생-반 정보 로드 완료: {}명", loadCount);
    }

    /**
     * 학생 이름으로 반 이름 조회 (부분 매칭)
     */
    public String getCourseName(String studentName) {
        // 정확히 일치하면 바로 반환
        if (studentCourseMap.containsKey(studentName)) {
            return studentCourseMap.get(studentName);
        }
        // 부분 매칭: 엑셀의 이름이 입력된 이름에 포함되어 있는지 확인
        return studentCourseMap.entrySet().stream()
                .filter(e -> studentName.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * 학생 이름으로 수업 시간(분) 조회
     */
    public Integer getDurationMinutes(String studentName) {
        String courseName = getCourseName(studentName);
        if (courseName == null) return null;
        return courseDurationMap.get(courseName);
    }

    /**
     * 반 이름으로 수업 시간(분) 조회
     */
    public Integer getDurationByCourseName(String courseName) {
        return courseDurationMap.get(courseName);
    }
    
    /**
     * 현재 로드된 학생 수 조회
     */
    public int getStudentCount() {
        return studentCourseMap.size();
    }
}
