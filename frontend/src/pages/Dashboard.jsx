import { useQuery } from '@tanstack/react-query';
import {
  studentAPI,
  attendanceAPI,
  reservationAPI,
  enrollmentAPI,
  scheduleAPI
} from '../services/api';
import '../styles/Dashboard.css';

function Dashboard() {
  // 오늘 날짜
  const today = new Date().toISOString().split('T')[0];

  // 전체 학생 수
  const { data: students = [] } = useQuery({
    queryKey: ['students'],
    queryFn: async () => {
      const response = await studentAPI.getAll();
      return response.data;
    },
  });

  // 오늘 출석 현황
  const { data: todayAttendance = [] } = useQuery({
    queryKey: ['todayAttendance', today],
    queryFn: async () => {
      const response = await attendanceAPI.getByDate(today);
      return response.data;
    },
  });

  // 오늘 예약 현황
  const { data: todayReservations = [] } = useQuery({
    queryKey: ['todayReservations', today],
    queryFn: async () => {
      const response = await reservationAPI.getByDate(today);
      return response.data;
    },
  });

  // 만료 임박 수강권 (7일 이내)
  const { data: expiringEnrollments = [] } = useQuery({
    queryKey: ['expiringEnrollments'],
    queryFn: async () => {
      const response = await enrollmentAPI.getExpiring(7);
      return response.data;
    },
  });

  // 오늘의 수업 목록
  const { data: todaySchedules = [] } = useQuery({
    queryKey: ['todaySchedules', today],
    queryFn: async () => {
      const response = await scheduleAPI.getByDate(today);
      return response.data;
    },
  });

  // 시간 포맷팅
  const formatTime = (timeString) => {
    if (!timeString) return '';
    return timeString.substring(0, 5); // HH:MM
  };

  return (
    <div className="dashboard-container">
      <div className="dashboard-header">
        <h1>대시보드</h1>
        <p className="dashboard-subtitle">학원 운영 현황을 한눈에 확인하세요</p>
      </div>

      {/* 통계 카드 */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon" style={{ background: '#E8F8F0' }}>
            <i className="fas fa-users"></i>
          </div>
          <div className="stat-content">
            <div className="stat-label">전체 학생</div>
            <div className="stat-value">{students.length}명</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: '#FFF5E6' }}>
            <i className="fas fa-check-circle"></i>
          </div>
          <div className="stat-content">
            <div className="stat-label">오늘 출석</div>
            <div className="stat-value">{todayAttendance.length}명</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: '#E6F7FF' }}>
            <i className="fas fa-calendar-alt"></i>
          </div>
          <div className="stat-content">
            <div className="stat-label">오늘 예약</div>
            <div className="stat-value">{todayReservations.length}건</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon" style={{ background: '#FFF0F6' }}>
            <i className="fas fa-exclamation-triangle"></i>
          </div>
          <div className="stat-content">
            <div className="stat-label">만료 임박 수강권</div>
            <div className="stat-value">{expiringEnrollments.length}개</div>
          </div>
        </div>
      </div>

      {/* 상세 정보 그리드 */}
      <div className="dashboard-grid">
        {/* 오늘의 수업 */}
        <div className="dashboard-card">
          <div className="card-header">
            <h2 className="card-title">오늘의 수업</h2>
            <span className="card-count">{todaySchedules.length}개</span>
          </div>
          <div className="card-body">
            {todaySchedules.length === 0 ? (
              <div className="empty-state">
                <p>예정된 수업이 없습니다</p>
              </div>
            ) : (
              <div className="schedule-list">
                {todaySchedules.slice(0, 5).map((schedule) => (
                  <div key={schedule.id} className="schedule-item">
                    <div className="schedule-time">
                      {formatTime(schedule.startTime)} - {formatTime(schedule.endTime)}
                    </div>
                    <div className="schedule-info">
                      <div className="schedule-course">{schedule.courseName}</div>
                      <div className="schedule-teacher">{schedule.teacherName}</div>
                    </div>
                    <div className={`schedule-status ${schedule.status.toLowerCase()}`}>
                      {schedule.status === 'SCHEDULED' ? '예정' :
                       schedule.status === 'COMPLETED' ? '완료' :
                       schedule.status === 'CANCELLED' ? '취소' : schedule.status}
                    </div>
                  </div>
                ))}
                {todaySchedules.length > 5 && (
                  <div className="show-more">
                    +{todaySchedules.length - 5}개 더 보기
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        {/* 만료 임박 수강권 */}
        <div className="dashboard-card">
          <div className="card-header">
            <h2 className="card-title">만료 임박 수강권</h2>
            <span className="card-count">{expiringEnrollments.length}개</span>
          </div>
          <div className="card-body">
            {expiringEnrollments.length === 0 ? (
              <div className="empty-state">
                <p>만료 임박 수강권이 없습니다</p>
              </div>
            ) : (
              <div className="enrollment-list">
                {expiringEnrollments.slice(0, 5).map((enrollment) => {
                  const daysLeft = Math.ceil(
                    (new Date(enrollment.endDate) - new Date()) / (1000 * 60 * 60 * 24)
                  );
                  return (
                    <div key={enrollment.id} className="enrollment-item">
                      <div className="enrollment-student">
                        <div className="student-name">{enrollment.studentName}</div>
                        <div className="course-name">{enrollment.courseName}</div>
                      </div>
                      <div className={`days-left ${daysLeft <= 3 ? 'urgent' : 'warning'}`}>
                        {daysLeft}일 남음
                      </div>
                    </div>
                  );
                })}
                {expiringEnrollments.length > 5 && (
                  <div className="show-more">
                    +{expiringEnrollments.length - 5}개 더 보기
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        {/* 최근 출석 현황 */}
        <div className="dashboard-card">
          <div className="card-header">
            <h2 className="card-title">오늘 출석 현황</h2>
            <span className="card-count">{todayAttendance.length}명</span>
          </div>
          <div className="card-body">
            {todayAttendance.length === 0 ? (
              <div className="empty-state">
                <p>출석 기록이 없습니다</p>
              </div>
            ) : (
              <div className="attendance-list">
                {todayAttendance.slice(0, 5).map((attendance) => (
                  <div key={attendance.id} className="attendance-item">
                    <div className="attendance-student">
                      <div className="student-name">{attendance.studentName}</div>
                      <div className="attendance-time">
                        체크인: {formatTime(attendance.checkInTime)}
                      </div>
                    </div>
                    <div className={`attendance-status ${attendance.status.toLowerCase()}`}>
                      {attendance.status === 'PRESENT' ? '출석' :
                       attendance.status === 'LATE' ? '지각' :
                       attendance.status === 'ABSENT' ? '결석' :
                       attendance.status === 'EXCUSED' ? '사유결석' : attendance.status}
                    </div>
                  </div>
                ))}
                {todayAttendance.length > 5 && (
                  <div className="show-more">
                    +{todayAttendance.length - 5}명 더 보기
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

        {/* 오늘 예약 현황 */}
        <div className="dashboard-card">
          <div className="card-header">
            <h2 className="card-title">오늘 예약 현황</h2>
            <span className="card-count">{todayReservations.length}건</span>
          </div>
          <div className="card-body">
            {todayReservations.length === 0 ? (
              <div className="empty-state">
                <p>예약이 없습니다</p>
              </div>
            ) : (
              <div className="reservation-list">
                {todayReservations.slice(0, 5).map((reservation) => (
                  <div key={reservation.id} className="reservation-item">
                    <div className="reservation-student">
                      <div className="student-name">{reservation.studentName}</div>
                      <div className="schedule-time">
                        {formatTime(reservation.scheduleStartTime)}
                      </div>
                    </div>
                    <div className={`reservation-status ${reservation.status.toLowerCase()}`}>
                      {reservation.status === 'PENDING' ? '대기' :
                       reservation.status === 'CONFIRMED' ? '확정' :
                       reservation.status === 'CANCELLED' ? '취소' :
                       reservation.status === 'COMPLETED' ? '완료' : reservation.status}
                    </div>
                  </div>
                ))}
                {todayReservations.length > 5 && (
                  <div className="show-more">
                    +{todayReservations.length - 5}건 더 보기
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default Dashboard;
