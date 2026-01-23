package web.kplay.studentmanagement.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class StudentCourseExcelService {

    private Map<String, String> studentCourseMap = new HashMap<>();
    private Map<String, Integer> courseDurationMap = new HashMap<>();

    public StudentCourseExcelService() {
        // 반별 수업 시간 설정
        courseDurationMap.put("Able", 60);
        courseDurationMap.put("Basic", 90);
        courseDurationMap.put("Core", 120);
        courseDurationMap.put("Development", 150);

        // 엑셀 파일 로드
        loadExcelData();
    }

    private void loadExcelData() {
        String excelPath = "src/main/resources/static/images/리틀베어_리딩클럽_학생목록 (16).xlsx";
        File excelFile = new File(excelPath);

        if (!excelFile.exists()) {
            log.warn("엑셀 파일 없음: {}", excelPath);
            return;
        }

        try {
            FileInputStream fis = new FileInputStream(excelFile);
            Workbook workbook = WorkbookFactory.create(fis);
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
                    log.debug("학생 로드: [{}] -> {}", studentName, courseName);
                }
            }

            workbook.close();
            fis.close();

            log.info("엑셀에서 학생-반 정보 로드 완료: {}명", loadCount);

        } catch (Exception e) {
            log.error("엑셀 로드 실패", e);
        }
    }

    /**
     * 학생 이름으로 반 이름 조회
     */
    public String getCourseName(String studentName) {
        return studentCourseMap.get(studentName);
    }

    /**
     * 학생 이름으로 수업 시간(분) 조회
     */
    public Integer getDurationMinutes(String studentName) {
        String courseName = studentCourseMap.get(studentName);
        if (courseName == null) return null;
        return courseDurationMap.get(courseName);
    }

    /**
     * 반 이름으로 수업 시간(분) 조회
     */
    public Integer getDurationByCourseName(String courseName) {
        return courseDurationMap.get(courseName);
    }
}
