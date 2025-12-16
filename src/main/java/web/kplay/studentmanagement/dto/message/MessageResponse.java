package web.kplay.studentmanagement.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import web.kplay.studentmanagement.domain.message.MessageType;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private String recipientPhone;
    private String recipientName;
    private MessageType messageType;
    private String content;
    private String sendStatus;
    private LocalDateTime sentAt;
    private String errorMessage;
}
