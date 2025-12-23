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

    /**
     * 특정 학생의 상담 이력 조회
     * @param studentId 학생 ID
     * @return 해당 학생의 상담 이력 목록 (최신순)
     */
    @Transactional(readOnly = true)
    public List<ConsultationResponse> getConsultationsByStudent(Long studentId) {
        return consultationRepository.findByStudentIdOrderByDateDesc(studentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 부모님의 자녀들 상담 이력 조회
     * @param username 부모님 사용자명
     * @return 자녀들의 모든 상담 이력 목록 (최신순)
     */
    @Transactional(readOnly = true)
    public List<ConsultationResponse> getConsultationsByParent(String username) {
        User parent = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("부모님 정보를 찾을 수 없습니다."));
        
        List<Student> children = studentRepository.findByParentUser(parent);
        
        return children.stream()
                .flatMap(child -> consultationRepository.findByStudentIdOrderByDateDesc(child.getId()).stream())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 상담 기록 수정 (제목, 내용, 파일 등)
     * @param id 상담 기록 ID
     * @param request 수정할 상담 정보
     * @return 수정된 상담 기록 정보
     */
    @Transactional
    public ConsultationResponse updateConsultation(Long id, ConsultationRequest request) {
        Consultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상담 기록을 찾을 수 없습니다."));
        
        consultation.updateContent(request.getTitle(), request.getContent(), request.getConsultationType());
        
        if (request.getRecordingFileUrl() != null) {
            consultation.addRecordingFile(request.getRecordingFileUrl());
        }
        
        if (request.getAttachmentFileUrl() != null) {
            consultation.addAttachmentFile(request.getAttachmentFileUrl());
        }
        
        if (request.getActionItems() != null) {
            consultation.updateActionItems(request.getActionItems());
        }
        
        if (request.getNextConsultationDate() != null) {
            consultation.scheduleNextConsultation(request.getNextConsultationDate());
        }
        
        return toResponse(consultation);
    }

    /**
     * 상담 기록 삭제
     * @param id 삭제할 상담 기록 ID
     * @return void
     */
    @Transactional
    public void deleteConsultation(Long id) {
        Consultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상담 기록을 찾을 수 없습니다."));
        
        consultationRepository.delete(consultation);
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
