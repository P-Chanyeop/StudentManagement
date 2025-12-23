import { useQuery } from '@tanstack/react-query';
import { authAPI, enrollmentAPI } from '../services/api';
import '../styles/Dashboard.css';

function UserDashboard() {
  // 사용자 프로필 조회
  const { data: profile } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
  });

  // 사용자 수강권 조회
  const { data: myEnrollments = [], isLoading } = useQuery({
    queryKey: ['myEnrollments'],
    queryFn: async () => {
      const response = await enrollmentAPI.getMyEnrollments();
      return response.data;
    },
    enabled: profile && (profile.role === 'STUDENT' || profile.role === 'PARENT'),
  });

  if (isLoading) {
    return <div className="dashboard-wrapper"><div className="loading">로딩 중...</div></div>;
  }

  const activeEnrollments = myEnrollments.filter(e => e.isActive);
  const expiringEnrollments = activeEnrollments.filter(e => {
    const endDate = new Date(e.endDate);
    const today = new Date();
    const diffDays = Math.ceil((endDate - today) / (1000 * 60 * 60 * 24));
    return diffDays <= 7 && diffDays > 0;
  });

  const calculateAttendanceRate = (enrollment) => {
    const usedCount = enrollment.usedCount || 0;
    const totalCount = enrollment.totalCount || 1;
    return Math.round((usedCount / totalCount) * 100);
  };

  return (
    <div className="dashboard-wrapper">
      {/* 히어로 섹션 */}
      <section className="hero">
        <div className="hero-container">
          <h1>안녕하세요, {profile?.name || '사용자'}님! 👋</h1>
          <p>{profile?.role === 'PARENT' ? '자녀의 학습 현황을 확인하세요' : '나의 학습 현황을 확인하세요'}</p>
        </div>
      </section>

      {/* 메인 컨텐츠 */}
      <div className="dashboard-container">
        {/* 통계 카드 */}
        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-card-header">
              <div className="stat-icon">
                <i className="fas fa-book-open"></i>
              </div>
              <div className="stat-trend">
                <i className="fas fa-arrow-up"></i>
                수강중
              </div>
            </div>
            <div className="stat-content">
              <h3>수강 중인 수업</h3>
              <div className="stat-value">
                {activeEnrollments.length}
                <span className="stat-unit">개</span>
              </div>
            </div>
            <div className="stat-footer">
              <i className="fas fa-info-circle"></i> 현재 수강 중인 수업
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-card-header">
              <div className="stat-icon">
                <i className="fas fa-clock"></i>
              </div>
              <div className="stat-trend warning">
                <i className="fas fa-exclamation-triangle"></i>
                임박
              </div>
            </div>
            <div className="stat-content">
              <h3>만료 임박</h3>
              <div className="stat-value">
                {expiringEnrollments.length}
                <span className="stat-unit">개</span>
              </div>
            </div>
            <div className="stat-footer">
              <i className="fas fa-info-circle"></i> 7일 이내 만료 예정
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-card-header">
              <div className="stat-icon">
                <i className="fas fa-chart-line"></i>
              </div>
              <div className="stat-trend success">
                <i className="fas fa-check"></i>
                평균
              </div>
            </div>
            <div className="stat-content">
              <h3>평균 출석률</h3>
              <div className="stat-value">
                {activeEnrollments.length > 0 
                  ? Math.round(activeEnrollments.reduce((sum, e) => sum + calculateAttendanceRate(e), 0) / activeEnrollments.length)
                  : 0}
                <span className="stat-unit">%</span>
              </div>
            </div>
            <div className="stat-footer">
              <i className="fas fa-info-circle"></i> 전체 수업 평균 출석률
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-card-header">
              <div className="stat-icon">
                <i className="fas fa-battery-three-quarters"></i>
              </div>
              <div className="stat-trend">
                <i className="fas fa-ticket-alt"></i>
                잔여
              </div>
            </div>
            <div className="stat-content">
              <h3>총 잔여 횟수</h3>
              <div className="stat-value">
                {activeEnrollments.reduce((sum, e) => sum + (e.remainingCount || 0), 0)}
                <span className="stat-unit">회</span>
              </div>
            </div>
            <div className="stat-footer">
              <i className="fas fa-info-circle"></i> 모든 수강권 잔여 횟수 합계
            </div>
          </div>
        </div>

        {/* 대시보드 그리드 */}
        <div className="dashboard-grid">
          {/* 수강 중인 수업 */}
          <div className="dashboard-card">
            <div className="card-header">
              <h2 className="card-title">
                <i className="fas fa-book-open"></i>
                {profile?.role === 'PARENT' ? '자녀 수업 현황' : '내 수업 현황'}
              </h2>
              <span className="card-badge">{activeEnrollments.length}개</span>
            </div>
            <div className="card-body">
              {activeEnrollments.length === 0 ? (
                <div className="empty-state">
                  <i className="fas fa-inbox"></i>
                  <p>등록된 수강권이 없습니다</p>
                </div>
              ) : (
                <div className="list">
                  {activeEnrollments.map((enrollment) => {
                    const attendanceRate = calculateAttendanceRate(enrollment);
                    return (
                      <div key={enrollment.id} className="list-item">
                        <div className="item-icon">
                          <i className="fas fa-graduation-cap"></i>
                        </div>
                        <div className="item-content">
                          <div className="item-title">{enrollment.course?.courseName || enrollment.courseName}</div>
                          <div className="item-subtitle">
                            {enrollment.student?.studentName || enrollment.studentName} · 
                            잔여: {enrollment.remainingCount}회 · 출석률: {attendanceRate}%
                          </div>
                        </div>
                        <div className={`item-badge ${enrollment.remainingCount <= 3 ? 'badge-warning' : 'badge-success'}`}>
                          {enrollment.isActive ? '수강중' : '종료'}
                        </div>
                      </div>
                    );
                  })}
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
                  {expiringEnrollments.map((enrollment) => {
                    const endDate = new Date(enrollment.endDate);
                    const today = new Date();
                    const diffDays = Math.ceil((endDate - today) / (1000 * 60 * 60 * 24));
                    return (
                      <div key={enrollment.id} className="list-item">
                        <div className={`item-icon ${diffDays <= 3 ? 'urgent' : 'warning'}`}>
                          <i className="fas fa-ticket-alt"></i>
                        </div>
                        <div className="item-content">
                          <div className="item-title">{enrollment.course?.courseName || enrollment.courseName}</div>
                          <div className="item-subtitle">
                            {enrollment.student?.studentName || enrollment.studentName} · 
                            잔여: {enrollment.remainingCount}회 · 종료일: {enrollment.endDate}
                          </div>
                        </div>
                        <div className={`item-badge ${diffDays <= 3 ? 'badge-error' : 'badge-warning'}`}>
                          {diffDays}일 남음
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
    </div>
  );
}

export default UserDashboard;
