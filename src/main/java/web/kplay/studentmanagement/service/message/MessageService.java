package web.kplay.studentmanagement.service.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.message.Message;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.dto.message.MessageResponse;
import web.kplay.studentmanagement.dto.message.MessageSendRequest;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.MessageRepository;
import web.kplay.studentmanagement.repository.StudentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public MessageResponse sendMessage(MessageSendRequest request) {
        Student student = null;
        if (request.getStudentId() != null) {
            student = studentRepository.findById(request.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));
        }

        Message message = Message.builder()
                .student(student)
                .recipientPhone(request.getRecipientPhone())
                .recipientName(request.getRecipientName())
                .messageType(request.getMessageType())
                .content(request.getContent())
                .sendStatus("PENDING")
                .build();

        Message savedMessage = messageRepository.save(message);

        // TODO: 실제 SMS API 연동 (추후 구현)
        // 현재는 PENDING 상태로만 저장
        log.info("문자 발송 요청: 수신자={}, 타입={}, 내용={}",
                request.getRecipientPhone(), request.getMessageType(), request.getContent());

        // 테스트용: 자동으로 발송 완료 처리 (실제 SMS API 연동 후 제거)
        savedMessage.markAsSent(LocalDateTime.now(), "TEST-" + savedMessage.getId());

        return toResponse(savedMessage);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessagesByStudent(Long studentId) {
        return messageRepository.findByStudentId(studentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getPendingMessages() {
        return messageRepository.findPendingMessages().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getAllMessages() {
        return messageRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private MessageResponse toResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .studentId(message.getStudent() != null ? message.getStudent().getId() : null)
                .studentName(message.getStudent() != null ? message.getStudent().getStudentName() : null)
                .recipientPhone(message.getRecipientPhone())
                .recipientName(message.getRecipientName())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .sendStatus(message.getSendStatus())
                .sentAt(message.getSentAt())
                .errorMessage(message.getErrorMessage())
                .build();
    }
}
