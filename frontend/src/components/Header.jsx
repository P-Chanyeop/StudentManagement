import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useLocation, Link } from 'react-router-dom';
import { authAPI, enrollmentAPI, notificationAPI } from '../services/api';
import '../styles/Header.css';

function Header() {
  const queryClient = useQueryClient();
  const [showDropdown, setShowDropdown] = useState(false);
  const [showEnrollments, setShowEnrollments] = useState(false);
  const [showNotifications, setShowNotifications] = useState(false);
  const location = useLocation();

  // 사용자 프로필 조회
  const { data: profile } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
    staleTime: 5 * 60 * 1000,
  });

  // 읽지 않은 알림 조회 (관리자만)
  const { data: notifications = [] } = useQuery({
    queryKey: ['adminNotifications'],
    queryFn: async () => {
      const response = await notificationAPI.getAll();
      return response.data;
    },
    enabled: !!profile && profile.role === 'ADMIN',
    refetchInterval: 30000, // 30초마다
    staleTime: 0,
    retry: false,
  });

  const unreadCount = notifications.filter(n => !n.read).length;

  // 브라우저 알림 권한 요청
  useEffect(() => {
    if (profile?.role === 'ADMIN' && 'Notification' in window) {
      if (Notification.permission === 'default') {
        Notification.requestPermission();
      }
    }
  }, [profile]);

  // 새 알림 브라우저 푸시
  useEffect(() => {
    if (profile?.role === 'ADMIN' && unreadCount > 0 && Notification.permission === 'granted') {
      const latest = notifications.find(n => !n.read);
      if (latest) {
        const notification = new Notification(latest.title, {
          body: latest.content,
          icon: '/favicon.ico',
          tag: 'admin-notification-' + latest.id
        });
        notification.onclick = () => { window.focus(); notification.close(); };
        setTimeout(() => notification.close(), 5000);
      }
    }
  }, [unreadCount, profile]);

  // 개별 읽음 처리
  const markAsReadMutation = useMutation({
    mutationFn: (id) => notificationAPI.markAsRead(id),
    onSuccess: () => queryClient.invalidateQueries(['adminNotifications']),
  });

  // 알림 제거 (X 버튼)
  const dismissMutation = useMutation({
    mutationFn: (id) => notificationAPI.dismiss(id),
    onSuccess: () => queryClient.invalidateQueries(['adminNotifications']),
  });

  const { data: myEnrollments = [] } = useQuery({
    queryKey: ['myEnrollments'],
    queryFn: async () => {
      const response = await enrollmentAPI.getMyEnrollments();
      return response.data;
    },
    enabled: profile && (profile.role === 'STUDENT' || profile.role === 'PARENT'),
    staleTime: 2 * 60 * 1000,
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

  const isParent = profile?.role === 'PARENT' || 
                   profile?.role === 'ROLE_PARENT' ||
                   profile?.authorities?.some(auth => auth.authority === 'ROLE_PARENT') ||
                   profile?.roles?.includes('PARENT');

  const getInitial = () => {
    const displayName = isParent ? profile.nickname : profile.name;
    return displayName ? displayName.charAt(0) : profile.username.charAt(0);
  };

  const isActive = (path) => {
    return location.pathname === path;
  };

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
        { path: '/parent-reservation', label: '수업 예약', icon: 'fa-calendar-plus' },
        { path: '/reservations', label: '예약 내역', icon: 'fa-calendar-alt' },
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
        <Link to="/" className="logo">
        </Link>

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

        <div className="user-section">
          {/* 알림 아이콘 (관리자만) */}
          {profile?.role === 'ADMIN' && (
            <div className="notification-menu">
              <button
                className="notification-button"
                onClick={() => {
                  setShowNotifications(!showNotifications);
                  setShowDropdown(false);
                }}
              >
                <i className="fas fa-bell"></i>
                {unreadCount > 0 && (
                  <span className="notification-badge">{unreadCount}</span>
                )}
              </button>

              {showNotifications && (
                <div className="notification-dropdown">
                  <div className="notification-header">
                    <h4>알림</h4>
                  </div>
                  <div className="notification-list">
                    {notifications.length === 0 ? (
                      <div className="no-notifications">
                        새로운 알림이 없습니다.
                      </div>
                    ) : (
                      notifications.map((noti) => (
                        <div
                          key={noti.id}
                          className={`notification-item ${noti.read ? 'notification-read' : ''}`}
                          onClick={() => !noti.read && markAsReadMutation.mutate(noti.id)}
                          style={{ cursor: noti.read ? 'default' : 'pointer' }}
                        >
                          <div className="notification-icon">
                            <i className={`fas ${noti.type === 'CONSULTATION' ? 'fa-comments' : 'fa-calendar-plus'}`}></i>
                          </div>
                          <div className="notification-content">
                            <div className="notification-title">{noti.title}</div>
                            <div className="notification-text">{noti.content}</div>
                            <div className="notification-time">
                              {new Date(noti.createdAt).toLocaleString()}
                            </div>
                          </div>
                          <button
                            className="notification-dismiss"
                            onClick={(e) => {
                              e.stopPropagation();
                              dismissMutation.mutate(noti.id);
                            }}
                            title="삭제"
                          >
                            <i className="fas fa-times"></i>
                          </button>
                        </div>
                      ))
                    )}
                  </div>
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
                <span className="user-name">{isParent ? profile.nickname : profile.name}</span>
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
                  <p className="dropdown-name">{isParent ? profile.nickname : profile.name}</p>
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
