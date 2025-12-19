package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.sms.SMSConfig;

import java.util.Optional;

@Repository
public interface SMSConfigRepository extends JpaRepository<SMSConfig, Long> {

    /**
     * 활성 설정 조회
     */
    Optional<SMSConfig> findByIsActiveTrue();

    /**
     * 최신 설정 조회 (ID 기준)
     */
    Optional<SMSConfig> findTopByOrderByIdDesc();
}
