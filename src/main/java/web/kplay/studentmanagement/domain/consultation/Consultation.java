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

    @Column(length = 500)
    private String recordingFileUrl; // 녹음 파일 링크

    @Column(length = 500)
    private String attachmentFileUrl; // 첨부 파일 링크

    // 상담 내용 업데이트
    public void updateContent(String title, String content) {
        this.title = title;
        this.content = content;
    }

    // 녹음 파일 추가
    public void addRecordingFile(String fileUrl) {
        this.recordingFileUrl = fileUrl;
    }

    // 첨부 파일 추가
    public void addAttachmentFile(String fileUrl) {
        this.attachmentFileUrl = fileUrl;
    }
}
