package web.kplay.studentmanagement.dto.notice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NoticeRequest {

    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200, message = "제목은 200자 이내여야 합니다")
    private String title;

    @NotBlank(message = "내용은 필수입니다")
    private String content;

    private Boolean isPinned; // 상단 고정 여부
}
