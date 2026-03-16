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
  const [selectedAtt, setSelectedAtt] = useState(null);
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

  // 날짜별 출석 상태 목록 (달력 점 표시용 - 여러 점)
  const getDateStatuses = (dateStr) => {
    const atts = monthlyAttendances.filter(a => a.attendanceDate === dateStr);
    if (atts.length === 0) return [];
    const statuses = new Set();
    atts.forEach(a => {
      if (a.status === 'ABSENT' || a.status === 'EXCUSED') statuses.add('absent');
      else if (a.status === 'LATE') statuses.add('late');
      else if (a.status === 'PRESENT') statuses.add('present');
    });
    // 빨강 → 노랑 → 초록 순서로 표시
    return ['absent', 'late', 'present'].filter(s => statuses.has(s));
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
      const statuses = getDateStatuses(dateStr);
      const dayOfWeek = new Date(year, month, d).getDay();
      days.push(
        <div
          key={d}
          className={`ci-day ${dateStr === selectedDate ? 'ci-selected' : ''} ${dateStr === today ? 'ci-today' : ''} ${dayOfWeek === 0 ? 'ci-sunday' : ''}`}
          onClick={() => setSelectedDate(dateStr)}
        >
          <span>{d}</span>
          {statuses.length > 0 && (
            <div style={{ display: 'flex', gap: 3, justifyContent: 'center' }}>
              {statuses.map(s => <div key={s} className={`ci-dot ci-dot-${s}`}></div>)}
            </div>
          )}
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

          {/* 월별 출석 현황 요약 */}
          <div className="ci-summary" style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
            {(() => {
              const counts = { PRESENT: 0, LATE: 0, ABSENT: 0, EXCUSED: 0, EARLY_LEAVE: 0 };
              monthlyAttendances.forEach(a => { if (counts[a.status] !== undefined) counts[a.status]++; });
              return [
                { label: '출석', count: counts.PRESENT, color: '#03C75A' },
                { label: '지각', count: counts.LATE, color: '#FF9500' },
                { label: '결석', count: counts.ABSENT, color: '#FF3B30' },
                { label: '사유결석', count: counts.EXCUSED, color: '#8E8E93' },
                { label: '조퇴', count: counts.EARLY_LEAVE, color: '#AF52DE' },
              ].map(s => (
                <div key={s.label} style={{ flex: 1, minWidth: 80, textAlign: 'center', padding: '10px 8px', background: '#f9f9f9', borderRadius: 8, borderLeft: `3px solid ${s.color}` }}>
                  <div style={{ fontSize: 20, fontWeight: 700, color: s.color }}>{s.count}</div>
                  <div style={{ fontSize: 12, color: '#666' }}>{s.label}</div>
                </div>
              ));
            })()}
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
                    <div key={att.id} className="ci-card" onClick={() => setSelectedAtt(att)} style={{ cursor: 'pointer' }}>
                      <div className="ci-card-top">
                        <div>
                          <div className="ci-card-name">{att.studentName}</div>
                          <div className="ci-card-course">{att.courseName || '-'}</div>
                        </div>
                        <span className={`ci-badge ${st.cls}`}>{st.text}</span>
                      </div>
                      <div className="ci-card-info">
                        <div><i className="fas fa-sign-in-alt"></i> 등원 <strong>{formatTime(att.checkInTime)}</strong></div>
                        <div><i className="fas fa-sign-out-alt"></i> 하원 <strong>{formatTime(att.checkOutTime)}</strong></div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* 출석 상세 모달 */}
      {selectedAtt && (() => {
        const att = selectedAtt;
        const st = statusMap[att.status] || { text: att.status, cls: '' };
        const hasAdditional = att.vocabularyClass || att.grammarClass || att.phonicsClass || att.speakingClass;
        return (
          <div className="modal-overlay" onClick={() => setSelectedAtt(null)}>
            <div className="modal-content" onClick={e => e.stopPropagation()} style={{ maxWidth: 440, padding: 24 }}>
              <div className="modal-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
                <h2 style={{ margin: 0, fontSize: 18 }}><i className="fas fa-clipboard-list"></i> 수업 상세</h2>
                <button onClick={() => setSelectedAtt(null)} style={{ background: 'none', border: 'none', fontSize: 20, cursor: 'pointer', color: '#999' }}>✕</button>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                <div>
                  <div style={{ fontSize: 18, fontWeight: 700 }}>{att.studentName}</div>
                  <div style={{ fontSize: 13, color: '#888' }}>{att.courseName || '-'}</div>
                </div>
                <span className={`ci-badge ${st.cls}`}>{st.text}</span>
              </div>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
                <tbody>
                  {[
                    ['등원 시간', formatTime(att.checkInTime)],
                    ['하원 예정', formatTime(att.expectedLeaveTime)],
                    ['실제 하원', formatTime(att.checkOutTime)],
                    ['수업 시간', att.startTime && att.endTime ? `${att.startTime} ~ ${att.endTime}` : '-'],
                    ['D/C', att.dcCheck || '-'],
                    ['W/R', att.wrCheck || '-'],
                    ['추가수업', hasAdditional ? [att.vocabularyClass && 'Vocabulary', att.grammarClass && 'Grammar', att.phonicsClass && 'Phonics', att.speakingClass && 'Speaking'].filter(Boolean).join(', ') : '-'],
                    ['추가수업 시간', formatTime(att.additionalClassTime)],
                    ['리딩 메모', att.readingNote || '-'],
                    ['비고', att.reason || '-'],
                  ].map(([label, value]) => (
                    <tr key={label} style={{ borderBottom: '1px solid #f0f0f0' }}>
                      <td style={{ padding: '10px 8px', color: '#666', width: 110 }}>{label}</td>
                      <td style={{ padding: '10px 8px', fontWeight: 500 }}>{value}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <button onClick={() => setSelectedAtt(null)} style={{ width: '100%', marginTop: 20, padding: '12px 0', background: '#03C75A', color: '#fff', border: 'none', borderRadius: 8, fontSize: 15, fontWeight: 600, cursor: 'pointer' }}>닫기</button>
            </div>
          </div>
        );
      })()}
    </div>
  );
}

export default ClassInfo;
