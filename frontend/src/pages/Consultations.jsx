import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import LoadingSpinner from '../components/LoadingSpinner';
import { consultationAPI, studentAPI, fileAPI, authAPI } from '../services/api';
import '../styles/Consultations.css';

function Consultations() {
  const queryClient = useQueryClient();
  const [selectedStudent, setSelectedStudent] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedConsultation, setSelectedConsultation] = useState(null);
  const [audioFile, setAudioFile] = useState(null);
  const [documentFile, setDocumentFile] = useState(null);
  const [newConsultation, setNewConsultation] = useState({
    studentId: '',
    title: '',
    content: '',
    consultationType: '학습상담',
    actionItems: '',
    nextConsultationDate: '',
  });

  // 사용자 프로필 조회
  const { data: profile } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
  });

  // 학생 목록 조회 (관리자/선생님만)
  const { data: students = [] } = useQuery({
    queryKey: ['students'],
    queryFn: async () => {
      const response = await studentAPI.getActive();
      return response.data;
    },
    enabled: !!profile && (profile.role === 'ADMIN' || profile.role === 'TEACHER'),
  });

  // 상담 이력 조회
  const { data: consultations = [], isLoading } = useQuery({
    queryKey: ['consultations', selectedStudent, profile?.role],
    queryFn: async () => {
      if (profile?.role === 'PARENT') {
        const response = await consultationAPI.getMyChildren();
        return response.data;
      } else if (selectedStudent) {
        const response = await consultationAPI.getByStudent(selectedStudent);
        return response.data;
      }
      return [];
    },
    enabled: !!profile && (profile.role === 'PARENT' || !!selectedStudent),
  });

  // 상담 생성
  const createMutation = useMutation({
    mutationFn: (data) => consultationAPI.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries(['consultations']);
      setShowCreateModal(false);
      resetForm();
      alert('상담 기록이 등록되었습니다.');
    },
    onError: (error) => {
      alert('상담 기록 등록에 실패했습니다.');
    }
  });

  // 상담 수정
  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => consultationAPI.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries(['consultations']);
      setShowEditModal(false);
      setSelectedConsultation(null);
      alert('상담 기록이 수정되었습니다.');
    },
    onError: (error) => {
      alert('상담 기록 수정에 실패했습니다.');
    }
  });

  // 상담 삭제
  const deleteMutation = useMutation({
    mutationFn: (id) => consultationAPI.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries(['consultations']);
      alert('상담 기록이 삭제되었습니다.');
    },
    onError: (error) => {
      alert('상담 기록 삭제에 실패했습니다.');
    }
  });

  const resetForm = () => {
    setNewConsultation({
      studentId: '',
      title: '',
      content: '',
      consultationType: '학습상담',
      actionItems: '',
      nextConsultationDate: '',
    });
    setAudioFile(null);
    setDocumentFile(null);
  };

  const handleFileUpload = async (file, type) => {
    try {
      const response = type === 'audio' 
        ? await fileAPI.uploadAudio(file)
        : await fileAPI.uploadDocument(file);
      return response.data.filePath;
    } catch (error) {
      alert('파일 업로드에 실패했습니다.');
      return null;
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    let recordingFileUrl = '';
    let attachmentFileUrl = '';

    // 파일 업로드
    if (audioFile) {
      recordingFileUrl = await handleFileUpload(audioFile, 'audio');
      if (!recordingFileUrl) return;
    }
    
    if (documentFile) {
      attachmentFileUrl = await handleFileUpload(documentFile, 'document');
      if (!attachmentFileUrl) return;
    }

    const consultationData = {
      ...newConsultation,
      consultationDate: new Date().toISOString().split('T')[0],
      recordingFileUrl,
      attachmentFileUrl
    };

    createMutation.mutate(consultationData);
  };

  const handleEdit = (consultation) => {
    setSelectedConsultation(consultation);
    setNewConsultation({
      studentId: consultation.studentId,
      title: consultation.title,
      content: consultation.content,
      consultationType: consultation.consultationType,
      actionItems: consultation.actionItems || '',
      nextConsultationDate: consultation.nextConsultationDate || '',
    });
    setShowEditModal(true);
  };

  const handleUpdate = async (e) => {
    e.preventDefault();
    
    let recordingFileUrl = selectedConsultation.recordingFileUrl;
    let attachmentFileUrl = selectedConsultation.attachmentFileUrl;

    // 새 파일 업로드
    if (audioFile) {
      recordingFileUrl = await handleFileUpload(audioFile, 'audio');
      if (!recordingFileUrl) return;
    }
    
    if (documentFile) {
      attachmentFileUrl = await handleFileUpload(documentFile, 'document');
      if (!attachmentFileUrl) return;
    }

    const consultationData = {
      ...newConsultation,
      recordingFileUrl,
      attachmentFileUrl
    };

    updateMutation.mutate({ id: selectedConsultation.id, data: consultationData });
  };

  const handleDelete = (id) => {
    if (window.confirm('정말 삭제하시겠습니까?')) {
      deleteMutation.mutate(id);
    }
  };

  const handleFileDownload = async (filePath, fileName) => {
    try {
      const response = await fileAPI.download(filePath);
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', fileName);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      alert('파일 다운로드에 실패했습니다.');
    }
  };

  if (isLoading) return <LoadingSpinner />;

  return (
    <div className="page-wrapper">
      <div className="page-header">
        <div className="page-header-content">
          <div className="page-title-section">
            <h1 className="page-title">
              <i className="fas fa-comments"></i>
              상담 내역
            </h1>
            <p className="page-subtitle">
              {profile?.role === 'PARENT' ? '자녀의 상담 이력을 확인하세요' : '학생 상담 이력을 관리합니다'}
            </p>
          </div>
          {(profile?.role === 'ADMIN' || profile?.role === 'TEACHER') && (
            <button className="btn-primary btn-with-icon" onClick={() => setShowCreateModal(true)}>
              <i className="fas fa-plus"></i>
              상담 등록
            </button>
          )}
        </div>
      </div>

      <div className="page-content">
        {/* 학생 선택 (관리자/선생님만) */}
        {(profile?.role === 'ADMIN' || profile?.role === 'TEACHER') && (
          <div className="search-section">
            <div className="student-selector">
              <select
                value={selectedStudent}
                onChange={(e) => setSelectedStudent(e.target.value)}
              >
                <option value="">담당 학생을 선택해주세요</option>
                {students.map((student) => (
                  <option key={student.id} value={student.id}>
                    {student.studentName} ({student.parentName})
                  </option>
                ))}
              </select>
            </div>
            {selectedStudent && (
              <div className="result-count">
                <i className="fas fa-comments"></i>
                총 <strong>{consultations.length}</strong>건의 상담 기록
              </div>
            )}
          </div>
        )}

        {/* 상담 이력 목록 */}
        <div className="consultations-section">
          {consultations.length === 0 ? (
            <div className="empty-state">
              <i className="fas fa-comments"></i>
              <h3>상담 이력이 없습니다</h3>
              <p>{profile?.role === 'PARENT' ? '아직 상담 기록이 없습니다.' : '학생을 선택하고 상담을 등록해보세요.'}</p>
            </div>
          ) : (
            <div className="consultations-list">
              {consultations.map((consultation) => (
                <div key={consultation.id} className="consultation-card">
                  <div className="consultation-header">
                    <div className="consultation-info">
                      <h3>{consultation.title}</h3>
                      <div className="consultation-meta">
                        <span className="student-name">{consultation.studentName}</span>
                        <span className="consultation-date">{consultation.consultationDate}</span>
                        <span className={`consultation-type type-${consultation.consultationType}`}>
                          {consultation.consultationType}
                        </span>
                      </div>
                    </div>
                    {(profile?.role === 'ADMIN' || profile?.role === 'TEACHER') && (
                      <div className="consultation-actions">
                        <button className="btn-edit" onClick={() => handleEdit(consultation)}>
                          <i className="fas fa-edit"></i>
                        </button>
                        <button className="btn-delete" onClick={() => handleDelete(consultation.id)}>
                          <i className="fas fa-trash"></i>
                        </button>
                      </div>
                    )}
                  </div>
                  
                  <div className="consultation-content">
                    <p>{consultation.content}</p>
                    
                    {consultation.actionItems && (
                      <div className="action-items">
                        <strong>후속 조치:</strong>
                        <p>{consultation.actionItems}</p>
                      </div>
                    )}
                    
                    {consultation.nextConsultationDate && (
                      <div className="next-consultation">
                        <strong>다음 상담 예정일:</strong> {consultation.nextConsultationDate}
                      </div>
                    )}
                  </div>

                  {/* 파일 다운로드 */}
                  {(consultation.recordingFileUrl || consultation.attachmentFileUrl) && (
                    <div className="consultation-files">
                      {consultation.recordingFileUrl && (
                        <button 
                          className="file-download-btn audio"
                          onClick={() => handleFileDownload(consultation.recordingFileUrl, `상담녹음_${consultation.studentName}_${consultation.consultationDate}.mp3`)}
                        >
                          <i className="fas fa-microphone"></i>
                          녹음 파일 다운로드
                        </button>
                      )}
                      {consultation.attachmentFileUrl && (
                        <button 
                          className="file-download-btn document"
                          onClick={() => handleFileDownload(consultation.attachmentFileUrl, `상담자료_${consultation.studentName}_${consultation.consultationDate}`)}
                        >
                          <i className="fas fa-file-alt"></i>
                          첨부 파일 다운로드
                        </button>
                      )}
                    </div>
                  )}

                  <div className="consultation-footer">
                    <span className="consultant">상담자: {consultation.consultantName}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* 상담 등록 모달 */}
      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal-content large" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>상담 등록</h2>
              <button className="modal-close" onClick={() => setShowCreateModal(false)}>×</button>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="modal-body">
                <div className="form-group">
                  <label>학생 선택 *</label>
                  <select 
                    value={newConsultation.studentId} 
                    onChange={(e) => setNewConsultation({...newConsultation, studentId: e.target.value})}
                    required
                  >
                    <option value="">학생을 선택하세요</option>
                    {students.map(student => (
                      <option key={student.id} value={student.id}>
                        {student.studentName} ({student.parentName})
                      </option>
                    ))}
                  </select>
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>제목 *</label>
                    <input 
                      type="text" 
                      value={newConsultation.title}
                      onChange={(e) => setNewConsultation({...newConsultation, title: e.target.value})}
                      required 
                    />
                  </div>
                  <div className="form-group">
                    <label>상담 유형</label>
                    <select 
                      value={newConsultation.consultationType}
                      onChange={(e) => setNewConsultation({...newConsultation, consultationType: e.target.value})}
                    >
                      <option value="학습상담">학습상담</option>
                      <option value="진로상담">진로상담</option>
                      <option value="학부모상담">학부모상담</option>
                      <option value="생활상담">생활상담</option>
                    </select>
                  </div>
                </div>

                <div className="form-group">
                  <label>상담 내용 *</label>
                  <textarea 
                    value={newConsultation.content}
                    onChange={(e) => setNewConsultation({...newConsultation, content: e.target.value})}
                    rows="5"
                    required
                  />
                </div>

                <div className="form-group">
                  <label>후속 조치 사항</label>
                  <textarea 
                    value={newConsultation.actionItems}
                    onChange={(e) => setNewConsultation({...newConsultation, actionItems: e.target.value})}
                    rows="3"
                  />
                </div>

                <div className="form-group">
                  <label>다음 상담 예정일</label>
                  <input 
                    type="date" 
                    value={newConsultation.nextConsultationDate}
                    onChange={(e) => setNewConsultation({...newConsultation, nextConsultationDate: e.target.value})}
                  />
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>녹음 파일</label>
                    <input 
                      type="file" 
                      accept="audio/*"
                      onChange={(e) => setAudioFile(e.target.files[0])}
                    />
                  </div>
                  <div className="form-group">
                    <label>첨부 파일</label>
                    <input 
                      type="file" 
                      accept=".pdf,.doc,.docx,.hwp,.txt"
                      onChange={(e) => setDocumentFile(e.target.files[0])}
                    />
                  </div>
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn-secondary" onClick={() => setShowCreateModal(false)}>
                  취소
                </button>
                <button type="submit" className="btn-primary">
                  등록
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 상담 수정 모달 */}
      {showEditModal && (
        <div className="modal-overlay" onClick={() => setShowEditModal(false)}>
          <div className="modal-content large" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>상담 수정</h2>
              <button className="modal-close" onClick={() => setShowEditModal(false)}>×</button>
            </div>
            <form onSubmit={handleUpdate}>
              <div className="modal-body">
                <div className="form-row">
                  <div className="form-group">
                    <label>제목 *</label>
                    <input 
                      type="text" 
                      value={newConsultation.title}
                      onChange={(e) => setNewConsultation({...newConsultation, title: e.target.value})}
                      required 
                    />
                  </div>
                  <div className="form-group">
                    <label>상담 유형</label>
                    <select 
                      value={newConsultation.consultationType}
                      onChange={(e) => setNewConsultation({...newConsultation, consultationType: e.target.value})}
                    >
                      <option value="학습상담">학습상담</option>
                      <option value="진로상담">진로상담</option>
                      <option value="학부모상담">학부모상담</option>
                      <option value="생활상담">생활상담</option>
                    </select>
                  </div>
                </div>

                <div className="form-group">
                  <label>상담 내용 *</label>
                  <textarea 
                    value={newConsultation.content}
                    onChange={(e) => setNewConsultation({...newConsultation, content: e.target.value})}
                    rows="5"
                    required
                  />
                </div>

                <div className="form-group">
                  <label>후속 조치 사항</label>
                  <textarea 
                    value={newConsultation.actionItems}
                    onChange={(e) => setNewConsultation({...newConsultation, actionItems: e.target.value})}
                    rows="3"
                  />
                </div>

                <div className="form-group">
                  <label>다음 상담 예정일</label>
                  <input 
                    type="date" 
                    value={newConsultation.nextConsultationDate}
                    onChange={(e) => setNewConsultation({...newConsultation, nextConsultationDate: e.target.value})}
                  />
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>새 녹음 파일</label>
                    <input 
                      type="file" 
                      accept="audio/*"
                      onChange={(e) => setAudioFile(e.target.files[0])}
                    />
                    {selectedConsultation?.recordingFileUrl && (
                      <small>현재: 녹음 파일 있음</small>
                    )}
                  </div>
                  <div className="form-group">
                    <label>새 첨부 파일</label>
                    <input 
                      type="file" 
                      accept=".pdf,.doc,.docx,.hwp,.txt"
                      onChange={(e) => setDocumentFile(e.target.files[0])}
                    />
                    {selectedConsultation?.attachmentFileUrl && (
                      <small>현재: 첨부 파일 있음</small>
                    )}
                  </div>
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn-secondary" onClick={() => setShowEditModal(false)}>
                  취소
                </button>
                <button type="submit" className="btn-primary">
                  수정
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default Consultations;
