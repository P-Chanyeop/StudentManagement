import { useQuery } from '@tanstack/react-query';
import { useState, useEffect } from 'react';
import { authAPI, enrollmentAPI, consultationAPI, fileAPI } from '../services/api';
import { holidayService } from '../services/holidayService';
import '../styles/Dashboard.css';

function UserDashboard() {
  const [selectedEnrollment, setSelectedEnrollment] = useState(null);
  const [showRecordingModal, setShowRecordingModal] = useState(false);
  const [holidays, setHolidays] = useState([]);

  useEffect(() => {
    const y = new Date().getFullYear();
    Promise.all([holidayService.getHolidays(y), holidayService.getHolidays(y + 1)])
      .then(([h1, h2]) => setHolidays([...h1, ...h2]))
      .catch(() => {});
  }, []);
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
          <h1>대시보드</h1>
          <p>자녀의 학습 현황을 확인하세요</p>
        </div>
      </section>

      {/* 메인 컨텐츠 */}
      <div className="parent-content">
        {myEnrollments.length === 0 ? (
          <div className="empty-state">
            <i className="fas fa-graduation-cap"></i>
            <p>등록된 수강권이 없습니다</p>
          </div>
        ) : (
          myEnrollments.map((enrollment) => {
            const daysLeft = holidayService.calculateRemainingBusinessDaysWithCache(
              enrollment.startDate, enrollment.endDate, holidays
            ) || 0;
            
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
                      {enrollment.totalCount 
                        ? `${enrollment.remainingCount}회 / ${enrollment.totalCount}회` 
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
                  <div className="info-row clickable" onClick={() => handleRecordingClick(enrollment)}>
                    <span className="info-label">레코딩 파일</span>
                    <span className="info-value">
                      {enrollment.actualRecordings || 0} / {enrollment.expectedRecordings || 0}
                      <i className="fas fa-chevron-right" style={{marginLeft: '8px', color: '#999'}}></i>
                    </span>
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
                        <button
                          className="btn-play"
                          onClick={async () => {
                            try {
                              const urls = consultation.recordingFileUrl.split(',');
                              for (const entry of urls) {
                                const parts = entry.split('::');
                                const filePath = parts.length > 1 ? parts[1] : parts[0];
                                const fileName = parts.length > 1 ? parts[0] : filePath.split('/').pop();
                                const response = await fileAPI.download(filePath);
                                const url = window.URL.createObjectURL(new Blob([response.data]));
                                const link = document.createElement('a');
                                link.href = url;
                                link.setAttribute('download', fileName);
                                document.body.appendChild(link);
                                link.click();
                                link.remove();
                                window.URL.revokeObjectURL(url);
                              }
                            } catch (e) { alert('파일 다운로드에 실패했습니다.'); }
                          }}
                        >
                          <i className="fas fa-download"></i>
                          다운로드
                        </button>
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
