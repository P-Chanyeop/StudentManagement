import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import LoadingSpinner from '../components/LoadingSpinner';
import { consultationAPI, studentAPI } from '../services/api';
import '../styles/Consultations.css';

function Consultations() {
  const queryClient = useQueryClient();
  const [selectedStudent, setSelectedStudent] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newConsultation, setNewConsultation] = useState({
    studentId: '',
    consultationDate: new Date().toISOString().split('T')[0],
    consultationType: 'GENERAL',
    content: '',
    followUpRequired: false,
    followUpDate: '',
  });

  // 학생 목록 조회
  const { data: students = [] } = useQuery({
    queryKey: ['students'],
    queryFn: async () => {
      const response = await studentAPI.getAll();
      return response.data;
    },
  });

  // 선택한 학생의 상담 내역 조회
  const { data: consultations = [], isLoading } = useQuery({
    queryKey: ['consultations', selectedStudent],
    queryFn: async () => {
      if (!selectedStudent) return [];
      const response = await consultationAPI.getByStudent(selectedStudent);
      return response.data;
    },
    enabled: !!selectedStudent,
  });

  // 상담 내역 생성 mutation
  const createMutation = useMutation({
    mutationFn: (data) => consultationAPI.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries(['consultations']);
      setShowCreateModal(false);
      setNewConsultation({
        studentId: '',
        consultationDate: new Date().toISOString().split('T')[0],
        consultationType: 'GENERAL',
        content: '',
        followUpRequired: false,
        followUpDate: '',
      });
      alert('상담 내역이 등록되었습니다.');
    },
    onError: (error) => {
      alert(`등록 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    },
  });

  const handleCreateConsultation = () => {
    if (!newConsultation.studentId || !newConsultation.content) {
      alert('학생과 상담 내용을 입력해주세요.');
      return;
    }

    if (newConsultation.followUpRequired && !newConsultation.followUpDate) {
      alert('후속 조치가 필요한 경우 날짜를 선택해주세요.');
      return;
    }

    createMutation.mutate(newConsultation);
  };

  // 상담 타입별 배지
  const getTypeBadge = (type) => {
    const typeMap = {
      GENERAL: { text: '일반 상담', color: '#03C75A' },
      ACADEMIC: { text: '학업 상담', color: '#0066FF' },
      BEHAVIORAL: { text: '생활 상담', color: '#FF9800' },
      CAREER: { text: '진로 상담', color: '#9C27B0' },
      PARENT_MEETING: { text: '학부모 면담', color: '#FF3B30' },
    };
    const { text, color } = typeMap[type] || { text: type, color: '#999' };
    return <span className="type-badge" style={{ backgroundColor: color }}>{text}</span>;
  };

  if (isLoading && selectedStudent) {
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
              <i className="fas fa-comments"></i>
              상담 내역 관리
            </h1>
            <p className="page-subtitle">학생들의 상담 내역을 기록하고 관리합니다</p>
          </div>
          <button
            className="btn-primary"
            onClick={() => setShowCreateModal(true)}
            disabled={!selectedStudent}
          >
            <i className="fas fa-plus"></i> 상담 내역 등록
          </button>
        </div>
      </div>

      <div className="page-content">
        <div className="search-section">
          <div className="student-selector">
            <label>학생 선택:</label>
            <select
              value={selectedStudent}
              onChange={(e) => setSelectedStudent(e.target.value)}
            >
              <option value="">학생을 선택하세요</option>
              {students.map((student) => (
                <option key={student.id} value={student.id}>
                  {student.studentName} ({student.studentPhone})
                </option>
              ))}
            </select>
          </div>
          {selectedStudent && (
            <div className="result-count">
              <i className="fas fa-comments"></i>
              총 <strong>{consultations.length}</strong>건
            </div>
          )}
        </div>

        {selectedStudent ? (
          <div className="consultations-list">
            {consultations.length === 0 ? (
              <div className="empty-state">등록된 상담 내역이 없습니다.</div>
            ) : (
            consultations.map((consultation) => (
              <div key={consultation.id} className="consultation-card">
                <div className="consultation-header">
                  <div className="date-info">
                    <span className="icon"><i className="fas fa-calendar-alt"></i></span>
                    <span className="date">
                      {new Date(consultation.consultationDate).toLocaleDateString('ko-KR')}
                    </span>
                  </div>
                  {getTypeBadge(consultation.consultationType)}
                </div>

                <div className="consultation-content">
                  <p>{consultation.content}</p>
                </div>

                {consultation.followUpRequired && (
                  <div className="follow-up">
                    <span className="follow-up-badge">후속 조치 필요</span>
                    {consultation.followUpDate && (
                      <span className="follow-up-date">
                        예정일: {new Date(consultation.followUpDate).toLocaleDateString('ko-KR')}
                      </span>
                    )}
                  </div>
                )}

                <div className="consultation-footer">
                  <span className="created-by">
                    작성자: {consultation.counselorName || '시스템'}
                  </span>
                  <span className="created-at">
                    {new Date(consultation.createdAt).toLocaleString('ko-KR')}
                  </span>
                </div>
              </div>
              ))
            )}
          </div>
        ) : (
          <div className="empty-state">학생을 선택하면 상담 내역이 표시됩니다.</div>
        )}
      </div>

      {/* 상담 내역 등록 모달 */}
      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>상담 내역 등록</h2>
              <button className="modal-close" onClick={() => setShowCreateModal(false)}>
                ×
              </button>
            </div>

            <div className="modal-body">
              <div className="form-group">
                <label>학생 선택 *</label>
                <select
                  value={newConsultation.studentId}
                  onChange={(e) =>
                    setNewConsultation({ ...newConsultation, studentId: e.target.value })
                  }
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
                  <label>상담 날짜 *</label>
                  <input
                    type="date"
                    value={newConsultation.consultationDate}
                    onChange={(e) =>
                      setNewConsultation({
                        ...newConsultation,
                        consultationDate: e.target.value,
                      })
                    }
                  />
                </div>

                <div className="form-group">
                  <label>상담 유형 *</label>
                  <select
                    value={newConsultation.consultationType}
                    onChange={(e) =>
                      setNewConsultation({
                        ...newConsultation,
                        consultationType: e.target.value,
                      })
                    }
                  >
                    <option value="GENERAL">일반 상담</option>
                    <option value="ACADEMIC">학업 상담</option>
                    <option value="BEHAVIORAL">생활 상담</option>
                    <option value="CAREER">진로 상담</option>
                    <option value="PARENT_MEETING">학부모 면담</option>
                  </select>
                </div>
              </div>

              <div className="form-group">
                <label>상담 내용 *</label>
                <textarea
                  value={newConsultation.content}
                  onChange={(e) =>
                    setNewConsultation({ ...newConsultation, content: e.target.value })
                  }
                  placeholder="상담 내용을 자세히 입력하세요"
                  rows="8"
                />
              </div>

              <div className="form-group">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={newConsultation.followUpRequired}
                    onChange={(e) =>
                      setNewConsultation({
                        ...newConsultation,
                        followUpRequired: e.target.checked,
                      })
                    }
                  />
                  <span>후속 조치 필요</span>
                </label>
              </div>

              {newConsultation.followUpRequired && (
                <div className="form-group">
                  <label>후속 조치 예정일</label>
                  <input
                    type="date"
                    value={newConsultation.followUpDate}
                    onChange={(e) =>
                      setNewConsultation({
                        ...newConsultation,
                        followUpDate: e.target.value,
                      })
                    }
                  />
                </div>
              )}
            </div>

            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setShowCreateModal(false)}>
                취소
              </button>
              <button className="btn-primary" onClick={handleCreateConsultation}>
                등록
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Consultations;
