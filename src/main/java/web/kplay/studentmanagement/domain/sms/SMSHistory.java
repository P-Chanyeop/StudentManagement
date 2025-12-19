package web.kplay.studentmanagement.domain.sms;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "sms_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SMSHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String senderNumber;

    @Column(nullable = false, length = 20)
    private String receiverNumber;

    @Column(length = 100)
    private String receiverName;

    @Column(nullable = false, length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SMSStatus status;

    @Column
    private LocalDateTime sentAt;

    @Column(length = 500)
    private String errorMessage;

    @Column(length = 100)
    private String msgId; // 알리고 메시지 ID

    @Column
    private Integer cost; // 발송 비용 (원)

    @Column(length = 50)
    private String smsType; // SMS, LMS, MMS

    @Column(length = 50)
    private String category; // 카테고리 (attendance, enrollment 등)

    public enum SMSStatus {
        PENDING,    // 대기
        SENT,       // 발송 성공
        FAILED      // 발송 실패
    }

    // 발송 성공 처리
    public void markAsSent(String msgId) {
        this.status = SMSStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.msgId = msgId;
    }

    // 발송 실패 처리
    public void markAsFailed(String errorMessage) {
        this.status = SMSStatus.FAILED;
        this.sentAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }
}
