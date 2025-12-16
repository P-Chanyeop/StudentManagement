import { useNavigate } from 'react-router-dom';
import { authAPI } from '../services/api';
import '../styles/Layout.css';

function Layout({ children }) {
  const navigate = useNavigate();
  const userName = localStorage.getItem('userName');
  const userRole = localStorage.getItem('userRole');

  const handleLogout = async () => {
    try {
      await authAPI.logout();
    } catch (error) {
      console.error('로그아웃 실패:', error);
    } finally {
      localStorage.clear();
      navigate('/login');
    }
  };

  const getRoleLabel = (role) => {
    const roleMap = {
      'ADMIN': '관리자',
      'TEACHER': '선생님',
      'PARENT': '학부모',
      'STUDENT': '학생',
    };
    return roleMap[role] || role;
  };

  return (
    <div className="layout">
      <header className="header">
        <div className="header-container">
          <div className="header-left">
            <div className="logo" onClick={() => navigate('/dashboard')}>
              <span className="logo-icon">📚</span>
              <span className="logo-text">학원 관리 시스템</span>
            </div>
            <nav className="nav">
              <button onClick={() => navigate('/dashboard')}>대시보드</button>
              <button onClick={() => navigate('/students')}>학생 관리</button>
              <button onClick={() => navigate('/attendance')}>출석 관리</button>
              <button onClick={() => navigate('/reservations')}>예약 관리</button>
            </nav>
          </div>
          <div className="header-right">
            <div className="user-info">
              <span className="user-role">{getRoleLabel(userRole)}</span>
              <span className="user-name">{userName}</span>
            </div>
            <button className="logout-button" onClick={handleLogout}>
              로그아웃
            </button>
          </div>
        </div>
      </header>

      <main className="main-content">
        {children}
      </main>

      <footer className="footer">
        <p>© 2025 학원 관리 시스템. SOFTCAT</p>
      </footer>
    </div>
  );
}

export default Layout;
