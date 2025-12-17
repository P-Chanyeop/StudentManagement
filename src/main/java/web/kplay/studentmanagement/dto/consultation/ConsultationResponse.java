package web.kplay.studentmanagement.dto.consultation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long consultantId;
    private String consultantName;
    private LocalDate consultationDate;
    private String title;
    private String content;
    private String consultationType;
    private String recordingFileUrl;
    private String attachmentFileUrl;
    private String actionItems;
    private LocalDate nextConsultationDate;
}
