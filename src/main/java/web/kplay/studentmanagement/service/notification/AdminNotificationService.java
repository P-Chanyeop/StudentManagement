package web.kplay.studentmanagement.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.notification.AdminNotification;
import web.kplay.studentmanagement.repository.AdminNotificationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private final AdminNotificationRepository notificationRepository;

    public void createNotification(String type, String title, String content, Long referenceId) {
        AdminNotification notification = AdminNotification.builder()
                .type(type)
                .title(title)
                .content(content)
                .referenceId(referenceId)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
        log.info("관리자 알림 생성: type={}, title={}", type, title);
    }

    @Transactional(readOnly = true)
    public List<AdminNotification> getNotifications() {
        return notificationRepository.findTop10ByIsDismissedFalseOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return notificationRepository.countByIsReadFalseAndIsDismissedFalse();
    }

    @Transactional
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(AdminNotification::markAsRead);
    }

    @Transactional
    public void dismiss(Long id) {
        notificationRepository.findById(id).ifPresent(AdminNotification::dismiss);
    }
}
