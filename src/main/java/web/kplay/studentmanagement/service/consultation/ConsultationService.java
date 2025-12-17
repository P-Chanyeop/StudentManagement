package web.kplay.studentmanagement.service.consultation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.consultation.Consultation;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.domain.user.User;
import web.kplay.studentmanagement.dto.consultation.ConsultationRequest;
import web.kplay.studentmanagement.dto.consultation.ConsultationResponse;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.ConsultationRepository;
import web.kplay.studentmanagement.repository.StudentRepository;
import web.kplay.studentmanagement.repository.UserRepository;
import web.kplay.studentmanagement.security.UserDetailsImpl;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    @Transactional
    public ConsultationResponse createConsultation(ConsultationRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));

        // 현재 로그인한 사용자를 상담자로 설정
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User consultant = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다"));

        Consultation consultation = Consultation.builder()
                .student(student)
                .consultant(consultant)
                .consultationDate(request.getConsultationDate())
                .title(request.getTitle())
                .content(request.getContent())
                .consultationType(request.getConsultationType())
                .recordingFileUrl(request.getRecordingFileUrl())
                .attachmentFileUrl(request.getAttachmentFileUrl())
                .actionItems(request.getActionItems())
                .nextConsultationDate(request.getNextConsultationDate())
                .build();

        Consultation savedConsultation = consultationRepository.save(consultation);
        log.info("상담 기록 저장: 학생={}, 상담자={}, 날짜={}",
                student.getStudentName(), consultant.getName(), request.getConsultationDate());

        return toResponse(savedConsultation);
    }

    @Transactional(readOnly = true)
    public List<ConsultationResponse> getConsultationsByStudent(Long studentId) {
        return consultationRepository.findByStudentIdOrderByDateDesc(studentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ConsultationResponse toResponse(Consultation consultation) {
        return ConsultationResponse.builder()
                .id(consultation.getId())
                .studentId(consultation.getStudent().getId())
                .studentName(consultation.getStudent().getStudentName())
                .consultantId(consultation.getConsultant().getId())
                .consultantName(consultation.getConsultant().getName())
                .consultationDate(consultation.getConsultationDate())
                .title(consultation.getTitle())
                .content(consultation.getContent())
                .consultationType(consultation.getConsultationType())
                .recordingFileUrl(consultation.getRecordingFileUrl())
                .attachmentFileUrl(consultation.getAttachmentFileUrl())
                .actionItems(consultation.getActionItems())
                .nextConsultationDate(consultation.getNextConsultationDate())
                .build();
    }
}
