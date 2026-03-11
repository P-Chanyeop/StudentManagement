package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import web.kplay.studentmanagement.domain.setting.SiteSetting;

import java.util.Optional;

public interface SiteSettingRepository extends JpaRepository<SiteSetting, Long> {
    Optional<SiteSetting> findBySettingKey(String settingKey);
}
