package web.kplay.studentmanagement.controller.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.domain.notification.AdminNotification;
import web.kplay.studentmanagement.service.notification.AdminNotificationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<AdminNotification>> getNotifications() {
        return ResponseEntity.ok(notificationService.getNotifications());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("message", "읽음 처리되었습니다"));
    }

    @PatchMapping("/{id}/dismiss")
    public ResponseEntity<Map<String, String>> dismiss(@PathVariable Long id) {
        notificationService.dismiss(id);
        return ResponseEntity.ok(Map.of("message", "알림이 제거되었습니다"));
    }
}
