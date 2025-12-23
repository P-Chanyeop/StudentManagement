import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useLocation, Link } from 'react-router-dom';
import { authAPI, enrollmentAPI } from '../services/api';
import '../styles/Header.css';

function Header() {
  const [showDropdown, setShowDropdown] = useState(false);
  const [showEnrollments, setShowEnrollments] = useState(false);
  const location = useLocation();

  // 사용자 프로필 조회
  const { data: profile } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5분 동안 캐시 유지
  });

  // 사용자 수강권 조회 (학생/학부모만)
  const { data: myEnrollments = [] } = useQuery({
    queryKey: ['myEnrollments'],
    queryFn: async () => {
      const response = await enrollmentAPI.getMyEnrollments();
      return response.data;
    },
    enabled: profile && (profile.role === 'STUDENT' || profile.role === 'PARENT'),
    staleTime: 2 * 60 * 1000, // 2분 동안 캐시 유지
  });

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (!event.target.closest('.user-menu')) {
        setShowDropdown(false);
      }
    };

    if (showDropdown) {
      document.addEventListener('click', handleClickOutside);
    }

    return () => {
      document.removeEventListener('click', handleClickOutside);
    };
  }, [showDropdown]);

  if (!profile) {
    return null;
  }

  const getInitial = () => {
    return profile.name ? profile.name.charAt(0) : profile.username.charAt(0);
  };

  const isActive = (path) => {
    return location.pathname === path;
  };

  // 역할별 메뉴 항목
  const getMenuItems = () => {
    const baseItems = [
      { path: '/', label: '대시보드', icon: 'fa-home' }
    ];

    if (profile.role === 'ADMIN' || profile.role === 'TEACHER') {
      return [
        ...baseItems,
        { path: '/students', label: '학생 관리', icon: 'fa-user-graduate' },
        { path: '/courses', label: '수업 관리', icon: 'fa-book-reader' },
        { path: '/attendance', label: '출석 관리', icon: 'fa-clipboard-check' },
        { path: '/enrollments', label: '수강권 관리', icon: 'fa-receipt' },
      ];
    } else if (profile.role === 'PARENT') {
      return [
        ...baseItems,
        { path: '/reservations', label: '수업 예약', icon: 'fa-calendar-alt' },
        { path: '/notices', label: '공지사항', icon: 'fa-bell' },
      ];
    } else if (profile.role === 'STUDENT') {
      return [
        ...baseItems,
        { path: '/reservations', label: '나의 수업', icon: 'fa-calendar-alt' },
        { path: '/notices', label: '공지사항', icon: 'fa-bell' },
      ];
    }

    return baseItems;
  };

  const menuItems = getMenuItems();

  return (
    <header className="header">
      <div className="header-container">
        {/* 로고 */}
        <Link to="/" className="logo">
          {/*<i className="fas fa-graduation-cap"></i>*/}
          {/*<span></span>*/}
        </Link>

        {/* 네비게이션 메뉴 */}
        <nav className="nav-menu">
          <ul>
            {menuItems.map((item) => (
              <li key={item.path}>
                <Link
                  to={item.path}
                  className={isActive(item.path) ? 'active' : ''}
                >
                  <i className={`fas ${item.icon}`}></i>
                  <span>{item.label}</span>
                </Link>
              </li>
            ))}
          </ul>
        </nav>

        {/* 사용자 섹션 */}
        <div className="user-section">
          {/* 사용자 메뉴 */}
          <div className="user-menu">
            <button
              className="user-button"
              onClick={() => {
                setShowDropdown(!showDropdown);
                setShowEnrollments(false);
              }}
            >
              <div className="user-avatar">
                {getInitial()}
              </div>
              <div className="user-info">
                <span className="user-name">{profile.name}</span>
                <span className="user-role">
                  {profile.role === 'ADMIN' && '관리자'}
                  {profile.role === 'TEACHER' && '선생님'}
                  {profile.role === 'PARENT' && '학부모'}
                  {profile.role === 'STUDENT' && '학생'}
                </span>
              </div>
              <svg
                className={`dropdown-icon ${showDropdown ? 'open' : ''}`}
                width="20"
                height="20"
                viewBox="0 0 20 20"
                fill="currentColor"
              >
                <path
                  fillRule="evenodd"
                  d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"
                  clipRule="evenodd"
                />
              </svg>
            </button>

            {showDropdown && (
              <div className="dropdown-menu">
                <div className="dropdown-header">
                  <p className="dropdown-name">{profile.name}</p>
                  <p className="dropdown-email">{profile.email || profile.username}</p>
                </div>
                
                {(profile?.role === 'STUDENT' || profile?.role === 'PARENT') && (
                  <>
                    <div className="dropdown-divider"></div>
                    <div className="enrollment-section">
                      <div className="enrollment-header-small">
                        <h4>내 수강권 정보</h4>
                      </div>
                      <div className="enrollment-list-small">
                        {myEnrollments.length === 0 ? (
                          <div className="no-enrollments-small">
                            등록된 수강권이 없습니다.
                          </div>
                        ) : (
                          myEnrollments.map((enrollment) => (
                            <div key={enrollment.id} className="enrollment-item-small">
                              <div className="enrollment-course-small">
                                {enrollment.course?.courseName || enrollment.courseName || '수업 정보 없음'}
                              </div>
                              <div className="enrollment-student-small">
                                {enrollment.student?.studentName || enrollment.studentName || '학생 정보 없음'}
                              </div>
                              <div className="enrollment-details-small">
                                <span>만료일: {enrollment.endDate}</span>
                                <span className={enrollment.remainingCount < 3 ? 'warning' : ''}>
                                  잔여: {enrollment.remainingCount}회
                                </span>
                              </div>
                            </div>
                          ))
                        )}
                      </div>
                    </div>
                  </>
                )}
                
                <div className="dropdown-divider"></div>
                <Link to="/mypage" className="dropdown-item">
                  <i className="fas fa-user"></i>
                  마이페이지
                </Link>
                <button
                  className="dropdown-item logout"
                  onClick={async () => {
                    try {
                      await authAPI.logout();
                      localStorage.clear();
                      window.location.href = '/login';
                    } catch (error) {
                      console.error('로그아웃 실패:', error);
                      localStorage.clear();
                      window.location.href = '/login';
                    }
                  }}
                >
                  <i className="fas fa-sign-out-alt"></i>
                  로그아웃
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}

export default Header;
