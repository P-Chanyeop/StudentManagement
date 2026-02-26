package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.auth.PasswordResetCode;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

    Optional<PasswordResetCode> findTopByUsernameAndPhoneNumberAndVerifiedFalseAndUsedFalseOrderByCreatedAtDesc(
            String username, String phoneNumber);

    Optional<PasswordResetCode> findByResetTokenAndUsedFalse(String resetToken);

    long countByPhoneNumberAndCreatedAtAfter(String phoneNumber, LocalDateTime after);

    Optional<PasswordResetCode> findTopByPhoneNumberOrderByCreatedAtDesc(String phoneNumber);
}
