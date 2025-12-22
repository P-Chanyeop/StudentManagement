import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import Layout from '../components/Layout';
import LoadingSpinner from '../components/LoadingSpinner';
import { mypageAPI } from '../services/api';
import '../styles/MyPage.css';

function MyPage() {
  const [activeTab, setActiveTab] = useState('overview'); // overview, enrollments, attendance, reservations, messages

  // 마이페이지 데이터 조회
  const { data: myPageData, isLoading } = useQuery({
    queryKey: ['mypage'],
    queryFn: async () => {
      const response = await mypageAPI.getMyPage();
      return response.data;
    },
  });

  if (isLoading) {
    return (
      <Layout>
        <div className="loading-container">
          <div className="loading-spinner"></div>
          <LoadingSpinner />
        </div>
      </Layout>
    );
  }

  if (!myPageData) {
    return (
      <Layout>
        <div className="error-container">
          <p>데이터를 불러올 수 없습니다</p>
        </div>
      </Layout>
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
    <Layout>
      <div className="page-wrapper">
        {/* 헤더 */}
        <div className="page-header">
          <div className="page-header-content mypage-header">
            <div className="student-profile">
              <div className="profile-avatar">
                {studentInfo.studentName.charAt(0)}
              </div>
              <div className="profile-info">
                <h1 className="page-title">
                  <i className="fas fa-user"></i>
                  {studentInfo.studentName}님
                </h1>
                <p className="page-subtitle student-details">
                  {studentInfo.school} {studentInfo.grade} | 레벨: {studentInfo.englishLevel || '-'}
                </p>
              </div>
            </div>

            {/* 통계 요약 */}
            <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-icon">🎓</div>
              <div className="stat-content">
                <div className="stat-value">{stats.activeEnrollmentCount}</div>
                <div className="stat-label">활성 수강권</div>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-icon"><i className="fas fa-calendar-alt"></i></div>
              <div className="stat-content">
                <div className="stat-value">{stats.upcomingReservationCount}</div>
                <div className="stat-label">예정 예약</div>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-icon"><i className="fas fa-check-circle"></i></div>
              <div className="stat-content">
                <div className="stat-value">{stats.monthlyAttendanceCount}</div>
                <div className="stat-label">이번 달 출석</div>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-icon"><i className="fas fa-chart-bar"></i></div>
              <div className="stat-content">
                <div className="stat-value">{stats.totalAttendanceCount}</div>
                <div className="stat-label">총 출석</div>
              </div>
            </div>
            </div>
          </div>
        </div>

        <div className="page-content">
          {/* 탭 네비게이션 */}
          <div className="tab-navigation">
          <button
            className={`tab-button ${activeTab === 'overview' ? 'active' : ''}`}
            onClick={() => setActiveTab('overview')}
          >
            개요
          </button>
          <button
            className={`tab-button ${activeTab === 'enrollments' ? 'active' : ''}`}
            onClick={() => setActiveTab('enrollments')}
          >
            수강권
          </button>
          <button
            className={`tab-button ${activeTab === 'attendance' ? 'active' : ''}`}
            onClick={() => setActiveTab('attendance')}
          >
            출석 기록
          </button>
          <button
            className={`tab-button ${activeTab === 'reservations' ? 'active' : ''}`}
            onClick={() => setActiveTab('reservations')}
          >
            예약 내역
          </button>
          <button
            className={`tab-button ${activeTab === 'messages' ? 'active' : ''}`}
            onClick={() => setActiveTab('messages')}
          >
            받은 메시지
          </button>
          </div>

          {/* 탭 컨텐츠 */}
          <div className="tab-content">
          {/* 개요 탭 */}
          {activeTab === 'overview' && (
            <div className="overview-tab">
              <div className="overview-grid">
                {/* 수강권 정보 */}
                <div className="info-section">
                  <h2 className="section-title"><i className="fas fa-book"></i> 활성 수강권</h2>
                  {activeEnrollments && activeEnrollments.length > 0 ? (
                    <div className="enrollment-cards">
                      {activeEnrollments.map((enrollment) => (
                        <div key={enrollment.id} className="enrollment-card">
                          <div className="enrollment-header">
                            <h3>{enrollment.courseName}</h3>
                            <span className="enrollment-type">수강권</span>
                          </div>
                          <div className="enrollment-details">
                            <p>시작일: {formatDate(enrollment.startDate)}</p>
                            <p>종료일: {formatDate(enrollment.endDate)}</p>
                            <p>남은 횟수: <strong>{enrollment.remainingCount}</strong> / {enrollment.totalCount}</p>
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
                    <p className="empty-message">활성 수강권이 없습니다</p>
                  )}
                </div>

                {/* 예정된 예약 */}
                <div className="info-section">
                  <h2 className="section-title">📅 예정된 예약</h2>
                  {upcomingReservations && upcomingReservations.length > 0 ? (
                    <div className="reservation-list">
                      {upcomingReservations.slice(0, 5).map((reservation) => (
                        <div key={reservation.id} className="reservation-item">
                          <div className="reservation-date">
                            {formatDate(reservation.scheduleDate)}
                          </div>
                          <div className="reservation-info">
                            <p className="reservation-time">{reservation.scheduleTime}</p>
                            {getStatusBadge(reservation.status)}
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="empty-message">예정된 예약이 없습니다</p>
                  )}
                </div>

                {/* 레벨테스트 일정 */}
                {upcomingLevelTests && upcomingLevelTests.length > 0 && (
                  <div className="info-section">
                    <h2 className="section-title">📝 예정된 레벨테스트</h2>
                    <div className="leveltest-list">
                      {upcomingLevelTests.map((test) => (
                        <div key={test.id} className="leveltest-item">
                          <div className="leveltest-date">
                            {formatDate(test.testDate)} {test.testTime}
                          </div>
                          <div className="leveltest-info">
                            <p>현재 레벨: {test.currentLevel || '-'}</p>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* 최근 상담 기록 */}
                {recentConsultations && recentConsultations.length > 0 && (
                  <div className="info-section">
                    <h2 className="section-title">💬 최근 상담 기록</h2>
                    <div className="consultation-list">
                      {recentConsultations.map((consultation) => (
                        <div key={consultation.id} className="consultation-item">
                          <div className="consultation-header">
                            <h4>{consultation.title}</h4>
                            <span className="consultation-date">
                              {formatDate(consultation.consultationDate)}
                            </span>
                          </div>
                          <p className="consultation-content">{consultation.content}</p>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* 수강권 탭 */}
          {activeTab === 'enrollments' && (
            <div className="enrollments-tab">
              <h2 className="tab-title">📚 수강권 상세 정보</h2>
              {activeEnrollments && activeEnrollments.length > 0 ? (
                <div className="table-wrapper">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>수업명</th>
                        <th>시작일</th>
                        <th>종료일</th>
                        <th>총 횟수</th>
                        <th>남은 횟수</th>
                        <th>메모</th>
                      </tr>
                    </thead>
                    <tbody>
                      {activeEnrollments.map((enrollment) => (
                        <tr key={enrollment.id}>
                          <td>{enrollment.courseName}</td>
                          <td>{formatDate(enrollment.startDate)}</td>
                          <td>{formatDate(enrollment.endDate)}</td>
                          <td>{enrollment.totalCount}</td>
                          <td><strong>{enrollment.remainingCount}</strong></td>
                          <td>{enrollment.memo || '-'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="empty-message">활성 수강권이 없습니다</p>
              )}
            </div>
          )}

          {/* 출석 기록 탭 */}
          {activeTab === 'attendance' && (
            <div className="attendance-tab">
              <h2 className="tab-title">✅ 출석 기록</h2>
              {recentAttendances && recentAttendances.length > 0 ? (
                <div className="table-wrapper">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>등원 시간</th>
                        <th>하원 시간</th>
                        <th>예상 하원</th>
                        <th>상태</th>
                        <th>비고</th>
                      </tr>
                    </thead>
                    <tbody>
                      {recentAttendances.map((attendance) => (
                        <tr key={attendance.id}>
                          <td>{formatDateTime(attendance.checkInTime)}</td>
                          <td>{formatDateTime(attendance.checkOutTime)}</td>
                          <td>{attendance.expectedLeaveTime || '-'}</td>
                          <td>{getStatusBadge(attendance.status)}</td>
                          <td>{attendance.memo || attendance.reason || '-'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="empty-message">출석 기록이 없습니다</p>
              )}
            </div>
          )}

          {/* 예약 내역 탭 */}
          {activeTab === 'reservations' && (
            <div className="reservations-tab">
              <h2 className="tab-title">📅 예약 내역</h2>
              {upcomingReservations && upcomingReservations.length > 0 ? (
                <div className="table-wrapper">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>날짜</th>
                        <th>시간</th>
                        <th>상태</th>
                        <th>메모</th>
                      </tr>
                    </thead>
                    <tbody>
                      {upcomingReservations.map((reservation) => (
                        <tr key={reservation.id}>
                          <td>{formatDate(reservation.scheduleDate)}</td>
                          <td>{reservation.scheduleTime}</td>
                          <td>{getStatusBadge(reservation.status)}</td>
                          <td>{reservation.memo || '-'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="empty-message">예정된 예약이 없습니다</p>
              )}
            </div>
          )}

          {/* 받은 메시지 탭 */}
          {activeTab === 'messages' && (
            <div className="messages-tab">
              <h2 className="tab-title">💌 받은 메시지</h2>
              {recentMessages && recentMessages.length > 0 ? (
                <div className="message-list">
                  {recentMessages.map((message) => (
                    <div key={message.id} className="message-item">
                      <div className="message-header">
                        <span className="message-type">{message.messageType}</span>
                        <span className="message-date">{formatDateTime(message.sentAt)}</span>
                      </div>
                      <p className="message-content">{message.content}</p>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="empty-message">받은 메시지가 없습니다</p>
              )}
            </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  );
}

export default MyPage;
