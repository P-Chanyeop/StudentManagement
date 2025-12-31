import { useQuery } from '@tanstack/react-query';
import {
  studentAPI,
  attendanceAPI,
  reservationAPI,
  enrollmentAPI,
  scheduleAPI,
  authAPI
} from '../services/api';
import '../styles/Dashboard.css';

function Dashboard() {
  // 오늘 날짜
  const today = new Date().toISOString().split('T')[0];

  // 사용자 프로필 조회
  const { data: profile } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
  });

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

  // 출석률 계산 및 실제 출석한 학생 수 계산
  const actualAttendedCount = todayAttendance.filter(attendance => 
    attendance.checkInTime && attendance.status !== 'ABSENT'
  ).length;
  
  const attendanceRate = todaySchedules.length > 0
    ? Math.round((actualAttendedCount / todaySchedules.length) * 100)
    : 0;

  return (
    <div className="dashboard-wrapper">
      {/* 히어로 섹션 */}
      <section className="hero">
        <div className="hero-container">
          <h1>안녕하세요, {profile?.name || '사용자'}님! 👋</h1>
          <p>오늘도 학원 운영을 효율적으로 관리하세요</p>
        </div>
      </section>

      {/* 메인 컨텐츠 */}
      <div className="dashboard-container">
        {/* 통계 카드 */}
        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-card-header">
              <div className="stat-icon">
                <i className="fas fa-users"></i>
              </div>
              <div className="stat-trend">
                <i className="fas fa-arrow-up"></i>
                NEW
              </div>
            </div>
            <div className="stat-content">
              <h3>전체 학생</h3>
              <div className="stat-value">
                {students.length}
                <span className="stat-unit">명</span>
              </div>
            </div>
            <div className="stat-footer">
              <i className="fas fa-info-circle"></i> 등록된 전체 학생 수
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-card-header">
              <div className="stat-icon">
                <i className="fas fa-chalkboard-teacher"></i>
              </div>
              <div className="stat-trend">
                <i className="fas fa-calendar-day"></i>
                오늘
              </div>
            </div>
            <div className="stat-content">
              <h3>오늘의 수업</h3>
              <div className="stat-value">
                {todaySchedules.length}
                <span className="stat-unit">개</span>
              </div>
            </div>
            <div className="stat-footer">
              <i className="fas fa-info-circle"></i> 오늘 예정된 수업
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-card-header">
              <div className="stat-icon">
                <i className="fas fa-check-circle"></i>
              </div>
              <div className="stat-trend success">
                <i className="fas fa-check"></i>
                {attendanceRate}%
              </div>
            </div>
            <div className="stat-content">
              <h3>오늘 출석</h3>
              <div className="stat-value">
                {actualAttendedCount}
                <span className="stat-unit">명</span>
              </div>
            </div>
            <div className="stat-footer">
              <i className="fas fa-info-circle"></i> 출석률 {attendanceRate}%
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-card-header">
              <div className="stat-icon">
                <i className="fas fa-exclamation-triangle"></i>
              </div>
              <div className="stat-trend warning">
                <i className="fas fa-clock"></i>
                임박
              </div>
            </div>
            <div className="stat-content">
              <h3>만료 임박 수강권</h3>
              <div className="stat-value">
                {expiringEnrollments.length}
                <span className="stat-unit">개</span>
              </div>
            </div>
            <div className="stat-footer">
              <i className="fas fa-info-circle"></i> 7일 이내 만료 예정
            </div>
          </div>
        </div>

        {/* 대시보드 그리드 */}
        <div className="dashboard-grid">
          {/* 오늘의 수업 */}
          <div className="dashboard-card">
            <div className="card-header">
              <h2 className="card-title">
                <i className="fas fa-calendar-day"></i>
                오늘의 수업
              </h2>
              <span className="card-badge">{todaySchedules.length}개</span>
            </div>
            <div className="card-body">
              {todaySchedules.length === 0 ? (
                <div className="empty-state">
                  <i className="fas fa-calendar-times"></i>
                  <p>예정된 수업이 없습니다</p>
                </div>
              ) : (
                <div className="list">
                  {todaySchedules.slice(0, 5).map((schedule) => (
                    <div key={schedule.id} className="list-item">
                      <div className="item-icon">
                        <i className="fas fa-book-open"></i>
                      </div>
                      <div className="item-content">
                        <div className="item-title">{schedule.courseName}</div>
                        <div className="item-subtitle">
                          {formatTime(schedule.startTime)} - {formatTime(schedule.endTime)} · {schedule.teacherName}
                        </div>
                      </div>
                      <div className={`item-badge badge-${schedule.status?.toLowerCase() || 'default'}`}>
                        {schedule.status === 'SCHEDULED' ? '예정' :
                         schedule.status === 'COMPLETED' ? '완료' :
                         schedule.status === 'CANCELLED' ? '취소' : schedule.status || '미정'}
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
              <h2 className="card-title">
                <i className="fas fa-exclamation-triangle"></i>
                만료 임박 수강권
              </h2>
              <span className="card-badge warning">{expiringEnrollments.length}개</span>
            </div>
            <div className="card-body">
              {expiringEnrollments.length === 0 ? (
                <div className="empty-state">
                  <i className="fas fa-check-circle"></i>
                  <p>만료 임박 수강권이 없습니다</p>
                </div>
              ) : (
                <div className="list">
                  {expiringEnrollments.slice(0, 5).map((enrollment) => {
                    const daysLeft = Math.ceil(
                      (new Date(enrollment.endDate) - new Date()) / (1000 * 60 * 60 * 24)
                    );
                    return (
                      <div key={enrollment.id} className="list-item">
                        <div className={`item-icon ${daysLeft <= 3 ? 'urgent' : 'warning'}`}>
                          <i className="fas fa-ticket-alt"></i>
                        </div>
                        <div className="item-content">
                          <div className="item-title">{enrollment.studentName} - {enrollment.courseName}</div>
                          <div className="item-subtitle">
                            남은 횟수: {enrollment.remainingCount}회 · 종료일: {enrollment.endDate}
                          </div>
                        </div>
                        <div className={`item-badge ${daysLeft <= 3 ? 'badge-error' : 'badge-warning'}`}>
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
              <h2 className="card-title">
                <i className="fas fa-user-check"></i>
                오늘 출석 현황
              </h2>
              <span className="card-badge">{todayAttendance.length}명</span>
            </div>
            <div className="card-body">
              {todayAttendance.length === 0 ? (
                <div className="empty-state">
                  <i className="fas fa-user-clock"></i>
                  <p>출석 기록이 없습니다</p>
                </div>
              ) : (
                <div className="list">
                  {todayAttendance.slice(0, 5).map((attendance) => (
                    <div key={attendance.id} className="list-item">
                      <div className="item-icon">
                        <i className="fas fa-user"></i>
                      </div>
                      <div className="item-content">
                        <div className="item-title">{attendance.studentName}</div>
                        <div className="item-subtitle">
                          체크인: {formatTime(attendance.checkInTime)}
                        </div>
                      </div>
                      <div className={`item-badge badge-${attendance.status.toLowerCase()}`}>
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
              <h2 className="card-title">
                <i className="fas fa-calendar-check"></i>
                오늘 예약 현황
              </h2>
              <span className="card-badge">{todayReservations.length}건</span>
            </div>
            <div className="card-body">
              {todayReservations.length === 0 ? (
                <div className="empty-state">
                  <i className="fas fa-calendar-times"></i>
                  <p>예약이 없습니다</p>
                </div>
              ) : (
                <div className="list">
                  {todayReservations.slice(0, 5).map((reservation) => (
                    <div key={reservation.id} className="list-item">
                      <div className="item-icon">
                        <i className="fas fa-bookmark"></i>
                      </div>
                      <div className="item-content">
                        <div className="item-title">{reservation.studentName}</div>
                        <div className="item-subtitle">
                          {formatTime(reservation.scheduleStartTime)}
                        </div>
                      </div>
                      <div className={`item-badge badge-${reservation.status.toLowerCase()}`}>
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
    </div>
  );
}

export default Dashboard;
