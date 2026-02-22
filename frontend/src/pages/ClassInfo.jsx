import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { attendanceAPI, authAPI, studentAPI } from '../services/api';
import { getTodayString } from '../utils/dateUtils';
import LoadingSpinner from '../components/LoadingSpinner';
import '../styles/ClassInfo.css';

function ClassInfo() {
  const [selectedDate, setSelectedDate] = useState(getTodayString());
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const navigate = useNavigate();

  const { data: profile } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
    retry: false,
  });

  // 학부모의 자녀 목록
  const { data: myStudents = [] } = useQuery({
    queryKey: ['myStudents'],
    queryFn: async () => {
      const response = await studentAPI.getMyStudents();
      return response.data;
    },
    enabled: profile?.role === 'PARENT',
  });

  // 월별 출석 데이터 (달력 점 표시용)
  const { data: monthlyAttendances = [] } = useQuery({
    queryKey: ['monthlyAttendances', currentMonth.getFullYear(), currentMonth.getMonth(), myStudents],
    queryFn: async () => {
      if (myStudents.length === 0) return [];
      const year = currentMonth.getFullYear();
      const month = currentMonth.getMonth() + 1;
      const startDate = `${year}-${String(month).padStart(2, '0')}-01`;
      const lastDay = new Date(year, month, 0).getDate();
      const endDate = `${year}-${String(month).padStart(2, '0')}-${lastDay}`;
      const all = [];
      for (const s of myStudents) {
        const res = await attendanceAPI.getByStudentAndRange(s.id, startDate, endDate);
        all.push(...(res.data || []));
      }
      return all;
    },
    enabled: myStudents.length > 0,
  });

  // 선택 날짜의 출석 데이터
  const { data: dayAttendances = [], isLoading } = useQuery({
    queryKey: ['dayAttendances', selectedDate, myStudents],
    queryFn: async () => {
      if (myStudents.length === 0) return [];
      const all = [];
      for (const s of myStudents) {
        const res = await attendanceAPI.getByStudentAndRange(s.id, selectedDate, selectedDate);
        all.push(...(res.data || []));
      }
      return all;
    },
    enabled: myStudents.length > 0,
  });

  // 날짜별 출석 상태 맵 (달력용)
  const getDateStatus = (dateStr) => {
    const atts = monthlyAttendances.filter(a => {
      if (a.checkInTime) return a.checkInTime.split('T')[0] === dateStr;
      return false;
    });
    if (atts.length === 0) return null;
    if (atts.some(a => a.status === 'ABSENT')) return 'absent';
    if (atts.some(a => a.status === 'LATE')) return 'late';
    return 'present';
  };

  const formatTime = (t) => {
    if (!t) return '-';
    if (t.includes('T')) return t.split('T')[1].substring(0, 5);
    return t.substring(0, 5);
  };

  const statusMap = {
    PRESENT: { text: '출석', cls: 'ci-badge-present' },
    LATE: { text: '지각', cls: 'ci-badge-late' },
    ABSENT: { text: '결석', cls: 'ci-badge-absent' },
    EXCUSED: { text: '사유결석', cls: 'ci-badge-excused' },
    EARLY_LEAVE: { text: '조퇴', cls: 'ci-badge-early' },
  };

  // 캘린더 렌더링
  const renderCalendar = () => {
    const year = currentMonth.getFullYear();
    const month = currentMonth.getMonth();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const firstDay = new Date(year, month, 1).getDay();
    const today = getTodayString();
    const days = [];

    for (let i = 0; i < firstDay; i++) {
      days.push(<div key={`p-${i}`} className="ci-day ci-other"></div>);
    }

    for (let d = 1; d <= daysInMonth; d++) {
      const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
      const status = getDateStatus(dateStr);
      days.push(
        <div
          key={d}
          className={`ci-day ${dateStr === selectedDate ? 'ci-selected' : ''} ${dateStr === today ? 'ci-today' : ''}`}
          onClick={() => setSelectedDate(dateStr)}
        >
          <span>{d}</span>
          {status && <div className={`ci-dot ci-dot-${status}`}></div>}
        </div>
      );
    }
    return days;
  };

  if (!profile) return <div className="page-wrapper"><LoadingSpinner /></div>;

  return (
    <div className="page-wrapper">
      <div className="page-header">
        <div className="page-header-content">
          <div className="page-title-section">
            <h1 className="page-title"><i className="fas fa-calendar-check"></i> 수업 정보</h1>
            <p className="page-subtitle">자녀의 수업 출석 현황을 확인합니다</p>
          </div>
        </div>
      </div>

      <div className="page-content">
        <div className="ci-layout">
          {/* 캘린더 */}
          <div className="ci-calendar">
            <div className="ci-cal-header">
              <button className="ci-nav" onClick={() => setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1))}>◀</button>
              <h3>{currentMonth.getFullYear()}년 {currentMonth.getMonth() + 1}월</h3>
              <button className="ci-nav" onClick={() => setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1))}>▶</button>
            </div>
            <div className="ci-weekdays">
              {['일','월','화','수','목','금','토'].map(d => <div key={d} className="ci-wk">{d}</div>)}
            </div>
            <div className="ci-grid">{renderCalendar()}</div>
            <div className="ci-legend">
              <span><span className="ci-dot ci-dot-present"></span> 출석</span>
              <span><span className="ci-dot ci-dot-late"></span> 지각</span>
              <span><span className="ci-dot ci-dot-absent"></span> 결석</span>
            </div>
          </div>

          {/* 선택 날짜 정보 */}
          <div className="ci-detail">
            <div className="ci-detail-header">
              <h2>{new Date(selectedDate + 'T00:00:00').toLocaleDateString('ko-KR', { month: 'long', day: 'numeric', weekday: 'short' })}</h2>
              <span className="ci-count">{dayAttendances.length}건</span>
            </div>

            {isLoading ? <LoadingSpinner /> : dayAttendances.length === 0 ? (
              <div className="ci-empty">
                <i className="fas fa-calendar-times"></i>
                <p>해당 날짜에 수업 기록이 없습니다</p>
              </div>
            ) : (
              <div className="ci-cards">
                {dayAttendances.map((att) => {
                  const st = statusMap[att.status] || { text: att.status, cls: '' };
                  return (
                    <div key={att.id} className="ci-card">
                      <div className="ci-card-top">
                        <div>
                          <div className="ci-card-name">{att.studentName}</div>
                          <div className="ci-card-course">{att.courseName || '-'}</div>
                        </div>
                        <span className={`ci-badge ${st.cls}`}>{st.text}</span>
                      </div>
                      <div className="ci-card-info">
                        <div><i className="fas fa-sign-in-alt"></i> 등원 <strong>{formatTime(att.checkInTime)}</strong></div>
                        {att.expectedLeaveTime && <div><i className="fas fa-clock"></i> 하원 예정 <strong>{formatTime(att.expectedLeaveTime)}</strong></div>}
                        <div><i className="fas fa-sign-out-alt"></i> 실제 하원 <strong>{formatTime(att.checkOutTime)}</strong></div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default ClassInfo;
