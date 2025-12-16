package web.kplay.studentmanagement.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import web.kplay.studentmanagement.domain.message.MessageType;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MessageSendRequest {

    private Long studentId;

    @NotBlank(message = "수신자 전화번호는 필수입니다")
    private String recipientPhone;

    private String recipientName;

    @NotNull(message = "메시지 타입은 필수입니다")
    private MessageType messageType;

    @NotBlank(message = "메시지 내용은 필수입니다")
    private String content;
}
