import { useState, useEffect } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { consultationAPI, authAPI, studentAPI } from '../services/api';
import '../styles/ConsultationReservation.css';

function ConsultationReservation() {
  // 사용자 프로필 조회
  const { data: profile } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
  });

  // 전체 학생 목록 조회 (관리자/선생님용)
  const { data: allStudents = [] } = useQuery({
    queryKey: ['allStudents'],
    queryFn: async () => {
      const response = await studentAPI.getAll();
      return response.data;
    },
    enabled: !!profile && (profile.role === 'ADMIN' || profile.role === 'TEACHER'),
  });
  const { data: myStudents = [] } = useQuery({
    queryKey: ['myStudents'],
    queryFn: async () => {
      const response = await studentAPI.getMyStudents();
      return response.data;
    },
    enabled: !!profile && profile.role === 'PARENT',
  });

  const [formData, setFormData] = useState({
    // 선택된 학생 ID
    selectedStudentId: '',
    
    // 상담 정보
    consultationDate: '',
    consultationTime: '',
    consultationType: '학생상담',
    content: '',
    memo: ''
  });

  const [errors, setErrors] = useState({});

  // 상담 생성
  const createConsultation = useMutation({
    mutationFn: (data) => consultationAPI.create(data),
    onSuccess: () => {
      alert('상담이 예약되었습니다.');
      // 폼 초기화
      setFormData({
        selectedStudentId: '',
        consultationDate: '',
        consultationTime: '',
        consultationType: '재원생상담',
        content: '',
        memo: ''
      });
      setErrors({});
    },
    onError: (error) => {
      alert(`상담 예약 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    }
  });

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    
    // 에러 제거
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
  };

  const validateForm = () => {
    const newErrors = {};
    
    if (!formData.selectedStudentId) {
      newErrors.selectedStudentId = '상담 대상 학생을 선택해주세요.';
    }
    
    if (!formData.consultationDate) {
      newErrors.consultationDate = '상담 날짜를 선택해주세요.';
    }
    
    if (!formData.consultationTime) {
      newErrors.consultationTime = '상담 시간을 선택해주세요.';
    }
    
    if (!formData.content.trim()) {
      newErrors.content = '상담 내용을 입력해주세요.';
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    try {
      // 선택된 학생 정보 가져오기
      const studentList = profile?.role === 'PARENT' ? myStudents : allStudents;
      const selectedStudent = studentList.find(s => s.id.toString() === formData.selectedStudentId);
      
      if (!selectedStudent) {
        alert('선택된 학생 정보를 찾을 수 없습니다.');
        return;
      }

      // 상담 요청 데이터 구성
      const consultationData = {
        studentId: selectedStudent.id,
        consultationDate: formData.consultationDate,
        title: formData.consultationType,
        content: formData.content,
        consultationType: formData.consultationType,
        recordingFileUrl: null
      };
      
      createConsultation.mutate(consultationData);
    } catch (error) {
      console.error('상담 예약 실패:', error);
      alert('상담 예약 중 오류가 발생했습니다.');
    }
  };

  const consultationTypes = [
    { value: '학생상담', label: '학생상담' },
    { value: '학부모상담', label: '학부모상담' }
  ];

  return (
    <div className="consultation-reservation-container">
      <div className="page-header">
        <div className="header-content">
          <div className="header-text">
            <h1 className="page-title">
              <i className="fas fa-comments"></i>
              상담 예약
            </h1>
            <p className="page-subtitle">상담을 예약해주세요</p>
          </div>
        </div>
      </div>

      <div className="reservation-content">
        <form onSubmit={handleSubmit} className="reservation-form">
          {/* 상담 유형 선택 */}
          <div className="form-section">
            <h2>상담 유형</h2>
            <div className="form-group">
              <label htmlFor="consultationType">상담 유형 *</label>
              <select
                id="consultationType"
                name="consultationType"
                value={formData.consultationType}
                onChange={handleInputChange}
                className={errors.consultationType ? 'error' : ''}
              >
                {consultationTypes.map(type => (
                  <option key={type.value} value={type.value}>{type.label}</option>
                ))}
              </select>
              {errors.consultationType && <span className="error-message">{errors.consultationType}</span>}
            </div>
          </div>

          {/* 학생 정보 */}
          <div className="form-section">
            <h2>상담 대상 학생 선택</h2>
            <div className="form-group">
              <label htmlFor="selectedStudentId">학생 선택 *</label>
              <select
                id="selectedStudentId"
                name="selectedStudentId"
                value={formData.selectedStudentId}
                onChange={handleInputChange}
                className={errors.selectedStudentId ? 'error' : ''}
              >
                <option value="">학생을 선택해주세요</option>
                {(profile?.role === 'PARENT' ? myStudents : allStudents).map((student) => (
                  <option key={student.id} value={student.id}>
                    {student.studentName} ({student.school || '학교 미등록'} {student.grade}학년)
                  </option>
                ))}
              </select>
              {errors.selectedStudentId && <span className="error-message">{errors.selectedStudentId}</span>}
            </div>
          </div>

          {/* 상담 일정 */}
          <div className="form-section">
            <h2>상담 일정</h2>
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="consultationDate">상담 날짜 *</label>
                <input
                  type="date"
                  id="consultationDate"
                  name="consultationDate"
                  value={formData.consultationDate}
                  onChange={handleInputChange}
                  min={new Date().toISOString().split('T')[0]}
                  className={errors.consultationDate ? 'error' : ''}
                />
                {errors.consultationDate && <span className="error-message">{errors.consultationDate}</span>}
              </div>
              
              <div className="form-group">
                <label htmlFor="consultationTime">상담 시간 *</label>
                <input
                  type="time"
                  id="consultationTime"
                  name="consultationTime"
                  value={formData.consultationTime}
                  onChange={handleInputChange}
                  className={errors.consultationTime ? 'error' : ''}
                />
                {errors.consultationTime && <span className="error-message">{errors.consultationTime}</span>}
              </div>
            </div>
          </div>

          {/* 상담 내용 */}
          <div className="form-section">
            <h2>상담 내용</h2>
            <div className="form-group">
              <label htmlFor="content">상담 내용 *</label>
              <textarea
                id="content"
                name="content"
                value={formData.content}
                onChange={handleInputChange}
                placeholder="상담하고 싶은 내용을 자세히 입력해주세요"
                rows="4"
                className={errors.content ? 'error' : ''}
              />
              {errors.content && <span className="error-message">{errors.content}</span>}
            </div>
            
            <div className="form-group">
              <label htmlFor="memo">추가 메모</label>
              <textarea
                id="memo"
                name="memo"
                value={formData.memo}
                onChange={handleInputChange}
                placeholder="추가로 전달하고 싶은 내용이 있으면 입력해주세요"
                rows="3"
              />
            </div>
          </div>

          {/* 제출 버튼 */}
          <div className="form-actions">
            <button 
              type="submit" 
              className="btn-primary"
              disabled={createConsultation.isLoading}
            >
              {createConsultation.isLoading ? '예약 중...' : '상담 예약하기'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default ConsultationReservation;
