package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.sms.SMSTemplate;

import java.util.List;

@Repository
public interface SMSTemplateRepository extends JpaRepository<SMSTemplate, Long> {

    /**
     * 활성 템플릿 조회
     */
    List<SMSTemplate> findByIsActiveTrue();

    /**
     * 카테고리별 템플릿 조회
     */
    List<SMSTemplate> findByCategoryAndIsActiveTrue(String category);

    /**
     * 전체 템플릿 조회 (활성/비활성 포함)
     */
    List<SMSTemplate> findAllByOrderByCreatedAtDesc();
}
