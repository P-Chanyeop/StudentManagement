import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import LoadingSpinner from '../components/LoadingSpinner';
import { enrollmentAPI, studentAPI, courseAPI } from '../services/api';
import { holidayService } from '../services/holidayService';
import '../styles/Enrollments.css';

function Enrollments() {
  const queryClient = useQueryClient();
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [selectedEnrollment, setSelectedEnrollment] = useState(null);

  // 남은 영업일 표시 컴포넌트
  const RemainingBusinessDays = ({ startDate, endDate }) => {
    const [remainingDays, setRemainingDays] = useState(null);

    useEffect(() => {
      const calculateDays = async () => {
        try {
          const days = await holidayService.calculateRemainingBusinessDays(startDate, endDate);
          setRemainingDays(days);
        } catch (error) {
          console.error('남은 영업일 계산 실패:', error);
          setRemainingDays('계산 실패');
        }
      };

      calculateDays();
    }, [startDate, endDate]);

    if (remainingDays === null) return <span>계산 중...</span>;
    
    const isExpiringSoon = remainingDays <= 20; // 20일 이하면 경고
    const isExpired = remainingDays <= 0;

    return (
      <span className={`remaining-days ${isExpired ? 'expired' : isExpiringSoon ? 'warning' : ''}`}>
        {isExpired ? '만료됨' : `${remainingDays}일`}
      </span>
    );
  };
  const [newEnrollment, setNewEnrollment] = useState({
    studentId: '',
    courseId: '',
    type: 'PERIOD_BASED',
    startDate: new Date().toISOString().split('T')[0],
    endDate: '',
    totalCount: 0,
    weeks: 12, // 주 단위 추가
    price: 0,
    notes: '',
  });

  // 시작일이나 주 수가 변경될 때 자동으로 종료일 계산
  useEffect(() => {
    if (newEnrollment.startDate && (newEnrollment.weeks || newEnrollment.totalCount)) {
      calculateEndDate();
    }
  }, [newEnrollment.startDate, newEnrollment.weeks, newEnrollment.totalCount, newEnrollment.type]);

  // 공휴일을 고려한 종료일 계산
  const calculateEndDate = async () => {
    try {
      let businessDays;
      if (newEnrollment.type === 'PERIOD_BASED' && newEnrollment.weeks) {
        // 주 단위 계산 (주 * 5일)
        businessDays = parseInt(newEnrollment.weeks) * 5;
      } else if (newEnrollment.totalCount) {
        // 횟수 기반 계산
        businessDays = parseInt(newEnrollment.totalCount);
      } else {
        return;
      }

      const endDate = await holidayService.calculateEndDate(
        newEnrollment.startDate, 
        businessDays
      );
      
      setNewEnrollment(prev => ({
        ...prev,
        endDate: endDate.toISOString().split('T')[0]
      }));
    } catch (error) {
      console.error('종료일 계산 실패:', error);
    }
  };

  // 전체 수강권 조회
  const { data: enrollments = [], isLoading } = useQuery({
    queryKey: ['enrollments'],
    queryFn: async () => {
      const response = await enrollmentAPI.getAll();
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

  // 코스 목록 조회
  const { data: courses = [] } = useQuery({
    queryKey: ['courses'],
    queryFn: async () => {
      const response = await courseAPI.getAll();
      return response.data;
    },
  });

  // 수강권 생성 mutation
  const createMutation = useMutation({
    mutationFn: (data) => enrollmentAPI.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries(['enrollments']);
      setShowCreateModal(false);
      setNewEnrollment({
        studentId: '',
        courseId: '',
        type: 'PERIOD_BASED',
        startDate: new Date().toISOString().split('T')[0],
        endDate: '',
        totalCount: 0,
        price: 0,
        notes: '',
      });
      alert('수강권이 등록되었습니다.');
    },
    onError: (error) => {
      alert(`등록 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    },
  });

  // 수강권 취소 mutation
  const cancelMutation = useMutation({
    mutationFn: (id) => enrollmentAPI.cancel(id),
    onSuccess: () => {
      queryClient.invalidateQueries(['enrollments']);
      setShowDetailModal(false);
      alert('수강권이 취소되었습니다.');
    },
    onError: (error) => {
      alert(`취소 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    },
  });

  // 수강권 연장 mutation
  const extendMutation = useMutation({
    mutationFn: ({ id, newEndDate }) => enrollmentAPI.extendPeriod(id, newEndDate),
    onSuccess: () => {
      queryClient.invalidateQueries(['enrollments']);
      setShowDetailModal(false);
      alert('수강권이 연장되었습니다.');
    },
    onError: (error) => {
      alert(`연장 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    },
  });

  // 개별 수업 시간 설정 mutation
  const setDurationMutation = useMutation({
    mutationFn: ({ id, durationMinutes }) => enrollmentAPI.setCustomDuration(id, durationMinutes),
    onSuccess: () => {
      queryClient.invalidateQueries(['enrollments']);
      alert('개별 수업 시간이 설정되었습니다.');
    },
    onError: (error) => {
      alert(`설정 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    },
  });

  const handleCreateEnrollment = () => {
    if (!newEnrollment.studentId || !newEnrollment.courseId) {
      alert('학생과 코스를 선택해주세요.');
      return;
    }

    if (!newEnrollment.totalCount || newEnrollment.totalCount <= 0) {
      alert('총 횟수를 입력해주세요.');
      return;
    }

    if (newEnrollment.type === 'COUNT_BASED' && newEnrollment.totalCount <= 0) {
      alert('수강 횟수를 입력해주세요.');
      return;
    }

    createMutation.mutate(newEnrollment);
  };

  const handleCancelEnrollment = (id) => {
    if (window.confirm('수강권을 취소하시겠습니까?')) {
      cancelMutation.mutate(id);
    }
  };

  const handleExtendEnrollment = (id) => {
    const days = prompt('연장할 일수를 입력하세요:', '30');
    if (days && !isNaN(days) && parseInt(days) > 0) {
      // 현재 날짜에서 days만큼 더한 새로운 종료일 계산
      const newEndDate = new Date();
      newEndDate.setDate(newEndDate.getDate() + parseInt(days));
      const formattedDate = newEndDate.toISOString().split('T')[0];
      extendMutation.mutate({ id, newEndDate: formattedDate });
    }
  };

  const handleSetCustomDuration = (id, currentDuration) => {
    const duration = prompt('개별 수업 시간을 입력하세요 (분):', currentDuration || '60');
    if (duration && !isNaN(duration) && parseInt(duration) > 0) {
      setDurationMutation.mutate({ id, durationMinutes: parseInt(duration) });
    }
  };

  const openDetailModal = (enrollment) => {
    setSelectedEnrollment(enrollment);
    setShowDetailModal(true);
  };

  // 필터링 로직
  const filteredEnrollments = enrollments.filter((enrollment) => {
    const matchesStatus = statusFilter === 'ALL' || enrollment.status === statusFilter;
    const matchesSearch =
      searchQuery === '' ||
      enrollment.student?.name?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      enrollment.course?.name?.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesStatus && matchesSearch;
  });

  // 남은 일수 계산
  const getRemainingDays = (endDate) => {
    const today = new Date();
    const end = new Date(endDate);
    const diffTime = end - today;
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays;
  };

  // 상태별 배지
  const getStatusBadge = (status) => {
    const statusMap = {
      ACTIVE: { text: '활성', color: '#03C75A' },
      EXPIRED: { text: '만료', color: '#999' },
      CANCELLED: { text: '취소', color: '#FF3B30' },
    };
    const { text, color } = statusMap[status] || { text: status, color: '#999' };
    return <span className="status-badge" style={{ backgroundColor: color }}>{text}</span>;
  };

  // 타입별 배지
  const getTypeBadge = (type) => {
    const typeMap = {
      PERIOD_BASED: { text: '기간제', color: '#0066FF' },
      COUNT_BASED: { text: '횟수제', color: '#FF9800' },
    };
    const { text, color } = typeMap[type] || { text: type, color: '#999' };
    return <span className="type-badge" style={{ backgroundColor: color }}>{text}</span>;
  };

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
              <i className="fas fa-receipt"></i>
              수강권 관리
            </h1>
            <p className="page-subtitle">학생들의 수강권 등록 및 관리</p>
          </div>
          <button className="btn-primary" onClick={() => setShowCreateModal(true)}>
            <i className="fas fa-plus"></i> 수강권 등록
          </button>
        </div>
      </div>

      <div className="page-content">
        <div className="enrollments-filters">
          <div className="filter-left">
            <div className="status-filters">
              {['ALL', 'ACTIVE', 'EXPIRED', 'CANCELLED'].map((status) => (
                <button
                  key={status}
                  className={`filter-btn ${statusFilter === status ? 'active' : ''}`}
                  onClick={() => setStatusFilter(status)}
                >
                  {status === 'ALL' ? '전체' : status === 'ACTIVE' ? '활성' : status === 'EXPIRED' ? '만료' : '취소'}
                  <span className="count">
                    ({status === 'ALL' ? enrollments.length : enrollments.filter(e => e.status === status).length})
                  </span>
                </button>
              ))}
            </div>
          </div>
          <div className="filter-right">
            <div className="search-input-wrapper">
              <i className="fas fa-search search-icon"></i>
              <input
                type="text"
                placeholder="학생명 또는 코스명으로 검색..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="search-input"
              />
            </div>
          </div>
        </div>

        <div className="enrollments-grid">
          {filteredEnrollments.length === 0 ? (
            <div className="empty-state">
              {searchQuery ? '검색 결과가 없습니다.' : '등록된 수강권이 없습니다.'}
            </div>
          ) : (
            filteredEnrollments.map((enrollment) => (
            <div
              key={enrollment.id}
              className="enrollment-card"
              onClick={() => openDetailModal(enrollment)}
            >
              <div className="enrollment-header">
                <div className="badges">
                  {getTypeBadge(enrollment.type)}
                  {getStatusBadge(enrollment.status)}
                </div>
              </div>

              <div className="enrollment-body">
                <h3 className="student-name">{enrollment.student?.name || '학생 정보 없음'}</h3>
                <p className="course-name">{enrollment.course?.name || '수업 정보 없음'}</p>

                <div className="enrollment-details">
                  {enrollment.type === 'PERIOD_BASED' ? (
                    <>
                      <div className="detail-item">
                        <span className="label">기간</span>
                        <span className="value">
                          {enrollment.startDate} ~ {enrollment.endDate}
                        </span>
                      </div>
                      <div className="detail-item">
                        <span className="label">남은 영업일</span>
                        <span className="value">
                          <RemainingBusinessDays 
                            startDate={enrollment.startDate} 
                            endDate={enrollment.endDate} 
                          />
                        </span>
                      </div>
                    </>
                  ) : enrollment.type === 'COUNT_BASED' ? (
                    <>
                      <div className="detail-item">
                        <span className="label">전체 횟수</span>
                        <span className="value">{enrollment.totalCount}회</span>
                      </div>
                      <div className="detail-item">
                        <span className="label">남은 횟수</span>
                        <span
                          className="value remaining"
                          style={{
                            color: enrollment.remainingCount < 3 ? '#FF3B30' : '#03C75A',
                          }}
                        >
                          {enrollment.remainingCount}회
                        </span>
                      </div>
                    </>
                  ) : null}
                  <div className="detail-item">
                    <span className="label">수업 시간</span>
                    <span className="value">
                      {enrollment.actualDurationMinutes || enrollment.course.durationMinutes}분
                      {enrollment.customDurationMinutes && (
                        <span className="custom-duration"> (개별설정)</span>
                      )}
                    </span>
                  </div>
                  <div className="detail-item">
                    <span className="label">가격</span>
                    <span className="value price">
                      {enrollment.price?.toLocaleString() || '0'}원
                    </span>
                  </div>
                </div>
              </div>
            </div>
            ))
          )}
        </div>
      </div>

      {/* 수강권 생성 모달 */}
      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>수강권 등록</h2>
              <button className="modal-close" onClick={() => setShowCreateModal(false)}>
                ×
              </button>
            </div>

            <div className="modal-body">
              <div className="form-group">
                <label>학생 선택 *</label>
                <select
                  value={newEnrollment.studentId}
                  onChange={(e) =>
                    setNewEnrollment({ ...newEnrollment, studentId: e.target.value })
                  }
                >
                  <option value="">학생을 선택하세요</option>
                  {students.map((student) => (
                    <option key={student.id} value={student.id}>
                      {student.name} ({student.phone})
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>코스 선택 *</label>
                <select
                  value={newEnrollment.courseId}
                  onChange={(e) =>
                    setNewEnrollment({ ...newEnrollment, courseId: e.target.value })
                  }
                >
                  <option value="">코스를 선택하세요</option>
                  {courses.map((course) => (
                    <option key={course.id} value={course.id}>
                      {course.name} - {course.level}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>수강권 타입 *</label>
                <div className="radio-group">
                  <label className="radio-label">
                    <input
                      type="radio"
                      name="type"
                      value="PERIOD_BASED"
                      checked={newEnrollment.type === 'PERIOD_BASED'}
                      onChange={(e) =>
                        setNewEnrollment({ ...newEnrollment, type: e.target.value })
                      }
                    />
                    <span>기간제</span>
                  </label>
                  <label className="radio-label">
                    <input
                      type="radio"
                      name="type"
                      value="COUNT_BASED"
                      checked={newEnrollment.type === 'COUNT_BASED'}
                      onChange={(e) =>
                        setNewEnrollment({ ...newEnrollment, type: e.target.value })
                      }
                    />
                    <span>횟수제</span>
                  </label>
                </div>
              </div>

              {newEnrollment.type === 'PERIOD_BASED' ? (
                <>
                  <div className="form-group">
                    <label>시작일 *</label>
                    <input
                      type="date"
                      value={newEnrollment.startDate}
                      onChange={(e) =>
                        setNewEnrollment({ ...newEnrollment, startDate: e.target.value })
                      }
                    />
                  </div>
                  <div className="form-group">
                    <label>수강 기간 (주) *</label>
                    <select
                      value={newEnrollment.weeks}
                      onChange={(e) =>
                        setNewEnrollment({ ...newEnrollment, weeks: parseInt(e.target.value) })
                      }
                    >
                      <option value={4}>4주</option>
                      <option value={8}>8주</option>
                      <option value={12}>12주</option>
                      <option value={16}>16주</option>
                      <option value={24}>24주</option>
                    </select>
                    <small className="form-help">공휴일을 제외한 영업일 기준으로 종료일이 자동 계산됩니다.</small>
                  </div>
                  <div className="form-group">
                    <label>종료일 (자동 계산)</label>
                    <input
                      type="date"
                      value={newEnrollment.endDate}
                      onChange={(e) =>
                        setNewEnrollment({ ...newEnrollment, endDate: e.target.value })
                      }
                      placeholder="시작일과 횟수로 자동 계산됩니다"
                    />
                    <small className="form-help">
                      시작일과 총 횟수를 기준으로 공휴일을 제외하여 자동 계산됩니다. 
                      직접 입력하면 수동 설정됩니다.
                    </small>
                  </div>
                </>
              ) : (
                <div className="form-group">
                  <label>수강 횟수 *</label>
                  <input
                    type="number"
                    value={newEnrollment.totalCount}
                    onChange={(e) =>
                      setNewEnrollment({ ...newEnrollment, totalCount: parseInt(e.target.value) || 0 })
                    }
                    placeholder="예: 10"
                    min="1"
                  />
                </div>
              )}

              <div className="form-group">
                <label>가격 *</label>
                <input
                  type="number"
                  value={newEnrollment.price}
                  onChange={(e) =>
                    setNewEnrollment({ ...newEnrollment, price: parseInt(e.target.value) || 0 })
                  }
                  placeholder="예: 300000"
                  min="0"
                />
              </div>

              <div className="form-group">
                <label>메모</label>
                <textarea
                  value={newEnrollment.notes}
                  onChange={(e) =>
                    setNewEnrollment({ ...newEnrollment, notes: e.target.value })
                  }
                  placeholder="추가 메모 사항을 입력하세요"
                  rows="3"
                />
              </div>
            </div>

            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setShowCreateModal(false)}>
                취소
              </button>
              <button className="btn-primary" onClick={handleCreateEnrollment}>
                등록
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 수강권 상세 모달 */}
      {showDetailModal && selectedEnrollment && (
        <div className="modal-overlay" onClick={() => setShowDetailModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>수강권 상세</h2>
              <button className="modal-close" onClick={() => setShowDetailModal(false)}>
                ×
              </button>
            </div>

            <div className="modal-body">
              <div className="detail-section">
                <div className="badges">
                  {getTypeBadge(selectedEnrollment.type)}
                  {getStatusBadge(selectedEnrollment.status)}
                </div>

                <div className="detail-info-grid">
                  <div className="detail-info-item">
                    <span className="info-label">학생</span>
                    <span className="info-value">{selectedEnrollment.student?.name || '학생 정보 없음'}</span>
                  </div>
                  <div className="detail-info-item">
                    <span className="info-label">연락처</span>
                    <span className="info-value">{selectedEnrollment.student?.phone || '연락처 정보 없음'}</span>
                  </div>
                  <div className="detail-info-item">
                    <span className="info-label">코스</span>
                    <span className="info-value">{selectedEnrollment.course?.name || '수업 정보 없음'}</span>
                  </div>
                  <div className="detail-info-item">
                    <span className="info-label">레벨</span>
                    <span className="info-value">{selectedEnrollment.course?.level || '레벨 정보 없음'}</span>
                  </div>

                  {selectedEnrollment.type === 'PERIOD_BASED' ? (
                    <>
                      <div className="detail-info-item">
                        <span className="info-label">시작일</span>
                        <span className="info-value">
                          {new Date(selectedEnrollment.startDate).toLocaleDateString()}
                        </span>
                      </div>
                      <div className="detail-info-item">
                        <span className="info-label">종료일</span>
                        <span className="info-value">
                          {new Date(selectedEnrollment.endDate).toLocaleDateString()}
                        </span>
                      </div>
                      {selectedEnrollment.status === 'ACTIVE' && (
                        <div className="detail-info-item">
                          <span className="info-label">남은 일수</span>
                          <span
                            className="info-value"
                            style={{
                              color: getRemainingDays(selectedEnrollment.endDate) < 7 ? '#FF3B30' : '#03C75A',
                              fontWeight: 'bold',
                            }}
                          >
                            {getRemainingDays(selectedEnrollment.endDate)}일
                          </span>
                        </div>
                      )}
                    </>
                  ) : (
                    <>
                      <div className="detail-info-item">
                        <span className="info-label">전체 횟수</span>
                        <span className="info-value">{selectedEnrollment.totalCount}회</span>
                      </div>
                      <div className="detail-info-item">
                        <span className="info-label">남은 횟수</span>
                        <span
                          className="info-value"
                          style={{
                            color: selectedEnrollment.remainingCount < 3 ? '#FF3B30' : '#03C75A',
                            fontWeight: 'bold',
                          }}
                        >
                          {selectedEnrollment.remainingCount}회
                        </span>
                      </div>
                    </>
                  )}

                  <div className="detail-info-item">
                    <span className="info-label">수업 시간</span>
                    <span className="info-value">
                      {selectedEnrollment.actualDurationMinutes || selectedEnrollment.course.durationMinutes}분
                      {selectedEnrollment.customDurationMinutes && (
                        <span className="custom-duration"> (개별설정)</span>
                      )}
                    </span>
                  </div>

                  <div className="detail-info-item">
                    <span className="info-label">가격</span>
                    <span className="info-value price">
                      {selectedEnrollment.price?.toLocaleString() || '0'}원
                    </span>
                  </div>

                  {selectedEnrollment.notes && (
                    <div className="detail-info-item full-width">
                      <span className="info-label">메모</span>
                      <span className="info-value">{selectedEnrollment.notes}</span>
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="modal-footer">
              {selectedEnrollment.status === 'ACTIVE' && (
                <>
                  {selectedEnrollment.type === 'PERIOD_BASED' && (
                    <button
                      className="btn-extend"
                      onClick={() => handleExtendEnrollment(selectedEnrollment.id)}
                    >
                      기간 연장
                    </button>
                  )}
                  <button
                    className="btn-set-duration"
                    onClick={() => handleSetCustomDuration(
                      selectedEnrollment.id, 
                      selectedEnrollment.actualDurationMinutes || selectedEnrollment.course.durationMinutes
                    )}
                  >
                    수업시간 설정
                  </button>
                  <button
                    className="btn-cancel-enrollment"
                    onClick={() => handleCancelEnrollment(selectedEnrollment.id)}
                  >
                    수강권 취소
                  </button>
                </>
              )}
              <button className="btn-secondary" onClick={() => setShowDetailModal(false)}>
                닫기
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Enrollments;
