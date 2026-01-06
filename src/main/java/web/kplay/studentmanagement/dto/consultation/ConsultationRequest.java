package web.kplay.studentmanagement.dto.consultation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationRequest {

    @NotNull(message = "학생 ID는 필수입니다")
    private Long studentId;

    @NotNull(message = "상담 날짜는 필수입니다")
    private LocalDate consultationDate;

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    private String content;

    private String recordingFileUrl;

    private String attachmentFileUrl;
}
