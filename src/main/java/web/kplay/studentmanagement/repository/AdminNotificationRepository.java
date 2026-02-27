package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.notification.AdminNotification;

import java.util.List;

@Repository
public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {

    List<AdminNotification> findTop10ByIsDismissedFalseOrderByCreatedAtDesc();

    long countByIsReadFalseAndIsDismissedFalse();
}
