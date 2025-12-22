import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import LoadingSpinner from '../components/LoadingSpinner';
import { levelTestAPI, studentAPI } from '../services/api';
import '../styles/LevelTests.css';

function LevelTests() {
  const queryClient = useQueryClient();
  const [dateRange, setDateRange] = useState({
    start: new Date().toISOString().split('T')[0],
    end: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
  });
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newTest, setNewTest] = useState({
    studentId: '',
    testDate: new Date().toISOString().split('T')[0],
    testTime: '10:00',
    memo: '',
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
        memo: '',
      });
      alert('레벨 테스트가 예약되었습니다.\n문자 알림이 자동으로 발송됩니다.');
    },
    onError: (error) => {
      alert(`예약 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
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

  // 상태별 배지
  const getStatusBadge = (status) => {
    const statusMap = {
      SCHEDULED: { text: '예정', color: '#0066FF' },
      COMPLETED: { text: '완료', color: '#03C75A' },
      CANCELLED: { text: '취소', color: '#999' },
    };
    const { text, color } = statusMap[status] || { text: status, color: '#999' };
    return <span className="status-badge" style={{ backgroundColor: color }}>{text}</span>;
  };

  // 테스트 날짜별로 그룹화
  const groupedTests = levelTests.reduce((acc, test) => {
    const date = test.testDate;
    if (!acc[date]) {
      acc[date] = [];
    }
    acc[date].push(test);
    return acc;
  }, {});

  // 날짜 정렬
  const sortedDates = Object.keys(groupedTests).sort((a, b) => new Date(a) - new Date(b));

  if (isLoading) {
    return (
      <div className="page-wrapper">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="page-wrapper">
      <div className="page-header">
        <div className="page-header-content">
          <div className="page-title-section">
            <h1 className="page-title">
              <i className="fas fa-clipboard-list"></i>
              레벨 테스트 일정 관리
            </h1>
            <p className="page-subtitle">학생들의 레벨 테스트 일정을 관리합니다</p>
          </div>
          <button className="btn-primary" onClick={() => setShowCreateModal(true)}>
            <i className="fas fa-plus"></i> 레벨 테스트 예약
          </button>
        </div>
      </div>

      <div className="page-content">
        <div className="search-section">
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
          <div className="result-count">
            <i className="fas fa-clipboard-list"></i>
            총 <strong>{levelTests.length}</strong>건
          </div>
        </div>

        <div className="calendar-view">
          {sortedDates.length === 0 ? (
            <div className="empty-state">
              <p><i className="fas fa-calendar-alt"></i> 등록된 레벨 테스트 일정이 없습니다.</p>
              <p className="empty-subtitle">위의 버튼을 눌러 새로운 테스트를 예약하세요.</p>
            </div>
          ) : (
          sortedDates.map((date) => (
            <div key={date} className="date-section">
              <div className="date-header">
                <h2>
                  {new Date(date).toLocaleDateString('ko-KR', {
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                    weekday: 'short',
                  })}
                </h2>
                <span className="date-count">{groupedTests[date].length}건</span>
              </div>

              <div className="tests-list">
                {groupedTests[date]
                  .sort((a, b) => a.testTime.localeCompare(b.testTime))
                  .map((test) => (
                    <div key={test.id} className="test-item">
                      <div className="test-time">
                        <span className="time-icon">🕐</span>
                        <span className="time-text">{test.testTime}</span>
                      </div>

                      <div className="test-content">
                        <div className="test-main">
                          <div className="student-info">
                            <h3>{test.studentName}</h3>
                            {getStatusBadge(test.testStatus)}
                          </div>
                          {test.memo && (
                            <div className="test-memo">
                              <span className="memo-icon"><i className="fas fa-edit"></i></span>
                              <span>{test.memo}</span>
                            </div>
                          )}
                        </div>

                        <div className="test-meta">
                          {test.teacherName && (
                            <div className="meta-item">
                              <span className="meta-icon">👨‍🏫</span>
                              <span>{test.teacherName}</span>
                            </div>
                          )}
                          {test.messageNotificationSent && (
                            <div className="meta-item notification-sent">
                              <span className="meta-icon"><i className="fas fa-envelope"></i></span>
                              <span>알림 발송 완료</span>
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
              </div>
            </div>
            ))
          )}
        </div>
      </div>

      {/* 레벨 테스트 예약 모달 */}
      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2><i className="fas fa-clipboard-list"></i> 레벨 테스트 예약</h2>
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
                  value={newTest.memo}
                  onChange={(e) => setNewTest({ ...newTest, memo: e.target.value })}
                  placeholder="추가 메모 사항을 입력하세요 (예: 준비물, 특이사항 등)"
                  rows="3"
                />
              </div>

              <div className="notification-info">
                <span className="info-icon">💡</span>
                <span>예약 시 학부모님께 문자 알림이 자동으로 발송됩니다.</span>
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
    </div>
  );
}

export default LevelTests;
