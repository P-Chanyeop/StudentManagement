import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import Layout from '../components/Layout';
import { attendanceAPI, scheduleAPI } from '../services/api';
import '../styles/Attendance.css';

function Attendance() {
  const queryClient = useQueryClient();
  const [selectedDate, setSelectedDate] = useState(
    new Date().toISOString().split('T')[0]
  );
  const [selectedSchedule, setSelectedSchedule] = useState(null);

  // 오늘 날짜의 스케줄 조회
  const { data: schedules, isLoading: schedulesLoading } = useQuery({
    queryKey: ['schedules', selectedDate],
    queryFn: async () => {
      const response = await scheduleAPI.getByDate(selectedDate);
      return response.data;
    },
  });

  // 선택된 스케줄의 출석 현황 조회
  const { data: attendances } = useQuery({
    queryKey: ['attendances', selectedSchedule?.id],
    queryFn: async () => {
      if (!selectedSchedule) return [];
      const response = await attendanceAPI.getBySchedule(selectedSchedule.id);
      return response.data;
    },
    enabled: !!selectedSchedule,
  });

  // 출석 체크인 mutation
  const checkInMutation = useMutation({
    mutationFn: (data) => attendanceAPI.checkIn(data),
    onSuccess: () => {
      queryClient.invalidateQueries(['attendances']);
      alert('출석 체크 완료!');
    },
    onError: (error) => {
      alert(error.response?.data?.message || '출석 체크 실패');
    },
  });

  // 하원 체크아웃 mutation
  const checkOutMutation = useMutation({
    mutationFn: (id) => attendanceAPI.checkOut(id),
    onSuccess: () => {
      queryClient.invalidateQueries(['attendances']);
      alert('하원 체크 완료!');
    },
  });

  const handleCheckIn = (studentId) => {
    if (!selectedSchedule) {
      alert('수업을 먼저 선택해주세요');
      return;
    }

    checkInMutation.mutate({
      studentId,
      scheduleId: selectedSchedule.id,
      expectedLeaveTime: selectedSchedule.endTime,
    });
  };

  const handleCheckOut = (attendanceId) => {
    if (confirm('하원 처리하시겠습니까?')) {
      checkOutMutation.mutate(attendanceId);
    }
  };

  const getStatusBadge = (status) => {
    const statusMap = {
      PRESENT: { label: '출석', className: 'present' },
      LATE: { label: '지각', className: 'late' },
      ABSENT: { label: '결석', className: 'absent' },
      EXCUSED: { label: '사유결석', className: 'excused' },
      EARLY_LEAVE: { label: '조퇴', className: 'early-leave' },
    };
    const status_info = statusMap[status] || { label: status, className: '' };
    return (
      <span className={`status-badge ${status_info.className}`}>
        {status_info.label}
      </span>
    );
  };

  const formatTime = (datetime) => {
    if (!datetime) return '-';
    return new Date(datetime).toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <Layout>
      <div className="attendance-page">
        <div className="page-header">
          <h1 className="page-title">출석 관리</h1>
          <p className="page-subtitle">학생 출석 체크 및 하원 관리</p>
        </div>

        {/* 날짜 선택 */}
        <div className="date-selector">
          <input
            type="date"
            value={selectedDate}
            onChange={(e) => {
              setSelectedDate(e.target.value);
              setSelectedSchedule(null);
            }}
            className="date-input"
          />
          <div className="date-label">
            {new Date(selectedDate).toLocaleDateString('ko-KR', {
              year: 'numeric',
              month: 'long',
              day: 'numeric',
              weekday: 'long',
            })}
          </div>
        </div>

        {/* 수업 목록 */}
        <div className="schedule-section">
          <h2 className="section-title">오늘의 수업</h2>
          {schedulesLoading ? (
            <div className="loading">수업 목록 로딩 중...</div>
          ) : schedules && schedules.length > 0 ? (
            <div className="schedule-grid">
              {schedules.map((schedule) => (
                <div
                  key={schedule.id}
                  className={`schedule-card ${
                    selectedSchedule?.id === schedule.id ? 'selected' : ''
                  }`}
                  onClick={() => setSelectedSchedule(schedule)}
                >
                  <div className="schedule-header">
                    <h3>{schedule.courseName}</h3>
                    <span className="schedule-time">
                      {schedule.startTime} - {schedule.endTime}
                    </span>
                  </div>
                  <div className="schedule-info">
                    <span className="schedule-students">
                      👥 {schedule.currentStudents}/{schedule.maxStudents}명
                    </span>
                    {schedule.isCancelled && (
                      <span className="cancelled-badge">수업취소</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              <p>오늘 예정된 수업이 없습니다</p>
            </div>
          )}
        </div>

        {/* 출석 현황 */}
        {selectedSchedule && (
          <div className="attendance-section">
            <div className="section-header">
              <h2 className="section-title">
                {selectedSchedule.courseName} - 출석 현황
              </h2>
              <div className="attendance-summary">
                출석: {attendances?.length || 0}명
              </div>
            </div>

            {attendances && attendances.length > 0 ? (
              <div className="attendance-grid">
                {attendances.map((attendance) => (
                  <div key={attendance.id} className="attendance-card">
                    <div className="student-info">
                      <div className="student-name">
                        {attendance.studentName}
                      </div>
                      {getStatusBadge(attendance.status)}
                    </div>

                    <div className="attendance-times">
                      <div className="time-item">
                        <span className="time-label">체크인</span>
                        <span className="time-value">
                          {formatTime(attendance.checkInTime)}
                        </span>
                      </div>
                      <div className="time-item">
                        <span className="time-label">체크아웃</span>
                        <span className="time-value">
                          {formatTime(attendance.checkOutTime)}
                        </span>
                      </div>
                      <div className="time-item">
                        <span className="time-label">예상 하원</span>
                        <span className="time-value">
                          {attendance.expectedLeaveTime || '-'}
                        </span>
                      </div>
                    </div>

                    {!attendance.checkOutTime && (
                      <button
                        className="btn-checkout"
                        onClick={() => handleCheckOut(attendance.id)}
                      >
                        하원 체크
                      </button>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <div className="empty-state">
                <p>아직 출석한 학생이 없습니다</p>
              </div>
            )}
          </div>
        )}
      </div>
    </Layout>
  );
}

export default Attendance;
