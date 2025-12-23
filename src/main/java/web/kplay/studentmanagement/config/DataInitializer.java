package web.kplay.studentmanagement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.domain.user.User;
import web.kplay.studentmanagement.domain.user.UserRole;
import web.kplay.studentmanagement.repository.StudentRepository;
import web.kplay.studentmanagement.repository.UserRepository;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // 개발 환경에서만 초기 데이터 생성
            if (userRepository.count() == 0) {
                // 관리자 계정 생성
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .name("관리자")
                        .phoneNumber("010-1234-5678")
                        .email("admin@example.com")
                        .role(UserRole.ADMIN)
                        .isActive(true)
                        .build();
                userRepository.save(admin);

                // 선생님 계정 생성
                User teacher = User.builder()
                        .username("teacher")
                        .password(passwordEncoder.encode("teacher123"))
                        .name("김선생")
                        .phoneNumber("010-2345-6789")
                        .email("teacher@example.com")
                        .role(UserRole.TEACHER)
                        .isActive(true)
                        .build();
                userRepository.save(teacher);

                // 부모 계정들 생성
                User parent1 = User.builder()
                        .username("parent1")
                        .password(passwordEncoder.encode("parent123"))
                        .name("김학부모")
                        .phoneNumber("010-3456-7890")
                        .email("parent1@example.com")
                        .role(UserRole.PARENT)
                        .isActive(true)
                        .build();
                userRepository.save(parent1);

                User parent2 = User.builder()
                        .username("parent2")
                        .password(passwordEncoder.encode("parent123"))
                        .name("이학부모")
                        .phoneNumber("010-4567-8901")
                        .email("parent2@example.com")
                        .role(UserRole.PARENT)
                        .isActive(true)
                        .build();
                userRepository.save(parent2);

                // 학생들 생성
                Student student1 = Student.builder()
                        .studentName("김영희")
                        .parentName("김학부모")
                        .parentPhone("010-3456-7890")
                        .parentUser(parent1)
                        .englishLevel("Beginner")
                        .memo("활발하고 적극적인 학생")
                        .isActive(true)
                        .build();
                studentRepository.save(student1);

                Student student2 = Student.builder()
                        .studentName("이철수")
                        .parentName("이학부모")
                        .parentPhone("010-4567-8901")
                        .parentUser(parent2)
                        .englishLevel("Intermediate")
                        .memo("차분하고 성실한 학생")
                        .isActive(true)
                        .build();
                studentRepository.save(student2);

                Student student3 = Student.builder()
                        .studentName("박민수")
                        .parentName("김학부모")
                        .parentPhone("010-3456-7890")
                        .parentUser(parent1)
                        .englishLevel("Advanced")
                        .memo("영어 실력이 뛰어난 학생")
                        .isActive(true)
                        .build();
                studentRepository.save(student3);

                log.info("초기 데이터 생성 완료");
                log.info("관리자 계정 - ID: admin, PW: admin123");
                log.info("선생님 계정 - ID: teacher, PW: teacher123");
                log.info("부모 계정 - ID: parent1/parent2, PW: parent123");
                log.info("학생 3명 생성 완료");
            }
        };
    }
}
