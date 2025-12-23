package web.kplay.studentmanagement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import web.kplay.studentmanagement.domain.course.Course;
import web.kplay.studentmanagement.domain.course.CourseSchedule;
import web.kplay.studentmanagement.domain.course.Enrollment;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.domain.user.User;
import web.kplay.studentmanagement.domain.user.UserRole;
import web.kplay.studentmanagement.repository.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 개발 환경용 초기 데이터 시더
 * - 관리자 계정
 * - 선생님 계정
 * - 테스트 학생 데이터
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final CourseScheduleRepository scheduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final web.kplay.studentmanagement.service.holiday.HolidayService holidayService;

    @Bean
    @Profile("dev") // dev 프로파일에서만 실행
    public CommandLineRunner loadInitialData() {
        return args -> {
            log.info("=== 초기 데이터 로딩 시작 ===");

            // 관리자 계정 생성
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .name("관리자")
                        .email("admin@kplay.web")
                        .phoneNumber("010-1234-5678")
                        .role(UserRole.ADMIN)
                        .isActive(true)
                        .build();
                userRepository.save(admin);
                log.info("✓ 관리자 계정 생성 완료 (username: admin)");
            }

            // 선생님 계정 생성
            if (userRepository.findByUsername("teacher1").isEmpty()) {
                User teacher1 = User.builder()
                        .username("teacher1")
                        .password(passwordEncoder.encode("teacher123"))
                        .name("김영어")
                        .email("teacher1@kplay.web")
                        .phoneNumber("010-2345-6789")
                        .role(UserRole.TEACHER)
                        .isActive(true)
                        .build();
                userRepository.save(teacher1);
                log.info("✓ 선생님 계정 생성 완료 (username: teacher1)");
            }

            if (userRepository.findByUsername("teacher2").isEmpty()) {
                User teacher2 = User.builder()
                        .username("teacher2")
                        .password(passwordEncoder.encode("teacher123"))
                        .name("이수학")
                        .email("teacher2@kplay.web")
                        .phoneNumber("010-3456-7890")
                        .role(UserRole.TEACHER)
                        .isActive(true)
                        .build();
                userRepository.save(teacher2);
                log.info("✓ 선생님 계정 생성 완료 (username: teacher2)");
            }

            // 학부모 계정 생성
            if (userRepository.findByUsername("parent1").isEmpty()) {
                User parent1 = User.builder()
                        .username("parent1")
                        .password(passwordEncoder.encode("parent123"))
                        .name("박학부모")
                        .email("parent1@kplay.web")
                        .phoneNumber("010-4567-8901")
                        .role(UserRole.PARENT)
                        .isActive(true)
                        .build();
                userRepository.save(parent1);
                log.info("✓ 학부모 계정 생성 완료 (username: parent1)");
            }

            // 테스트 학생 데이터 생성
            if (studentRepository.count() == 0) {
                // 학생1용 User 계정
                User studentUser1 = User.builder()
                        .username("student1")
                        .password(passwordEncoder.encode("student123"))
                        .name("홍길동")
                        .email("student1@kplay.web")
                        .phoneNumber("010-5678-9012")
                        .role(UserRole.STUDENT)
                        .isActive(true)
                        .build();
                studentUser1 = userRepository.save(studentUser1);

                Student student1 = Student.builder()
                        .user(studentUser1)
                        .studentName("홍길동")
                        .birthDate(LocalDate.of(2010, 3, 15))
                        .studentPhone("010-5678-9012")
                        .parentPhone("010-4567-8901")
                        .parentName("박학부모")
                        .school("서울초등학교")
                        .grade("6")
                        .address("서울시 강남구 테헤란로 123")
                        .memo("영어 초급반")
                        .isActive(true)
                        .build();
                studentRepository.save(student1);

                // 학생2용 User 계정
                User studentUser2 = User.builder()
                        .username("student2")
                        .password(passwordEncoder.encode("student123"))
                        .name("김민수")
                        .email("student2@kplay.web")
                        .phoneNumber("010-6789-0123")
                        .role(UserRole.STUDENT)
                        .isActive(true)
                        .build();
                studentUser2 = userRepository.save(studentUser2);

                Student student2 = Student.builder()
                        .user(studentUser2)
                        .studentName("김민수")
                        .birthDate(LocalDate.of(2011, 7, 20))
                        .studentPhone("010-6789-0123")
                        .parentPhone("010-7890-1234")
                        .parentName("김학부모")
                        .school("서울초등학교")
                        .grade("5")
                        .address("서울시 강남구 역삼동 456")
                        .memo("수학 중급반")
                        .isActive(true)
                        .build();
                studentRepository.save(student2);

                // 학생3용 User 계정
                User studentUser3 = User.builder()
                        .username("student3")
                        .password(passwordEncoder.encode("student123"))
                        .name("이지은")
                        .email("student3@kplay.web")
                        .phoneNumber("010-7890-1234")
                        .role(UserRole.STUDENT)
                        .isActive(true)
                        .build();
                studentUser3 = userRepository.save(studentUser3);

                Student student3 = Student.builder()
                        .user(studentUser3)
                        .studentName("이지은")
                        .birthDate(LocalDate.of(2012, 11, 5))
                        .studentPhone("010-7890-1234")
                        .parentPhone("010-8901-2345")
                        .parentName("이학부모")
                        .school("한강초등학교")
                        .grade("4")
                        .address("서울시 서초구 반포동 789")
                        .memo("영어 중급반, 수학 초급반")
                        .isActive(true)
                        .build();
                studentRepository.save(student3);

                // 추가 학생 4-10
                String[] names = {"박서준", "최유나", "정민호", "강하늘", "윤서아", "임재현", "송지우"};
                String[] schools = {"서울초등학교", "한강초등학교", "강남초등학교"};
                
                for (int i = 0; i < names.length; i++) {
                    User studentUser = User.builder()
                            .username("student" + (i + 4))
                            .password(passwordEncoder.encode("student123"))
                            .name(names[i])
                            .email("student" + (i + 4) + "@kplay.web")
                            .phoneNumber("010-" + String.format("%04d", 8000 + i) + "-" + String.format("%04d", 1234 + i))
                            .role(UserRole.STUDENT)
                            .isActive(true)
                            .build();
                    studentUser = userRepository.save(studentUser);

                    Student student = Student.builder()
                            .user(studentUser)
                            .studentName(names[i])
                            .birthDate(LocalDate.of(2010 + (i % 3), (i % 12) + 1, (i % 28) + 1))
                            .studentPhone("010-" + String.format("%04d", 8000 + i) + "-" + String.format("%04d", 1234 + i))
                            .parentPhone("010-" + String.format("%04d", 9000 + i) + "-" + String.format("%04d", 2345 + i))
                            .parentName(names[i].substring(0, 1) + "학부모")
                            .school(schools[i % 3])
                            .grade(String.valueOf(4 + (i % 3)))
                            .address("서울시 강남구 " + (i + 1) + "번지")
                            .memo("테스트 학생")
                            .isActive(true)
                            .build();
                    studentRepository.save(student);
                }

                log.info("✓ 테스트 학생 10명 생성 완료");
                
                // 기존 학생에 parentUser 연결
                User parent1 = userRepository.findByUsername("parent1").orElse(null);
                if (parent1 != null) {
                    List<Student> studentsToUpdate = studentRepository.findByParentPhone("010-4567-8901");
                    for (Student student : studentsToUpdate) {
                        if (student.getParentUser() == null) {
                            student.setParentUser(parent1);
                            studentRepository.save(student);
                            log.info("✓ 학생 {} 에게 학부모 계정 연결 완료", student.getStudentName());
                        }
                    }
                }
            }

            // 테스트 수업 및 스케줄 생성
            if (courseRepository.count() == 0) {
                // 선생님 조회
                User teacher = userRepository.findByUsername("teacher1").orElse(null);
                
                // 수업 생성
                Course englishCourse = Course.builder()
                        .courseName("초급 영어")
                        .description("초등학생 대상 기초 영어 수업")
                        .teacher(teacher)
                        .maxStudents(8)
                        .durationMinutes(120)
                        .level("초급")
                        .color("#4CAF50")
                        .isActive(true)
                        .build();
                courseRepository.save(englishCourse);

                // 오늘 스케줄 생성
                CourseSchedule todaySchedule = CourseSchedule.builder()
                        .course(englishCourse)
                        .scheduleDate(LocalDate.now())
                        .startTime(LocalTime.of(14, 0))
                        .endTime(LocalTime.of(16, 0))
                        .dayOfWeek(LocalDate.now().getDayOfWeek().name())
                        .currentStudents(0)
                        .isCancelled(false)
                        .build();
                scheduleRepository.save(todaySchedule);

                // 학생들에게 수강권 할당 (12주 기간 + 24회 사용 가능)
                List<Student> students = studentRepository.findAll();
                for (int i = 0; i < Math.min(5, students.size()); i++) {
                    Student student = students.get(i);
                    LocalDate startDate = LocalDate.now();
                    // 영업일 기준 12주 (60영업일) 계산
                    LocalDate endDate = holidayService.calculateEndDate(startDate, 60);
                    
                    Enrollment enrollment = Enrollment.builder()
                            .student(student)
                            .course(englishCourse)
                            .startDate(startDate)
                            .endDate(endDate)
                            .totalCount(24) // 12주 동안 24회 사용 가능
                            .usedCount(i * 2) // 학생별로 다른 사용 횟수
                            .remainingCount(24 - (i * 2))
                            .isActive(true)
                            .build();
                    enrollmentRepository.save(enrollment);
                }

                log.info("✓ 테스트 수업 및 스케줄 생성 완료 (오늘 14:00-16:00)");
            }

            log.info("=== 초기 데이터 로딩 완료 ===");
            log.info("");
            log.info("📋 초기 계정 생성됨 (비밀번호는 CREDENTIALS.md 참조)");
            log.info("  - admin (관리자)");
            log.info("  - teacher1, teacher2 (선생님)");
            log.info("  - parent1 (학부모)");
            log.info("  - student1, student2, student3 (학생)");
            log.info("");
            log.info("🌐 Swagger UI: http://localhost:8080/swagger-ui.html");
            log.info("🗄️  H2 Console: http://localhost:8080/h2-console (ADMIN 계정 필요)");
            log.info("");
        };
    }
}
