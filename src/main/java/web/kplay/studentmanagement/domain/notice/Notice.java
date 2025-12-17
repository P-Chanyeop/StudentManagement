package web.kplay.studentmanagement.domain.notice;

import jakarta.persistence.*;
import lombok.*;
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
    private Boolean isPinned = false; // 상단 고정 여부

    @Column(nullable = false)
    private Boolean isActive = true; // 활성화 여부

    @Column
    private Integer viewCount = 0; // 조회수

    // 조회수 증가
    public void incrementViewCount() {
        this.viewCount++;
    }

    // 공지사항 수정
    public void updateNotice(String title, String content, Boolean isPinned) {
        this.title = title;
        this.content = content;
        this.isPinned = isPinned != null ? isPinned : false;
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
