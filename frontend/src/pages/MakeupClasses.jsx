import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import LoadingSpinner from '../components/LoadingSpinner';
import { makeupClassAPI, studentAPI, courseAPI } from '../services/api';
import '../styles/MakeupClasses.css';

function MakeupClasses() {
  const queryClient = useQueryClient();
  const [showModal, setShowModal] = useState(false);
  const [editingMakeup, setEditingMakeup] = useState(null);
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [formData, setFormData] = useState({
    studentId: '',
    courseId: '',
    originalDate: '',
    makeupDate: '',
    makeupTime: '',
    reason: '',
    memo: '',
  });

  // 보강 수업 목록 조회
  const { data: makeupClasses, isLoading } = useQuery({
    queryKey: ['makeupClasses', filterStatus],
    queryFn: async () => {
      if (filterStatus === 'ALL') {
        const response = await makeupClassAPI.getAll();
        return response.data;
      } else {
        const response = await makeupClassAPI.getByStatus(filterStatus);
        return response.data;
      }
    },
  });

  // 학생 목록 조회
  const { data: students } = useQuery({
    queryKey: ['students'],
    queryFn: async () => {
      const response = await studentAPI.getAll();
      return response.data;
    },
  });

  // 수업 목록 조회
  const { data: courses } = useQuery({
    queryKey: ['courses'],
    queryFn: async () => {
      const response = await courseAPI.getAll();
      return response.data;
    },
  });

  // 보강 수업 생성
  const createMutation = useMutation({
    mutationFn: (data) => makeupClassAPI.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries(['makeupClasses']);
      setShowModal(false);
      resetForm();
      alert('보강 수업이 등록되었습니다.');
    },
    onError: (error) => {
      alert(`등록 실패: ${error.response?.data?.message || error.message}`);
    },
  });

  // 보강 수업 수정
  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => makeupClassAPI.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries(['makeupClasses']);
      setShowModal(false);
      setEditingMakeup(null);
      resetForm();
      alert('보강 수업이 수정되었습니다.');
    },
    onError: (error) => {
      alert(`수정 실패: ${error.response?.data?.message || error.message}`);
    },
  });

  // 보강 수업 삭제
  const deleteMutation = useMutation({
    mutationFn: (id) => makeupClassAPI.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries(['makeupClasses']);
      alert('보강 수업이 삭제되었습니다.');
    },
    onError: (error) => {
      alert(`삭제 실패: ${error.response?.data?.message || error.message}`);
    },
  });

  // 보강 수업 완료
  const completeMutation = useMutation({
    mutationFn: (id) => makeupClassAPI.complete(id),
    onSuccess: () => {
      queryClient.invalidateQueries(['makeupClasses']);
      alert('보강 수업이 완료 처리되었습니다.');
    },
  });

  // 보강 수업 취소
  const cancelMutation = useMutation({
    mutationFn: (id) => makeupClassAPI.cancel(id),
    onSuccess: () => {
      queryClient.invalidateQueries(['makeupClasses']);
      alert('보강 수업이 취소되었습니다.');
    },
  });

  const resetForm = () => {
    setFormData({
      studentId: '',
      courseId: '',
      originalDate: '',
      makeupDate: '',
      makeupTime: '',
      reason: '',
      memo: '',
    });
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    if (editingMakeup) {
      updateMutation.mutate({
        id: editingMakeup.id,
        data: {
          makeupDate: formData.makeupDate,
          makeupTime: formData.makeupTime,
          reason: formData.reason,
          memo: formData.memo,
        },
      });
    } else {
      createMutation.mutate(formData);
    }
  };

  const handleEdit = (makeup) => {
    setEditingMakeup(makeup);
    setFormData({
      studentId: makeup.studentId,
      courseId: makeup.courseId,
      originalDate: makeup.originalDate,
      makeupDate: makeup.makeupDate,
      makeupTime: makeup.makeupTime,
      reason: makeup.reason || '',
      memo: makeup.memo || '',
    });
    setShowModal(true);
  };

  const handleDelete = (id) => {
    if (window.confirm('정말 삭제하시겠습니까?')) {
      deleteMutation.mutate(id);
    }
  };

  const handleComplete = (id) => {
    if (window.confirm('보강 수업을 완료 처리하시겠습니까?')) {
      completeMutation.mutate(id);
    }
  };

  const handleCancel = (id) => {
    if (window.confirm('보강 수업을 취소하시겠습니까?')) {
      cancelMutation.mutate(id);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  const getStatusBadge = (status) => {
    const badges = {
      SCHEDULED: { text: '예정', className: 'makeup-status-scheduled' },
      COMPLETED: { text: '완료', className: 'makeup-status-completed' },
      CANCELLED: { text: '취소', className: 'makeup-status-cancelled' },
    };
    return badges[status] || { text: status, className: '' };
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
              <i className="fas fa-redo-alt"></i>
              보강 수업 관리
            </h1>
            <p className="page-subtitle">결석한 학생들의 보강 수업을 관리합니다</p>
          </div>
          <button className="btn-primary" onClick={() => setShowModal(true)}>
            <i className="fas fa-plus"></i> 보강 수업 등록
          </button>
        </div>
      </div>

      <div className="page-content">

        <div className="search-section">
          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="makeup-filter-select"
          >
            <option value="ALL">전체</option>
            <option value="SCHEDULED">예정</option>
            <option value="COMPLETED">완료</option>
            <option value="CANCELLED">취소</option>
          </select>
          <div className="result-count">
            <i className="fas fa-redo-alt"></i>
            총 <strong>{makeupClasses?.length || 0}</strong>건
          </div>
        </div>

        <div className="makeup-list">
          {makeupClasses && makeupClasses.length > 0 ? (
            <div className="makeup-table-wrapper">
              <table className="makeup-data-table">
              <thead>
                <tr>
                  <th>학생 정보</th>
                  <th>수업 정보</th>
                  <th>원래 날짜</th>
                  <th>보강 일정</th>
                  <th>사유</th>
                  <th>상태</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                {makeupClasses.map((makeup) => (
                  <tr key={makeup.id}>
                    <td>
                      <div className="makeup-student-info">
                        <div className="makeup-student-details">
                          <div className="makeup-student-name">{makeup.studentName}</div>
                        </div>
                      </div>
                    </td>
                    <td>
                      <div className="makeup-course-info">
                        <div className="makeup-course-name">{makeup.courseName}</div>
                      </div>
                    </td>
                    <td>
                      <div className="makeup-date">
                        {formatDate(makeup.originalDate)}
                      </div>
                    </td>
                    <td>
                      <div className="makeup-datetime">
                        <div className="makeup-date">
                          {formatDate(makeup.makeupDate)}
                        </div>
                        <div className="makeup-time">
                          {makeup.makeupTime}
                        </div>
                      </div>
                    </td>
                    <td>
                      <div className="makeup-memo">{makeup.reason || '-'}</div>
                    </td>
                    <td>
                      <span className={`makeup-status-badge makeup-status-${getStatusBadge(makeup.status).className}`}>
                        {makeup.status === 'SCHEDULED' && <i className="fas fa-clock"></i>}
                        {makeup.status === 'COMPLETED' && <i className="fas fa-check"></i>}
                        {makeup.status === 'CANCELLED' && <i className="fas fa-times"></i>}
                        {getStatusBadge(makeup.status).text}
                      </span>
                    </td>
                    <td>
                      <div className="makeup-action-buttons">
                        {makeup.status === 'SCHEDULED' && (
                          <>
                            <button
                              className="makeup-btn-complete"
                              onClick={() => handleComplete(makeup.id)}
                            >
                              <i className="fas fa-check"></i>
                              완료
                            </button>
                            <button
                              className="makeup-btn-edit"
                              onClick={() => handleEdit(makeup)}
                            >
                              <i className="fas fa-edit"></i>
                              수정
                            </button>
                          </>
                        )}
                        <button
                          className="makeup-btn-delete"
                          onClick={() => handleDelete(makeup.id)}
                        >
                          <i className="fas fa-trash"></i>
                          삭제
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
              </table>
            </div>
          ) : (
            <div className="empty-state">
              <i className="fas fa-calendar-alt"></i>
              <p>등록된 보강 수업이 없습니다.</p>
            </div>
          )}
        </div>
      </div>

        {showModal && (
          <div className="modal-overlay" onClick={() => {
            setShowModal(false);
            setEditingMakeup(null);
            resetForm();
          }}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
              <div className="modal-header">
                <h2>{editingMakeup ? '보강 수업 수정' : '보강 수업 등록'}</h2>
                <button className="modal-close" onClick={() => {
                  setShowModal(false);
                  setEditingMakeup(null);
                  resetForm();
                }}>
                  ×
                </button>
              </div>

              <div className="modal-body">
                <form onSubmit={handleSubmit}>
                  {!editingMakeup && (
                    <>
                      <div className="form-group">
                        <label>학생 *</label>
                        <select
                          value={formData.studentId}
                          onChange={(e) => setFormData({ ...formData, studentId: e.target.value })}
                          required
                        >
                          <option value="">학생 선택</option>
                          {students?.map((student) => (
                            <option key={student.id} value={student.id}>
                              {student.studentName}
                            </option>
                          ))}
                        </select>
                      </div>

                      <div className="form-group">
                        <label>수업 *</label>
                        <select
                          value={formData.courseId}
                          onChange={(e) => setFormData({ ...formData, courseId: e.target.value })}
                          required
                        >
                          <option value="">수업 선택</option>
                          {courses?.map((course) => (
                            <option key={course.id} value={course.id}>
                              {course.courseName}
                            </option>
                          ))}
                        </select>
                      </div>

                      <div className="form-group">
                        <label>원래 수업 날짜 *</label>
                        <input
                          type="date"
                          value={formData.originalDate}
                          onChange={(e) => setFormData({ ...formData, originalDate: e.target.value })}
                          required
                        />
                      </div>
                    </>
                  )}

                  <div className="form-group">
                    <label>보강 날짜 *</label>
                    <input
                      type="date"
                      value={formData.makeupDate}
                      onChange={(e) => setFormData({ ...formData, makeupDate: e.target.value })}
                      min={new Date().toISOString().split('T')[0]}
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label>보강 시간 *</label>
                    <input
                      type="time"
                      value={formData.makeupTime}
                      onChange={(e) => setFormData({ ...formData, makeupTime: e.target.value })}
                      required
                    />
                  </div>

                  <div className="form-group">
                    <label>사유</label>
                    <textarea
                      value={formData.reason}
                      onChange={(e) => setFormData({ ...formData, reason: e.target.value })}
                      placeholder="보강 사유를 입력하세요"
                      rows="3"
                    />
                  </div>

                  <div className="form-group">
                    <label>메모</label>
                    <textarea
                      value={formData.memo}
                      onChange={(e) => setFormData({ ...formData, memo: e.target.value })}
                      placeholder="추가 메모를 입력하세요"
                      rows="3"
                    />
                  </div>
                </form>
              </div>

              <div className="modal-footer">
                <button type="submit" className="btn-primary" onClick={handleSubmit}>
                  {editingMakeup ? '수정' : '등록'}
                </button>
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => {
                    setShowModal(false);
                    setEditingMakeup(null);
                    resetForm();
                  }}
                >
                  취소
                </button>
              </div>
            </div>
          </div>
        )}
    </div>
  );
}

export default MakeupClasses;
