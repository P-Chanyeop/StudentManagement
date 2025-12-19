package web.kplay.studentmanagement.service.parent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import web.kplay.studentmanagement.domain.parent.ParentStudentRelation;
import web.kplay.studentmanagement.domain.student.Student;
import web.kplay.studentmanagement.domain.user.User;
import web.kplay.studentmanagement.dto.parent.ParentStudentRelationRequest;
import web.kplay.studentmanagement.dto.parent.ParentStudentRelationResponse;
import web.kplay.studentmanagement.dto.parent.PermissionUpdateRequest;
import web.kplay.studentmanagement.exception.BusinessException;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.ParentStudentRelationRepository;
import web.kplay.studentmanagement.repository.StudentRepository;
import web.kplay.studentmanagement.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 보호자 접근 제어 서비스
 * 보호자-학생 관계 관리 및 권한 검증
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParentAccessControlService {

    private final ParentStudentRelationRepository relationRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    /**
     * 보호자-학생 관계 생성
     */
    @Transactional
    public ParentStudentRelationResponse createRelation(ParentStudentRelationRequest request) {
        User parent = userRepository.findById(request.getParentId())
                .orElseThrow(() -> new ResourceNotFoundException("보호자를 찾을 수 없습니다"));

        if (!parent.getRole().name().equals("ROLE_PARENT")) {
            throw new BusinessException("보호자 역할의 사용자만 관계를 생성할 수 있습니다");
        }

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("학생을 찾을 수 없습니다"));

        // 이미 존재하는 관계 확인
        relationRepository.findByParentAndStudent(request.getParentId(), request.getStudentId())
                .ifPresent(existing -> {
                    throw new BusinessException("이미 등록된 관계입니다");
                });

        ParentStudentRelation relation = ParentStudentRelation.builder()
                .parent(parent)
                .student(student)
                .relationship(request.getRelationship())
                .isActive(true)
                .canViewAttendance(true)
                .canViewGrades(true)
                .canViewInvoices(true)
                .canMakeReservations(true)
                .canReceiveMessages(true)
                .build();

        ParentStudentRelation savedRelation = relationRepository.save(relation);

        log.info("보호자-학생 관계 생성: 보호자={}, 학생={}, 관계={}",
                parent.getUsername(), student.getStudentName(), request.getRelationship());

        return toResponse(savedRelation);
    }

    /**
     * 보호자가 관리하는 학생 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ParentStudentRelationResponse> getStudentsByParent(Long parentId) {
        return relationRepository.findActiveRelationsByParent(parentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 학생의 보호자 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ParentStudentRelationResponse> getParentsByStudent(Long studentId) {
        return relationRepository.findActiveRelationsByStudent(studentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 보호자가 학생에 대한 접근 권한이 있는지 확인
     */
    @Transactional(readOnly = true)
    public boolean hasAccessToStudent(Long parentId, Long studentId) {
        return relationRepository.hasAccessToStudent(parentId, studentId);
    }

    /**
     * 보호자의 학생 ID 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Long> getStudentIdsByParent(Long parentId) {
        return relationRepository.findStudentIdsByParent(parentId);
    }

    /**
     * 특정 권한 확인
     */
    @Transactional(readOnly = true)
    public boolean canViewAttendance(Long parentId, Long studentId) {
        return relationRepository.findByParentAndStudent(parentId, studentId)
                .map(r -> r.getIsActive() && r.getCanViewAttendance())
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean canViewInvoices(Long parentId, Long studentId) {
        return relationRepository.findByParentAndStudent(parentId, studentId)
                .map(r -> r.getIsActive() && r.getCanViewInvoices())
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean canMakeReservations(Long parentId, Long studentId) {
        return relationRepository.findByParentAndStudent(parentId, studentId)
                .map(r -> r.getIsActive() && r.getCanMakeReservations())
                .orElse(false);
    }

    /**
     * 권한 업데이트
     */
    @Transactional
    public ParentStudentRelationResponse updatePermissions(Long relationId, PermissionUpdateRequest request) {
        ParentStudentRelation relation = relationRepository.findById(relationId)
                .orElseThrow(() -> new ResourceNotFoundException("관계를 찾을 수 없습니다"));

        relation.updatePermissions(
                request.getCanViewAttendance(),
                request.getCanViewGrades(),
                request.getCanViewInvoices(),
                request.getCanMakeReservations(),
                request.getCanReceiveMessages()
        );

        log.info("권한 업데이트: relationId={}", relationId);

        return toResponse(relation);
    }

    /**
     * 관계 비활성화
     */
    @Transactional
    public void deactivateRelation(Long relationId) {
        ParentStudentRelation relation = relationRepository.findById(relationId)
                .orElseThrow(() -> new ResourceNotFoundException("관계를 찾을 수 없습니다"));

        relation.deactivate();
        log.info("보호자-학생 관계 비활성화: relationId={}", relationId);
    }

    /**
     * 관계 활성화
     */
    @Transactional
    public void activateRelation(Long relationId) {
        ParentStudentRelation relation = relationRepository.findById(relationId)
                .orElseThrow(() -> new ResourceNotFoundException("관계를 찾을 수 없습니다"));

        relation.activate();
        log.info("보호자-학생 관계 활성화: relationId={}", relationId);
    }

    /**
     * 관계 삭제
     */
    @Transactional
    public void deleteRelation(Long relationId) {
        ParentStudentRelation relation = relationRepository.findById(relationId)
                .orElseThrow(() -> new ResourceNotFoundException("관계를 찾을 수 없습니다"));

        relationRepository.delete(relation);
        log.info("보호자-학생 관계 삭제: relationId={}", relationId);
    }

    /**
     * 접근 권한 검증 (예외 발생)
     */
    public void validateAccess(Long parentId, Long studentId, String action) {
        if (!hasAccessToStudent(parentId, studentId)) {
            throw new BusinessException(String.format(
                    "해당 학생에 대한 %s 권한이 없습니다", action));
        }
    }

    /**
     * Entity -> DTO 변환
     */
    private ParentStudentRelationResponse toResponse(ParentStudentRelation relation) {
        return ParentStudentRelationResponse.builder()
                .id(relation.getId())
                .parentId(relation.getParent().getId())
                .parentName(relation.getParent().getName())
                .parentUsername(relation.getParent().getUsername())
                .studentId(relation.getStudent().getId())
                .studentName(relation.getStudent().getStudentName())
                .relationship(relation.getRelationship())
                .isActive(relation.getIsActive())
                .canViewAttendance(relation.getCanViewAttendance())
                .canViewGrades(relation.getCanViewGrades())
                .canViewInvoices(relation.getCanViewInvoices())
                .canMakeReservations(relation.getCanMakeReservations())
                .canReceiveMessages(relation.getCanReceiveMessages())
                .createdAt(relation.getCreatedAt())
                .build();
    }
}
