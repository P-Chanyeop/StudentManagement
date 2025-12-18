import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { authAPI } from '../services/api';
import '../styles/Header.css';

function Header() {
  const [showDropdown, setShowDropdown] = useState(false);

  // 사용자 프로필 조회
  const { data: profile } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
    staleTime: 5 * 60 * 1000, // 5분 동안 캐시 유지
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

  const formatDate = (date) => {
    if (!date) return '';
    return new Date(date).toLocaleDateString('ko-KR', {
      month: 'numeric',
      day: 'numeric',
    });
  };

  return (
    <div className="header">
      <div className="header-content">
        {/* 수강권 정보 (학생인 경우만) */}
        {profile.studentId && profile.enrollmentSummaries && profile.enrollmentSummaries.length > 0 && (
          <div className="enrollments-summary">
            {profile.enrollmentSummaries.slice(0, 2).map((enrollment) => (
              <div
                key={enrollment.enrollmentId}
                className={`enrollment-badge ${enrollment.isExpiring ? 'expiring' : ''}`}
              >
                <span className="course-name">{enrollment.courseName}</span>
                {enrollment.enrollmentType === 'PERIOD' ? (
                  <span className="enrollment-info">
                    {enrollment.daysRemaining >= 0 ? (
                      <>D-{enrollment.daysRemaining}</>
                    ) : (
                      <>만료됨</>
                    )}
                    {' '}({formatDate(enrollment.endDate)})
                  </span>
                ) : (
                  <span className="enrollment-info">
                    {enrollment.remainingCount}/{enrollment.totalCount}회
                  </span>
                )}
              </div>
            ))}
            {profile.enrollmentSummaries.length > 2 && (
              <span className="more-enrollments">
                +{profile.enrollmentSummaries.length - 2}
              </span>
            )}
          </div>
        )}

        {/* 사용자 메뉴 */}
        <div className="user-menu">
          <button
            className="user-button"
            onClick={() => setShowDropdown(!showDropdown)}
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
              <div className="dropdown-divider"></div>
              <a href="/mypage" className="dropdown-item">
                <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                  <path
                    fillRule="evenodd"
                    d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z"
                    clipRule="evenodd"
                  />
                </svg>
                마이페이지
              </a>
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
                <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                  <path
                    fillRule="evenodd"
                    d="M3 3a1 1 0 00-1 1v12a1 1 0 001 1h12a1 1 0 001-1V4a1 1 0 00-1-1H3zm11 4.414l-4.293 4.293a1 1 0 01-1.414 0L6.586 10 5.172 11.414l2.707 2.707a3 3 0 004.242 0L15.828 10 14.414 8.586z"
                    clipRule="evenodd"
                  />
                </svg>
                로그아웃
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default Header;
