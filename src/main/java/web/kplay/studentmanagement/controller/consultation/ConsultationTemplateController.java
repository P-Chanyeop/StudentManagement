package web.kplay.studentmanagement.controller.consultation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.domain.consultation.ConsultationTemplate;
import web.kplay.studentmanagement.repository.ConsultationTemplateRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/consultation-templates")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class ConsultationTemplateController {

    private final ConsultationTemplateRepository templateRepository;

    @GetMapping
    public List<ConsultationTemplate> list() {
        return templateRepository.findAllByOrderBySortOrderAsc();
    }

    @PostMapping
    public ConsultationTemplate create(@RequestBody Map<String, String> body) {
        ConsultationTemplate t = new ConsultationTemplate();
        t.setName(body.get("name"));
        t.setContent(body.get("content"));
        t.setSortOrder((int) templateRepository.count());
        return templateRepository.save(t);
    }

    @PutMapping("/{id}")
    public ConsultationTemplate update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        ConsultationTemplate t = templateRepository.findById(id).orElseThrow();
        if (body.containsKey("name")) t.setName(body.get("name"));
        if (body.containsKey("content")) t.setContent(body.get("content"));
        return templateRepository.save(t);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        templateRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
