import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import Layout from '../components/Layout';
import LoadingSpinner from '../components/LoadingSpinner';
import { noticeAPI } from '../services/api';
import '../styles/Notices.css';

function Notices() {
  const [searchKeyword, setSearchKeyword] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const [selectedNotice, setSelectedNotice] = useState(null);
  const pageSize = 10;

  // 상단 고정 공지 조회
  const { data: pinnedNotices } = useQuery({
    queryKey: ['notices', 'pinned'],
    queryFn: async () => {
      const response = await noticeAPI.getPinned();
      return response.data;
    },
  });

  // 공지사항 목록 조회
  const { data: noticesPage, isLoading } = useQuery({
    queryKey: ['notices', currentPage, searchKeyword],
    queryFn: async () => {
      if (searchKeyword) {
        const response = await noticeAPI.search(searchKeyword, currentPage, pageSize);
        return response.data;
      } else {
        const response = await noticeAPI.getAll(currentPage, pageSize);
        return response.data;
      }
    },
  });

  const handleSearch = (e) => {
    e.preventDefault();
    setCurrentPage(0);
  };

  const handleNoticeClick = async (noticeId) => {
    try {
      const response = await noticeAPI.getById(noticeId);
      setSelectedNotice(response.data);
    } catch (error) {
      console.error('공지사항 조회 실패:', error);
      alert('공지사항을 불러올 수 없습니다.');
    }
  };

  const formatDate = (datetime) => {
    if (!datetime) return '';
    const date = new Date(datetime);
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  const formatDateTime = (datetime) => {
    if (!datetime) return '';
    const date = new Date(datetime);
    return date.toLocaleString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <Layout>
      <div className="notices-page">
        <div className="page-header">
          <h1 className="page-title"><i className="fas fa-bullhorn"></i> 공지사항</h1>
          <p className="page-subtitle">학원의 중요한 소식을 확인하세요</p>
        </div>

        {/* 검색 */}
        <div className="search-section">
          <form onSubmit={handleSearch} className="search-form">
            <input
              type="text"
              placeholder="제목이나 내용으로 검색..."
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              className="search-input"
            />
            <button type="submit" className="search-button">
              <i className="fas fa-search"></i> 검색
            </button>
            {searchKeyword && (
              <button
                type="button"
                onClick={() => {
                  setSearchKeyword('');
                  setCurrentPage(0);
                }}
                className="clear-button"
              >
                초기화
              </button>
            )}
          </form>
        </div>

        {/* 상단 고정 공지 */}
        {pinnedNotices && pinnedNotices.length > 0 && (
          <div className="pinned-section">
            <h2 className="section-title">📌 중요 공지</h2>
            <div className="pinned-notices">
              {pinnedNotices.map((notice) => (
                <div
                  key={notice.id}
                  className="pinned-notice-card"
                  onClick={() => handleNoticeClick(notice.id)}
                >
                  <div className="notice-header">
                    <span className="pinned-badge">📌 공지</span>
                    <h3 className="notice-title">{notice.title}</h3>
                  </div>
                  <div className="notice-meta">
                    <span className="notice-author">{notice.authorName}</span>
                    <span className="notice-date">{formatDate(notice.createdAt)}</span>
                    <span className="notice-views"><i className="fas fa-eye"></i> {notice.viewCount}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* 공지사항 목록 */}
        <div className="notices-section">
          <h2 className="section-title"><i className="fas fa-clipboard-list"></i> 전체 공지</h2>
          {isLoading ? (
            <LoadingSpinner />
          ) : noticesPage && noticesPage.content && noticesPage.content.length > 0 ? (
            <>
              <div className="notice-list">
                {noticesPage.content.map((notice) => (
                  <div
                    key={notice.id}
                    className="notice-item"
                    onClick={() => handleNoticeClick(notice.id)}
                  >
                    <div className="notice-main">
                      <h4 className="notice-item-title">
                        {notice.isPinned && <span className="pin-icon">📌</span>}
                        {notice.title}
                      </h4>
                      <p className="notice-preview">
                        {notice.content.length > 100
                          ? notice.content.substring(0, 100) + '...'
                          : notice.content}
                      </p>
                    </div>
                    <div className="notice-info">
                      <span className="notice-author">{notice.authorName}</span>
                      <span className="notice-date">{formatDate(notice.createdAt)}</span>
                      <span className="notice-views">조회 {notice.viewCount}</span>
                    </div>
                  </div>
                ))}
              </div>

              {/* 페이지네이션 */}
              <div className="pagination">
                <button
                  onClick={() => setCurrentPage(Math.max(0, currentPage - 1))}
                  disabled={currentPage === 0}
                  className="page-button"
                >
                  이전
                </button>
                <span className="page-info">
                  {currentPage + 1} / {noticesPage.totalPages || 1}
                </span>
                <button
                  onClick={() => setCurrentPage(currentPage + 1)}
                  disabled={currentPage >= (noticesPage.totalPages || 1) - 1}
                  className="page-button"
                >
                  다음
                </button>
              </div>
            </>
          ) : (
            <div className="empty-state">
              <p>공지사항이 없습니다</p>
            </div>
          )}
        </div>

        {/* 공지사항 상세 모달 */}
        {selectedNotice && (
          <div className="modal-overlay" onClick={() => setSelectedNotice(null)}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
              <div className="modal-header">
                <h2 className="modal-title">{selectedNotice.title}</h2>
                <button
                  className="modal-close"
                  onClick={() => setSelectedNotice(null)}
                >
                  ✕
                </button>
              </div>
              <div className="modal-meta">
                <span className="meta-item">
                  작성자: {selectedNotice.authorName}
                </span>
                <span className="meta-item">
                  작성일: {formatDateTime(selectedNotice.createdAt)}
                </span>
                <span className="meta-item">
                  조회수: {selectedNotice.viewCount}
                </span>
              </div>
              <div className="modal-body">
                <p className="notice-content">{selectedNotice.content}</p>
              </div>
              <div className="modal-footer">
                <button
                  className="close-button"
                  onClick={() => setSelectedNotice(null)}
                >
                  닫기
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}

export default Notices;
