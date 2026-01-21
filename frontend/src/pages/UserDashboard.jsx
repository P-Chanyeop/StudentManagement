import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { authAPI, enrollmentAPI, consultationAPI } from '../services/api';
import '../styles/Dashboard.css';

function UserDashboard() {
  const [selectedEnrollment, setSelectedEnrollment] = useState(null);
  const [showRecordingModal, setShowRecordingModal] = useState(false);
  // 사용자 프로필 조회
  const { data: profile } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
  });

  // 사용자 수강권 조회
  const { data: myEnrollments = [], isLoading } = useQuery({
    queryKey: ['myEnrollments', profile?.studentId],
    queryFn: async () => {
      if (profile?.studentId) {
        const response = await enrollmentAPI.getByStudent(profile.studentId);
        return response.data;
      }
      return [];
    },
    enabled: !!profile?.studentId,
  });

  // 레코딩 파일 목록 조회
  const { data: recordingFiles = [] } = useQuery({
    queryKey: ['recordingFiles', selectedEnrollment?.studentId],
    queryFn: async () => {
      if (selectedEnrollment?.studentId) {
        const response = await consultationAPI.getByStudent(selectedEnrollment.studentId);
        return response.data.filter(consultation => consultation.recordingFileUrl);
      }
      return [];
    },
    enabled: !!selectedEnrollment?.studentId && showRecordingModal,
  });

  // 레코딩 파일 모달 열기
  const handleRecordingClick = (enrollment) => {
    setSelectedEnrollment(enrollment);
    setShowRecordingModal(true);
  };

  // 모달 닫기
  const closeModal = () => {
    setShowRecordingModal(false);
    setSelectedEnrollment(null);
  };

  if (isLoading) {
    return <div className="dashboard-wrapper"><div className="loading">로딩 중...</div></div>;
  }

  return (
    <div className="dashboard-wrapper">
      {/* 히어로 섹션 */}
      <section className="hero">
        <div className="hero-container">
          <h1>안녕하세요, {profile?.nickname}님! 👋</h1>
          <p>자녀의 학습 현황을 확인하세요</p>
        </div>
      </section>

      {/* 메인 컨텐츠 */}
      <div className="dashboard-container">
        {myEnrollments.length === 0 ? (
          <div className="empty-state">
            <i className="fas fa-graduation-cap"></i>
            <p>등록된 수강권이 없습니다</p>
          </div>
        ) : (
          myEnrollments.map((enrollment) => {
            const daysLeft = Math.ceil(
              (new Date(enrollment.endDate) - new Date()) / (1000 * 60 * 60 * 24)
            );
            
            return (
              <div key={enrollment.id} className="student-info-section">
                <h3 className="student-name-header">{enrollment.studentName}</h3>
                
                {/* 통계 카드 그리드 */}
                <div className="stats-grid">
                  {/* 반 카드 */}
                  <div className="stat-card">
                    <div className="stat-card-header">
                      <div className="stat-icon">
                        <i className="fas fa-chalkboard-teacher"></i>
                      </div>
                      <div className="stat-trend success">
                        <i className="fas fa-check"></i>
                        수강중
                      </div>
                    </div>
                    <div className="stat-card-body">
                      <div className="stat-number">{enrollment.courseName}</div>
                      <div className="stat-label">반</div>
                    </div>
                    <div className="stat-footer">
                      <i className="fas fa-info-circle"></i> 
                      현재 수강 중인 반
                    </div>
                  </div>

                  {/* 수업 시간 카드 */}
                  <div className="stat-card">
                    <div className="stat-card-header">
                      <div className="stat-icon">
                        <i className="fas fa-clock"></i>
                      </div>
                      <div className="stat-trend info">
                        <i className="fas fa-calendar"></i>
                        일정
                      </div>
                    </div>
                    <div className="stat-card-body">
                      <div className="stat-number schedule-time">
                        {enrollment.courseSchedules?.map(schedule => 
                          `${schedule.dayOfWeek}`
                        ).join(', ') || '미정'}
                      </div>
                      <div className="stat-label">수업 시간</div>
                    </div>
                    <div className="stat-footer">
                      <i className="fas fa-clock"></i> 
                      {enrollment.courseSchedules?.map(schedule => 
                        `${schedule.startTime}-${schedule.endTime}`
                      ).join(', ') || '시간 미정'}
                    </div>
                  </div>

                  {/* 잔여 횟수 카드 */}
                  <div className="stat-card">
                    <div className="stat-card-header">
                      <div className="stat-icon">
                        <i className="fas fa-ticket-alt"></i>
                      </div>
                      <div className="stat-trend success">
                        <i className="fas fa-check"></i>
                        이용가능
                      </div>
                    </div>
                    <div className="stat-card-body">
                      <div className="stat-number">
                        {enrollment.enrollmentType === 'COUNT' 
                          ? enrollment.remainingCount 
                          : '∞'}
                      </div>
                      <div className="stat-label">잔여 횟수</div>
                    </div>
                    <div className="stat-footer">
                      <i className="fas fa-calculator"></i> 
                      {enrollment.enrollmentType === 'COUNT' 
                        ? `총 ${enrollment.totalCount}회 중 ${enrollment.usedCount}회 사용`
                        : '무제한 이용'}
                    </div>
                  </div>

                  {/* 수강 기간 카드 */}
                  <div className="stat-card">
                    <div className="stat-card-header">
                      <div className="stat-icon">
                        <i className="fas fa-calendar-alt"></i>
                      </div>
                      <div className="stat-trend info">
                        <i className="fas fa-info"></i>
                        기간
                      </div>
                    </div>
                    <div className="stat-card-body">
                      <div className="stat-number period-dates">
                        {new Date(enrollment.startDate).toLocaleDateString('ko-KR', {month: 'short', day: 'numeric'})} ~ 
                        {new Date(enrollment.endDate).toLocaleDateString('ko-KR', {month: 'short', day: 'numeric'})}
                      </div>
                      <div className="stat-label">수강 기간</div>
                    </div>
                    <div className="stat-footer">
                      <i className="fas fa-calendar-check"></i> 
                      {new Date(enrollment.startDate).getFullYear()}년 수강
                    </div>
                  </div>

                  {/* 남은 일수 카드 */}
                  <div className="stat-card">
                    <div className="stat-card-header">
                      <div className="stat-icon">
                        <i className="fas fa-hourglass-half"></i>
                      </div>
                      <div className={`stat-trend ${daysLeft <= 7 ? 'warning' : 'success'}`}>
                        <i className={`fas ${daysLeft <= 7 ? 'fa-exclamation-triangle' : 'fa-check'}`}></i>
                        {daysLeft <= 7 ? '주의' : '정상'}
                      </div>
                    </div>
                    <div className="stat-card-body">
                      <div className="stat-number">{daysLeft > 0 ? daysLeft : 0}</div>
                      <div className="stat-label">남은 일수</div>
                    </div>
                    <div className="stat-footer">
                      <i className="fas fa-calendar-times"></i> 
                      {daysLeft > 0 ? `${daysLeft}일 후 만료` : '만료됨'}
                    </div>
                  </div>

                  {/* 레코딩 파일 카드 */}
                  <div className="stat-card clickable" onClick={() => handleRecordingClick(enrollment)}>
                    <div className="stat-card-header">
                      <div className="stat-icon">
                        <i className="fas fa-video"></i>
                      </div>
                      <div className="stat-trend info">
                        <i className="fas fa-upload"></i>
                        업로드
                      </div>
                    </div>
                    <div className="stat-card-body">
                      <div className="stat-number">
                        {enrollment.actualRecordings || 0}/{enrollment.expectedRecordings || 0}
                      </div>
                      <div className="stat-label">레코딩 파일</div>
                    </div>
                    <div className="stat-footer">
                      <i className="fas fa-file-video"></i> 
                      클릭하여 파일 목록 보기
                    </div>
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* 레코딩 파일 모달 */}
      {showRecordingModal && selectedEnrollment && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-content recording-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>
                <i className="fas fa-video"></i>
                {selectedEnrollment.studentName} - 레코딩 파일 목록
              </h2>
              <button className="modal-close" onClick={closeModal}>
                <i className="fas fa-times"></i>
              </button>
            </div>
            <div className="modal-body">
              {recordingFiles.length === 0 ? (
                <div className="empty-state">
                  <i className="fas fa-video-slash"></i>
                  <p>업로드된 레코딩 파일이 없습니다</p>
                </div>
              ) : (
                <div className="recording-list">
                  {recordingFiles.map((consultation, index) => (
                    <div key={consultation.id} className="recording-item">
                      <div className="recording-info">
                        <div className="recording-title">
                          <i className="fas fa-play-circle"></i>
                          상담 #{index + 1}
                        </div>
                        <div className="recording-date">
                          {new Date(consultation.consultationDate).toLocaleDateString('ko-KR', {
                            year: 'numeric',
                            month: 'long',
                            day: 'numeric',
                            weekday: 'short'
                          })}
                        </div>
                        {consultation.memo && (
                          <div className="recording-memo">
                            <i className="fas fa-sticky-note"></i>
                            {consultation.memo}
                          </div>
                        )}
                      </div>
                      <div className="recording-actions">
                        <a 
                          href={consultation.recordingFileUrl} 
                          target="_blank" 
                          rel="noopener noreferrer"
                          className="btn-play"
                        >
                          <i className="fas fa-external-link-alt"></i>
                          재생
                        </a>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default UserDashboard;
