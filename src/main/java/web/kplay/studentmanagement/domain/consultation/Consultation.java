package web.kplay.studentmanagement.domain.consultation;

import jakarta.persistence.*;
import lombok.*;
import web.kplay.studentmanagement.domain.BaseEntity;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.domain.user.User;

import java.time.LocalDate;

@Entity
@Table(name = "consultations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Consultation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consultant_id", nullable = false)
    private User consultant; // 상담자 (선생님 또는 관리자)

    @Column(nullable = false)
    private LocalDate consultationDate;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(length = 50)
    private String consultationType; // 학습상담, 진로상담, 학부모상담 등

    @Column(length = 500)
    private String recordingFileUrl; // 녹음 파일 링크

    @Column(length = 500)
    private String attachmentFileUrl; // 첨부 파일 링크

    @Column(length = 1000)
    private String actionItems; // 후속 조치 사항

    @Column
    private LocalDate nextConsultationDate; // 다음 상담 예정일

    // 상담 내용 업데이트
    public void updateContent(String title, String content, String consultationType) {
        this.title = title;
        this.content = content;
        this.consultationType = consultationType;
    }

    // 녹음 파일 추가
    public void addRecordingFile(String fileUrl) {
        this.recordingFileUrl = fileUrl;
    }

    // 첨부 파일 추가
    public void addAttachmentFile(String fileUrl) {
        this.attachmentFileUrl = fileUrl;
    }

    // 후속 조치 사항 업데이트
    public void updateActionItems(String actionItems) {
        this.actionItems = actionItems;
    }

    // 다음 상담 일정 설정
    public void scheduleNextConsultation(LocalDate nextDate) {
        this.nextConsultationDate = nextDate;
    }
}
