package web.kplay.studentmanagement.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import web.kplay.studentmanagement.domain.notice.Notice;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 활성화된 공지사항 조회 (상단 고정 + 일반 공지사항)
    @Query("SELECT n FROM Notice n WHERE n.isActive = true ORDER BY n.isPinned DESC, n.createdAt DESC")
    Page<Notice> findActiveNotices(Pageable pageable);

    // 상단 고정 공지사항 조회
    @Query("SELECT n FROM Notice n WHERE n.isActive = true AND n.isPinned = true ORDER BY n.createdAt DESC")
    List<Notice> findPinnedNotices();

    // 제목 또는 내용 검색
    @Query("SELECT n FROM Notice n WHERE n.isActive = true AND (n.title LIKE %:keyword% OR n.content LIKE %:keyword%) ORDER BY n.isPinned DESC, n.createdAt DESC")
    Page<Notice> searchNotices(String keyword, Pageable pageable);
}
