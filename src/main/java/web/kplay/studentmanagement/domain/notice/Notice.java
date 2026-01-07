package web.kplay.studentmanagement.domain.notice;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.web.util.HtmlUtils;
import web.kplay.studentmanagement.domain.BaseEntity;
import web.kplay.studentmanagement.domain.user.User;

@Entity
@Table(name = "notices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPinned = false; // 상단 고정 여부

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true; // 활성화 여부

    @Column
    @Builder.Default
    private Integer viewCount = 0; // 조회수

    // 조회수 증가
    public void incrementViewCount() {
        this.viewCount++;
    }

    // 공지사항 수정 (입력 검증 및 XSS 방지)
    public void updateNotice(String title, String content, Boolean isPinned) {
        // 입력 검증: null/빈 문자열 체크
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("제목은 필수입니다.");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("내용은 필수입니다.");
        }

        // 입력 검증: 길이 제한
        if (title.length() > 200) {
            throw new IllegalArgumentException("제목은 200자를 초과할 수 없습니다.");
        }
        if (content.length() > 50000) {
            throw new IllegalArgumentException("내용은 50000자를 초과할 수 없습니다.");
        }

        // XSS 방지: HTML 특수 문자 이스케이프
        // 참고: 실제 서비스에서는 프론트엔드에서도 추가 검증 필요
        this.title = sanitizeInput(title);
        this.content = sanitizeInput(content);
        this.isPinned = isPinned != null ? isPinned : false;
    }

    /**
     * 입력 문자열 sanitize (XSS 방지)
     * HTML 특수 문자를 escape하여 스크립트 실행 방지
     */
    private String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        // HTML 특수 문자 이스케이프: <, >, &, ", ' 등
        return HtmlUtils.htmlEscape(input);
    }

    // 상단 고정 설정/해제
    public void togglePin() {
        this.isPinned = !this.isPinned;
    }

    // 공지사항 비활성화 (삭제)
    public void deactivate() {
        this.isActive = false;
    }

    // 공지사항 활성화
    public void activate() {
        this.isActive = true;
    }
}
