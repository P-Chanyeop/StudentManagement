import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { attendanceAPI, authAPI } from '../services/api';
import LoadingSpinner from '../components/LoadingSpinner';
import '../styles/ClassInfo.css';

function ClassInfo() {
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);

  // 사용자 프로필 조회
  const { data: profile } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
  });

  // 선택된 날짜의 자녀 출석 정보 조회
  const { data: attendances = [], isLoading } = useQuery({
    queryKey: ['myChildAttendances', selectedDate],
    queryFn: async () => {
      const response = await attendanceAPI.getMyChildAttendances(selectedDate);
      return response.data;
    },
    enabled: !!profile && profile.role === 'PARENT',
  });

  // 출석 상태별 배지
  const getStatusBadge = (status) => {
    const statusMap = {
      PRESENT: { text: '출석', color: '#03C75A' },
      LATE: { text: '지각', color: '#FFA500' },
      ABSENT: { text: '결석', color: '#FF0000' },
      EXCUSED: { text: '사유결석', color: '#0066FF' },
      EARLY_LEAVE: { text: '조퇴', color: '#9C27B0' },
    };
    const { text, color } = statusMap[status] || { text: status, color: '#999' };
    return <span className="status-badge" style={{ backgroundColor: color }}>{text}</span>;
  };

  // 시간 포맷팅
  const formatTime = (timeString) => {
    if (!timeString) return '-';
    return timeString.substring(0, 5); // HH:MM 형식
  };

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
              수업 정보
            </h1>
            <p className="page-subtitle">자녀의 수업 출석 현황을 확인합니다</p>
          </div>
          <div className="date-selector">
            <input
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="date-input"
            />
          </div>
        </div>
      </div>

      {/* 메인 컨텐츠 */}
      <div className="page-content">
        <div className="class-info-container">
          <div className="selected-date-info">
            <h2>{new Date(selectedDate).toLocaleDateString('ko-KR', { 
              year: 'numeric', 
              month: 'long', 
              day: 'numeric',
              weekday: 'long'
            })} 수업 현황</h2>
            <span className="attendance-count">{attendances.length}건</span>
          </div>

          {attendances.length === 0 ? (
            <div className="empty-state">
              <i className="fas fa-calendar-times"></i>
              <h3>해당 날짜에 수업이 없습니다</h3>
              <p>다른 날짜를 선택해보세요.</p>
            </div>
          ) : (
            <div className="attendance-list">
              {attendances.map((attendance) => (
                <div key={attendance.id} className="attendance-card">
                  <div className="card-header">
                    <div className="student-course-info">
                      <h3>{attendance.student.name}</h3>
                      <span className="course-name">{attendance.schedule.course.name}</span>
                    </div>
                    {getStatusBadge(attendance.status)}
                  </div>

                  <div className="card-body">
                    <div className="info-grid">
                      <div className="info-item">
                        <span className="label">
                          <i className="fas fa-clock"></i>
                          수업 시간
                        </span>
                        <span className="value">
                          {formatTime(attendance.schedule.startTime)} - {formatTime(attendance.schedule.endTime)}
                        </span>
                      </div>

                      <div className="info-item">
                        <span className="label">
                          <i className="fas fa-sign-in-alt"></i>
                          출석 시간
                        </span>
                        <span className="value">
                          {attendance.checkInTime ? formatTime(attendance.checkInTime) : '-'}
                        </span>
                      </div>

                      <div className="info-item">
                        <span className="label">
                          <i className="fas fa-sign-out-alt"></i>
                          퇴실 시간
                        </span>
                        <span className="value">
                          {attendance.checkOutTime ? formatTime(attendance.checkOutTime) : '-'}
                        </span>
                      </div>

                      <div className="info-item">
                        <span className="label">
                          <i className="fas fa-user-tie"></i>
                          담당 강사
                        </span>
                        <span className="value">
                          {attendance.teacherName || '미배정'}
                        </span>
                      </div>
                    </div>

                    {attendance.notes && (
                      <div className="notes-section">
                        <span className="notes-label">
                          <i className="fas fa-sticky-note"></i>
                          메모
                        </span>
                        <p className="notes-content">{attendance.notes}</p>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default ClassInfo;
