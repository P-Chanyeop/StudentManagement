import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import LoadingSpinner from '../components/LoadingSpinner';
import { mypageAPI } from '../services/api';
import '../styles/MyPage.css';

function MyPage() {
  const [activeTab, setActiveTab] = useState('overview');

  // 마이페이지 데이터 조회
  const { data: myPageData, isLoading } = useQuery({
    queryKey: ['mypage'],
    queryFn: async () => {
      const response = await mypageAPI.getMyPage();
      return response.data;
    },
  });

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (!myPageData) {
    return (
      <div className="empty-state">
        <i className="fas fa-exclamation-triangle"></i>
        <p>데이터를 불러올 수 없습니다</p>
      </div>
    );
  }

  const { studentInfo, activeEnrollments, recentAttendances, upcomingReservations,
          upcomingLevelTests, recentMessages, recentConsultations, stats } = myPageData;

  const formatDate = (date) => {
    if (!date) return '-';
    return new Date(date).toLocaleDateString('ko-KR');
  };

  const formatDateTime = (datetime) => {
    if (!datetime) return '-';
    const date = new Date(datetime);
    return `${date.toLocaleDateString('ko-KR')} ${date.toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
    })}`;
  };

  const getStatusBadge = (status) => {
    const statusMap = {
      PRESENT: { label: '출석', className: 'present' },
      LATE: { label: '지각', className: 'late' },
      ABSENT: { label: '결석', className: 'absent' },
      EXCUSED: { label: '사유결석', className: 'excused' },
      CONFIRMED: { label: '예약확정', className: 'confirmed' },
      PENDING: { label: '대기중', className: 'pending' },
      CANCELLED: { label: '취소됨', className: 'cancelled' },
    };
    const info = statusMap[status] || { label: status, className: '' };
    return <span className={`status-badge ${info.className}`}>{info.label}</span>;
  };

  return (
    <div className="page-wrapper">
      <div className="page-header">
        <div className="page-header-content">
          <div className="page-title-section">
            <h1 className="page-title">
              <i className="fas fa-user"></i>
              마이페이지
            </h1>
            <p className="page-subtitle">
              {studentInfo?.studentName}님의 학습 현황을 확인하세요
            </p>
          </div>
        </div>
      </div>

      <div className="page-content">
        <div className="profile-section">
          <div className="profile-card">
            <div className="profile-avatar">
              {studentInfo?.studentName?.charAt(0) || 'U'}
            </div>
            <div className="profile-info">
              <h2>{studentInfo?.studentName || '사용자'}</h2>
              <p className="profile-details">
                {studentInfo?.school} {studentInfo?.grade} | 레벨: {studentInfo?.englishLevel || '-'}
              </p>
            </div>
          </div>
        </div>

        <div className="stats-section">
          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-icon">
                <i className="fas fa-graduation-cap"></i>
              </div>
              <div className="stat-content">
                <div className="stat-value">{stats?.activeEnrollmentCount || 0}</div>
                <div className="stat-label">활성 수강권</div>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-icon">
                <i className="fas fa-calendar-check"></i>
              </div>
              <div className="stat-content">
                <div className="stat-value">{stats?.attendanceRate || 0}%</div>
                <div className="stat-label">출석률</div>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-icon">
                <i className="fas fa-clock"></i>
              </div>
              <div className="stat-content">
                <div className="stat-value">{stats?.upcomingReservationCount || 0}</div>
                <div className="stat-label">예정된 수업</div>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-icon">
                <i className="fas fa-comments"></i>
              </div>
              <div className="stat-content">
                <div className="stat-value">{stats?.consultationCount || 0}</div>
                <div className="stat-label">상담 이력</div>
              </div>
            </div>
          </div>
        </div>

        <div className="tab-section">
          <div className="tab-navigation">
            <button
              className={`tab-button ${activeTab === 'overview' ? 'active' : ''}`}
              onClick={() => setActiveTab('overview')}
            >
              <i className="fas fa-chart-pie"></i>
              개요
            </button>
            <button
              className={`tab-button ${activeTab === 'enrollments' ? 'active' : ''}`}
              onClick={() => setActiveTab('enrollments')}
            >
              <i className="fas fa-receipt"></i>
              수강권
            </button>
            <button
              className={`tab-button ${activeTab === 'attendance' ? 'active' : ''}`}
              onClick={() => setActiveTab('attendance')}
            >
              <i className="fas fa-calendar-check"></i>
              출석 기록
            </button>
            <button
              className={`tab-button ${activeTab === 'reservations' ? 'active' : ''}`}
              onClick={() => setActiveTab('reservations')}
            >
              <i className="fas fa-clock"></i>
              예약 내역
            </button>
            <button
              className={`tab-button ${activeTab === 'messages' ? 'active' : ''}`}
              onClick={() => setActiveTab('messages')}
            >
              <i className="fas fa-envelope"></i>
              받은 메시지
            </button>
          </div>
        </div>

        <div className="tab-content">
          {activeTab === 'overview' && (
            <div className="overview-section">
              <div className="content-grid">
                {/* 활성 수강권 */}
                <div className="content-card">
                  <div className="card-header">
                    <h3 className="card-title">
                      <i className="fas fa-graduation-cap"></i>
                      활성 수강권
                    </h3>
                    <span className="card-count">{activeEnrollments?.length || 0}</span>
                  </div>
                  <div className="card-body">
                    {activeEnrollments && activeEnrollments.length > 0 ? (
                      <div className="item-list">
                        {activeEnrollments.map((enrollment) => (
                          <div key={enrollment.id} className="item-card">
                            <div className="item-header">
                              <h4 className="item-title">{enrollment.courseName}</h4>
                              <div className="item-meta">
                                {formatDate(enrollment.startDate)} ~ {formatDate(enrollment.endDate)}
                              </div>
                            </div>
                            <div className="progress-section">
                              <div className="progress-info">
                                <span className="progress-text">
                                  남은 횟수 <strong>{enrollment.remainingCount}</strong> / {enrollment.totalCount}
                                </span>
                                <span className="progress-percent">
                                  {Math.round((enrollment.remainingCount / enrollment.totalCount) * 100)}%
                                </span>
                              </div>
                              <div className="progress-bar">
                                <div
                                  className="progress-fill"
                                  style={{
                                    width: `${(enrollment.remainingCount / enrollment.totalCount) * 100}%`,
                                  }}
                                ></div>
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="empty-state-small">
                        <i className="fas fa-graduation-cap"></i>
                        <p>활성 수강권이 없습니다</p>
                      </div>
                    )}
                  </div>
                </div>

                {/* 예정된 예약 */}
                <div className="content-card">
                  <div className="card-header">
                    <h3 className="card-title">
                      <i className="fas fa-calendar-alt"></i>
                      예정된 예약
                    </h3>
                    <span className="card-count">{upcomingReservations?.length || 0}</span>
                  </div>
                  <div className="card-body">
                    {upcomingReservations && upcomingReservations.length > 0 ? (
                      <div className="item-list">
                        {upcomingReservations.slice(0, 5).map((reservation) => (
                          <div key={reservation.id} className="item-card">
                            <div className="item-header">
                              <h4 className="item-title">{reservation.courseName}</h4>
                              <div className="item-status">
                                {getStatusBadge(reservation.status)}
                              </div>
                            </div>
                            <div className="item-meta">
                              {formatDate(reservation.scheduleDate)} {reservation.scheduleTime}
                            </div>
                          </div>
                        ))}
                      </div>
                    ) : (
                      <div className="empty-state-small">
                        <i className="fas fa-calendar-alt"></i>
                        <p>예정된 예약이 없습니다</p>
                      </div>
                    )}
                  </div>
                </div>

                {/* 레벨테스트 일정 */}
                {upcomingLevelTests && upcomingLevelTests.length > 0 && (
                  <div className="content-card">
                    <div className="card-header">
                      <h3 className="card-title">
                        <i className="fas fa-clipboard-check"></i>
                        예정된 레벨테스트
                      </h3>
                      <span className="card-count">{upcomingLevelTests.length}</span>
                    </div>
                    <div className="card-body">
                      <div className="item-list">
                        {upcomingLevelTests.map((test) => (
                          <div key={test.id} className="item-card">
                            <div className="item-header">
                              <h4 className="item-title">레벨테스트</h4>
                              <div className="item-status">
                                <span className="status-badge pending">{test.status}</span>
                              </div>
                            </div>
                            <div className="item-meta">
                              {formatDate(test.testDate)} {test.testTime}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                )}

                {/* 최근 상담 이력 */}
                {recentConsultations && recentConsultations.length > 0 && (
                  <div className="content-card">
                    <div className="card-header">
                      <h3 className="card-title">
                        <i className="fas fa-comments"></i>
                        최근 상담 이력
                      </h3>
                      <span className="card-count">{recentConsultations.length}</span>
                    </div>
                    <div className="card-body">
                      <div className="item-list">
                        {recentConsultations.slice(0, 3).map((consultation) => (
                          <div key={consultation.id} className="item-card">
                            <div className="item-header">
                              <h4 className="item-title">{consultation.title}</h4>
                              <div className="item-type">
                                <span className="type-badge">{consultation.consultationType}</span>
                              </div>
                            </div>
                            <div className="item-meta">
                              {formatDate(consultation.consultationDate)}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}

          {activeTab === 'enrollments' && (
            <div className="enrollments-section">
              <div className="section-header">
                <h2 className="section-title">
                  <i className="fas fa-receipt"></i>
                  수강권 상세 정보
                </h2>
              </div>
              {activeEnrollments && activeEnrollments.length > 0 ? (
                <div className="enrollment-grid">
                  {activeEnrollments.map((enrollment) => (
                    <div key={enrollment.id} className="enrollment-card">
                      <div className="enrollment-header">
                        <h3 className="enrollment-title">{enrollment.courseName}</h3>
                        <div className="enrollment-type">
                          <span className="type-badge">
                            {enrollment.type === 'PERIOD_BASED' ? '기간제' : '횟수제'}
                          </span>
                        </div>
                      </div>
                      <div className="enrollment-details">
                        <div className="detail-item">
                          <span className="detail-label">수강 기간</span>
                          <span className="detail-value">
                            {formatDate(enrollment.startDate)} ~ {formatDate(enrollment.endDate)}
                          </span>
                        </div>
                        <div className="detail-item">
                          <span className="detail-label">남은 횟수</span>
                          <span className="detail-value">
                            <strong>{enrollment.remainingCount}</strong> / {enrollment.totalCount}
                          </span>
                        </div>
                        <div className="progress-section">
                          <div className="progress-bar">
                            <div
                              className="progress-fill"
                              style={{
                                width: `${(enrollment.remainingCount / enrollment.totalCount) * 100}%`,
                              }}
                            ></div>
                          </div>
                          <span className="progress-percent">
                            {Math.round((enrollment.remainingCount / enrollment.totalCount) * 100)}%
                          </span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="empty-state">
                  <i className="fas fa-receipt"></i>
                  <p>수강권이 없습니다</p>
                </div>
              )}
            </div>
          )}

          {activeTab === 'attendance' && (
            <div className="attendance-section">
              <div className="section-header">
                <h2 className="section-title">
                  <i className="fas fa-calendar-check"></i>
                  최근 출석 기록
                </h2>
              </div>
              {recentAttendances && recentAttendances.length > 0 ? (
                <div className="attendance-list">
                  {recentAttendances.map((attendance) => (
                    <div key={attendance.id} className="attendance-item">
                      <div className="attendance-info">
                        <div className="attendance-course">
                          <h4>{attendance.courseName}</h4>
                          <span className="attendance-date">{formatDate(attendance.attendanceDate)}</span>
                        </div>
                        <div className="attendance-status">
                          {getStatusBadge(attendance.status)}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="empty-state">
                  <i className="fas fa-calendar-check"></i>
                  <p>출석 기록이 없습니다</p>
                </div>
              )}
            </div>
          )}

          {activeTab === 'reservations' && (
            <div className="reservations-section">
              <div className="section-header">
                <h2 className="section-title">
                  <i className="fas fa-clock"></i>
                  예약 내역
                </h2>
              </div>
              {upcomingReservations && upcomingReservations.length > 0 ? (
                <div className="reservation-list">
                  {upcomingReservations.map((reservation) => (
                    <div key={reservation.id} className="reservation-item">
                      <div className="reservation-info">
                        <div className="reservation-course">
                          <h4>{reservation.courseName}</h4>
                          <span className="reservation-datetime">
                            {formatDate(reservation.scheduleDate)} {reservation.scheduleTime}
                          </span>
                        </div>
                        <div className="reservation-status">
                          {getStatusBadge(reservation.status)}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="empty-state">
                  <i className="fas fa-clock"></i>
                  <p>예약 내역이 없습니다</p>
                </div>
              )}
            </div>
          )}

          {activeTab === 'messages' && (
            <div className="messages-section">
              <div className="section-header">
                <h2 className="section-title">
                  <i className="fas fa-envelope"></i>
                  받은 메시지
                </h2>
              </div>
              {recentMessages && recentMessages.length > 0 ? (
                <div className="message-list">
                  {recentMessages.map((message) => (
                    <div key={message.id} className="message-item">
                      <div className="message-header">
                        <div className="message-type">
                          <i className="fas fa-envelope"></i>
                          <span>{message.messageType}</span>
                        </div>
                        <div className="message-time">
                          {formatDateTime(message.sentAt)}
                        </div>
                      </div>
                      <div className="message-content">
                        {message.content}
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="empty-state">
                  <i className="fas fa-envelope"></i>
                  <p>받은 메시지가 없습니다</p>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default MyPage;
