package web.kplay.studentmanagement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import web.kplay.studentmanagement.domain.user.User;
import web.kplay.studentmanagement.domain.user.UserRole;
import web.kplay.studentmanagement.repository.UserRepository;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
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

                log.info("초기 데이터 생성 완료");
                log.info("관리자 계정 - ID: admin, PW: admin123");
                log.info("선생님 계정 - ID: teacher, PW: teacher123");
            }
        };
    }
}
