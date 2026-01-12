package web.kplay.studentmanagement.dto.notice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeViewResponse {
    private Long id;
    private String userName;
    private String userRole;
    private LocalDateTime viewedAt;
}
