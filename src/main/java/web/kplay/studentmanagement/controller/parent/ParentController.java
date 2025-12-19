package web.kplay.studentmanagement.controller.parent;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.dto.parent.ParentStudentRelationRequest;
import web.kplay.studentmanagement.dto.parent.ParentStudentRelationResponse;
import web.kplay.studentmanagement.dto.parent.PermissionUpdateRequest;
import web.kplay.studentmanagement.security.UserDetailsImpl;
import web.kplay.studentmanagement.service.parent.ParentAccessControlService;

import java.util.List;

/**
 * 보호자 관리 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/parents")
@RequiredArgsConstructor
public class ParentController {

    private final ParentAccessControlService parentAccessControlService;

    /**
     * 보호자-학생 관계 생성
     */
    @PostMapping("/relations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ParentStudentRelationResponse> createRelation(
            @Valid @RequestBody ParentStudentRelationRequest request) {
        ParentStudentRelationResponse response = parentAccessControlService.createRelation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 내가 관리하는 학생 목록 조회 (보호자용)
     */
    @GetMapping("/my-students")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<List<ParentStudentRelationResponse>> getMyStudents(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<ParentStudentRelationResponse> responses =
                parentAccessControlService.getStudentsByParent(userDetails.getId());
        return ResponseEntity.ok(responses);
    }

    /**
     * 보호자가 관리하는 학생 목록 조회 (관리자용)
     */
    @GetMapping("/{parentId}/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<ParentStudentRelationResponse>> getStudentsByParent(@PathVariable Long parentId) {
        List<ParentStudentRelationResponse> responses =
                parentAccessControlService.getStudentsByParent(parentId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 학생의 보호자 목록 조회
     */
    @GetMapping("/students/{studentId}/parents")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<ParentStudentRelationResponse>> getParentsByStudent(@PathVariable Long studentId) {
        List<ParentStudentRelationResponse> responses =
                parentAccessControlService.getParentsByStudent(studentId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 권한 업데이트
     */
    @PatchMapping("/relations/{relationId}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ParentStudentRelationResponse> updatePermissions(
            @PathVariable Long relationId,
            @Valid @RequestBody PermissionUpdateRequest request) {
        ParentStudentRelationResponse response =
                parentAccessControlService.updatePermissions(relationId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 관계 비활성화
     */
    @PatchMapping("/relations/{relationId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateRelation(@PathVariable Long relationId) {
        parentAccessControlService.deactivateRelation(relationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 관계 활성화
     */
    @PatchMapping("/relations/{relationId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateRelation(@PathVariable Long relationId) {
        parentAccessControlService.activateRelation(relationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 관계 삭제
     */
    @DeleteMapping("/relations/{relationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRelation(@PathVariable Long relationId) {
        parentAccessControlService.deleteRelation(relationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 접근 권한 확인 (보호자가 특정 학생에 대한 접근 권한이 있는지)
     */
    @GetMapping("/access-check/{studentId}")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<Boolean> checkAccess(
            @PathVariable Long studentId,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        boolean hasAccess = parentAccessControlService.hasAccessToStudent(userDetails.getId(), studentId);
        return ResponseEntity.ok(hasAccess);
    }
}
