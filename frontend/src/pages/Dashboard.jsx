import { useQuery } from '@tanstack/react-query';
import { useState, useEffect } from 'react';
import {
  studentAPI,
  attendanceAPI,
  reservationAPI,
  enrollmentAPI,
  scheduleAPI,
  authAPI,
  dashboardAPI
} from '../services/api';
import { getTodayString } from '../utils/dateUtils';
import { holidayService } from '../services/holidayService';
import '../styles/Dashboard.css';

function Dashboard() {
  // 수강권 상세 모달 상태
  const [selectedEnrollment, setSelectedEnrollment] = useState(null);
  const [showEnrollmentModal, setShowEnrollmentModal] = useState(false);
  
  // 더보기 상태
  const [showAllSchedules, setShowAllSchedules] = useState(false);
  const [showAllEnrollments, setShowAllEnrollments] = useState(false);
  const [enrollmentFilter, setEnrollmentFilter] = useState('all'); // 'all', 'expiring', 'lowCount'
  const [showAllAttendance, setShowAllAttendance] = useState(false);
  const [showAllReservations, setShowAllReservations] = useState(false);

  // 공휴일 데이터
  const [holidays, setHolidays] = useState([]);
  useEffect(() => {
    const y = new Date().getFullYear();
    Promise.all([holidayService.getHolidays(y), holidayService.getHolidays(y + 1)])
      .then(([h1, h2]) => setHolidays([...h1, ...h2]))
      .catch(() => {});
  }, []);

  const getBusinessDaysLeft = (startDate, endDate) => {
    return holidayService.calculateRemainingBusinessDaysWithCache(startDate, endDate, holidays);
  };

  // 오늘 날짜
  const today = getTodayString();

  // 사용자 프로필 조회
  const { data: profile } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
  });

  // 모든 가능한 role 형태 확인
  const isParent = profile?.role === 'PARENT' || 
                   profile?.role === 'ROLE_PARENT' ||
                   profile?.authorities?.some(auth => auth.authority === 'ROLE_PARENT') ||
                   profile?.roles?.includes('PARENT');
  
  // 디버깅용 로그
  console.log('=== DASHBOARD DEBUG ===');
  console.log('Profile:', profile);
  console.log('Profile nickname:', profile?.nickname);
  console.log('Profile name:', profile?.name);
  console.log('Profile keys:', Object.keys(profile || {}));
  console.log('Profile role:', profile?.role);
  console.log('Profile authorities:', profile?.authorities);
  console.log('Profile roles:', profile?.roles);
  console.log('Is Parent:', isParent);
  console.log('=======================');

  // 대시보드 통계 조회 (관리자/선생님만)
  const { data: dashboardStats } = useQuery({
    queryKey: ['dashboardStats'],
    queryFn: async () => {
      const response = await dashboardAPI.getStats();
      return response.data;
    },
    enabled: !isParent, // 학부모가 아닐 때만 조회
  });

  // 오늘 출석 현황
  const { data: todayAttendance = [] } = useQuery({
    queryKey: ['todayAttendance', today],
    queryFn: async () => {
      const response = await attendanceAPI.getByDate(today);
      return response.data;
    },
  });

  // 오늘 예약 현황
  const { data: todayReservations = [] } = useQuery({
    queryKey: ['todayReservations', today],
    queryFn: async () => {
      const response = await reservationAPI.getByDate(today);
      return response.data;
    },
  });

  // 수강권 정보 (역할별 분기)
  const { data: enrollments = [] } = useQuery({
    queryKey: ['enrollments', isParent],
    queryFn: async () => {
      if (isParent && profile?.studentId) {
        // 학부모: 본인 자녀의 모든 수강권
        const response = await enrollmentAPI.getByStudent(profile.studentId);
        return response.data;
      } else if (!isParent) {
        // 관리자/선생님: 전체 수강권 통계용 데이터
        const response = await enrollmentAPI.getAll();
        return response.data;
      }
      return [];
    },
    enabled: !!profile,
  });

  // 오늘의 수업 목록 (관리자/선생님: 전체, 학부모: 본인 자녀만)
  const { data: todaySchedules = [] } = useQuery({
    queryKey: ['todaySchedules', today, isParent],
    queryFn: async () => {
      if (isParent && profile?.studentId) {
        // 학부모: 본인 자녀가 예약한 오늘 수업만
        const response = await reservationAPI.getByStudent(profile.studentId);
        const todayReservations = response.data.filter(reservation => 
          reservation.scheduleDate === today
        );
        return todayReservations.map(reservation => ({
          id: reservation.scheduleId,
          courseName: reservation.courseName,
          startTime: reservation.startTime,
          endTime: reservation.endTime,
          currentStudents: 1,
          maxStudents: 1,
          isReservation: true
        }));
      } else if (!isParent) {
        // 관리자/선생님: 모든 오늘 수업
        const response = await scheduleAPI.getByDate(today);
        return response.data;
      }
      return [];
    },
    enabled: !!profile,
  });

  // 수강권 클릭 핸들러
  const handleEnrollmentClick = (enrollment) => {
    setSelectedEnrollment(enrollment);
    setShowEnrollmentModal(true);
  };

  // 모달 닫기
  const closeModal = () => {
    setShowEnrollmentModal(false);
    setSelectedEnrollment(null);
  };

  // 시간 포맷팅
  const formatTime = (timeString) => {
    if (!timeString) return '';
    if (timeString.includes('T')) return timeString.split('T')[1].substring(0, 5);
    return timeString.substring(0, 5);
  };

  // 대시보드 통계에서 값 추출 (기본값 설정)
  const totalStudents = dashboardStats?.totalStudents || 0;
  const todaySchedulesCount = dashboardStats?.todaySchedules || 0;
  const todayAttendanceCount = dashboardStats?.todayAttendance || 0;
  const attendanceRate = dashboardStats?.attendanceRate || 0;
  
  // 수강권 통계 계산
  const getEnrollmentStats = () => {
    if (isParent) {
      return {
        count: enrollments.length,
        label: '내 자녀 수강권'
      };
    } else {
      // 관리자/선생님: 전체 수강권 통계
      const activeEnrollments = enrollments.filter(e => e.isActive);
      const expiringEnrollments = enrollments.filter(e => {
        if (!e.isActive || !e.endDate) return false;
        const daysLeft = getBusinessDaysLeft(e.startDate, e.endDate);
        return daysLeft <= 7 && daysLeft >= 0;
      });
      const lowCountEnrollments = enrollments.filter(e => 
        e.isActive && e.type === 'COUNT_BASED' && e.remainingCount <= 5
      );
      
      return {
        total: enrollments.length,
        active: activeEnrollments.length,
        expiring: expiringEnrollments.length,
        lowCount: lowCountEnrollments.length,
        count: activeEnrollments.length,
        label: '활성 수강권'
      };
    }
  };
  
  const enrollmentStats = getEnrollmentStats();

  return (
    <div className="dashboard-wrapper">
      {/* 학부모 전용 간단한 대시보드 */}
      {isParent ? (
        <div className="parent-dashboard">
          <div className="dashboard-header">
            <h1>대시보드</h1>
            <p>자녀의 학습 현황을 확인하세요</p>
          </div>
          
          <div className="parent-content">
            {enrollments.length === 0 ? (
              <div className="empty-state">
                <i className="fas fa-graduation-cap"></i>
                <p>등록된 수강권이 없습니다</p>
              </div>
            ) : (
              enrollments.map((enrollment) => {
                const daysLeft = getBusinessDaysLeft(enrollment.startDate, enrollment.endDate);
                
                return (
                  <div key={enrollment.id} className="student-section">
                    <div className="student-header">
                      <h2>{enrollment.studentName}</h2>
                      <span className={`days-badge ${daysLeft <= 7 ? 'urgent' : ''}`}>
                        {daysLeft > 0 ? `D-${daysLeft}` : '만료'}
                      </span>
                    </div>
                    
                    <div className="info-table">
                      <div className="info-row">
                        <span className="info-label">반</span>
                        <span className="info-value">{enrollment.courseName}</span>
                      </div>
                      <div className="info-row">
                        <span className="info-label">수업 시간</span>
                        <span className="info-value">
                          {enrollment.courseSchedules?.map(schedule => 
                            `${schedule.dayOfWeek} ${schedule.startTime}-${schedule.endTime}`
                          ).join(', ') || '미정'}
                        </span>
                      </div>
                      <div className="info-row">
                        <span className="info-label">잔여 횟수</span>
                        <span className="info-value highlight">
                          {enrollment.enrollmentType === 'COUNT' 
                            ? `${enrollment.remainingCount}회` 
                            : '무제한'}
                        </span>
                      </div>
                      <div className="info-row">
                        <span className="info-label">수강 기간</span>
                        <span className="info-value">
                          {new Date(enrollment.startDate).toLocaleDateString('ko-KR')} ~ {new Date(enrollment.endDate).toLocaleDateString('ko-KR')}
                        </span>
                      </div>
                      <div className="info-row">
                        <span className="info-label">남은 일수</span>
                        <span className={`info-value ${daysLeft <= 7 ? 'urgent' : ''}`}>
                          {daysLeft > 0 ? `${daysLeft}일` : '만료'}
                        </span>
                      </div>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      ) : (
        <>
          {/* 히어로 섹션 */}
          <section className="hero">
            <div className="hero-container">
              <h1>대시보드</h1>
              <p>오늘도 학원 운영을 효율적으로 관리하세요</p>
            </div>
          </section>

          {/* 메인 컨텐츠 */}
          <div className="dashboard-container">
        {/* 통계 카드 - 관리자/선생님만 */}
        {!isParent && (
          <div className="dash-stat-grid">
          <div className="dash-stat-card">
            <div className="dash-stat-header">
              <div className="dash-stat-icon"><i className="fas fa-users"></i></div>
              <div className="dash-stat-trend">NEW</div>
            </div>
            <div className="dash-stat-body">
              <span className="dash-stat-title">전체 학생</span>
              <span className="dash-stat-number">{totalStudents}<small>명</small></span>
            </div>
            <div className="dash-stat-foot">{profile?.role === 'TEACHER' ? '전체 학생 수' : '등록된 전체 학생 수'}</div>
          </div>

          <div className="dash-stat-card">
            <div className="dash-stat-header">
              <div className="dash-stat-icon lesson"><i className="fas fa-chalkboard-teacher"></i></div>
              <div className="dash-stat-trend">오늘</div>
            </div>
            <div className="dash-stat-body">
              <span className="dash-stat-title">오늘의 수업</span>
              <span className="dash-stat-number">{todaySchedulesCount}<small>개</small></span>
            </div>
            <div className="dash-stat-foot">{profile?.role === 'TEACHER' ? '오늘 담당 수업' : '오늘 예정된 수업'}</div>
          </div>

          <div className="dash-stat-card">
            <div className="dash-stat-header">
              <div className="dash-stat-icon attend"><i className="fas fa-check-circle"></i></div>
              <div className="dash-stat-trend success">{attendanceRate}%</div>
            </div>
            <div className="dash-stat-body">
              <span className="dash-stat-title">오늘 출석</span>
              <span className="dash-stat-number">{todayAttendanceCount}<small>명</small></span>
            </div>
            <div className="dash-stat-foot">출석률 {attendanceRate}%</div>
          </div>

          <div className="dash-stat-card">
            <div className="dash-stat-header">
              <div className="dash-stat-icon enroll"><i className="fas fa-credit-card"></i></div>
              <div className="dash-stat-trend info">통계</div>
            </div>
            <div className="dash-stat-body">
              <span className="dash-stat-title">{enrollmentStats.label}</span>
              <span className="dash-stat-number">{enrollmentStats.count}<small>개</small></span>
            </div>
            <div className="dash-stat-foot">전체 {enrollmentStats.total}개 · 만료임박 {enrollmentStats.expiring}개</div>
          </div>
        </div>
        )}

        {/* 대시보드 그리드 */}
        <div className="dashboard-grid">
          {/* 오늘의 수업 */}
          <div className="dashboard-card">
            <div className="card-header">
              <h2 className="card-title">
                <i className="fas fa-calendar-day"></i>
                오늘의 수업
              </h2>
              <span className="card-badge">{todaySchedulesCount}개</span>
            </div>
            <div className="card-body">
              {todaySchedules.length === 0 ? (
                <div className="empty-state">
                  <i className="fas fa-calendar-times"></i>
                  <p>예정된 수업이 없습니다</p>
                </div>
              ) : (
                <div className="list">
                  {(showAllSchedules ? todaySchedules : todaySchedules.slice(0, 5)).map((schedule) => (
                    <div key={schedule.id} className="list-item">
                      <div className="item-icon">
                        <i className="fas fa-book-open"></i>
                      </div>
                      <div className="item-content">
                        <div className="item-title">{schedule.courseName}</div>
                        <div className="item-subtitle">
                          {formatTime(schedule.startTime)} - {formatTime(schedule.endTime)} · {schedule.teacherName}
                        </div>
                      </div>
                      <div className={`item-badge badge-${schedule.status?.toLowerCase() || 'default'}`}>
                        {schedule.status === 'SCHEDULED' ? '예정' :
                         schedule.status === 'COMPLETED' ? '완료' :
                         schedule.status === 'CANCELLED' ? '취소' : schedule.status || '미정'}
                      </div>
                    </div>
                  ))}
                  {todaySchedules.length > 5 && (
                    <button 
                      type="button"
                      className="show-more-btn"
                      onClick={() => {
                        setShowAllSchedules(!showAllSchedules);
                      }}
                      style={{
                        transition: 'all 0.2s ease',
                        transform: showAllSchedules ? 'scale(0.98)' : 'scale(1)'
                      }}
                    >
                      {showAllSchedules ? 
                        `▲ 접기` : 
                        `▼ +${todaySchedules.length - 5}개 더 보기`
                      }
                    </button>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* 수강권 정보 */}
          <div className="dashboard-card">
            <div className="card-header">
              <h2 className="card-title">
                <i className="fas fa-ticket-alt"></i>
                {isParent ? '내 자녀 수강권' : '수강권 현황'}
              </h2>
              <span className={`card-badge ${isParent ? '' : 'info'}`}>
                {isParent ? enrollments.length : enrollmentStats.active}개
              </span>
            </div>
            <div className="card-body">
              {enrollments.length === 0 ? (
                <div className="empty-state">
                  <i className="fas fa-check-circle"></i>
                  <p>{isParent ? '등록된 수강권이 없습니다' : '활성 수강권이 없습니다'}</p>
                </div>
              ) : (
                <>
                  {!isParent && (
                    <div className="stats-summary">
                      <div className="summary-item">
                        <span className="summary-label">전체</span>
                        <span className="summary-value">{enrollmentStats.total}개</span>
                      </div>
                      <div className="summary-item">
                        <span className="summary-label">활성</span>
                        <span className="summary-value">{enrollmentStats.active}개</span>
                      </div>
                      <div className="summary-item warning" style={{ cursor: 'pointer', background: enrollmentFilter === 'expiring' ? '#fff3e0' : undefined, borderRadius: '6px', padding: '4px 8px' }} onClick={() => setEnrollmentFilter(enrollmentFilter === 'expiring' ? 'all' : 'expiring')}>
                        <span className="summary-label">만료임박</span>
                        <span className="summary-value">{enrollmentStats.expiring}개</span>
                      </div>
                      <div className="summary-item urgent" style={{ cursor: 'pointer', background: enrollmentFilter === 'lowCount' ? '#fce4ec' : undefined, borderRadius: '6px', padding: '4px 8px' }} onClick={() => setEnrollmentFilter(enrollmentFilter === 'lowCount' ? 'all' : 'lowCount')}>
                        <span className="summary-label">횟수부족</span>
                        <span className="summary-value">{enrollmentStats.lowCount}개</span>
                      </div>
                    </div>
                  )}
                  <div className="list">
                    {(() => {
                      let filtered = isParent ? enrollments : enrollments.filter(e => e.isActive);
                      if (enrollmentFilter === 'expiring') {
                        filtered = filtered.filter(e => {
                          if (!e.endDate) return false;
                          const d = getBusinessDaysLeft(e.startDate, e.endDate);
                          return d <= 7 && d >= 0;
                        });
                      } else if (enrollmentFilter === 'lowCount') {
                        filtered = filtered.filter(e => e.type === 'COUNT_BASED' && e.remainingCount <= 5);
                      }
                      return (showAllEnrollments ? filtered : filtered.slice(0, 5)).map((enrollment) => {
                    const daysLeft = getBusinessDaysLeft(enrollment.startDate, enrollment.endDate);
                    return (
                      <div 
                        key={enrollment.id} 
                        className={`list-item ${isParent ? 'clickable' : ''}`}
                        onClick={isParent ? () => handleEnrollmentClick(enrollment) : undefined}
                      >
                        <div className={`item-icon ${daysLeft <= 3 ? 'urgent' : 'warning'}`}>
                          <i className="fas fa-ticket-alt"></i>
                        </div>
                        <div className="item-content">
                          <div className="item-title">{enrollment.studentName} - {enrollment.courseName}</div>
                          <div className="item-subtitle">
                            남은 횟수: {enrollment.remainingCount}회 · 종료일: {enrollment.endDate}
                          </div>
                        </div>
                        <div className={`item-badge ${daysLeft <= 3 ? 'badge-error' : 'badge-warning'}`}>
                          {daysLeft}일 남음
                        </div>
                        {isParent && (
                          <div className="item-action">
                            <i className="fas fa-chevron-right"></i>
                          </div>
                        )}
                      </div>
                    );
                  });
                    })()}
                  {(isParent ? enrollments.length : enrollmentStats.active) > 5 && (
                    <button 
                      type="button"
                      className="show-more-btn"
                      onClick={() => {
                        setShowAllEnrollments(!showAllEnrollments);
                      }}
                      style={{
                        transition: 'all 0.2s ease',
                        transform: showAllEnrollments ? 'scale(0.98)' : 'scale(1)'
                      }}
                    >
                      {showAllEnrollments ? 
                        `▲ 접기` : 
                        `▼ +${(isParent ? enrollments.length : enrollmentStats.active) - 5}개 더 보기`
                      }
                    </button>
                  )}
                </div>
                </>
              )}
            </div>
          </div>

          {/* 최근 출석 현황 */}
          <div className="dashboard-card">
            <div className="card-header">
              <h2 className="card-title">
                <i className="fas fa-user-check"></i>
                오늘 출석 현황
              </h2>
              <span className="card-badge">{todayAttendance.length}명</span>
            </div>
            <div className="card-body">
              {todayAttendance.length === 0 ? (
                <div className="empty-state">
                  <i className="fas fa-user-clock"></i>
                  <p>출석 기록이 없습니다</p>
                </div>
              ) : (
                <div className="list">
                  {(showAllAttendance ? todayAttendance : todayAttendance.slice(0, 5)).map((attendance) => (
                    <div key={attendance.id} className="list-item">
                      <div className="item-icon">
                        <i className="fas fa-user"></i>
                      </div>
                      <div className="item-content">
                        <div className="item-title">{attendance.studentName}</div>
                        <div className="item-subtitle">
                          등원: {formatTime(attendance.checkInTime)}
                          {attendance.checkOutTime && ` / 하원: ${formatTime(attendance.checkOutTime)}`}
                        </div>
                      </div>
                      <div className={`item-badge badge-${attendance.status.toLowerCase()}`} style={attendance.status === 'NOTYET' ? { backgroundColor: '#9e9e9e', color: '#fff' } : {}}>
                        {attendance.status === 'PRESENT' ? '출석' :
                         attendance.status === 'LATE' ? '지각' :
                         attendance.status === 'ABSENT' ? '결석' :
                         attendance.status === 'EXCUSED' ? '사유결석' :
                         attendance.status === 'NOTYET' ? '미출석' : attendance.status}
                      </div>
                    </div>
                  ))}
                  {todayAttendance.length > 5 && (
                    <button 
                      type="button"
                      className="show-more-btn"
                      onClick={() => {
                        setShowAllAttendance(!showAllAttendance);
                      }}
                      style={{
                        transition: 'all 0.2s ease',
                        transform: showAllAttendance ? 'scale(0.98)' : 'scale(1)'
                      }}
                    >
                      {showAllAttendance ? 
                        `▲ 접기` : 
                        `▼ +${todayAttendance.length - 5}명 더 보기`
                      }
                    </button>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* 오늘 예약 현황 */}
          <div className="dashboard-card">
            <div className="card-header">
              <h2 className="card-title">
                <i className="fas fa-calendar-check"></i>
                오늘 예약 현황
              </h2>
              <span className="card-badge">{todayReservations.length}건</span>
            </div>
            <div className="card-body">
              {todayReservations.length === 0 ? (
                <div className="empty-state">
                  <i className="fas fa-calendar-times"></i>
                  <p>예약이 없습니다</p>
                </div>
              ) : (
                <div className="list">
                  {(showAllReservations ? todayReservations : todayReservations.slice(0, 5)).map((reservation) => (
                    <div key={reservation.id} className="list-item">
                      <div className="item-icon">
                        <i className="fas fa-bookmark"></i>
                      </div>
                      <div className="item-content">
                        <div className="item-title">{reservation.studentName}</div>
                        <div className="item-subtitle">
                          {formatTime(reservation.scheduleStartTime)}
                        </div>
                      </div>
                      <div className={`item-badge badge-${reservation.status.toLowerCase()}`}>
                        {reservation.status === 'PENDING' ? '대기' :
                         reservation.status === 'CONFIRMED' ? '확정' :
                         reservation.status === 'CANCELLED' ? '취소' :
                         reservation.status === 'COMPLETED' ? '완료' : reservation.status}
                      </div>
                    </div>
                  ))}
                  {todayReservations.length > 5 && (
                    <button 
                      type="button"
                      className="show-more-btn"
                      onClick={() => {
                        setShowAllReservations(!showAllReservations);
                      }}
                      style={{
                        transition: 'all 0.2s ease',
                        transform: showAllReservations ? 'scale(0.98)' : 'scale(1)'
                      }}
                    >
                      {showAllReservations ? 
                        `▲ 접기` : 
                        `▼ +${todayReservations.length - 5}건 더 보기`
                      }
                    </button>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* 수강권 상세 모달 */}
      {showEnrollmentModal && selectedEnrollment && (
        <div className="modal-overlay">
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>
                <i className="fas fa-ticket-alt"></i>
                수강권 상세 정보
              </h2>
              <button className="modal-close" onClick={closeModal}>
                <i className="fas fa-times"></i>
              </button>
            </div>
            <div className="modal-body">
              <div className="enrollment-details">
                <div className="detail-section">
                  <h3>기본 정보</h3>
                  <div className="detail-grid">
                    <div className="detail-item">
                      <span className="detail-label">학생명</span>
                      <span className="detail-value">{selectedEnrollment.studentName}</span>
                    </div>
                    <div className="detail-item">
                      <span className="detail-label">수업명</span>
                      <span className="detail-value">{selectedEnrollment.courseName}</span>
                    </div>
                    <div className="detail-item">
                      <span className="detail-label">수강 기간</span>
                      <span className="detail-value">
                        {selectedEnrollment.startDate} ~ {selectedEnrollment.endDate}
                      </span>
                    </div>
                  </div>
                </div>
                
                <div className="detail-section">
                  <h3>수강 현황</h3>
                  <div className="detail-grid">
                    <div className="detail-item">
                      <span className="detail-label">총 횟수</span>
                      <span className="detail-value">{selectedEnrollment.totalCount}회</span>
                    </div>
                    <div className="detail-item">
                      <span className="detail-label">사용 횟수</span>
                      <span className="detail-value">{selectedEnrollment.usedCount}회</span>
                    </div>
                    <div className="detail-item">
                      <span className="detail-label">남은 횟수</span>
                      <span className="detail-value highlight">{selectedEnrollment.remainingCount}회</span>
                    </div>
                  </div>
                </div>

                <div className="detail-section">
                  <h3>상태</h3>
                  <div className="status-info">
                    <span className={`status-badge ${selectedEnrollment.isActive ? 'active' : 'inactive'}`}>
                      {selectedEnrollment.isActive ? '활성' : '비활성'}
                    </span>
                    {selectedEnrollment.memo && (
                      <div className="memo">
                        <span className="detail-label">메모</span>
                        <p>{selectedEnrollment.memo}</p>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
        </>
      )}
    </div>
  );
}

export default Dashboard;
