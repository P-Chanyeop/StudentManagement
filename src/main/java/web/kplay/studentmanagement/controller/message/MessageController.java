package web.kplay.studentmanagement.controller.message;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.dto.message.MessageResponse;
import web.kplay.studentmanagement.dto.message.MessageSendRequest;
import web.kplay.studentmanagement.service.message.MessageService;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<MessageResponse> sendMessage(@Valid @RequestBody MessageSendRequest request) {
        MessageResponse response = messageService.sendMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'PARENT')")
    public ResponseEntity<List<MessageResponse>> getMessagesByStudent(@PathVariable Long studentId) {
        List<MessageResponse> responses = messageService.getMessagesByStudent(studentId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<MessageResponse>> getPendingMessages() {
        List<MessageResponse> responses = messageService.getPendingMessages();
        return ResponseEntity.ok(responses);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<MessageResponse>> getAllMessages() {
        List<MessageResponse> responses = messageService.getAllMessages();
        return ResponseEntity.ok(responses);
    }
}
