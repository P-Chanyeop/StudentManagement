import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useLocation, Link } from 'react-router-dom';
import { authAPI, enrollmentAPI, reservationAPI } from '../services/api';
import '../styles/Header.css';

function Header() {
  const [showDropdown, setShowDropdown] = useState(false);
  const [showEnrollments, setShowEnrollments] = useState(false);
  const [showNotifications, setShowNotifications] = useState(false);
  const [lastNotificationCheck, setLastNotificationCheck] = useState(
    localStorage.getItem('lastNotificationCheck') || new Date().toISOString()
  );
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

  // 새로운 예약 조회 (관리자만)
  const { data: newReservations = [] } = useQuery({
    queryKey: ['newReservations', lastNotificationCheck],
    queryFn: async () => {
      try {
        console.log('알림 체크 시간:', lastNotificationCheck);
        const response = await reservationAPI.getNewReservations(lastNotificationCheck);
        console.log('새 예약 응답:', response.data);
        return response.data;
      } catch (error) {
        console.error('새 예약 조회 실패:', error);
        return [];
      }
    },
    enabled: profile && profile.role === 'ADMIN',
    refetchInterval: 30000, // 30초마다 새로고침
    staleTime: 0,
    retry: false, // 에러 시 재시도 하지 않음
  });

  // 브라우저 알림 권한 요청
  useEffect(() => {
    if (profile?.role === 'ADMIN' && 'Notification' in window) {
      if (Notification.permission === 'default') {
        Notification.requestPermission();
      }
    }
  }, [profile]);

  // 새 예약 알림
  useEffect(() => {
    if (profile?.role === 'ADMIN' && newReservations.length > 0) {
      // 브라우저 푸시 알림
      if (Notification.permission === 'granted') {
        const notification = new Notification('새로운 예약이 있습니다!', {
          body: `${newReservations.length}건의 새로운 예약이 들어왔습니다.`,
          icon: '/favicon.ico',
          tag: 'new-reservation'
        });

        notification.onclick = () => {
          window.focus();
          window.location.href = '/reservations';
          notification.close();
        };

        // 5초 후 자동 닫기
        setTimeout(() => notification.close(), 5000);
      }
    }
  }, [newReservations, profile]);
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
      if (!event.target.closest('.user-menu') && !event.target.closest('.notification-menu')) {
        setShowDropdown(false);
        setShowNotifications(false);
      }
    };

    if (showDropdown || showNotifications) {
      document.addEventListener('click', handleClickOutside);
    }

    return () => {
      document.removeEventListener('click', handleClickOutside);
    };
  }, [showDropdown, showNotifications]);

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
          {/* 알림 아이콘 (관리자만) */}
          {profile?.role === 'ADMIN' && (
            <div className="notification-menu">
              <button
                className="notification-button"
                onClick={() => {
                  setShowNotifications(!showNotifications);
                  setShowDropdown(false);
                  // 알림 확인 시간 업데이트
                  const now = new Date().toISOString();
                  setLastNotificationCheck(now);
                  localStorage.setItem('lastNotificationCheck', now);
                }}
              >
                <i className="fas fa-bell"></i>
                {newReservations.length > 0 && (
                  <span className="notification-badge">{newReservations.length}</span>
                )}
              </button>

              {showNotifications && (
                <div className="notification-dropdown">
                  <div className="notification-header">
                    <h4>새로운 알림</h4>
                  </div>
                  <div className="notification-list">
                    {newReservations.length === 0 ? (
                      <div className="no-notifications">
                        새로운 알림이 없습니다.
                      </div>
                    ) : (
                      newReservations.map((reservation) => (
                        <div key={reservation.id} className="notification-item">
                          <div className="notification-icon">
                            <i className="fas fa-calendar-plus"></i>
                          </div>
                          <div className="notification-content">
                            <div className="notification-title">새로운 예약</div>
                            <div className="notification-text">
                              {reservation.studentName}님이 {reservation.courseName} 수업을 예약했습니다.
                            </div>
                            <div className="notification-time">
                              {new Date(reservation.createdAt).toLocaleString()}
                            </div>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                  {newReservations.length > 0 && (
                    <div className="notification-footer">
                      <Link to="/reservations" onClick={() => setShowNotifications(false)}>
                        모든 예약 보기
                      </Link>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
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
