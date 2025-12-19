package web.kplay.studentmanagement.controller.notice;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import web.kplay.studentmanagement.dto.notice.NoticeRequest;
import web.kplay.studentmanagement.dto.notice.NoticeResponse;
import web.kplay.studentmanagement.security.UserDetailsImpl;
import web.kplay.studentmanagement.service.notice.NoticeService;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 공지사항 생성
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<NoticeResponse> createNotice(
            @Valid @RequestBody NoticeRequest request,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        NoticeResponse response = noticeService.createNotice(request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 공지사항 조회 (상세)
     */
    @GetMapping("/{id}")
    public ResponseEntity<NoticeResponse> getNotice(@PathVariable Long id) {
        NoticeResponse response = noticeService.getNotice(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 활성화된 공지사항 목록 (페이징)
     */
    @GetMapping
    public ResponseEntity<Page<NoticeResponse>> getActiveNotices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<NoticeResponse> responses = noticeService.getActiveNotices(page, size);
        return ResponseEntity.ok(responses);
    }

    /**
     * 상단 고정 공지사항 조회
     */
    @GetMapping("/pinned")
    public ResponseEntity<List<NoticeResponse>> getPinnedNotices() {
        List<NoticeResponse> responses = noticeService.getPinnedNotices();
        return ResponseEntity.ok(responses);
    }

    /**
     * 공지사항 검색
     */
    @GetMapping("/search")
    public ResponseEntity<Page<NoticeResponse>> searchNotices(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<NoticeResponse> responses = noticeService.searchNotices(keyword, page, size);
        return ResponseEntity.ok(responses);
    }

    /**
     * 공지사항 수정
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<NoticeResponse> updateNotice(
            @PathVariable Long id,
            @Valid @RequestBody NoticeRequest request) {
        NoticeResponse response = noticeService.updateNotice(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 상단 고정 토글
     */
    @PatchMapping("/{id}/toggle-pin")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<NoticeResponse> togglePin(@PathVariable Long id) {
        NoticeResponse response = noticeService.togglePin(id);
        return ResponseEntity.ok(response);
    }

    /**
     * 공지사항 삭제
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return ResponseEntity.noContent().build();
    }
}
