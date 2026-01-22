package web.kplay.studentmanagement.dto.consultation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentPhone;
    private Long consultantId;
    private String consultantName;
    private LocalDate consultationDate;
    private LocalTime consultationTime;
    private String consultationType;
    private String title;
    private String content;
    private String recordingFileUrl;
    private String attachmentFileUrl;
}
