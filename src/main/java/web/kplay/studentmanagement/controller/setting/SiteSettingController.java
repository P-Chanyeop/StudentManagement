package web.kplay.studentmanagement.controller.setting;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.domain.setting.SiteSetting;
import web.kplay.studentmanagement.repository.SiteSettingRepository;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SiteSettingController {

    private final SiteSettingRepository repository;

    @GetMapping("/{key}")
    public ResponseEntity<Map<String, String>> get(@PathVariable String key) {
        return repository.findBySettingKey(key)
                .map(s -> ResponseEntity.ok(Map.of("key", s.getSettingKey(), "value", s.getSettingValue())))
                .orElse(ResponseEntity.ok(Map.of("key", key, "value", "")));
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> update(@PathVariable String key, @RequestBody Map<String, String> body) {
        SiteSetting setting = repository.findBySettingKey(key)
                .orElseGet(() -> repository.save(SiteSetting.builder().settingKey(key).settingValue("").build()));
        setting.updateValue(body.get("value"));
        repository.save(setting);
        return ResponseEntity.ok(Map.of("key", key, "value", setting.getSettingValue()));
    }
}
