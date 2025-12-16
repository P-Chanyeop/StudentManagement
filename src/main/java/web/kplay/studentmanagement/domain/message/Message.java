package web.kplay.studentmanagement.domain.message;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;
import web.kplay.studentmanagement.domain.student.Student;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(nullable = false, length = 20)
    private String recipientPhone;

    @Column(length = 50)
    private String recipientName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MessageType messageType;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false, length = 20)
    private String sendStatus; // PENDING, SENT, FAILED

    @Column
    private LocalDateTime sentAt;

    @Column(length = 500)
    private String errorMessage;

    @Column(length = 100)
    private String externalMessageId; // 외부 SMS API에서 발급한 메시지 ID

    // 발송 완료 처리
    public void markAsSent(LocalDateTime sentAt, String externalMessageId) {
        this.sendStatus = "SENT";
        this.sentAt = sentAt;
        this.externalMessageId = externalMessageId;
    }

    // 발송 실패 처리
    public void markAsFailed(String errorMessage) {
        this.sendStatus = "FAILED";
        this.errorMessage = errorMessage;
    }

    // 재발송을 위한 상태 리셋
    public void resetForResend() {
        this.sendStatus = "PENDING";
        this.sentAt = null;
        this.errorMessage = null;
        this.externalMessageId = null;
    }
}
