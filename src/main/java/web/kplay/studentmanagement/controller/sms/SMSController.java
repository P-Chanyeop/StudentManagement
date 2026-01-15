package web.kplay.studentmanagement.controller.sms;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.service.sms.AligoSmsService;

@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsController {

    private final AligoSmsService aligoSmsService;

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<AligoSmsService.SmsResponse> sendSms(@RequestBody SmsRequest request) {
        AligoSmsService.SmsResponse response = aligoSmsService.sendSms(
            request.getReceiver(),
            request.getMessage(),
            request.getTitle(),
            request.getMsgType()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/remain")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<AligoSmsService.RemainResponse> getRemainCount() {
        AligoSmsService.RemainResponse response = aligoSmsService.getRemainCount();
        return ResponseEntity.ok(response);
    }

    @lombok.Getter
    @lombok.Setter
    public static class SmsRequest {
        private String receiver;
        private String message;
        private String title;
        private String msgType;
    }
}
