package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.message.Message;
import web.kplay.studentmanagement.domain.message.MessageType;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByStudentId(Long studentId);

    List<Message> findBySendStatus(String sendStatus);

    List<Message> findByMessageType(MessageType messageType);

    @Query("SELECT m FROM Message m WHERE m.sendStatus = 'PENDING' ORDER BY m.createdAt ASC")
    List<Message> findPendingMessages();

    @Query("SELECT m FROM Message m WHERE m.sentAt BETWEEN :startDate AND :endDate")
    List<Message> findBySentDateRange(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);
}
