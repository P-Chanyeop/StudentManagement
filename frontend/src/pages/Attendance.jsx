import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import Layout from '../components/Layout';
import LoadingSpinner from '../components/LoadingSpinner';
import { attendanceAPI, scheduleAPI } from '../services/api';
import '../styles/Attendance.css';

function Attendance() {
  const queryClient = useQueryClient();
  const [selectedDate, setSelectedDate] = useState(
    new Date().toISOString().split('T')[0]
  );
  const [selectedSchedule, setSelectedSchedule] = useState(null);
  const [sortBy, setSortBy] = useState('arrival'); // 'arrival' or 'departure'

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

  // 출석 데이터 정렬
  const sortedAttendances = attendances
    ? [...attendances].sort((a, b) => {
        if (sortBy === 'arrival') {
          // 등원 시간순 정렬 (빠른 순)
          if (!a.checkInTime) return 1;
          if (!b.checkInTime) return -1;
          return new Date(a.checkInTime) - new Date(b.checkInTime);
        } else {
          // 하원 시간순 정렬 (빠른 순)
          if (!a.checkOutTime) return 1;
          if (!b.checkOutTime) return -1;
          return new Date(a.checkOutTime) - new Date(b.checkOutTime);
        }
      })
    : [];

  return (
    <Layout>
      <div className="attendance-page">
        <div className="page-header">
          <h1 className="page-title"><i className="fas fa-clipboard-list"></i> 출석부</h1>
          <p className="page-subtitle">학생 등원/하원 관리</p>
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
            <LoadingSpinner />
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
                      <i className="fas fa-users"></i> {schedule.currentStudents}/{schedule.maxStudents}명
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

        {/* 출석 현황 테이블 */}
        {selectedSchedule && (
          <div className="attendance-section">
            <div className="section-header">
              <div>
                <h2 className="section-title">
                  {selectedSchedule.courseName} - 출석 현황
                </h2>
                <div className="class-time-info">
                  ⏰ 수업시간: {selectedSchedule.startTime} - {selectedSchedule.endTime}
                </div>
              </div>
              <div className="header-actions">
                <div className="sort-buttons">
                  <button
                    className={`sort-btn ${sortBy === 'arrival' ? 'active' : ''}`}
                    onClick={() => setSortBy('arrival')}
                  >
                    등원순 정렬
                  </button>
                  <button
                    className={`sort-btn ${sortBy === 'departure' ? 'active' : ''}`}
                    onClick={() => setSortBy('departure')}
                  >
                    하원순 정렬
                  </button>
                </div>
                <div className="attendance-summary">
                  출석: {attendances?.length || 0}명
                </div>
              </div>
            </div>

            {sortedAttendances && sortedAttendances.length > 0 ? (
              <div className="attendance-table-wrapper">
                <table className="attendance-table">
                  <thead>
                    <tr>
                      <th className="col-checkbox">
                        <input type="checkbox" disabled />
                      </th>
                      <th className="col-number">순번</th>
                      <th className="col-name">학생 이름</th>
                      <th className="col-status">상태</th>
                      <th className="col-time">등원 시간</th>
                      <th className="col-time">하원 시간</th>
                      <th className="col-time">예상 하원</th>
                      <th className="col-remarks">비고</th>
                      <th className="col-actions">하원 처리</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sortedAttendances.map((attendance, index) => (
                      <tr
                        key={attendance.id}
                        className={attendance.checkOutTime ? 'checked-out' : ''}
                      >
                        <td className="col-checkbox">
                          <input
                            type="checkbox"
                            checked={!!attendance.checkOutTime}
                            disabled
                          />
                        </td>
                        <td className="col-number">{index + 1}</td>
                        <td className="col-name">
                          <strong>{attendance.studentName}</strong>
                        </td>
                        <td className="col-status">
                          {getStatusBadge(attendance.status)}
                        </td>
                        <td className="col-time arrival-time">
                          {formatTime(attendance.checkInTime)}
                        </td>
                        <td className="col-time departure-time">
                          {formatTime(attendance.checkOutTime)}
                        </td>
                        <td className="col-time expected-time">
                          {attendance.expectedLeaveTime || '-'}
                        </td>
                        <td className="col-remarks">
                          {attendance.memo || attendance.reason || '-'}
                        </td>
                        <td className="col-actions">
                          {!attendance.checkOutTime ? (
                            <button
                              className="btn-checkout"
                              onClick={() => handleCheckOut(attendance.id)}
                            >
                              하원 체크
                            </button>
                          ) : (
                            <span className="checkout-done">완료</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
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
