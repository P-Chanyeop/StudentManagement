package web.kplay.studentmanagement.domain.sms;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;

@Entity
@Table(name = "sms_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SMSConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SMSProvider provider = SMSProvider.ALIGO;

    @Column(nullable = false, length = 200)
    private String apiKey;

    @Column(nullable = false, length = 100)
    private String userId;

    @Column(nullable = false, length = 20)
    private String senderNumber;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean testMode = false;

    // 자동 발송 설정
    @Column(nullable = false)
    private Boolean autoAttendanceReminder = false;

    @Column(nullable = false)
    private Boolean autoEnrollmentExpiry = false;

    @Column(nullable = false)
    private Boolean autoPaymentReminder = false;

    public enum SMSProvider {
        ALIGO,
        MESSAGEKOREA
    }

    // 설정 업데이트
    public void updateSettings(String apiKey, String userId, String senderNumber) {
        if (apiKey != null && !apiKey.isEmpty()) {
            this.apiKey = apiKey;
        }
        if (userId != null && !userId.isEmpty()) {
            this.userId = userId;
        }
        if (senderNumber != null && !senderNumber.isEmpty()) {
            this.senderNumber = senderNumber;
        }
    }

    // 자동 발송 설정 업데이트
    public void updateAutoSettings(Boolean attendanceReminder, Boolean enrollmentExpiry, Boolean paymentReminder) {
        if (attendanceReminder != null) this.autoAttendanceReminder = attendanceReminder;
        if (enrollmentExpiry != null) this.autoEnrollmentExpiry = enrollmentExpiry;
        if (paymentReminder != null) this.autoPaymentReminder = paymentReminder;
    }

    // 테스트 모드 전환
    public void toggleTestMode() {
        this.testMode = !this.testMode;
    }
}
