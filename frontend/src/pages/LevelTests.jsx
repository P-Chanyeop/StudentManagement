import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import LoadingSpinner from '../components/LoadingSpinner';
import { levelTestAPI, studentAPI, reservationAPI, authAPI } from '../services/api';
import { getTodayString, getDateAfterDays } from '../utils/dateUtils';
import '../styles/LevelTests.css';

function LevelTests() {
  const queryClient = useQueryClient();
  
  // 사용자 정보 조회
  const { data: currentUser } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
  });

  const isParent = currentUser?.role === 'PARENT';
  const [dateRange, setDateRange] = useState({
    start: getTodayString(),
    end: getDateAfterDays(30),
  });
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newTest, setNewTest] = useState({
    studentId: '',
    testDate: getTodayString(),
    testTime: '10:00',
    memo: '',
  });
  
  // 학생 선택 드롭다운 상태
  const [showStudentDropdown, setShowStudentDropdown] = useState(false);
  const [studentSearchQuery, setStudentSearchQuery] = useState('');

  const [searchParams, setSearchParams] = useState({
    start: getTodayString(),
    end: getDateAfterDays(30)
  });

  // 날짜 범위로 레벨 테스트 조회 (수동 트리거)
  const { data: levelTests = [], isLoading, refetch } = useQuery({
    queryKey: ['levelTests', searchParams.start, searchParams.end],
    queryFn: async () => {
      const response = await levelTestAPI.getByRange(searchParams.start, searchParams.end);
      return response.data;
    },
  });

  // 레벨테스트 예약 조회 (학부모는 본인 예약만, 관리자/선생님은 모든 예약)
  const { data: levelTestReservations = [] } = useQuery({
    queryKey: ['levelTestReservations', searchParams.start, searchParams.end, currentUser?.id],
    queryFn: async () => {
      if (isParent && currentUser?.studentId) {
        // 학부모는 본인 학생의 예약만 조회
        const response = await reservationAPI.getByStudent(currentUser.studentId);
        return response.data.filter(reservation => 
          reservation.consultationType === '레벨테스트'
        );
      } else if (!isParent) {
        // 관리자/선생님은 날짜 범위 내 모든 레벨테스트 예약 조회
        const startDate = new Date(searchParams.start);
        const endDate = new Date(searchParams.end);
        const allReservations = [];
        
        for (let d = new Date(startDate); d <= endDate; d.setDate(d.getDate() + 1)) {
          const dateStr = d.toISOString().split('T')[0];
          try {
            const response = await reservationAPI.getByDate(dateStr);
            const levelTestReservations = response.data.filter(reservation => 
              reservation.consultationType === '레벨테스트'
            );
            allReservations.push(...levelTestReservations);
          } catch (error) {
            console.log(`No reservations for ${dateStr}`);
          }
        }
        
        return allReservations;
      }
      return [];
    },
    enabled: !!currentUser,
  });

  // 레벨테스트와 예약 데이터 합치기
  const combinedTests = [
    ...levelTests,
    ...levelTestReservations.map(reservation => ({
      id: `reservation-${reservation.id}`,
      studentName: reservation.studentName,
      testDate: reservation.scheduleDate,
      testTime: reservation.startTime,
      memo: reservation.memo || '상담 예약에서 등록',
      status: 'SCHEDULED',
      isReservation: true,
      reservationId: reservation.id
    }))
  ];

  // 디버깅용 로그
  console.log('Level test reservations:', levelTestReservations);
  console.log('Combined tests:', combinedTests);

  // 검색 버튼 클릭 핸들러
  const handleSearch = () => {
    // 날짜 유효성 검사
    if (dateRange.start > dateRange.end) {
      alert('시작일은 종료일보다 늦을 수 없습니다.');
      return;
    }
    setSearchParams({ ...dateRange });
  };

  // 학생 목록 조회
  const { data: students = [] } = useQuery({
    queryKey: ['students'],
    queryFn: async () => {
      const response = await studentAPI.getAll();
      return response.data;
    },
  });

  // 학생 선택 핸들러
  const handleStudentSelect = (student) => {
    setNewTest(prev => ({
      ...prev,
      studentId: student.id.toString()
    }));
    setShowStudentDropdown(false);
    setStudentSearchQuery('');
  };

  // 선택된 학생 정보 가져오기
  const getSelectedStudent = () => {
    return students.find(student => student.id.toString() === newTest.studentId);
  };

  // 필터링된 학생 목록
  const getFilteredStudents = () => {
    if (!studentSearchQuery) return students;
    
    return students.filter(student => 
      student.studentName.toLowerCase().includes(studentSearchQuery.toLowerCase()) ||
      student.parentName?.toLowerCase().includes(studentSearchQuery.toLowerCase())
    );
  };

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
  const groupedTests = combinedTests.reduce((acc, test) => {
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
              {isParent ? '레벨 테스트 예약 내역' : '레벨 테스트 일정 관리'}
            </h1>
            <p className="page-subtitle">
              {isParent ? '내 레벨 테스트 예약 내역을 확인합니다' : '학생들의 레벨 테스트 일정을 관리합니다'}
            </p>
          </div>
        </div>
      </div>

      <div className="page-content">
        {!isParent && (
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
                min={dateRange.start}
                onChange={(e) => setDateRange({ ...dateRange, end: e.target.value })}
              />
              <button className="btn-search" onClick={handleSearch}>
                <i className="fas fa-search"></i>
                검색
              </button>
            </div>
            <div className="result-count">
              <i className="fas fa-clipboard-list"></i>
              총 <strong>{combinedTests.length}</strong>건
            </div>
          </div>
        )}

        {isParent && (
          <div className="parent-info">
            <div className="result-count">
              <i className="fas fa-clipboard-list"></i>
              내 예약 <strong>{combinedTests.length}</strong>건
            </div>
          </div>
        )}

        <div className="calendar-view">
          {sortedDates.length === 0 ? (
            <div className="empty-state">
              <i className="fas fa-calendar-alt"></i>
              <p>등록된 레벨 테스트 일정이 없습니다.</p>
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
        <div className="modal-overlay">
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2><i className="fas fa-clipboard-list"></i> 레벨 테스트 예약</h2>
              <button className="modal-close" onClick={() => setShowCreateModal(false)}>
                ×
              </button>
            </div>

            <div className="modal-body">
              <div className="form-group">
                <label>학생을 선택해 주세요 *</label>
                <div className="student-select-wrapper">
                  <div 
                    className="student-select-input"
                    onClick={() => setShowStudentDropdown(!showStudentDropdown)}
                  >
                    {getSelectedStudent() ? (
                      <div className="selected-student-info">
                        <span className="student-name">{getSelectedStudent().studentName}</span>
                        <span className="parent-info">
                          {getSelectedStudent().parentName} · {getSelectedStudent().studentPhone}
                        </span>
                      </div>
                    ) : (
                      <span className="placeholder">학생을 선택해 주세요</span>
                    )}
                    <i className={`fas fa-chevron-${showStudentDropdown ? 'up' : 'down'}`}></i>
                  </div>
                  
                  {showStudentDropdown && (
                    <div className="student-dropdown">
                      <div className="student-search">
                        <input
                          type="text"
                          placeholder="학생 이름으로 검색..."
                          value={studentSearchQuery}
                          onChange={(e) => setStudentSearchQuery(e.target.value)}
                          onClick={(e) => e.stopPropagation()}
                        />
                      </div>
                      <div className="student-list">
                        {getFilteredStudents().map(student => (
                          <div
                            key={student.id}
                            className="student-option"
                            onClick={() => handleStudentSelect(student)}
                          >
                            <div className="student-info">
                              <span className="student-name">{student.studentName}</span>
                              <span className="parent-info">
                                {student.parentName} · {student.studentPhone}
                              </span>
                            </div>
                          </div>
                        ))}
                        {getFilteredStudents().length === 0 && (
                          <div className="no-students">검색 결과가 없습니다.</div>
                        )}
                      </div>
                    </div>
                  )}
                </div>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>테스트 날짜 *</label>
                  <input
                    type="date"
                    value={newTest.testDate}
                    onChange={(e) => setNewTest({ ...newTest, testDate: e.target.value })}
                    min={new Date().toISOString().split('T')[0]}
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
