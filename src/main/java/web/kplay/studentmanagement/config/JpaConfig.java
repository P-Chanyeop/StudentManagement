package web.kplay.studentmanagement.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "web.kplay.studentmanagement.repository")
public class JpaConfig {
}
