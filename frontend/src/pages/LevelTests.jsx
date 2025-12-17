import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { levelTestAPI, studentAPI } from '../services/api';
import '../styles/LevelTests.css';

function LevelTests() {
  const queryClient = useQueryClient();
  const [dateRange, setDateRange] = useState({
    start: new Date().toISOString().split('T')[0],
    end: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
  });
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showCompleteModal, setShowCompleteModal] = useState(false);
  const [selectedTest, setSelectedTest] = useState(null);
  const [newTest, setNewTest] = useState({
    studentId: '',
    testDate: new Date().toISOString().split('T')[0],
    testTime: '10:00',
    notes: '',
  });
  const [testResult, setTestResult] = useState({
    score: 0,
    listeningScore: 0,
    speakingScore: 0,
    readingScore: 0,
    writingScore: 0,
    recommendedLevel: 'BEGINNER',
    feedback: '',
  });

  // 날짜 범위로 레벨 테스트 조회
  const { data: levelTests = [], isLoading } = useQuery({
    queryKey: ['levelTests', dateRange.start, dateRange.end],
    queryFn: async () => {
      const response = await levelTestAPI.getByRange(dateRange.start, dateRange.end);
      return response.data;
    },
  });

  // 학생 목록 조회
  const { data: students = [] } = useQuery({
    queryKey: ['students'],
    queryFn: async () => {
      const response = await studentAPI.getAll();
      return response.data;
    },
  });

  // 레벨 테스트 생성 mutation
  const createMutation = useMutation({
    mutationFn: (data) => levelTestAPI.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries(['levelTests']);
      setShowCreateModal(false);
      setNewTest({
        studentId: '',
        testDate: new Date().toISOString().split('T')[0],
        testTime: '10:00',
        notes: '',
      });
      alert('레벨 테스트가 예약되었습니다.');
    },
    onError: (error) => {
      alert(`예약 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    },
  });

  // 레벨 테스트 완료 mutation
  const completeMutation = useMutation({
    mutationFn: ({ id, data }) => levelTestAPI.complete(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries(['levelTests']);
      setShowCompleteModal(false);
      setSelectedTest(null);
      alert('레벨 테스트가 완료되었습니다.');
    },
    onError: (error) => {
      alert(`완료 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    },
  });

  const handleCreateTest = () => {
    if (!newTest.studentId || !newTest.testDate) {
      alert('학생과 테스트 날짜를 선택해주세요.');
      return;
    }

    const testDateTime = `${newTest.testDate}T${newTest.testTime}:00`;
    createMutation.mutate({
      ...newTest,
      testDateTime,
    });
  };

  const handleCompleteTest = () => {
    // 총점 자동 계산
    const calculatedScore = Math.round(
      (testResult.listeningScore +
        testResult.speakingScore +
        testResult.readingScore +
        testResult.writingScore) /
        4
    );

    if (calculatedScore < 0 || calculatedScore > 100) {
      alert('점수는 0-100 사이여야 합니다.');
      return;
    }

    completeMutation.mutate({
      id: selectedTest.id,
      data: {
        ...testResult,
        score: calculatedScore,
      },
    });
  };

  const openCompleteModal = (test) => {
    setSelectedTest(test);
    setTestResult({
      score: 0,
      listeningScore: 0,
      speakingScore: 0,
      readingScore: 0,
      writingScore: 0,
      recommendedLevel: 'BEGINNER',
      feedback: '',
    });
    setShowCompleteModal(true);
  };

  // 상태별 배지
  const getStatusBadge = (status) => {
    const statusMap = {
      SCHEDULED: { text: '예정', color: '#0066FF' },
      COMPLETED: { text: '완료', color: '#03C75A' },
      CANCELLED: { text: '취소', color: '#999' },
      NO_SHOW: { text: '노쇼', color: '#FF3B30' },
    };
    const { text, color } = statusMap[status] || { text: status, color: '#999' };
    return <span className="status-badge" style={{ backgroundColor: color }}>{text}</span>;
  };

  // 레벨별 배지
  const getLevelBadge = (level) => {
    const levelMap = {
      BEGINNER: { text: '초급', color: '#03C75A' },
      INTERMEDIATE: { text: '중급', color: '#0066FF' },
      ADVANCED: { text: '고급', color: '#FF9800' },
      EXPERT: { text: '전문가', color: '#9C27B0' },
    };
    const { text, color } = levelMap[level] || { text: level, color: '#999' };
    return <span className="level-badge" style={{ backgroundColor: color }}>{text}</span>;
  };

  if (isLoading) {
    return <div className="level-tests-container">로딩 중...</div>;
  }

  return (
    <div className="level-tests-container">
      <div className="level-tests-header">
        <h1>레벨 테스트 관리</h1>
        <button className="btn-create-test" onClick={() => setShowCreateModal(true)}>
          + 레벨 테스트 예약
        </button>
      </div>

      <div className="level-tests-filters">
        <div className="date-range">
          <label>시작일:</label>
          <input
            type="date"
            value={dateRange.start}
            onChange={(e) => setDateRange({ ...dateRange, start: e.target.value })}
          />
          <label>종료일:</label>
          <input
            type="date"
            value={dateRange.end}
            onChange={(e) => setDateRange({ ...dateRange, end: e.target.value })}
          />
        </div>
        <span className="result-count">총 {levelTests.length}건</span>
      </div>

      <div className="level-tests-grid">
        {levelTests.length === 0 ? (
          <div className="empty-state">등록된 레벨 테스트가 없습니다.</div>
        ) : (
          levelTests.map((test) => (
            <div key={test.id} className="test-card">
              <div className="test-header">
                <h3>{test.studentName}</h3>
                {getStatusBadge(test.status)}
              </div>

              <div className="test-details">
                <div className="detail-row">
                  <span className="icon">📅</span>
                  <span className="label">일시:</span>
                  <span className="value">
                    {new Date(test.testDateTime).toLocaleString('ko-KR')}
                  </span>
                </div>

                {test.status === 'COMPLETED' && (
                  <>
                    <div className="detail-row">
                      <span className="icon">📊</span>
                      <span className="label">총점:</span>
                      <span className="value score">{test.score}점</span>
                    </div>
                    <div className="detail-row">
                      <span className="icon">🎯</span>
                      <span className="label">권장 레벨:</span>
                      {getLevelBadge(test.recommendedLevel)}
                    </div>
                    <div className="scores-grid">
                      <div className="score-item">
                        <span className="score-label">듣기</span>
                        <span className="score-value">{test.listeningScore}</span>
                      </div>
                      <div className="score-item">
                        <span className="score-label">말하기</span>
                        <span className="score-value">{test.speakingScore}</span>
                      </div>
                      <div className="score-item">
                        <span className="score-label">읽기</span>
                        <span className="score-value">{test.readingScore}</span>
                      </div>
                      <div className="score-item">
                        <span className="score-label">쓰기</span>
                        <span className="score-value">{test.writingScore}</span>
                      </div>
                    </div>
                    {test.feedback && (
                      <div className="feedback">
                        <strong>피드백:</strong>
                        <p>{test.feedback}</p>
                      </div>
                    )}
                  </>
                )}

                {test.notes && (
                  <div className="detail-row">
                    <span className="icon">📝</span>
                    <span className="label">메모:</span>
                    <span className="value">{test.notes}</span>
                  </div>
                )}
              </div>

              {test.status === 'SCHEDULED' && (
                <div className="test-actions">
                  <button className="btn-complete" onClick={() => openCompleteModal(test)}>
                    결과 입력
                  </button>
                </div>
              )}
            </div>
          ))
        )}
      </div>

      {/* 레벨 테스트 예약 모달 */}
      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>레벨 테스트 예약</h2>
              <button className="modal-close" onClick={() => setShowCreateModal(false)}>
                ×
              </button>
            </div>

            <div className="modal-body">
              <div className="form-group">
                <label>학생 선택 *</label>
                <select
                  value={newTest.studentId}
                  onChange={(e) => setNewTest({ ...newTest, studentId: e.target.value })}
                >
                  <option value="">학생을 선택하세요</option>
                  {students.map((student) => (
                    <option key={student.id} value={student.id}>
                      {student.studentName} ({student.studentPhone})
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>테스트 날짜 *</label>
                  <input
                    type="date"
                    value={newTest.testDate}
                    onChange={(e) => setNewTest({ ...newTest, testDate: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label>테스트 시간 *</label>
                  <input
                    type="time"
                    value={newTest.testTime}
                    onChange={(e) => setNewTest({ ...newTest, testTime: e.target.value })}
                  />
                </div>
              </div>

              <div className="form-group">
                <label>메모</label>
                <textarea
                  value={newTest.notes}
                  onChange={(e) => setNewTest({ ...newTest, notes: e.target.value })}
                  placeholder="추가 메모 사항을 입력하세요"
                  rows="3"
                />
              </div>
            </div>

            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setShowCreateModal(false)}>
                취소
              </button>
              <button className="btn-primary" onClick={handleCreateTest}>
                예약
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 레벨 테스트 완료 모달 */}
      {showCompleteModal && selectedTest && (
        <div className="modal-overlay" onClick={() => setShowCompleteModal(false)}>
          <div className="modal-content large" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>레벨 테스트 결과 입력 - {selectedTest.studentName}</h2>
              <button className="modal-close" onClick={() => setShowCompleteModal(false)}>
                ×
              </button>
            </div>

            <div className="modal-body">
              <div className="form-row">
                <div className="form-group">
                  <label>듣기 점수 *</label>
                  <input
                    type="number"
                    value={testResult.listeningScore}
                    onChange={(e) =>
                      setTestResult({
                        ...testResult,
                        listeningScore: parseInt(e.target.value) || 0,
                      })
                    }
                    min="0"
                    max="100"
                  />
                </div>

                <div className="form-group">
                  <label>말하기 점수 *</label>
                  <input
                    type="number"
                    value={testResult.speakingScore}
                    onChange={(e) =>
                      setTestResult({
                        ...testResult,
                        speakingScore: parseInt(e.target.value) || 0,
                      })
                    }
                    min="0"
                    max="100"
                  />
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>읽기 점수 *</label>
                  <input
                    type="number"
                    value={testResult.readingScore}
                    onChange={(e) =>
                      setTestResult({
                        ...testResult,
                        readingScore: parseInt(e.target.value) || 0,
                      })
                    }
                    min="0"
                    max="100"
                  />
                </div>

                <div className="form-group">
                  <label>쓰기 점수 *</label>
                  <input
                    type="number"
                    value={testResult.writingScore}
                    onChange={(e) =>
                      setTestResult({
                        ...testResult,
                        writingScore: parseInt(e.target.value) || 0,
                      })
                    }
                    min="0"
                    max="100"
                  />
                </div>
              </div>

              <div className="form-group">
                <label>총점 (자동 계산)</label>
                <input
                  type="number"
                  value={Math.round(
                    (testResult.listeningScore +
                      testResult.speakingScore +
                      testResult.readingScore +
                      testResult.writingScore) /
                      4
                  )}
                  readOnly
                  disabled
                />
              </div>

              <div className="form-group">
                <label>권장 레벨 *</label>
                <select
                  value={testResult.recommendedLevel}
                  onChange={(e) =>
                    setTestResult({ ...testResult, recommendedLevel: e.target.value })
                  }
                >
                  <option value="BEGINNER">초급</option>
                  <option value="INTERMEDIATE">중급</option>
                  <option value="ADVANCED">고급</option>
                  <option value="EXPERT">전문가</option>
                </select>
              </div>

              <div className="form-group">
                <label>피드백</label>
                <textarea
                  value={testResult.feedback}
                  onChange={(e) =>
                    setTestResult({ ...testResult, feedback: e.target.value })
                  }
                  placeholder="학생에 대한 피드백을 입력하세요"
                  rows="5"
                />
              </div>
            </div>

            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setShowCompleteModal(false)}>
                취소
              </button>
              <button className="btn-primary" onClick={handleCompleteTest}>
                완료
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default LevelTests;
