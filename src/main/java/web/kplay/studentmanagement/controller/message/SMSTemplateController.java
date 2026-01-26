package web.kplay.studentmanagement.controller.message;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.domain.sms.SMSTemplate;
import web.kplay.studentmanagement.repository.SMSTemplateRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sms-templates")
@RequiredArgsConstructor
public class SMSTemplateController {

    private final SMSTemplateRepository smsTemplateRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<SMSTemplate>> getAllTemplates() {
        return ResponseEntity.ok(smsTemplateRepository.findByIsActiveTrue());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SMSTemplate> createTemplate(@RequestBody SMSTemplate template) {
        return ResponseEntity.ok(smsTemplateRepository.save(template));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SMSTemplate> updateTemplate(@PathVariable Long id, @RequestBody SMSTemplate template) {
        SMSTemplate existing = smsTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("템플릿을 찾을 수 없습니다"));
        existing.update(template.getName(), template.getCategory(), template.getContent(), template.getDescription());
        return ResponseEntity.ok(smsTemplateRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id) {
        SMSTemplate template = smsTemplateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("템플릿을 찾을 수 없습니다"));
        template.deactivate();
        smsTemplateRepository.save(template);
        return ResponseEntity.ok(Map.of("message", "템플릿이 삭제되었습니다"));
    }
}
