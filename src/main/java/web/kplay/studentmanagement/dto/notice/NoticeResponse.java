package web.kplay.studentmanagement.dto.notice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeResponse {
    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private String authorName;
    private Boolean isPinned;
    private Boolean isActive;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
