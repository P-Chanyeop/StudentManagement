import React, { useState, useEffect } from 'react';
import axios from 'axios';
import '../styles/MyQuizScores.css';

const MyQuizScores = () => {
  const [children, setChildren] = useState([]);
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [quizData, setQuizData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 5;

  // Axios 인터셉터로 토큰 자동 추가
  axios.interceptors.request.use(
    (config) => {
      const token = localStorage.getItem('accessToken');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    },
    (error) => Promise.reject(error)
  );

  useEffect(() => {
    fetchChildren();
  }, []);

  const fetchChildren = async () => {
    try {
      const response = await axios.get('/api/students/my-students');
      setChildren(response.data);
    } catch (err) {
      console.error('자녀 목록 조회 실패:', err);
      setError('자녀 목록을 불러올 수 없습니다.');
    }
  };

  const fetchQuizData = async (studentId, studentName) => {
    setLoading(true);
    setError(null);
    setSelectedStudent(studentName);

    try {
      const response = await axios.get(`/api/quiz/student/${studentId}`);
      setQuizData(response.data);
      setCurrentPage(1);
    } catch (err) {
      console.error('퀴즈 데이터 조회 실패:', err);
      setError(err.response?.data?.message || '퀴즈 데이터를 불러올 수 없습니다.');
      setQuizData([]);
    } finally {
      setLoading(false);
    }
  };

  const getScoreBadgeClass = (score) => {
    if (score >= 90) return 'score-excellent';
    if (score >= 70) return 'score-good';
    if (score >= 50) return 'score-fair';
    return 'score-poor';
  };

  const stats = quizData.length > 0 ? {
    total: quizData.length,
    avg: (quizData.reduce((sum, q) => sum + (parseInt(q.percentCorrect) || 0), 0) / quizData.length).toFixed(1),
    max: Math.max(...quizData.map(q => parseInt(q.percentCorrect) || 0))
  } : { total: 0, avg: 0, max: 0 };

  const totalPages = Math.ceil(quizData.length / itemsPerPage);
  const paginatedQuizData = quizData.slice((currentPage - 1) * itemsPerPage, currentPage * itemsPerPage);

  return (
    <div className="my-quiz-scores">
      <div className="quiz-page-header">
        <div className="header-content">
          <h1><i className="fas fa-book-reader"></i> 영어 퀴즈 성적</h1>
          <p>자녀의 르네상스 퀴즈 기록을 확인하세요</p>
        </div>
      </div>

      <div className="student-selection-card">
        {children.length > 0 ? (
          <div className="student-list">
            {children.map(child => (
              <div
                key={child.id}
                className={`student-item ${selectedStudent === child.studentName ? 'selected' : ''} ${!child.renaissanceUsername ? 'disabled' : ''}`}
              >
                <div className="student-main">
                  <div className="student-avatar-large">
                    {child.studentName.charAt(0)}
                  </div>
                  <div className="student-details">
                    <h3>{child.studentName}</h3>
                    <div className="student-meta">
                      {child.englishLevel && <span className="level-tag">레벨: {child.englishLevel}</span>}
                      {child.renaissanceUsername ? (
                        <span className="status-tag active">✓ 르네상스 ID 등록됨</span>
                      ) : (
                        <span className="status-tag inactive">⚠ ID 미등록</span>
                      )}
                    </div>
                  </div>
                </div>
                <button
                  className="quiz-view-btn"
                  onClick={() => fetchQuizData(child.id, child.studentName)}
                  disabled={!child.renaissanceUsername}
                >
                  {selectedStudent === child.studentName ? (
                    <>
                      <span className="btn-icon">✓</span>
                      <span>선택됨</span>
                    </>
                  ) : (
                    <>
                      <span className="btn-icon"><i className="fas fa-chart-bar"></i></span>
                      <span>퀴즈 점수 보기</span>
                    </>
                  )}
                </button>
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-state">
            <div className="empty-icon"><i className="fas fa-child"></i></div>
            <p>등록된 자녀가 없습니다</p>
          </div>
        )}
      </div>

      {loading && (
        <div className="loading-card">
          <div className="spinner"></div>
          <p>퀴즈 데이터를 불러오는 중...</p>
        </div>
      )}

      {error && !loading && (
        <div className="error-card">
          <div className="error-icon"><i className="fas fa-exclamation-triangle"></i></div>
          <p>{error}</p>
        </div>
      )}

      {!loading && selectedStudent && quizData.length > 0 && (
        <>
          <div className="selected-student-banner">
            <i className="fas fa-user-graduate student-emoji"></i>
            <span className="student-name-display">{selectedStudent}</span>
            <span className="student-label">학생의 퀴즈 기록</span>
          </div>

          {quizData[0].yearSummary && Object.keys(quizData[0].yearSummary).length > 0 && (
            <div className="year-summary-card">
              <h3><i className="fas fa-chart-bar"></i> 연간 통계 요약</h3>
              <div className="year-summary-grid">
                {Object.entries(quizData[0].yearSummary).map(([key, value]) => (
                  <div key={key} className="summary-box">
                    <div className="summary-label">{key}</div>
                    <div className="summary-value">{value}</div>
                  </div>
                ))}
              </div>
            </div>
          )}

          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-icon"><i className="fas fa-clipboard-list"></i></div>
              <div className="stat-content">
                <div className="stat-value">{stats.total}개</div>
                <div className="stat-label">총 퀴즈 수</div>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-icon"><i className="fas fa-chart-line"></i></div>
              <div className="stat-content">
                <div className="stat-value">{stats.avg}%</div>
                <div className="stat-label">평균 점수</div>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-icon"><i className="fas fa-trophy"></i></div>
              <div className="stat-content">
                <div className="stat-value">{stats.max}%</div>
                <div className="stat-label">최고 점수</div>
              </div>
            </div>
          </div>

          {paginatedQuizData.map((quiz, index) => (
            <div key={index} className="quiz-detail-card">
              <div className="quiz-card-header">
                <div className="quiz-title-section">
                  <h3><i className="fas fa-book"></i> {quiz.bookTitle || '-'}</h3>
                  <p className="quiz-author"><i className="fas fa-pen"></i> {quiz.author || '-'}</p>
                </div>
                <div className="quiz-date">
                  <i className="fas fa-calendar-alt"></i> {quiz.quizDate || '-'}
                </div>
              </div>

              <div className="quiz-card-body">
                <div className="info-section">
                  <h4><i className="fas fa-info-circle"></i> 책 정보</h4>
                  <div className="info-grid">
                    <div className="info-item">
                      <span className="info-label">난이도</span>
                      <span className="info-value">{quiz.atosLevel || '-'}</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">퀴즈 번호</span>
                      <span className="info-value">{quiz.quizNumber || '-'}</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">관심 레벨</span>
                      <span className="info-value">{quiz.interestLevel || '-'}</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">TWI</span>
                      <span className="info-value">{quiz.twi || '-'}</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">유형</span>
                      <span className="info-value">{quiz.type || '-'}</span>
                    </div>
                    <div className="info-item">
                      <span className="info-label">단어 수</span>
                      <span className="info-value">{quiz.wordCount || '-'}</span>
                    </div>
                  </div>
                </div>

                <div className="info-section">
                  <h4><i className="fas fa-check-circle"></i> 퀴즈 결과</h4>
                  <div className="results-grid">
                    <div className="result-item highlight">
                      <span className="result-label">결과</span>
                      <span className="result-value">{quiz.quizResult || '-'}</span>
                    </div>
                    <div className="result-item">
                      <span className="result-label">정답률</span>
                      <span className={`score-badge ${getScoreBadgeClass(parseInt(quiz.percentCorrect) || 0)}`}>
                        {quiz.percentCorrect || '-'}
                      </span>
                    </div>
                    <div className="result-item">
                      <span className="result-label">획득 포인트</span>
                      <span className="result-value points">{quiz.pointsEarned || '-'}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))}

          {totalPages > 1 && (
            <div className="pagination">
              <button 
                className="page-btn" 
                onClick={() => setCurrentPage(p => p - 1)} 
                disabled={currentPage === 1}
              >
                <i className="fas fa-chevron-left"></i> 이전
              </button>
              <span className="page-info">{currentPage} / {totalPages}</span>
              <button 
                className="page-btn" 
                onClick={() => setCurrentPage(p => p + 1)} 
                disabled={currentPage === totalPages}
              >
                다음 <i className="fas fa-chevron-right"></i>
              </button>
            </div>
          )}
        </>
      )}

      {!loading && selectedStudent && quizData.length === 0 && !error && (
        <div className="empty-state-card">
          <div className="empty-icon"><i className="fas fa-inbox"></i></div>
          <h3>퀴즈 기록이 없습니다</h3>
          <p>아직 완료한 퀴즈가 없어요</p>
        </div>
      )}
    </div>
  );
};

export default MyQuizScores;
