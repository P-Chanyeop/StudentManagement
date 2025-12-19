package web.kplay.studentmanagement.service.notice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import web.kplay.studentmanagement.domain.notice.Notice;
import web.kplay.studentmanagement.domain.user.User;
import web.kplay.studentmanagement.dto.notice.NoticeRequest;
import web.kplay.studentmanagement.dto.notice.NoticeResponse;
import web.kplay.studentmanagement.exception.ResourceNotFoundException;
import web.kplay.studentmanagement.repository.NoticeRepository;
import web.kplay.studentmanagement.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;

    /**
     * 공지사항 생성 (XSS 방지)
     */
    @Transactional
    public NoticeResponse createNotice(NoticeRequest request, Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("작성자를 찾을 수 없습니다"));

        // 입력 검증 및 XSS 방지: HTML 특수 문자 이스케이프
        String sanitizedTitle = sanitizeInput(request.getTitle());
        String sanitizedContent = sanitizeInput(request.getContent());

        // 입력 검증: null/빈 문자열 체크
        if (sanitizedTitle == null || sanitizedTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("제목은 필수입니다.");
        }
        if (sanitizedContent == null || sanitizedContent.trim().isEmpty()) {
            throw new IllegalArgumentException("내용은 필수입니다.");
        }

        // 입력 검증: 길이 제한
        if (sanitizedTitle.length() > 200) {
            throw new IllegalArgumentException("제목은 200자를 초과할 수 없습니다.");
        }
        if (sanitizedContent.length() > 50000) {
            throw new IllegalArgumentException("내용은 50000자를 초과할 수 없습니다.");
        }

        Notice notice = Notice.builder()
                .title(sanitizedTitle)
                .content(sanitizedContent)
                .author(author)
                .isPinned(request.getIsPinned() != null ? request.getIsPinned() : false)
                .isActive(true)
                .viewCount(0)
                .build();

        Notice saved = noticeRepository.save(notice);
        log.info("공지사항 생성: 제목={}, 작성자={}", sanitizedTitle, author.getUsername());

        return toResponse(saved);
    }

    /**
     * 입력 문자열 sanitize (XSS 방지)
     */
    private String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        // HTML 특수 문자 이스케이프: <, >, &, ", ' 등
        return HtmlUtils.htmlEscape(input);
    }

    /**
     * 공지사항 조회 (조회수 증가)
     */
    @Transactional
    public NoticeResponse getNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("공지사항을 찾을 수 없습니다"));

        // 조회수 증가
        notice.incrementViewCount();

        return toResponse(notice);
    }

    /**
     * 활성화된 공지사항 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<NoticeResponse> getActiveNotices(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notice> notices = noticeRepository.findActiveNotices(pageable);
        return notices.map(this::toResponse);
    }

    /**
     * 상단 고정 공지사항 조회
     */
    @Transactional(readOnly = true)
    public List<NoticeResponse> getPinnedNotices() {
        List<Notice> notices = noticeRepository.findPinnedNotices();
        return notices.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 공지사항 검색
     */
    @Transactional(readOnly = true)
    public Page<NoticeResponse> searchNotices(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notice> notices = noticeRepository.searchNotices(keyword, pageable);
        return notices.map(this::toResponse);
    }

    /**
     * 공지사항 수정
     */
    @Transactional
    public NoticeResponse updateNotice(Long id, NoticeRequest request) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("공지사항을 찾을 수 없습니다"));

        notice.updateNotice(
                request.getTitle(),
                request.getContent(),
                request.getIsPinned()
        );

        log.info("공지사항 수정: id={}, 제목={}", id, request.getTitle());
        return toResponse(notice);
    }

    /**
     * 상단 고정 토글
     */
    @Transactional
    public NoticeResponse togglePin(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("공지사항을 찾을 수 없습니다"));

        notice.togglePin();
        log.info("공지사항 고정 토글: id={}, 고정={}", id, notice.getIsPinned());

        return toResponse(notice);
    }

    /**
     * 공지사항 삭제 (비활성화)
     */
    @Transactional
    public void deleteNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("공지사항을 찾을 수 없습니다"));

        notice.deactivate();
        log.info("공지사항 삭제: id={}", id);
    }

    /**
     * Notice를 NoticeResponse로 변환
     */
    private NoticeResponse toResponse(Notice notice) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .authorId(notice.getAuthor() != null ? notice.getAuthor().getId() : null)
                .authorName(notice.getAuthor() != null ? notice.getAuthor().getUsername() : null)
                .isPinned(notice.getIsPinned())
                .isActive(notice.getIsActive())
                .viewCount(notice.getViewCount())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .build();
    }
}
