import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import LoadingSpinner from '../components/LoadingSpinner';
import { enrollmentAPI, studentAPI } from '../services/api';
import '../styles/EnrollmentAdjustment.css';

function EnrollmentAdjustment() {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedEnrollment, setSelectedEnrollment] = useState(null);
  const [showAdjustModal, setShowAdjustModal] = useState(false);
  const [adjustmentData, setAdjustmentData] = useState({
    adjustment: '',
    reason: ''
  });
  const [errors, setErrors] = useState({});

  // 수강권 목록 조회
  const { data: enrollments, isLoading } = useQuery({
    queryKey: ['enrollments'],
    queryFn: async () => {
      const response = await enrollmentAPI.getEnrollments();
      return response.data;
    },
  });

  // 학생 목록 조회 (검색용)
  const { data: students } = useQuery({
    queryKey: ['students'],
    queryFn: async () => {
      const response = await studentAPI.getStudents();
      return response.data;
    },
  });

  // 수강권 횟수 조정 mutation
  const adjustMutation = useMutation({
    mutationFn: async ({ enrollmentId, adjustmentData }) => {
      const response = await enrollmentAPI.manualAdjustCount(enrollmentId, adjustmentData);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['enrollments']);
      setShowAdjustModal(false);
      setSelectedEnrollment(null);
      setAdjustmentData({ adjustment: '', reason: '' });
      setErrors({});
      alert('횟수가 성공적으로 조정되었습니다.');
    },
    onError: (error) => {
      alert(error.response?.data?.message || '조정 중 오류가 발생했습니다.');
    },
  });

  // 검색 필터링
  const filteredEnrollments = enrollments?.filter(enrollment => {
    if (!searchQuery) return true;
    const student = students?.find(s => s.id === enrollment.studentId);
    return student?.studentName.toLowerCase().includes(searchQuery.toLowerCase()) ||
           enrollment.courseName.toLowerCase().includes(searchQuery.toLowerCase());
  }) || [];

  const handleAdjustClick = (enrollment) => {
    setSelectedEnrollment(enrollment);
    setShowAdjustModal(true);
    setErrors({});
  };

  const handleAdjustSubmit = (e) => {
    e.preventDefault();
    const newErrors = {};
    
    if (!adjustmentData.adjustment) {
      newErrors.adjustment = '조정 횟수를 입력해주세요.';
    } else {
      const adjustment = parseInt(adjustmentData.adjustment);
      if (isNaN(adjustment)) {
        newErrors.adjustment = '숫자를 입력해주세요.';
      } else if (adjustment === 0) {
        newErrors.adjustment = '조정 횟수는 0이 될 수 없습니다. 양수(추가) 또는 음수(차감)를 입력해주세요.';
      }
    }
    
    if (!adjustmentData.reason) {
      newErrors.reason = '조정 사유를 입력해주세요.';
    }
    
    setErrors(newErrors);
    
    if (Object.keys(newErrors).length > 0) {
      return;
    }

    adjustMutation.mutate({
      enrollmentId: selectedEnrollment.id,
      adjustmentData: {
        adjustment: parseInt(adjustmentData.adjustment),
        reason: adjustmentData.reason
      }
    });
  };

  const getStudentName = (studentId) => {
    const student = students?.find(s => s.id === studentId);
    return student?.studentName || '알 수 없음';
  };

  if (isLoading) {
    return <LoadingSpinner />;
  }

  return (
    <div className="page-wrapper">
      <div className="page-header">
        <div className="page-header-content">
          <div className="page-title-section">
            <h1 className="page-title">
              <i className="fas fa-edit"></i>
              수강권 횟수 조정
            </h1>
            <p className="page-subtitle">
              관리자가 수강권 횟수를 수동으로 조정할 수 있습니다
            </p>
          </div>
        </div>
      </div>

      <div className="page-content">
        {/* 검색 섹션 */}
        <div className="search-section">
          <div className="search-input-wrapper">
            <i className="fas fa-search search-icon"></i>
            <input
              type="text"
              className="search-input"
              placeholder="학생명 또는 수업명으로 검색..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <div className="result-count">
            <i className="fas fa-list"></i>
            <span>총 <strong>{filteredEnrollments.length}</strong>개 수강권</span>
          </div>
        </div>

        {/* 수강권 목록 */}
        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>학생명</th>
                <th>수업명</th>
                <th>총 횟수</th>
                <th>사용 횟수</th>
                <th>남은 횟수</th>
                <th>상태</th>
                <th>수강 기간</th>
                <th>액션</th>
              </tr>
            </thead>
            <tbody>
              {filteredEnrollments.map((enrollment) => (
                <tr key={enrollment.id}>
                  <td>
                    <div className="student-info">
                      <i className="fas fa-user"></i>
                      <strong>{getStudentName(enrollment.studentId)}</strong>
                    </div>
                  </td>
                  <td>{enrollment.courseName}</td>
                  <td>{enrollment.totalCount}회</td>
                  <td>{enrollment.usedCount}회</td>
                  <td>
                    <span className={`remaining-count ${enrollment.remainingCount <= 3 ? 'low' : ''}`}>
                      {enrollment.remainingCount}회
                    </span>
                  </td>
                  <td>
                    <span className={`status-badge ${enrollment.isActive ? 'active' : 'inactive'}`}>
                      {enrollment.isActive ? '활성' : '비활성'}
                    </span>
                  </td>
                  <td>
                    {new Date(enrollment.startDate).toLocaleDateString()} ~ {new Date(enrollment.endDate).toLocaleDateString()}
                  </td>
                  <td>
                    <div className="action-buttons">
                      <button
                        className="btn-table-edit"
                        onClick={() => handleAdjustClick(enrollment)}
                        title="횟수 조정"
                      >
                        <i className="fas fa-edit"></i>
                        조정
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {filteredEnrollments.length === 0 && (
            <div className="empty-state">
              <i className="fas fa-receipt"></i>
              <p>조건에 맞는 수강권이 없습니다</p>
            </div>
          )}
        </div>
      </div>

      {/* 횟수 조정 모달 */}
      {showAdjustModal && (
        <div className="modal-overlay" onClick={() => setShowAdjustModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>수강권 횟수 조정</h2>
              <button
                className="modal-close"
                onClick={() => setShowAdjustModal(false)}
              >
                ×
              </button>
            </div>
            <div className="modal-body">
              <div className="enrollment-info-section">
                <h3>수강권 정보</h3>
                <div className="info-grid">
                  <div className="info-item">
                    <span className="info-label">학생명:</span>
                    <span className="info-value">{getStudentName(selectedEnrollment?.studentId)}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">수업명:</span>
                    <span className="info-value">{selectedEnrollment?.courseName}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">현재 총 횟수:</span>
                    <span className="info-value">{selectedEnrollment?.totalCount}회</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">현재 남은 횟수:</span>
                    <span className="info-value">{selectedEnrollment?.remainingCount}회</span>
                  </div>
                </div>
              </div>

              <form onSubmit={handleAdjustSubmit}>
                <div className="form-section">
                  <h3>횟수 조정</h3>
                  <div className="form-group">
                    <label htmlFor="adjustment">조정 횟수</label>
                    <input
                      type="number"
                      id="adjustment"
                      value={adjustmentData.adjustment}
                      onChange={(e) => {
                        setAdjustmentData({
                          ...adjustmentData,
                          adjustment: e.target.value
                        });
                        if (errors.adjustment) {
                          setErrors({...errors, adjustment: ''});
                        }
                      }}
                      placeholder="양수: 추가, 음수: 차감"
                      className={errors.adjustment ? 'error' : ''}
                      required
                    />
                    {errors.adjustment && (
                      <span className="error-message">{errors.adjustment}</span>
                    )}
                    <small className="form-help">
                      양수를 입력하면 횟수가 추가되고, 음수를 입력하면 차감됩니다.
                    </small>
                  </div>
                  <div className="form-group">
                    <label htmlFor="reason">조정 사유</label>
                    <textarea
                      id="reason"
                      value={adjustmentData.reason}
                      onChange={(e) => {
                        setAdjustmentData({
                          ...adjustmentData,
                          reason: e.target.value
                        });
                        if (errors.reason) {
                          setErrors({...errors, reason: ''});
                        }
                      }}
                      placeholder="조정 사유를 입력하세요 (예: 보강 수업, 시스템 오류 보정 등)"
                      rows="3"
                      className={errors.reason ? 'error' : ''}
                      required
                    />
                    {errors.reason && (
                      <span className="error-message">{errors.reason}</span>
                    )}
                  </div>
                </div>
              </form>
            </div>
            <div className="modal-footer">
              <button
                type="button"
                className="btn-secondary"
                onClick={() => setShowAdjustModal(false)}
              >
                취소
              </button>
              <button
                type="submit"
                className="btn-primary"
                onClick={handleAdjustSubmit}
                disabled={adjustMutation.isLoading}
              >
                {adjustMutation.isLoading ? '조정 중...' : '조정하기'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default EnrollmentAdjustment;
