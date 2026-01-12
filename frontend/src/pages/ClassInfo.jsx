import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { attendanceAPI, authAPI, scheduleAPI, reservationAPI } from '../services/api';
import LoadingSpinner from '../components/LoadingSpinner';
import '../styles/ClassInfo.css';

function ClassInfo() {
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const navigate = useNavigate();

  // 사용자 프로필 조회
  const { data: profile, error: profileError } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
    retry: false,
    onError: (error) => {
      if (error.response?.status === 401) {
        localStorage.clear();
        navigate('/login');
      }
    }
  });

  const isParent = profile?.role === 'PARENT';
  const isTeacher = profile?.role === 'TEACHER';
  const isAdmin = profile?.role === 'ADMIN';

  // 선택된 날짜의 수업 정보 조회 (역할별)
  const { data: classData = [], isLoading, error: classDataError } = useQuery({
    queryKey: ['classInfo', selectedDate, profile?.role],
    queryFn: async () => {
      if (!profile) return [];
      
      if (profile.role === 'PARENT') {
        const today = new Date().toISOString().split('T')[0];
        const isToday = selectedDate === today;
        const isFuture = selectedDate > today;
        
        if (isFuture) {
          // 미래 날짜: 예약 정보만 표시
          const reservationsResponse = await reservationAPI.getByDate(selectedDate);
          const reservations = reservationsResponse.data || [];
          
          return reservations.map(reservation => ({
            id: `reservation-${reservation.id}`,
            type: 'reservation',
            student: reservation.student || { 
              name: reservation.studentName, 
              studentName: reservation.studentName 
            },
            schedule: {
              id: reservation.scheduleId,
              startTime: reservation.startTime,
              endTime: reservation.endTime,
              course: {
                name: reservation.courseName,
                courseName: reservation.courseName
              }
            },
            status: reservation.status,
            teacherName: '미배정',
            checkInTime: null,
            checkOutTime: null,
            memo: reservation.memo,
            reservationStatus: reservation.status
          }));
        } else {
          // 과거/현재 날짜: 출석 정보만 표시
          const attendancesResponse = await attendanceAPI.getMyChildSchedules(selectedDate);
          return attendancesResponse.data || [];
        }
      } else if (profile.role === 'TEACHER') {
        const response = await scheduleAPI.getMySchedules(selectedDate);
        return response.data;
      } else if (profile.role === 'ADMIN') {
        const response = await scheduleAPI.getAllSchedules(selectedDate);
        return response.data;
      }
      return [];
    },
    enabled: !!profile,
    retry: false
  });

  // 월별 스케줄 조회 (캘린더용)
  const { data: monthlySchedules = [] } = useQuery({
    queryKey: ['monthlySchedules', currentMonth.getFullYear(), currentMonth.getMonth(), profile?.role],
    queryFn: async () => {
      if (!profile) return [];
      
      const year = currentMonth.getFullYear();
      const month = currentMonth.getMonth() + 1;
      
      if (profile.role === 'PARENT') {
        const response = await attendanceAPI.getMyChildMonthlySchedules(year, month);
        return response.data;
      } else if (profile.role === 'TEACHER') {
        const response = await scheduleAPI.getMyMonthlySchedules(year, month);
        return response.data;
      } else if (profile.role === 'ADMIN') {
        const response = await scheduleAPI.getAllMonthlySchedules(year, month);
        return response.data;
      }
      return [];
    },
    enabled: !!profile,
    retry: false
  });

  // 캘린더 렌더링
  const renderCalendar = () => {
    const year = currentMonth.getFullYear();
    const month = currentMonth.getMonth();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const firstDay = new Date(year, month, 1).getDay();
    
    const days = [];
    const weekDays = ['일', '월', '화', '수', '목', '금', '토'];
    
    // 요일 헤더
    weekDays.forEach(day => {
      days.push(
        <div key={`header-${day}`} className="calendar-header-day">
          {day}
        </div>
      );
    });
    
    // 빈 칸 (이전 달)
    for (let i = 0; i < firstDay; i++) {
      const prevMonth = month === 0 ? 11 : month - 1;
      const prevYear = month === 0 ? year - 1 : year;
      const prevDaysInMonth = new Date(prevYear, prevMonth + 1, 0).getDate();
      const prevDay = prevDaysInMonth - firstDay + i + 1;
      const prevDate = new Date(prevYear, prevMonth, prevDay);
      // 시간대 문제 해결을 위해 로컬 날짜 문자열 생성
      const dateStr = `${prevYear}-${String(prevMonth + 1).padStart(2, '0')}-${String(prevDay).padStart(2, '0')}`;
      
      const hasSchedule = monthlySchedules.some(schedule => 
        schedule.scheduleDate === dateStr || schedule.date === dateStr
      );
      
      days.push(
        <div
          key={`prev-${i}`}
          className={`calendar-day other-month ${dateStr === selectedDate ? 'selected' : ''} ${hasSchedule ? 'has-schedule' : ''}`}
          onClick={() => setSelectedDate(dateStr)}
        >
          <span className="day-number">{prevDay}</span>
          {hasSchedule && <div className="schedule-indicator"></div>}
        </div>
      );
    }
    
    // 현재 달의 날짜들
    for (let day = 1; day <= daysInMonth; day++) {
      // 시간대 문제 해결을 위해 로컬 날짜 문자열 생성
      const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      
      const hasSchedule = monthlySchedules.some(schedule => 
        schedule.scheduleDate === dateStr || schedule.date === dateStr
      );
      
      days.push(
        <div
          key={day}
          className={`calendar-day ${dateStr === selectedDate ? 'selected' : ''} ${hasSchedule ? 'has-schedule' : ''}`}
          onClick={() => setSelectedDate(dateStr)}
        >
          <span className="day-number">{day}</span>
          {hasSchedule && <div className="schedule-indicator"></div>}
        </div>
      );
    }
    
    // 다음 달 날짜들 (6주 완성을 위해)
    const totalCells = Math.ceil((firstDay + daysInMonth) / 7) * 7;
    const remainingCells = totalCells - (firstDay + daysInMonth);
    
    for (let day = 1; day <= remainingCells; day++) {
      const nextMonth = month === 11 ? 0 : month + 1;
      const nextYear = month === 11 ? year + 1 : year;
      // 시간대 문제 해결을 위해 로컬 날짜 문자열 생성
      const dateStr = `${nextYear}-${String(nextMonth + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      
      const hasSchedule = monthlySchedules.some(schedule => 
        schedule.scheduleDate === dateStr || schedule.date === dateStr
      );
      
      days.push(
        <div
          key={`next-${day}`}
          className={`calendar-day other-month ${dateStr === selectedDate ? 'selected' : ''} ${hasSchedule ? 'has-schedule' : ''}`}
          onClick={() => setSelectedDate(dateStr)}
        >
          <span className="day-number">{day}</span>
          {hasSchedule && <div className="schedule-indicator"></div>}
        </div>
      );
    }

    return days;
  };

  // 시간 포맷팅
  const formatTime = (timeString) => {
    if (!timeString) return '-';
    return timeString.substring(0, 5); // HH:MM 형식
  };

  // 출석 상태별 배지
  const getStatusBadge = (status) => {
    const statusMap = {
      PRESENT: { text: '출석', color: '#03C75A' },
      LATE: { text: '지각', color: '#FFA500' },
      ABSENT: { text: '결석', color: '#FF0000' },
      EXCUSED: { text: '사유결석', color: '#0066FF' },
      EARLY_LEAVE: { text: '조퇴', color: '#9C27B0' },
      PENDING: { text: '예약대기', color: '#6c757d' },
      CONFIRMED: { text: '예약확정', color: '#28a745' },
      CANCELLED: { text: '취소됨', color: '#dc3545' },
    };
    const { text, color } = statusMap[status] || { text: status, color: '#999' };
    return <span className="status-badge" style={{ backgroundColor: color }}>{text}</span>;
  };

  // 페이지 제목 (역할별)
  const getPageTitle = () => {
    if (isParent) return '자녀 수업 정보';
    if (isTeacher) return '수업 스케줄';
    return '수업 스케줄';
  };

  const getPageSubtitle = () => {
    if (isParent) return '자녀의 수업 출석 현황을 확인합니다';
    if (isTeacher) return '담당 클래스 스케줄과 수업 현황을 관리합니다';
    return '전체 클래스 스케줄과 수업 현황을 관리합니다';
  };

  // 데이터 렌더링 (역할별)
  const renderClassData = () => {
    if (classData.length === 0) {
      return (
        <div className="empty-state">
          <i className="fas fa-calendar-times"></i>
          <h3>해당 날짜에 수업이 없습니다</h3>
          <p>다른 날짜를 선택해보세요.</p>
        </div>
      );
    }

    if (isParent) {
      return (
        <div className="attendance-list">
          {classData.map((item) => (
            <div key={item.id} className="attendance-card">
              <div className="card-header">
                <div className="student-course-info">
                  <h3>{item.student?.name || item.student?.studentName || '학생'}</h3>
                  <span className="course-name">{item.schedule?.course?.name || item.schedule?.course?.courseName || '수업'}</span>
                </div>
                {item.status && getStatusBadge(item.status)}
              </div>
              <div className="card-body">
                <div className="info-grid">
                  <div className="info-item">
                    <span className="label"><i className="fas fa-clock"></i>수업 시간</span>
                    <span className="value">{formatTime(item.schedule?.startTime)} - {formatTime(item.schedule?.endTime)}</span>
                  </div>
                  <div className="info-item">
                    <span className="label"><i className="fas fa-user-tie"></i>담당 강사</span>
                    <span className="value">{item.teacherName || '미배정'}</span>
                  </div>
                  <div className="info-item">
                    <span className="label"><i className="fas fa-sign-in-alt"></i>출석 시간</span>
                    <span className="value">{item.checkInTime ? formatTime(item.checkInTime) : '미출석'}</span>
                  </div>
                  <div className="info-item">
                    <span className="label"><i className="fas fa-sign-out-alt"></i>퇴실 시간</span>
                    <span className="value">{item.checkOutTime ? formatTime(item.checkOutTime) : '미퇴실'}</span>
                  </div>
                </div>
                {item.memo && item.memo.trim() && (
                  <div className="notes-section">
                    <span className="notes-label">
                      <i className="fas fa-sticky-note"></i>
                      메모
                    </span>
                    <p className="notes-content">{item.memo}</p>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      );
    } else {
      return (
        <div className="schedule-list">
          {classData.map((schedule) => (
            <div key={schedule.id} className="schedule-card">
              <div className="card-header">
                <div className="course-info">
                  <h3>{schedule.course?.name || schedule.courseName || '수업'}</h3>
                  <span className="time-info">{formatTime(schedule.startTime)} - {formatTime(schedule.endTime)}</span>
                </div>
                <span className="capacity-badge">{schedule.currentStudents || 0}/{schedule.maxCapacity || schedule.maxStudents || 0}</span>
              </div>
              <div className="card-body">
                {isTeacher && (
                  <div className="teacher-info">
                    <span className="label"><i className="fas fa-user-tie"></i>담당 강사</span>
                    <span className="value">{schedule.teacherName || '미배정'}</span>
                  </div>
                )}
                {schedule.currentStudents > 0 && (
                  <div className="students-list">
                    <span className="label"><i className="fas fa-users"></i>수강생</span>
                    <div className="students-info">
                      <span className="student-count">{schedule.currentStudents}명 수강 중</span>
                    </div>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      );
    }
  };

  if (profileError?.response?.status === 401) {
    return null; // 이미 리다이렉트 처리됨
  }

  if (isLoading) {
    return (
      <div className="page-wrapper">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="page-wrapper">
      {/* 페이지 헤더 */}
      <div className="page-header">
        <div className="page-header-content">
          <div className="page-title-section">
            <h1 className="page-title">
              <i className="fas fa-calendar-check"></i>
              {getPageTitle()}
            </h1>
            <p className="page-subtitle">{getPageSubtitle()}</p>
          </div>
        </div>
      </div>

      {/* 메인 컨텐츠 */}
      <div className="page-content">
        <div className="class-info-layout">
          {/* 캘린더 섹션 */}
          <div className="calendar-section">
            <div className="calendar-header">
              <button 
                className="nav-button"
                onClick={() => setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1))}
              >
                <i className="fas fa-chevron-left"></i>
              </button>
              <h3>{currentMonth.getFullYear()}년 {currentMonth.getMonth() + 1}월</h3>
              <button 
                className="nav-button"
                onClick={() => setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1))}
              >
                <i className="fas fa-chevron-right"></i>
              </button>
            </div>
            
            <div className="calendar-grid">
              {renderCalendar()}
            </div>
          </div>

          {/* 선택된 날짜 정보 섹션 */}
          <div className="selected-info-section">
            <div className="selected-date-header">
              <h2>{new Date(selectedDate).toLocaleDateString('ko-KR', { 
                year: 'numeric', 
                month: 'long', 
                day: 'numeric',
                weekday: 'long'
              })} 수업 현황</h2>
              <span className="count-badge">{classData.length}건</span>
            </div>

            {renderClassData()}
          </div>
        </div>
      </div>
    </div>
  );
}

export default ClassInfo;
