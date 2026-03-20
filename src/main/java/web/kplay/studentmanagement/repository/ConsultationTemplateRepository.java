package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import web.kplay.studentmanagement.domain.consultation.ConsultationTemplate;

import java.util.List;

public interface ConsultationTemplateRepository extends JpaRepository<ConsultationTemplate, Long> {
    List<ConsultationTemplate> findAllByOrderBySortOrderAsc();
}
