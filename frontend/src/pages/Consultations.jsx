import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import LoadingSpinner from '../components/LoadingSpinner';
import { consultationAPI, studentAPI, fileAPI, authAPI } from '../services/api';
import { getTodayString, getLocalDateString } from '../utils/dateUtils';
import '../styles/Consultations.css';

function Consultations() {
  const queryClient = useQueryClient();
  const [selectedStudent, setSelectedStudent] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedConsultation, setSelectedConsultation] = useState(null);
  const [audioFile, setAudioFile] = useState(null);
  const [documentFile, setDocumentFile] = useState(null);
  const [audioFiles, setAudioFiles] = useState([]);
  const [documentFiles, setDocumentFiles] = useState([]);
  const [existingAudioFiles, setExistingAudioFiles] = useState([]);
  const [existingDocumentFiles, setExistingDocumentFiles] = useState([]);
  const [selectedStudentsForConsultation, setSelectedStudentsForConsultation] = useState([]);
  const [studentSearchTerm, setStudentSearchTerm] = useState('');
  
  // 학생 선택 드롭다운 상태
  const [showStudentDropdown, setShowStudentDropdown] = useState(false);
  const [studentSearchQuery, setStudentSearchQuery] = useState('');
  const [showExportModal, setShowExportModal] = useState(false);
  const [exportDateRange, setExportDateRange] = useState({
    startDate: '',
    endDate: ''
  });
  const [newConsultation, setNewConsultation] = useState({
    studentId: '',
    title: '',
    content: '',
  });

  // 상담 예약 가능한 최소 날짜 계산
  const getMinConsultationDate = () => {
    const now = new Date();
    const currentHour = now.getHours();
    
    // 오늘 18시가 지났다면 다다음날부터, 아니면 내일부터
    const daysToAdd = currentHour >= 18 ? 2 : 1;
    
    const minDate = new Date();
    minDate.setDate(minDate.getDate() + daysToAdd);
    
    return getLocalDateString(minDate);
  };

  // 사용자 프로필 조회
  // 사용자 프로필 조회
  const { data: profile } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
  });

  const isParent = profile?.role === 'PARENT';

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

  // 학생 선택 드롭다운 핸들러
  const handleStudentSelect = (student) => {
    setNewConsultation(prev => ({
      ...prev,
      studentId: student.id.toString()
    }));
    setSelectedStudentsForConsultation([student.id]);
    setShowStudentDropdown(false);
    setStudentSearchQuery('');
  };

  // 선택된 학생 정보 가져오기
  const getSelectedStudent = () => {
    return students.find(student => student.id.toString() === newConsultation.studentId);
  };

  // 필터링된 학생 목록 (드롭다운용)
  const getFilteredStudentsForDropdown = () => {
    if (!studentSearchQuery) return students;
    
    return students.filter(student => 
      student.studentName.toLowerCase().includes(studentSearchQuery.toLowerCase()) ||
      student.parentName?.toLowerCase().includes(studentSearchQuery.toLowerCase())
    );
  };

  const resetForm = () => {
    setNewConsultation({
      studentId: '',
      title: '',
      content: '',
      consultationType: '재원생상담',
      actionItems: '',
      nextConsultationDate: '',
    });
    setSelectedStudentsForConsultation([]);
    setStudentSearchTerm('');
    setAudioFile(null);
    setDocumentFile(null);
    setAudioFiles([]);
    setDocumentFiles([]);
    
    // 파일 input 초기화
    const audioInput = document.getElementById('audio-file');
    const documentInput = document.getElementById('document-file');
    if (audioInput) audioInput.value = '';
    if (documentInput) documentInput.value = '';
  };

  const handleFileUpload = async (files, type) => {
    try {
      if (files.length === 1) {
        const response = type === 'audio' 
          ? await fileAPI.uploadAudio(files[0])
          : await fileAPI.uploadDocument(files[0]);
        return `${response.data.fileName}::${response.data.filePath}`;
      } else {
        const response = type === 'audio' 
          ? await fileAPI.uploadMultipleAudio(files)
          : await fileAPI.uploadMultipleDocument(files);
        return response.data.files.map(f => `${f.fileName}::${f.filePath}`).join(',');
      }
    } catch (error) {
      alert('파일 업로드에 실패했습니다.');
      return null;
    }
  };

  // "원본파일명::경로" 또는 "경로" 에서 표시명/경로 추출
  const parseFileEntry = (entry) => {
    const trimmed = entry.trim();
    if (trimmed.includes('::')) {
      const [name, path] = trimmed.split('::');
      return { name, path };
    }
    return { name: trimmed.split('/').pop(), path: trimmed };
  };

  const handleStudentToggle = (studentId) => {
    setSelectedStudentsForConsultation(prev => 
      prev.includes(studentId) 
        ? prev.filter(id => id !== studentId)
        : [...prev, studentId]
    );
  };

  const handleSelectAll = () => {
    if (selectedStudentsForConsultation.length === filteredStudents.length) {
      setSelectedStudentsForConsultation([]);
    } else {
      setSelectedStudentsForConsultation(filteredStudents.map(student => student.id));
    }
  };

  // 학생 목록 필터링 및 정렬
  const filteredStudents = students
    .filter(student => 
      student.studentName.toLowerCase().includes(studentSearchTerm.toLowerCase()) ||
      student.parentName.toLowerCase().includes(studentSearchTerm.toLowerCase())
    )
    .sort((a, b) => a.studentName.localeCompare(b.studentName, 'ko'));

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (selectedStudentsForConsultation.length === 0) {
      alert('학생을 선택해주세요.');
      return;
    }
    
    let recordingFileUrl = '';
    let attachmentFileUrl = '';

    // 파일 업로드
    if (audioFiles.length > 0) {
      recordingFileUrl = await handleFileUpload(audioFiles, 'audio');
      if (!recordingFileUrl) return;
    }
    
    if (documentFiles.length > 0) {
      attachmentFileUrl = await handleFileUpload(documentFiles, 'document');
      if (!attachmentFileUrl) return;
    }

    // 선택된 각 학생에 대해 상담 기록 생성
    for (const studentId of selectedStudentsForConsultation) {
      const consultationData = {
        ...newConsultation,
        studentId,
        consultationDate: getTodayString(),
        recordingFileUrl,
        attachmentFileUrl
      };
      
      await createMutation.mutateAsync(consultationData);
    }
    
    queryClient.invalidateQueries(['consultations']);
    setShowCreateModal(false);
    resetForm();
    alert(`${selectedStudentsForConsultation.length}명의 학생에 대한 상담 기록이 등록되었습니다.`);
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
    
    // 기존 파일들을 새 파일 배열에 통합
    const audioFiles = consultation.recordingFileUrl ? consultation.recordingFileUrl.split(',') : [];
    const documentFiles = consultation.attachmentFileUrl ? consultation.attachmentFileUrl.split(',') : [];
    
    setAudioFiles(audioFiles.map((entry, index) => {
      const { name, path } = parseFileEntry(entry);
      return { name, isExisting: true, url: entry.trim() };
    }));
    
    setDocumentFiles(documentFiles.map((entry, index) => {
      const { name, path } = parseFileEntry(entry);
      return { name, isExisting: true, url: entry.trim() };
    }));
    
    setShowEditModal(true);
  };

  const handleUpdate = async (e) => {
    e.preventDefault();
    
    // 기존 파일 URL 수집
    const existingAudioUrls = audioFiles.filter(f => f.isExisting).map(f => f.url);
    const existingDocUrls = documentFiles.filter(f => f.isExisting).map(f => f.url);
    
    // 새 파일 업로드
    const newAudioFiles = audioFiles.filter(f => !f.isExisting).map(f => f.file);
    const newDocFiles = documentFiles.filter(f => !f.isExisting).map(f => f.file);
    
    let recordingFileUrl = existingAudioUrls.join(',');
    let attachmentFileUrl = existingDocUrls.join(',');

    if (newAudioFiles.length > 0) {
      const uploaded = await handleFileUpload(newAudioFiles, 'audio');
      if (!uploaded) return;
      recordingFileUrl = [recordingFileUrl, uploaded].filter(Boolean).join(',');
    }
    
    if (newDocFiles.length > 0) {
      const uploaded = await handleFileUpload(newDocFiles, 'document');
      if (!uploaded) return;
      attachmentFileUrl = [attachmentFileUrl, uploaded].filter(Boolean).join(',');
    }

    const consultationData = {
      ...newConsultation,
      consultationDate: selectedConsultation.consultationDate,
      recordingFileUrl: recordingFileUrl || null,
      attachmentFileUrl: attachmentFileUrl || null
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
      // 서버에서 보낸 Content-Type 사용
      const contentType = response.headers['content-type'] || 'application/octet-stream';
      const url = window.URL.createObjectURL(new Blob([response.data], { type: contentType }));
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

  /**
   * Excel 파일 다운로드 처리
   * @param {Blob} blob Excel 파일 데이터
   * @param {string} fileName 파일명
   */
  const downloadExcelFile = (blob, fileName) => {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', fileName);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  };

  /**
   * 전체 상담 이력 Excel 다운로드
   */
  const handleExportAll = async () => {
    console.log('=== Export All Debug ===');
    try {
      console.log('Calling exportAll API...');
      const response = await consultationAPI.exportAll();
      console.log('Export All Response:', response);
      const fileName = `전체_상담이력_${getTodayString()}.xlsx`;
      downloadExcelFile(response.data, fileName);
      alert('전체 상담 이력이 다운로드되었습니다.');
      console.log('=== Export All Success ===');
    } catch (error) {
      console.error('=== Export All Error ===');
      console.error('Error:', error);
      console.error('Error response:', error.response);
      alert(`전체 상담 이력 다운로드에 실패했습니다: ${error.message}`);
    }
  };

  /**
   * 선택된 학생의 상담 이력 Excel 다운로드
   */
  const handleExportByStudent = async () => {
    if (!selectedStudent) {
      alert('학생을 먼저 선택해주세요.');
      return;
    }
    
    console.log('=== Excel Export Debug ===');
    console.log('Selected Student ID:', selectedStudent);
    
    try {
      console.log('Calling API...');
      const response = await consultationAPI.exportByStudent(selectedStudent);
      console.log('API Response:', response);
      console.log('Response status:', response.status);
      console.log('Response data type:', typeof response.data);
      console.log('Response data size:', response.data?.size || 'unknown');
      
      const studentName = students.find(s => s.id == selectedStudent)?.studentName || '학생';
      const fileName = `${studentName}_상담이력_${getTodayString()}.xlsx`;
      
      console.log('Student name:', studentName);
      console.log('File name:', fileName);
      
      downloadExcelFile(response.data, fileName);
      alert(`${studentName} 학생의 상담 이력이 다운로드되었습니다.`);
      console.log('=== Excel Export Success ===');
    } catch (error) {
      console.error('=== Excel Export Error ===');
      console.error('Error object:', error);
      console.error('Error message:', error.message);
      console.error('Error response:', error.response);
      console.error('Error response status:', error.response?.status);
      console.error('Error response data:', error.response?.data);
      console.error('Error response headers:', error.response?.headers);
      
      // Blob 내용 확인
      if (error.response?.data instanceof Blob) {
        console.log('Response is Blob, reading text...');
        const text = await error.response.data.text();
        console.error('Blob content:', text);
        try {
          const jsonError = JSON.parse(text);
          console.error('Parsed error:', jsonError);
        } catch (e) {
          console.error('Could not parse as JSON:', text);
        }
      }
      
      alert(`Excel 다운로드에 실패했습니다: ${error.message}`);
    }
  };

  /**
   * 기간별 상담 이력 Excel 다운로드
   */
  const handleExportByDateRange = async () => {
    if (!exportDateRange.startDate || !exportDateRange.endDate) {
      alert('시작일과 종료일을 모두 선택해주세요.');
      return;
    }
    
    try {
      const response = await consultationAPI.exportByDateRange(exportDateRange.startDate, exportDateRange.endDate);
      const fileName = `상담이력_${exportDateRange.startDate}_${exportDateRange.endDate}.xlsx`;
      downloadExcelFile(response.data, fileName);
      setShowExportModal(false);
      setExportDateRange({ startDate: '', endDate: '' });
      alert('기간별 상담 이력이 다운로드되었습니다.');
    } catch (error) {
      alert('Excel 다운로드에 실패했습니다.');
    }
  };

  if (isLoading) return <LoadingSpinner />;

  return (
    <div className="page-wrapper">
      <div className="page-header">
        <div className="page-header-content">
          <div className="page-title-section">
            <h1 className="page-title">
              <i className="fas fa-chart-line"></i>
              학습 현황
            </h1>
            <p className="page-subtitle">
              {profile?.role === 'PARENT' ? '자녀의 학습 현황을 확인하세요' : '학생 학습 현황을 관리합니다'}
            </p>
          </div>
          {(profile?.role === 'ADMIN' || profile?.role === 'TEACHER') && (
            <div className="header-actions">
              <button className="btn-secondary btn-with-icon" onClick={() => setShowExportModal(true)}>
                <i className="fas fa-file-excel"></i>
                Excel 내보내기
              </button>
              <button className="btn-primary btn-with-icon" onClick={() => {
                resetForm();
                setShowCreateModal(true);
              }}>
                <i className="fas fa-plus"></i>
                학습 기록 등록
              </button>
            </div>
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
              <div className="result-count-actions">
                <div className="result-count">
                  <i className="fas fa-chart-line"></i>
                  총 <strong>{consultations.length}</strong>건의 학습 기록
                </div>
                {(profile?.role === 'ADMIN' || profile?.role === 'TEACHER') && (
                  <button className="btn-export" onClick={handleExportByStudent}>
                    <i className="fas fa-download"></i>
                    이 학생 Excel 다운로드
                  </button>
                )}
              </div>
            )}
          </div>
        )}

        {/* 상담 이력 목록 */}
        <div className="consultations-section">
          {consultations.length === 0 ? (
            <div className="empty-state">
              <i className="fas fa-chart-line"></i>
              <h3>학습 기록이 없습니다</h3>
              <p>{profile?.role === 'PARENT' ? '아직 학습 기록이 없습니다.' : '학생을 선택하고 학습 기록을 등록해보세요.'}</p>
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
                        <div className="file-column">
                          <div className="file-column-title"><i className="fas fa-microphone"></i> 녹음 파일</div>
                          {consultation.recordingFileUrl.split(',').map((entry, idx) => {
                            const { name, path } = parseFileEntry(entry);
                            return (
                              <div key={`rec-${idx}`} className="file-download-item">
                                <button className="file-download-btn audio" onClick={() => handleFileDownload(path, name)}>
                                  <i className="fas fa-download"></i> {name}
                                </button>
                              </div>
                            );
                          })}
                        </div>
                      )}
                      {consultation.attachmentFileUrl && (
                        <div className="file-column">
                          <div className="file-column-title"><i className="fas fa-file-alt"></i> 첨부 파일</div>
                          {consultation.attachmentFileUrl.split(',').map((entry, idx) => {
                            const { name, path } = parseFileEntry(entry);
                            return (
                              <div key={`att-${idx}`} className="file-download-item">
                                <button className="file-download-btn document" onClick={() => handleFileDownload(path, name)}>
                                  <i className="fas fa-download"></i> {name}
                                </button>
                              </div>
                            );
                          })}
                        </div>
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

      {/* 학습 기록 등록 모달 */}
      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal-content large" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>학습 기록 등록</h2>
              <button className="modal-close" onClick={() => {
                setShowCreateModal(false);
                resetForm();
              }}>×</button>
            </div>
            <form onSubmit={handleSubmit}>
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
                            {getSelectedStudent().parentName} · {getSelectedStudent().parentPhone || getSelectedStudent().phoneNumber}
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
                            placeholder="학생 이름 또는 학부모 이름으로 검색..."
                            value={studentSearchQuery}
                            onChange={(e) => setStudentSearchQuery(e.target.value)}
                            onClick={(e) => e.stopPropagation()}
                          />
                        </div>
                        <div className="student-list">
                          {getFilteredStudentsForDropdown().map(student => (
                            <div
                              key={student.id}
                              className="student-option"
                              onClick={() => handleStudentSelect(student)}
                            >
                              <div className="student-info">
                                <span className="student-name">{student.studentName}</span>
                                <span className="parent-info">
                                  {student.parentName} · {student.parentPhone || student.phoneNumber}
                                </span>
                              </div>
                            </div>
                          ))}
                          {getFilteredStudentsForDropdown().length === 0 && (
                            <div className="no-students">검색 결과가 없습니다.</div>
                          )}
                        </div>
                      </div>
                    )}
                  </div>
                </div>

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
                  <label>학습 기록 내용 *</label>
                  <textarea 
                    value={newConsultation.content}
                    onChange={(e) => setNewConsultation({...newConsultation, content: e.target.value})}
                    rows="5"
                    required
                  />
                </div>

                <div className="form-group">
                  <div className="form-group">
                    <label>녹음 파일 (여러 개 선택 가능)</label>
                    <div className="file-input-wrapper">
                      <input 
                        type="file" 
                        id="audio-file"
                        accept="audio/*"
                        multiple
                        onChange={(e) => setAudioFiles([...e.target.files])}
                        className="file-input-hidden"
                      />
                      <label htmlFor="audio-file" className="file-input-custom audio">
                        <i className="fas fa-microphone"></i>
                        <span>
                          {audioFiles.length > 0 
                            ? `${audioFiles.length}개 파일 선택됨` 
                            : '녹음 파일 선택'
                          }
                        </span>
                      </label>
                      {audioFiles.length > 0 && (
                        <button 
                          type="button" 
                          className="file-remove-btn"
                          onClick={() => {
                            setAudioFiles([]);
                            document.getElementById('audio-file').value = '';
                          }}
                        >
                          <i className="fas fa-times"></i>
                        </button>
                      )}
                    </div>
                    {audioFiles.length > 0 && (
                      <div className="file-list">
                        {audioFiles.map((file, index) => (
                          <div key={index} className="file-item">
                            <i className="fas fa-microphone"></i>
                            <span>{file.name}</span>
                            <button 
                              type="button"
                              onClick={() => {
                                const newFiles = audioFiles.filter((_, i) => i !== index);
                                setAudioFiles(newFiles);
                              }}
                              className="file-item-remove"
                            >
                              <i className="fas fa-times"></i>
                            </button>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                  <div className="form-group">
                    <label>첨부 파일 (여러 개 선택 가능)</label>
                    <div className="file-input-wrapper">
                      <input 
                        type="file" 
                        id="document-file"
                        accept=".pdf,.doc,.docx,.hwp,.txt"
                        multiple
                        onChange={(e) => setDocumentFiles([...e.target.files])}
                        className="file-input-hidden"
                      />
                      <label htmlFor="document-file" className="file-input-custom document">
                        <i className="fas fa-file-alt"></i>
                        <span>
                          {documentFiles.length > 0 
                            ? `${documentFiles.length}개 파일 선택됨` 
                            : '문서 파일 선택'
                          }
                        </span>
                      </label>
                      {documentFiles.length > 0 && (
                        <button 
                          type="button" 
                          className="file-remove-btn"
                          onClick={() => {
                            setDocumentFiles([]);
                            document.getElementById('document-file').value = '';
                          }}
                        >
                          <i className="fas fa-times"></i>
                        </button>
                      )}
                    </div>
                    {documentFiles.length > 0 && (
                      <div className="file-list">
                        {documentFiles.map((file, index) => (
                          <div key={index} className="file-item">
                            <i className="fas fa-file-alt"></i>
                            <span>{file.name}</span>
                            <button 
                              type="button"
                              onClick={() => {
                                const newFiles = documentFiles.filter((_, i) => i !== index);
                                setDocumentFiles(newFiles);
                              }}
                              className="file-item-remove"
                            >
                              <i className="fas fa-times"></i>
                            </button>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn-secondary" onClick={() => {
                  setShowCreateModal(false);
                  resetForm();
                }}>
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

      {/* 학습 기록 수정 모달 */}
      {showEditModal && (
        <div className="modal-overlay" onClick={() => setShowEditModal(false)}>
          <div className="modal-content large" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>학습 기록 수정</h2>
              <button className="modal-close" onClick={() => setShowEditModal(false)}>×</button>
            </div>
            <form onSubmit={handleUpdate}>
              <div className="modal-body">
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
                  <label>학습 기록 내용 *</label>
                  <textarea 
                    value={newConsultation.content}
                    onChange={(e) => setNewConsultation({...newConsultation, content: e.target.value})}
                    rows="5"
                    required
                  />
                </div>

                <div className="form-group">
                  <div className="form-group">
                    <label>녹음 파일 (여러 개 선택 가능)</label>
                    <div className="file-input-wrapper">
                      <input 
                        type="file" 
                        id="edit-audio-file"
                        accept="audio/*"
                        multiple
                        onChange={(e) => {
                          const newFiles = [...e.target.files].map(file => ({
                            name: file.name,
                            file: file,
                            isExisting: false
                          }));
                          setAudioFiles(prev => [...prev.filter(f => f.isExisting), ...newFiles]);
                        }}
                        className="file-input-hidden"
                      />
                      <label htmlFor="edit-audio-file" className="file-input-custom audio">
                        <i className="fas fa-microphone"></i>
                        <span>
                          {audioFiles.length > 0 
                            ? `${audioFiles.length}개 파일 선택됨` 
                            : '녹음 파일 선택'
                          }
                        </span>
                      </label>
                      {audioFiles.length > 0 && (
                        <button 
                          type="button" 
                          className="file-remove-btn"
                          onClick={() => {
                            setAudioFiles([]);
                            document.getElementById('edit-audio-file').value = '';
                          }}
                        >
                          <i className="fas fa-times"></i>
                        </button>
                      )}
                    </div>
                    {audioFiles.length > 0 && (
                      <div className="file-list">
                        {audioFiles.map((file, index) => (
                          <div key={index} className={`file-item ${file.isExisting ? 'existing' : ''}`}>
                            <i className="fas fa-microphone"></i>
                            <span>{file.name}</span>
                            <button 
                              type="button"
                              onClick={() => {
                                const newFiles = audioFiles.filter((_, i) => i !== index);
                                setAudioFiles(newFiles);
                              }}
                              className="file-item-remove"
                            >
                              <i className="fas fa-times"></i>
                            </button>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                  <div className="form-group">
                    <label>첨부 파일 (여러 개 선택 가능)</label>
                    <div className="file-input-wrapper">
                      <input 
                        type="file" 
                        id="edit-document-file"
                        accept=".pdf,.doc,.docx,.hwp,.txt"
                        multiple
                        onChange={(e) => {
                          const newFiles = [...e.target.files].map(file => ({
                            name: file.name,
                            file: file,
                            isExisting: false
                          }));
                          setDocumentFiles(prev => [...prev.filter(f => f.isExisting), ...newFiles]);
                        }}
                        className="file-input-hidden"
                      />
                      <label htmlFor="edit-document-file" className="file-input-custom document">
                        <i className="fas fa-file-alt"></i>
                        <span>
                          {documentFiles.length > 0 
                            ? `${documentFiles.length}개 파일 선택됨` 
                            : '문서 파일 선택'
                          }
                        </span>
                      </label>
                      {documentFiles.length > 0 && (
                        <button 
                          type="button" 
                          className="file-remove-btn"
                          onClick={() => {
                            setDocumentFiles([]);
                            document.getElementById('edit-document-file').value = '';
                          }}
                        >
                          <i className="fas fa-times"></i>
                        </button>
                      )}
                    </div>
                    {documentFiles.length > 0 && (
                      <div className="file-list">
                        {documentFiles.map((file, index) => (
                          <div key={index} className={`file-item ${file.isExisting ? 'existing' : ''}`}>
                            <i className="fas fa-file-alt"></i>
                            <span>{file.name}</span>
                            <button 
                              type="button"
                              onClick={() => {
                                const newFiles = documentFiles.filter((_, i) => i !== index);
                                setDocumentFiles(newFiles);
                              }}
                              className="file-item-remove"
                            >
                              <i className="fas fa-times"></i>
                            </button>
                          </div>
                        ))}
                      </div>
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

      {/* Excel 내보내기 모달 */}
      {showExportModal && (
        <div className="modal-overlay" onClick={() => setShowExportModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>상담 이력 Excel 내보내기</h2>
              <button className="modal-close" onClick={() => setShowExportModal(false)}>×</button>
            </div>
            <div className="modal-body">
              <div className="export-options">
                <button className="export-option-btn" onClick={handleExportAll}>
                  <i className="fas fa-globe"></i>
                  <div>
                    <h3>전체 상담 이력</h3>
                    <p>모든 학생의 상담 이력을 다운로드합니다</p>
                  </div>
                </button>
                
                <button 
                  className="export-option-btn" 
                  onClick={handleExportByStudent}
                  disabled={!selectedStudent}
                >
                  <i className="fas fa-user"></i>
                  <div>
                    <h3>선택된 학생 상담 이력</h3>
                    <p>{selectedStudent ? 
                      `${students.find(s => s.id == selectedStudent)?.studentName} 학생의 상담 이력` : 
                      '학생을 먼저 선택해주세요'
                    }</p>
                  </div>
                </button>
                
                <div className="export-option-btn date-range">
                  <i className="fas fa-calendar-alt"></i>
                  <div>
                    <h3>기간별 상담 이력</h3>
                    <div className="date-inputs">
                      <input 
                        type="date" 
                        value={exportDateRange.startDate}
                        onChange={(e) => setExportDateRange({...exportDateRange, startDate: e.target.value})}
                        placeholder="시작일"
                      />
                      <span>~</span>
                      <input 
                        type="date" 
                        value={exportDateRange.endDate}
                        onChange={(e) => setExportDateRange({...exportDateRange, endDate: e.target.value})}
                        placeholder="종료일"
                      />
                      <button 
                        className="btn-primary btn-sm"
                        onClick={handleExportByDateRange}
                        disabled={!exportDateRange.startDate || !exportDateRange.endDate}
                      >
                        다운로드
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Consultations;
