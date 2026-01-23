package web.kplay.studentmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import web.kplay.studentmanagement.domain.course.Course;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.repository.CourseRepository;
import web.kplay.studentmanagement.repository.StudentRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentCourseImportService {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public Map<String, Object> importFromExcel(MultipartFile file) throws IOException {
        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        int successCount = 0;
        int skipCount = 0;
        int errorCount = 0;

        // 2번째 행부터 읽기 (1번째 행은 헤더)
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Cell nameCell = row.getCell(0);
            Cell classCell = row.getCell(1);

            if (nameCell == null || classCell == null) continue;

            String studentName = nameCell.getStringCellValue().trim();
            String classInfo = classCell.getStringCellValue().trim();

            // 선생님 제외
            if (classInfo.contains("선생님") || studentName.endsWith("T")) {
                skipCount++;
                continue;
            }

            // 클래스명 추출
            String courseName = null;
            if (classInfo.contains("Able")) courseName = "Able";
            else if (classInfo.contains("Basic")) courseName = "Basic";
            else if (classInfo.contains("Core")) courseName = "Core";
            else if (classInfo.contains("Development")) courseName = "Development";

            if (courseName == null) {
                log.warn("알 수 없는 클래스: {} - {}", studentName, classInfo);
                errorCount++;
                continue;
            }

            // Course 조회
            Course course = courseRepository.findByCourseName(courseName)
                    .orElse(null);

            if (course == null) {
                log.warn("존재하지 않는 반: {}", courseName);
                errorCount++;
                continue;
            }

            // Student 조회 (이름으로)
            Student student = studentRepository.findByStudentName(studentName)
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (student == null) {
                log.warn("존재하지 않는 학생: {}", studentName);
                errorCount++;
                continue;
            }

            // 기본 반 설정
            student.setDefaultCourse(course);
            studentRepository.save(student);
            successCount++;
            log.info("학생 반 설정: {} -> {}", studentName, courseName);
        }

        workbook.close();

        Map<String, Object> result = new HashMap<>();
        result.put("success", successCount);
        result.put("skip", skipCount);
        result.put("error", errorCount);
        result.put("total", successCount + skipCount + errorCount);

        log.info("엑셀 임포트 완료: 성공={}, 건너뜀={}, 실패={}", successCount, skipCount, errorCount);
        return result;
    }
}
