import { NavLink, useNavigate } from 'react-router-dom';
import '../styles/Sidebar.css';

function Sidebar() {
  const navigate = useNavigate();

  const handleLogout = () => {
    if (window.confirm('로그아웃 하시겠습니까?')) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      navigate('/login');
    }
  };

  const menuItems = [
    { path: '/dashboard', icon: '📊', label: '대시보드' },
    { path: '/students', icon: '👥', label: '학생 관리' },
    { path: '/courses', icon: '📚', label: '코스 관리' },
    { path: '/attendance', icon: '✅', label: '출석 체크' },
    { path: '/reservations', icon: '📅', label: '예약 관리' },
    { path: '/enrollments', icon: '🎫', label: '수강권 관리' },
    { path: '/leveltests', icon: '📝', label: '레벨 테스트' },
    { path: '/consultations', icon: '💬', label: '상담 내역' },
    { path: '/messages', icon: '📨', label: '문자 발송' },
  ];

  return (
    <div className="sidebar">
      <div className="sidebar-header">
        <div className="logo">
          <span className="logo-icon">🎓</span>
          <h2>학원 관리 시스템</h2>
        </div>
      </div>

      <nav className="sidebar-nav">
        {menuItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
          >
            <span className="nav-icon">{item.icon}</span>
            <span className="nav-label">{item.label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-footer">
        <button className="logout-btn" onClick={handleLogout}>
          <span className="nav-icon">🚪</span>
          <span className="nav-label">로그아웃</span>
        </button>
      </div>
    </div>
  );
}

export default Sidebar;
