import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import {
  studentAPI,
  attendanceAPI,
  reservationAPI,
  enrollmentAPI,
  scheduleAPI,
  authAPI,
  dashboardAPI
} from '../services/api';
import '../styles/Dashboard.css';

function Dashboard() {
  // 수강권 상세 모달 상태
  const [selectedEnrollment, setSelectedEnrollment] = useState(null);
  const [showEnrollmentModal, setShowEnrollmentModal] = useState(false);

  // 오늘 날짜
  const today = new Date().toISOString().split('T')[0];

  // 사용자 프로필 조회
  const { data: profile } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
  });

  const isParent = profile?.role === 'PARENT';

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
    return timeString.substring(0, 5); // HH:MM
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
        const daysLeft = Math.ceil((new Date(e.endDate) - new Date()) / (1000 * 60 * 60 * 24));
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
      {/* 히어로 섹션 */}
      <section className="hero">
        <div className="hero-container">
          <h1>안녕하세요, {profile?.name || '사용자'}님! 👋</h1>
          <p>오늘도 학원 운영을 효율적으로 관리하세요</p>
        </div>
      </section>

      {/* 메인 컨텐츠 */}
      <div className="dashboard-container">
        {/* 통계 카드 - 관리자/선생님만 */}
        {!isParent && (
          <div className="stats-grid">
          <div className="stat-card">
            <div className="stat-card-header">
              <div className="stat-icon">
                <i className="fas fa-users"></i>
              </div>
              <div className="stat-trend">
                <i className="fas fa-arrow-up"></i>
                NEW
              </div>
            </div>
            <div className="stat-content">
              <h3>전체 학생</h3>
              <div className="stat-value">
                {totalStudents}
                <span className="stat-unit">명</span>
              </div>
            </div>
            <div className="stat-footer">
              <i className="fas fa-info-circle"></i> {profile?.role === 'TEACHER' ? '전체 학생 수' : '등록된 전체 학생 수'}
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-card-header">
              <div className="stat-icon">
                <i className="fas fa-chalkboard-teacher"></i>
              </div>
              <div className="stat-trend">
                <i className="fas fa-calendar-day"></i>
                오늘
              </div>
            </div>
            <div className="stat-content">
              <h3>오늘의 수업</h3>
              <div className="stat-value">
                {todaySchedulesCount}
                <span className="stat-unit">개</span>
              </div>
            </div>
            <div className="stat-footer">
              <i className="fas fa-info-circle"></i> {profile?.role === 'TEACHER' ? '오늘 담당 수업' : '오늘 예정된 수업'}
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-card-header">
              <div className="stat-icon">
                <i className="fas fa-check-circle"></i>
              </div>
              <div className="stat-trend success">
                <i className="fas fa-check"></i>
                {attendanceRate}%
              </div>
            </div>
            <div className="stat-content">
              <h3>오늘 출석</h3>
              <div className="stat-value">
                {todayAttendanceCount}
                <span className="stat-unit">명</span>
              </div>
            </div>
            <div className="stat-footer">
              <i className="fas fa-info-circle"></i> 출석률 {attendanceRate}%
            </div>
          </div>

          <div 
            className={`stat-card ${isParent ? 'clickable' : ''}`}
            onClick={isParent && enrollmentStats.count > 0 ? () => handleEnrollmentClick(enrollments[0]) : undefined}
          >
            <div className="stat-card-header">
              <div className="stat-icon">
                <i className={`fas ${isParent ? 'fa-ticket-alt' : 'fa-credit-card'}`}></i>
              </div>
              <div className={`stat-trend ${isParent ? '' : 'info'}`}>
                <i className={`fas ${isParent ? 'fa-info' : 'fa-chart-line'}`}></i>
                {isParent ? '정보' : '통계'}
              </div>
            </div>
            <div className="stat-content">
              <h3>{enrollmentStats.label}</h3>
              <div className="stat-value">
                {enrollmentStats.count}
                <span className="stat-unit">개</span>
              </div>
            </div>
            <div className="stat-footer">
              <i className="fas fa-info-circle"></i> 
              {isParent ? '클릭하여 상세 정보 확인' : 
               `전체 ${enrollmentStats.total}개 · 만료임박 ${enrollmentStats.expiring}개`}
            </div>
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
                  {todaySchedules.slice(0, 5).map((schedule) => (
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
                    <div className="show-more">
                      +{todaySchedules.length - 5}개 더 보기
                    </div>
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
                      <div className="summary-item warning">
                        <span className="summary-label">만료임박</span>
                        <span className="summary-value">{enrollmentStats.expiring}개</span>
                      </div>
                      <div className="summary-item urgent">
                        <span className="summary-label">횟수부족</span>
                        <span className="summary-value">{enrollmentStats.lowCount}개</span>
                      </div>
                    </div>
                  )}
                  <div className="list">
                    {(isParent ? enrollments : enrollments.filter(e => e.isActive)).slice(0, 5).map((enrollment) => {
                    const daysLeft = Math.ceil(
                      (new Date(enrollment.endDate) - new Date()) / (1000 * 60 * 60 * 24)
                    );
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
                  })}
                  {(isParent ? enrollments.length : enrollmentStats.active) > 5 && (
                    <div className="show-more">
                      +{(isParent ? enrollments.length : enrollmentStats.active) - 5}개 더 보기
                    </div>
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
                  {todayAttendance.slice(0, 5).map((attendance) => (
                    <div key={attendance.id} className="list-item">
                      <div className="item-icon">
                        <i className="fas fa-user"></i>
                      </div>
                      <div className="item-content">
                        <div className="item-title">{attendance.studentName}</div>
                        <div className="item-subtitle">
                          체크인: {formatTime(attendance.checkInTime)}
                        </div>
                      </div>
                      <div className={`item-badge badge-${attendance.status.toLowerCase()}`}>
                        {attendance.status === 'PRESENT' ? '출석' :
                         attendance.status === 'LATE' ? '지각' :
                         attendance.status === 'ABSENT' ? '결석' :
                         attendance.status === 'EXCUSED' ? '사유결석' : attendance.status}
                      </div>
                    </div>
                  ))}
                  {todayAttendance.length > 5 && (
                    <div className="show-more">
                      +{todayAttendance.length - 5}명 더 보기
                    </div>
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
                  {todayReservations.slice(0, 5).map((reservation) => (
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
                    <div className="show-more">
                      +{todayReservations.length - 5}건 더 보기
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* 수강권 상세 모달 */}
      {showEnrollmentModal && selectedEnrollment && (
        <div className="modal-overlay" onClick={closeModal}>
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
    </div>
  );
}

export default Dashboard;
