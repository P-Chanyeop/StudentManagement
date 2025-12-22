import { useState, useEffect } from 'react';
import { useMutation } from '@tanstack/react-query';
import { reservationAPI } from '../services/api';
import '../styles/ParentReservation.css';

function ParentReservation() {
  const [formData, setFormData] = useState({
    // 학부모 정보
    parentName: '',
    parentPhone: '',
    
    // 학생 정보
    studentName: '',
    studentPhone: '',
    
    // 예약 정보
    preferredDate: '',
    preferredTime: '',
    courseType: '',
    
    // 요청사항
    requirements: ''
  });

  const [errors, setErrors] = useState({});
  const [currentMonth, setCurrentMonth] = useState(new Date());

  // 캘린더 관련 함수들
  const getDaysInMonth = (date) => {
    return new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
  };

  const getFirstDayOfMonth = (date) => {
    return new Date(date.getFullYear(), date.getMonth(), 1).getDay();
  };

  const formatDate = (year, month, day) => {
    return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
  };

  const isDateDisabled = (year, month, day) => {
    const date = new Date(year, month, day);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return date < today;
  };

  const handleDateSelect = (year, month, day) => {
    const selectedDate = formatDate(year, month, day);
    setFormData(prev => ({
      ...prev,
      preferredDate: selectedDate
    }));
    
    if (errors.preferredDate) {
      setErrors(prev => ({
        ...prev,
        preferredDate: ''
      }));
    }
  };

  const renderCalendar = () => {
    const year = currentMonth.getFullYear();
    const month = currentMonth.getMonth();
    const daysInMonth = getDaysInMonth(currentMonth);
    const firstDay = getFirstDayOfMonth(currentMonth);
    
    const days = [];
    const weekDays = ['일', '월', '화', '수', '목', '금', '토'];
    
    // 요일 헤더
    weekDays.forEach(day => {
      days.push(
        <div key={`header-${day}`} className="calendar-header">
          {day}
        </div>
      );
    });
    
    // 빈 칸 (이전 달)
    for (let i = 0; i < firstDay; i++) {
      days.push(<div key={`empty-${i}`} className="calendar-day empty"></div>);
    }
    
    // 현재 달의 날짜들
    for (let day = 1; day <= daysInMonth; day++) {
      const isDisabled = isDateDisabled(year, month, day);
      const isSelected = formData.preferredDate === formatDate(year, month, day);
      
      days.push(
        <div
          key={day}
          className={`calendar-day ${isDisabled ? 'disabled' : ''} ${isSelected ? 'selected' : ''}`}
          onClick={() => !isDisabled && handleDateSelect(year, month, day)}
        >
          {day}
        </div>
      );
    }
    
    return days;
  };

  const navigateMonth = (direction) => {
    setCurrentMonth(prev => {
      const newDate = new Date(prev);
      newDate.setMonth(prev.getMonth() + direction);
      return newDate;
    });
  };

  // 예약 생성 mutation
  const createReservation = useMutation({
    mutationFn: (data) => reservationAPI.create(data),
    onSuccess: () => {
      alert('예약 요청이 완료되었습니다! 확인 후 연락드리겠습니다.');
      setFormData({
        parentName: '',
        parentPhone: '',
        studentName: '',
        studentPhone: '',
        preferredDate: '',
        preferredTime: '',
        courseType: '',
        requirements: ''
      });
      setErrors({});
    },
    onError: (error) => {
      alert('예약 요청 중 오류가 발생했습니다. 다시 시도해주세요.');
      console.error('예약 오류:', error);
    },
  });

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    
    // 에러 메시지 제거
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
  };

  const validateForm = () => {
    const newErrors = {};
    
    if (!formData.parentName.trim()) {
      newErrors.parentName = '학부모 이름을 입력해주세요.';
    }
    
    if (!formData.parentPhone.trim()) {
      newErrors.parentPhone = '학부모 전화번호를 입력해주세요.';
    } else if (!/^010-\d{4}-\d{4}$/.test(formData.parentPhone)) {
      newErrors.parentPhone = '전화번호 형식이 올바르지 않습니다. (010-0000-0000)';
    }
    
    if (!formData.studentName.trim()) {
      newErrors.studentName = '학생 이름을 입력해주세요.';
    }
    
    if (!formData.studentPhone.trim()) {
      newErrors.studentPhone = '학생 전화번호를 입력해주세요.';
    } else if (!/^010-\d{4}-\d{4}$/.test(formData.studentPhone)) {
      newErrors.studentPhone = '전화번호 형식이 올바르지 않습니다. (010-0000-0000)';
    }
    
    if (!formData.preferredDate) {
      newErrors.preferredDate = '희망 날짜를 선택해주세요.';
    }
    
    if (!formData.preferredTime) {
      newErrors.preferredTime = '희망 시간을 선택해주세요.';
    }
    
    if (!formData.courseType) {
      newErrors.courseType = '수업 유형을 선택해주세요.';
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }
    
    createReservation.mutate(formData);
  };

  const courseTypes = [
    { value: 'beginner', label: '초급 영어' },
    { value: 'intermediate', label: '중급 영어' },
    { value: 'advanced', label: '고급 영어' },
    { value: 'conversation', label: '영어 회화' },
    { value: 'grammar', label: '영어 문법' },
    { value: 'toeic', label: 'TOEIC' },
    { value: 'toefl', label: 'TOEFL' },
    { value: 'ielts', label: 'IELTS' }
  ];

  const timeSlots = [
    '09:00', '10:00', '11:00', '12:00', '13:00', '14:00', 
    '15:00', '16:00', '17:00', '18:00', '19:00', '20:00'
  ];

  return (
    <div className="page-wrapper">
      <div className="page-header">
        <div className="page-header-content">
          <div className="page-title-section">
            <h1 className="page-title">
              <i className="fas fa-calendar-plus"></i>
              수업 예약
            </h1>
            <p className="page-subtitle">원하시는 수업을 예약해주세요</p>
          </div>
        </div>
      </div>

      <div className="page-content">
        <form onSubmit={handleSubmit} className="reservation-form">
          {/* 희망 날짜 선택 */}
          <div className="form-section">
            <h2>희망 날짜 선택</h2>
            <div className="calendar-container">
              <div className="calendar-header-nav">
                <button type="button" onClick={() => navigateMonth(-1)}>
                  <i className="fas fa-chevron-left"></i>
                </button>
                <span className="calendar-month">
                  {currentMonth.getFullYear()}년 {currentMonth.getMonth() + 1}월
                </span>
                <button type="button" onClick={() => navigateMonth(1)}>
                  <i className="fas fa-chevron-right"></i>
                </button>
              </div>
              <div className="calendar-grid">
                {renderCalendar()}
              </div>
              {formData.preferredDate && (
                <div className="selected-date">
                  선택된 날짜: {new Date(formData.preferredDate).toLocaleDateString('ko-KR')}
                </div>
              )}
              {errors.preferredDate && <span className="error-message">{errors.preferredDate}</span>}
            </div>
          </div>

          {/* 학부모 정보 */}
          <div className="form-section">
            <h2>학부모 정보</h2>
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="parentName">학부모 이름 *</label>
                <input
                  type="text"
                  id="parentName"
                  name="parentName"
                  value={formData.parentName}
                  onChange={handleInputChange}
                  placeholder="이름을 입력해주세요"
                  className={errors.parentName ? 'error' : ''}
                />
                {errors.parentName && <span className="error-message">{errors.parentName}</span>}
              </div>
              
              <div className="form-group">
                <label htmlFor="parentPhone">학부모 전화번호 *</label>
                <input
                  type="tel"
                  id="parentPhone"
                  name="parentPhone"
                  value={formData.parentPhone}
                  onChange={handleInputChange}
                  placeholder="010-0000-0000"
                  className={errors.parentPhone ? 'error' : ''}
                />
                {errors.parentPhone && <span className="error-message">{errors.parentPhone}</span>}
              </div>
            </div>
          </div>

          {/* 학생 정보 */}
          <div className="form-section">
            <h2>학생 정보</h2>
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="studentName">학생 이름 *</label>
                <input
                  type="text"
                  id="studentName"
                  name="studentName"
                  value={formData.studentName}
                  onChange={handleInputChange}
                  placeholder="학생 이름을 입력해주세요"
                  className={errors.studentName ? 'error' : ''}
                />
                {errors.studentName && <span className="error-message">{errors.studentName}</span>}
              </div>
              
              <div className="form-group">
                <label htmlFor="studentPhone">학생 전화번호 *</label>
                <input
                  type="tel"
                  id="studentPhone"
                  name="studentPhone"
                  value={formData.studentPhone}
                  onChange={handleInputChange}
                  placeholder="010-0000-0000"
                  className={errors.studentPhone ? 'error' : ''}
                />
                {errors.studentPhone && <span className="error-message">{errors.studentPhone}</span>}
              </div>
            </div>
          </div>

          {/* 예약 정보 */}
          <div className="form-section">
            <h2>예약 정보</h2>
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="preferredTime">희망 시간 *</label>
                <select
                  id="preferredTime"
                  name="preferredTime"
                  value={formData.preferredTime}
                  onChange={handleInputChange}
                  className={errors.preferredTime ? 'error' : ''}
                >
                  <option value="">시간을 선택해주세요</option>
                  {timeSlots.map(time => (
                    <option key={time} value={time}>{time}</option>
                  ))}
                </select>
                {errors.preferredTime && <span className="error-message">{errors.preferredTime}</span>}
              </div>
              
              <div className="form-group">
                <label htmlFor="courseType">수업 유형 *</label>
                <select
                  id="courseType"
                  name="courseType"
                  value={formData.courseType}
                  onChange={handleInputChange}
                  className={errors.courseType ? 'error' : ''}
                >
                  <option value="">수업 유형을 선택해주세요</option>
                  {courseTypes.map(course => (
                    <option key={course.value} value={course.value}>{course.label}</option>
                  ))}
                </select>
                {errors.courseType && <span className="error-message">{errors.courseType}</span>}
              </div>
            </div>
          </div>

          {/* 요청사항 */}
          <div className="form-section">
            <h2>요청사항</h2>
            <div className="form-group">
              <label htmlFor="requirements">추가 요청사항</label>
              <textarea
                id="requirements"
                name="requirements"
                value={formData.requirements}
                onChange={handleInputChange}
                placeholder="수업에 대한 특별한 요청사항이나 학생의 특이사항을 입력해주세요"
                rows="4"
              />
            </div>
          </div>

          {/* 제출 버튼 */}
          <div className="form-actions">
            <button 
              type="submit" 
              className="submit-btn"
              disabled={createReservation.isPending}
            >
              {createReservation.isPending ? '예약 요청 중...' : '예약 요청하기'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default ParentReservation;
