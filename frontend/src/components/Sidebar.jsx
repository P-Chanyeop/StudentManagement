import { useState, useEffect } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { authAPI } from '../services/api';
import '../styles/Sidebar.css';

function Sidebar() {
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [sidebarTop, setSidebarTop] = useState(window.innerHeight / 2);
  const navigate = useNavigate();

  // 사용자 프로필 조회
  const { data: profile } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
  });

  useEffect(() => {
    let timeoutId;
    
    const handleScroll = () => {
      // 기존 타이머 클리어
      clearTimeout(timeoutId);
      
      // 0.05초 후에 현재 화면 정중앙으로 이동
      timeoutId = setTimeout(() => {
        const newTop = window.scrollY + (window.innerHeight / 2);
        setSidebarTop(newTop);
      }, 50);
    };

    window.addEventListener('scroll', handleScroll);
    return () => {
      window.removeEventListener('scroll', handleScroll);
      clearTimeout(timeoutId);
    };
  }, []);

  const handleLogout = () => {
    if (window.confirm('로그아웃 하시겠습니까?')) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      navigate('/login');
    }
  };

  const toggleSidebar = () => {
    setIsCollapsed(!isCollapsed);
  };

  // 역할별 메뉴 아이템 정의
  const getMenuItems = () => {
    const adminTeacherMenus = [
      { path: '/dashboard', icon: <i className="fas fa-chart-bar"></i>, label: '대시보드' },
      { path: '/students', icon: <i className="fas fa-users"></i>, label: '학생 관리' },
      { path: '/courses', icon: <i className="fas fa-chalkboard-teacher"></i>, label: '수업 관리' },
      { path: '/attendance', icon: <i className="fas fa-check-circle"></i>, label: '출석 체크' },
      { path: '/reservations', icon: <i className="fas fa-calendar-alt"></i>, label: '예약 관리' },
      { path: '/parent-reservation', icon: <i className="fas fa-calendar-plus"></i>, label: '상담 예약' },
      { path: '/enrollments', icon: <i className="fas fa-ticket-alt"></i>, label: '수강권 관리' },
      { path: '/enrollment-adjustment', icon: <i className="fas fa-edit"></i>, label: '횟수 조정' },
      { path: '/makeup-classes', icon: <i className="fas fa-redo"></i>, label: '보강 수업' },
      { path: '/consultations', icon: <i className="fas fa-comments"></i>, label: '상담 내역' },
      { path: '/messages', icon: <i className="fas fa-envelope"></i>, label: '문자 발송' },
      { path: '/notices', icon: <i className="fas fa-bell"></i>, label: '공지사항' },
    ];

    const studentParentMenus = [
      { path: '/dashboard', icon: <i className="fas fa-chart-bar"></i>, label: '대시보드' },
      { path: '/students', icon: <i className="fas fa-user"></i>, label: '학생 관리' },
      { path: '/parent-reservation', icon: <i className="fas fa-calendar-plus"></i>, label: '상담 예약' },
      { path: '/reservations', icon: <i className="fas fa-calendar-alt"></i>, label: '예약 관리' },
      { path: '/enrollments', icon: <i className="fas fa-ticket-alt"></i>, label: '수강권' },
      { path: '/makeup-classes', icon: <i className="fas fa-redo"></i>, label: '보강 수업' },
      { path: '/consultations', icon: <i className="fas fa-comments"></i>, label: '상담 내역' },
      { path: '/notices', icon: <i className="fas fa-bell"></i>, label: '공지사항' },
    ];

    if (profile?.role === 'PARENT') {
      return studentParentMenus;
    }
    return adminTeacherMenus;
  };

  const menuItems = getMenuItems();

  return (
    <div 
      className={`sidebar ${isCollapsed ? 'collapsed' : ''}`}
      style={{
        top: `${sidebarTop}px`
      }}
    >
      <button className="toggle-btn" onClick={toggleSidebar}>
        <i className={`fas ${isCollapsed ? 'fa-chevron-right' : 'fa-chevron-left'}`}></i>
      </button>
      
      <div className="sidebar-header">
        <div className="logo">
          <span className="logo-icon">🎓</span>
          {!isCollapsed && <h2>학원 관리 시스템</h2>}
        </div>
      </div>

      <nav className="sidebar-nav">
        {menuItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
            title={isCollapsed ? item.label : ''}
          >
            <span className="nav-icon">{item.icon}</span>
            {!isCollapsed && <span className="nav-label">{item.label}</span>}
          </NavLink>
        ))}
      </nav>

      <div className="sidebar-footer">
        <button className="logout-btn" onClick={handleLogout} title={isCollapsed ? '로그아웃' : ''}>
          <span className="nav-icon"><i className="fas fa-sign-out-alt"></i></span>
          {!isCollapsed && <span className="nav-label">로그아웃</span>}
        </button>
      </div>
    </div>
  );
}

export default Sidebar;
