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

    // 특정 학생의 특정 타입 메시지 중 특정 시간 이후 발송된 것 조회 (중복 발송 방지)
    @Query("SELECT m FROM Message m WHERE m.student = :student AND m.messageType = :messageType AND m.sentAt > :sentAfter AND m.sendStatus = 'SENT'")
    List<Message> findByStudentAndMessageTypeAndSentAtAfter(@Param("student") web.kplay.studentmanagement.domain.student.Student student,
                                                              @Param("messageType") MessageType messageType,
                                                              @Param("sentAfter") LocalDateTime sentAfter);

    // 마이페이지용 메서드
    List<Message> findTop20ByStudentIdOrderByCreatedAtDesc(Long studentId);
}
