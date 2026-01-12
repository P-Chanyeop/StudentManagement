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
  
  // 날짜 선택을 위한 분리된 상태
  const [dateComponents, setDateComponents] = useState({
    year: '',
    month: '',
    day: ''
  });
  
  // 학생 선택 드롭다운 상태
  const [showStudentDropdown, setShowStudentDropdown] = useState(false);
  const [studentSearchQuery, setStudentSearchQuery] = useState('');

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

  // 날짜 컴포넌트 변경 핸들러
  const handleDateComponentChange = (component, value) => {
    const newDateComponents = {
      ...dateComponents,
      [component]: value
    };
    setDateComponents(newDateComponents);
    
    // 모든 컴포넌트가 선택되면 consultationDate 업데이트
    if (newDateComponents.year && newDateComponents.month && newDateComponents.day) {
      const formattedDate = `${newDateComponents.year}-${newDateComponents.month.padStart(2, '0')}-${newDateComponents.day.padStart(2, '0')}`;
      setFormData(prev => ({
        ...prev,
        consultationDate: formattedDate
      }));
      
      // 에러 제거
      if (errors.consultationDate) {
        setErrors(prev => ({
          ...prev,
          consultationDate: ''
        }));
      }
    }
  };

  // 드롭다운 외부 클릭 시 닫기
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (showStudentDropdown && !event.target.closest('.student-select-wrapper')) {
        setShowStudentDropdown(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [showStudentDropdown]);

  // 학생 선택 핸들러
  const handleStudentSelect = (student) => {
    setFormData(prev => ({
      ...prev,
      selectedStudentId: student.id.toString()
    }));
    setShowStudentDropdown(false);
    setStudentSearchQuery('');
    
    // 에러 메시지 제거
    if (errors.selectedStudentId) {
      setErrors(prev => ({
        ...prev,
        selectedStudentId: ''
      }));
    }
  };

  // 선택된 학생 정보 가져오기
  const getSelectedStudent = () => {
    const students = profile?.role === 'PARENT' ? myStudents : allStudents;
    return students.find(student => student.id.toString() === formData.selectedStudentId);
  };

  // 필터링된 학생 목록
  const getFilteredStudents = () => {
    const students = profile?.role === 'PARENT' ? myStudents : allStudents;
    if (!studentSearchQuery) return students;
    
    return students.filter(student => 
      student.studentName.toLowerCase().includes(studentSearchQuery.toLowerCase()) ||
      student.parentName.toLowerCase().includes(studentSearchQuery.toLowerCase())
    );
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
    <div className="page-wrapper">
      <div className="page-header">
        <div className="page-header-content">
          <div className="page-title-section">
            <h1 className="page-title">
              <i className="fas fa-comments"></i>
              상담 예약
            </h1>
            <p className="page-subtitle">상담을 예약해주세요</p>
          </div>
        </div>
      </div>

      <div className="page-content">
        <div className="reservation-content">
          <form onSubmit={handleSubmit} className="reservation-form">
          {/* 상담 유형 선택 - 학부모가 아닐 때만 표시 */}
          {profile?.role !== 'PARENT' && (
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
          )}

          {/* 학생 정보 */}
          <div className="form-section">
            <h2>상담 대상 학생 선택</h2>
            <div className="form-group">
              <label htmlFor="selectedStudentId">학생을 선택해 주세요 *</label>
              <div className="student-select-wrapper">
                <div 
                  className={`student-select-input ${errors.selectedStudentId ? 'error' : ''}`}
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
                      {getFilteredStudents().map(student => (
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
                      {getFilteredStudents().length === 0 && (
                        <div className="no-students">검색 결과가 없습니다.</div>
                      )}
                    </div>
                  </div>
                )}
              </div>
              {errors.selectedStudentId && <span className="error-message">{errors.selectedStudentId}</span>}
            </div>
          </div>

          {/* 상담 일정 */}
          <div className="form-section">
            <h2>상담 일정</h2>
            <div className="form-row">
              <div className="form-group">
                <label>상담 날짜 *</label>
                <div className="date-inputs">
                  <select
                    value={dateComponents.year}
                    onChange={(e) => handleDateComponentChange('year', e.target.value)}
                    className={errors.consultationDate ? 'error' : ''}
                  >
                    <option value="">년도</option>
                    {Array.from({length: 3}, (_, i) => {
                      const year = new Date().getFullYear() + i;
                      return <option key={year} value={year}>{year}년</option>;
                    })}
                  </select>
                  
                  <select
                    value={dateComponents.month}
                    onChange={(e) => handleDateComponentChange('month', e.target.value)}
                    className={errors.consultationDate ? 'error' : ''}
                  >
                    <option value="">월</option>
                    {Array.from({length: 12}, (_, i) => {
                      const month = i + 1;
                      return <option key={month} value={month}>{month}월</option>;
                    })}
                  </select>
                  
                  <select
                    value={dateComponents.day}
                    onChange={(e) => handleDateComponentChange('day', e.target.value)}
                    className={errors.consultationDate ? 'error' : ''}
                  >
                    <option value="">일</option>
                    {Array.from({length: 31}, (_, i) => {
                      const day = i + 1;
                      return <option key={day} value={day}>{day}일</option>;
                    })}
                  </select>
                </div>
                {errors.consultationDate && <span className="error-message">{errors.consultationDate}</span>}
              </div>
            </div>
            
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="consultationTime">상담 시간 *</label>
                <div className="time-input-wrapper">
                  <select
                    id="consultationTime"
                    name="consultationTime"
                    value={formData.consultationTime}
                    onChange={handleInputChange}
                    className={errors.consultationTime ? 'error' : ''}
                  >
                    <option value="">시간을 선택하세요</option>
                    {Array.from({length: 12}, (_, i) => {
                      const times = [];
                      const hour = (i + 9).toString().padStart(2, '0');
                      times.push(`${hour}:00`);
                      times.push(`${hour}:30`);
                      return times;
                    }).flat().map(time => (
                      <option key={time} value={time}>{time}</option>
                    ))}
                  </select>
                </div>
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
            
            {/* 추가 메모 - 학부모가 아닐 때만 표시 */}
            {profile?.role !== 'PARENT' && (
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
            )}
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
    </div>
  );
}

export default ConsultationReservation;
