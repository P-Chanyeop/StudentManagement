package web.kplay.studentmanagement;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptTest {
    @Test
    public void generateBcrypt() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "7647";
        String encoded = encoder.encode(password);
        System.out.println("=== BCrypt Hash ===");
        System.out.println(encoded);
        System.out.println("===================");
    }
}
