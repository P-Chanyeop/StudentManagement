package web.kplay.studentmanagement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import web.kplay.studentmanagement.domain.course.Course;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.repository.CourseRepository;
import web.kplay.studentmanagement.repository.StudentRepository;

import java.io.File;
import java.io.FileInputStream;

@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class StudentCourseInitializer implements CommandLineRunner {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    @Override
    public void run(String... args) {
        // 더 이상 사용하지 않음 - StudentCourseExcelService가 엑셀 직접 읽음
        log.info("StudentCourseInitializer 비활성화됨 (StudentCourseExcelService 사용)");
    }
}
