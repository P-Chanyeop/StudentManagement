import Layout from '../components/Layout';
import '../styles/Dashboard.css';

function Dashboard() {
  return (
    <Layout>
      <div className="dashboard">
        <div className="page-header">
          <h1 className="page-title">대시보드</h1>
          <p className="page-subtitle">학원 운영 현황을 한눈에 확인하세요</p>
        </div>

        <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-icon" style={{ background: '#E8F8F0' }}>
              👥
            </div>
            <div className="stat-content">
              <div className="stat-label">전체 학생</div>
              <div className="stat-value">0명</div>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon" style={{ background: '#FFF5E6' }}>
              ✅
            </div>
            <div className="stat-content">
              <div className="stat-label">오늘 출석</div>
              <div className="stat-value">0명</div>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon" style={{ background: '#E6F7FF' }}>
              📅
            </div>
            <div className="stat-content">
              <div className="stat-label">오늘 예약</div>
              <div className="stat-value">0건</div>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon" style={{ background: '#FFF0F6' }}>
              ⚠️
            </div>
            <div className="stat-content">
              <div className="stat-label">만료 임박 수강권</div>
              <div className="stat-value">0개</div>
            </div>
          </div>
        </div>

        <div className="dashboard-grid">
          <div className="dashboard-card">
            <div className="card-header">
              <h2 className="card-title">최근 공지사항</h2>
            </div>
            <div className="card-body">
              <div className="empty-state">
                <p>공지사항이 없습니다</p>
              </div>
            </div>
          </div>

          <div className="dashboard-card">
            <div className="card-header">
              <h2 className="card-title">오늘의 수업</h2>
            </div>
            <div className="card-body">
              <div className="empty-state">
                <p>예정된 수업이 없습니다</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Layout>
  );
}

export default Dashboard;
