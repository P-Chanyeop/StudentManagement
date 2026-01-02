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
  const [audioFiles, setAudioFiles] = useState([]);
  const [documentFiles, setDocumentFiles] = useState([]);
  const [existingAudioFiles, setExistingAudioFiles] = useState([]);
  const [existingDocumentFiles, setExistingDocumentFiles] = useState([]);
  const [selectedStudentsForConsultation, setSelectedStudentsForConsultation] = useState([]);
  const [studentSearchTerm, setStudentSearchTerm] = useState('');
  const [showExportModal, setShowExportModal] = useState(false);
  const [exportDateRange, setExportDateRange] = useState({
    startDate: '',
    endDate: ''
  });
  const [newConsultation, setNewConsultation] = useState({
    studentId: '',
    title: '',
    content: '',
    consultationType: '재원생상담',
    actionItems: '',
    nextConsultationDate: '',
  });

  // 상담 예약 가능한 최소 날짜 계산
  const getMinConsultationDate = () => {
    const now = new Date();
    const currentHour = now.getHours();
    
    // 오늘 18시가 지났다면 다다음날부터, 아니면 내일부터
    const daysToAdd = currentHour >= 18 ? 2 : 1;
    
    const minDate = new Date();
    minDate.setDate(minDate.getDate() + daysToAdd);
    
    return minDate.toISOString().split('T')[0];
  };

  // 사용자 프로필 조회
  const { data: profile } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
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
        // 단일 파일
        const response = type === 'audio' 
          ? await fileAPI.uploadAudio(files[0])
          : await fileAPI.uploadDocument(files[0]);
        return response.data.filePath;
      } else {
        // 다중 파일
        const response = type === 'audio' 
          ? await fileAPI.uploadMultipleAudio(files)
          : await fileAPI.uploadMultipleDocument(files);
        return response.data.files.map(file => file.filePath).join(',');
      }
    } catch (error) {
      alert('파일 업로드에 실패했습니다.');
      return null;
    }
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
        consultationDate: new Date().toISOString().split('T')[0],
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
    
    setAudioFiles(audioFiles.map((url, index) => ({
      name: url.trim().split('/').pop() || `녹음파일_${index + 1}`,
      isExisting: true,
      url: url.trim()
    })));
    
    setDocumentFiles(documentFiles.map((url, index) => ({
      name: url.trim().split('/').pop() || `첨부파일_${index + 1}`,
      isExisting: true,
      url: url.trim()
    })));
    
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
    try {
      const response = await consultationAPI.exportAll();
      const fileName = `전체_상담이력_${new Date().toISOString().split('T')[0]}.xlsx`;
      downloadExcelFile(response.data, fileName);
      alert('전체 상담 이력이 다운로드되었습니다.');
    } catch (error) {
      alert('Excel 다운로드에 실패했습니다.');
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
    
    try {
      const response = await consultationAPI.exportByStudent(selectedStudent);
      const studentName = students.find(s => s.id == selectedStudent)?.studentName || '학생';
      const fileName = `${studentName}_상담이력_${new Date().toISOString().split('T')[0]}.xlsx`;
      downloadExcelFile(response.data, fileName);
      alert(`${studentName} 학생의 상담 이력이 다운로드되었습니다.`);
    } catch (error) {
      alert('Excel 다운로드에 실패했습니다.');
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
              <i className="fas fa-comments"></i>
              상담 내역
            </h1>
            <p className="page-subtitle">
              {profile?.role === 'PARENT' ? '자녀의 상담 이력을 확인하세요' : '학생 상담 이력을 관리합니다'}
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
                상담 등록
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
                  <i className="fas fa-comments"></i>
                  총 <strong>{consultations.length}</strong>건의 상담 기록
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
}
export default Consultations;
