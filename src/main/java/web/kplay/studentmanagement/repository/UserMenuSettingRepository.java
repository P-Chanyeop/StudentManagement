package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import web.kplay.studentmanagement.domain.user.UserMenuSetting;

import java.util.Optional;

public interface UserMenuSettingRepository extends JpaRepository<UserMenuSetting, Long> {
    Optional<UserMenuSetting> findByUserId(Long userId);
}
