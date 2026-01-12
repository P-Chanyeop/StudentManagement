package web.kplay.studentmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import web.kplay.studentmanagement.domain.notice.NoticeView;

import java.util.List;
import java.util.Optional;

public interface NoticeViewRepository extends JpaRepository<NoticeView, Long> {
    
    Optional<NoticeView> findByNoticeIdAndUserId(Long noticeId, Long userId);
    
    @Query("SELECT nv FROM NoticeView nv " +
           "JOIN FETCH nv.user u " +
           "WHERE nv.notice.id = :noticeId " +
           "ORDER BY nv.createdAt DESC")
    List<NoticeView> findByNoticeIdWithUser(@Param("noticeId") Long noticeId);
    
    long countByNoticeId(Long noticeId);
}
