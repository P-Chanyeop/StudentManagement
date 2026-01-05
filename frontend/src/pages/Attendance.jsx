import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import LoadingSpinner from '../components/LoadingSpinner';
import { attendanceAPI, studentAPI, scheduleAPI } from '../services/api';
import '../styles/Attendance.css';

function Attendance() {
  const queryClient = useQueryClient();
  const [selectedDate, setSelectedDate] = useState(
    new Date().toISOString().split('T')[0]
  );
  const [sortBy, setSortBy] = useState('schedule'); // 기본값을 수업 시간순으로 변경
  const [searchName, setSearchName] = useState('');
  const [tableSearchName, setTableSearchName] = useState('');

  // 오늘 날짜의 전체 출석 현황 조회
  const { data: attendances, isLoading } = useQuery({
    queryKey: ['attendances', selectedDate],
    queryFn: async () => {
      const response = await attendanceAPI.getByDate(selectedDate);
      return response.data;
    },
  });

  // 모든 학생 조회 (출석 체크용)
  const { data: allStudents } = useQuery({
    queryKey: ['students'],
    queryFn: async () => {
      const response = await studentAPI.getAll();
      return response.data;
    },
  });

  // 해당 날짜의 스케줄 조회
  const { data: todaySchedules } = useQuery({
    queryKey: ['schedules', selectedDate],
    queryFn: async () => {
      const response = await scheduleAPI.getByDate(selectedDate);
      return response.data;
    },
  });

  // 출석 체크인 mutation
  const checkInMutation = useMutation({
    mutationFn: ({ studentId, scheduleId }) => {
      return attendanceAPI.checkIn({ 
        studentId, 
        scheduleId 
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['attendances', selectedDate]);
      alert('출석 체크가 완료되었습니다!');
    },
    onError: (error) => {
      alert(`출석 체크 중 오류가 발생했습니다: ${error.message}`);
      console.error('출석 체크 오류:', error);
    },
  });

  // 출석 취소 mutation
  const cancelAttendanceMutation = useMutation({
    mutationFn: (attendanceId) => attendanceAPI.cancelAttendance(attendanceId),
    onSuccess: () => {
      queryClient.invalidateQueries(['attendances', selectedDate]);
      alert('출석이 취소되었습니다.');
    },
    onError: (error) => {
      alert(`출석 취소 중 오류가 발생했습니다: ${error.message}`);
      console.error('출석 취소 오류:', error);
    },
  });

  // 수업 완료 체크 mutation
  const updateClassComplete = useMutation({
    mutationFn: (attendanceId) => 
      attendanceAPI.updateClassComplete(attendanceId),
    onSuccess: () => {
      queryClient.invalidateQueries(['attendances', selectedDate]);
    },
  });

  // 사유 업데이트 mutation
  const updateReasonMutation = useMutation({
    mutationFn: ({ attendanceId, reason }) => 
      attendanceAPI.updateReason(attendanceId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries(['attendances', selectedDate]);
    },
  });

  // 사유 입력 후 포커스 해제 시 자동 저장
  const handleReasonBlur = (attendanceId, reason) => {
    if (attendanceId && reason !== undefined) {
      updateReasonMutation.mutate({ attendanceId, reason });
    }
  };

  // 학생 검색 필터링
  const filteredStudents = allStudents ? allStudents
    .filter(student => student.name && student.name.includes(searchName))
    .map(student => ({
      ...student,
      isCheckedIn: attendances?.some(att => att.student?.id === student.id && att.checkInTime)
    })) : [];

  const handleCancelAttendance = (attendance) => {
    if (window.confirm(`${attendance.studentName} 학생의 출석을 취소하시겠습니까?`)) {
      cancelAttendanceMutation.mutate(attendance.id);
    }
  };

  // 학생 출석 체크인
  const handleStudentCheckIn = (attendance) => {
    if (attendance.checkInTime) return; // 이미 체크인된 경우
    
    checkInMutation.mutate({
      studentId: attendance.studentId,
      scheduleId: attendance.scheduleId
    });
  };

  // 출석 목록 정렬
  const sortedAttendances = attendances ? [...attendances].sort((a, b) => {
    if (sortBy === 'schedule') {
      // 수업 시간순 정렬 (같은 시간이면 이름순)
      const timeA = a.schedule?.startTime || '';
      const timeB = b.schedule?.startTime || '';
      
      if (timeA !== timeB) {
        return timeA.localeCompare(timeB);
      }
      // 같은 수업 시간이면 이름 가나다순
      return a.studentName.localeCompare(b.studentName);
    } else if (sortBy === 'arrival') {
      // 출석한 학생 먼저 (등원 시간순), 그 다음 미출석 학생 (이름순)
      if (a.checkInTime && b.checkInTime) {
        return new Date(a.checkInTime) - new Date(b.checkInTime);
      }
      if (a.checkInTime && !b.checkInTime) return -1;
      if (!a.checkInTime && b.checkInTime) return 1;
      // 둘 다 미출석이면 이름순
      return a.studentName.localeCompare(b.studentName);
    } else if (sortBy === 'departure') {
      // 하원한 학생 먼저 (하원 시간순), 그 다음 미하원 학생 (등원 시간순 또는 이름순)
      if (a.checkOutTime && b.checkOutTime) {
        return new Date(a.checkOutTime) - new Date(b.checkOutTime);
      }
      if (a.checkOutTime && !b.checkOutTime) return -1;
      if (!a.checkOutTime && b.checkOutTime) return 1;
      // 둘 다 미하원이면 등원한 학생 먼저, 그 다음 이름순
      if (a.checkInTime && b.checkInTime) {
        return new Date(a.checkInTime) - new Date(b.checkInTime);
      }
      if (a.checkInTime && !b.checkInTime) return -1;
      if (!a.checkInTime && b.checkInTime) return 1;
      return a.studentName.localeCompare(b.studentName);
    } else {
      // 기본: 이름순
      return a.studentName.localeCompare(b.studentName);
    }
  }) : [];

  // 테이블 필터링된 출석 목록
  const filteredAttendances = sortedAttendances.filter(attendance => 
    !tableSearchName || attendance.studentName?.includes(tableSearchName)
  );

  const formatTime = (timeString) => {
    if (!timeString) return '-';
    
    let date;
    
    // LocalTime 형식 (HH:mm:ss.nnnnnnn) 처리
    if (typeof timeString === 'string' && timeString.match(/^\d{1,2}:\d{2}:\d{2}/)) {
      const timePart = timeString.substring(0, 8); // HH:mm:ss
      date = new Date(`2000-01-01T${timePart}`);
    } else {
      // DateTime 형식 처리
      try {
        date = new Date(timeString);
      } catch (error) {
        console.error('Time format error:', timeString, error);
        return '-';
      }
    }
    
    return date.toLocaleTimeString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: true
    });
  };

  if (isLoading) return <LoadingSpinner />;

  return (
    <div className="page-wrapper">
      <div className="page-header">
        <div className="page-header-content">
          <div className="page-title-section">
            <h1 className="page-title">
              <i className="fas fa-clipboard-check"></i>
              출석부
            </h1>
            <p className="page-subtitle">학생 출석 현황 및 수업 관리</p>
          </div>
        </div>
      </div>

      <div className="page-content">
        <div className="attendance-controls">
          <div className="date-selector">
            <label htmlFor="date">날짜 선택:</label>
            <input
              type="date"
              id="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
            />
          </div>

          <div className="sort-selector">
            <label htmlFor="sort">정렬 기준:</label>
            <select
              id="sort"
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
            >
              <option value="schedule">수업 시간순</option>
              <option value="name">이름순</option>
              <option value="arrival">등원순</option>
              <option value="departure">하원순</option>
            </select>
          </div>
        </div>

        <div className="attendance-summary">
          <div className="summary-card">
            <h3>출석 현황</h3>
            <div className="summary-stats">
              <div className="stat">
                <span className="stat-label">총 학생</span>
                <span className="stat-value">{attendances?.length || 0}명</span>
              </div>
              <div className="stat">
                <span className="stat-label">출석</span>
                <span className="stat-value present">
                  {attendances?.filter(a => a.checkInTime).length || 0}명
                </span>
              </div>
              <div className="stat">
                <span className="stat-label">미출석</span>
                <span className="stat-value absent">
                  {attendances?.filter(a => !a.checkInTime).length || 0}명
                </span>
              </div>
            </div>
            
            <div className="attendance-progress">
              <div className="progress-header">
                <span className="progress-label">출석률</span>
                <span className="progress-percentage">
                  {attendances?.length > 0 
                    ? Math.round((attendances.filter(a => a.checkInTime).length / attendances.length) * 100)
                    : 0}%
                </span>
              </div>
              <div className="progress-bar">
                <div 
                  className="progress-fill"
                  style={{
                    width: `${attendances?.length > 0 
                      ? (attendances.filter(a => a.checkInTime).length / attendances.length) * 100
                      : 0}%`
                  }}
                ></div>
              </div>
            </div>
          </div>
        </div>

        <div className="attendance-table-container">
          <div className="table-search">
            <input
              type="text"
              placeholder="테이블에서 학생 이름 검색..."
              value={tableSearchName}
              onChange={(e) => setTableSearchName(e.target.value)}
              className="table-search-input"
            />
          </div>
          
          <table className="attendance-table">
            <thead>
              <tr>
                <th>학생 이름</th>
                <th>수업 시간</th>
                <th>등원 시간</th>
                <th>하원 시간</th>
                <th>수업 완료</th>
                <th>비고</th>
                <th>출석 체크</th>
              </tr>
            </thead>
            <tbody>
              {filteredAttendances.map((attendance, index) => (
                <tr 
                  key={attendance.id || `attendance-${index}`}
                  className={`attendance-row ${!attendance.checkInTime ? 'absent' : ''}`}
                >
                  <td className="student-name-td">
                    <div className="student-info">
                      <span className="name">{attendance.studentName}</span>
                      <span className="status-badge">
                        {attendance.checkInTime ? '출석' : '미출석'}
                      </span>
                    </div>
                  </td>
                  <td className="class-time">
                    <div className="class-info">
                      <span className="course-name">{attendance.courseName}</span>
                      <span className="time-range">{attendance.startTime} - {attendance.endTime}</span>
                    </div>
                  </td>
                  <td className="check-in-time">
                    {formatTime(attendance.checkInTime)}
                  </td>
                  <td className="check-out-time">
                    {attendance.checkOutTime ? (
                      formatTime(attendance.checkOutTime)
                    ) : attendance.checkInTime ? (
                      <span className="expected-time">
                        {formatTime(attendance.expectedLeaveTime)} (예정)
                      </span>
                    ) : (
                      '-'
                    )}
                  </td>
                  <td className="class-complete">
                    <label className="checkbox-container">
                      <input
                        type="checkbox"
                        checked={attendance.classCompleted || false}
                        onChange={() => 
                          updateClassComplete.mutate(attendance.id)
                        }
                      />
                      <span className="checkmark"></span>
                    </label>
                  </td>
                  <td className="notes">
                    <input
                      type="text"
                      placeholder="결석/지각 사유 입력..."
                      defaultValue={attendance.reason || ''}
                      className="notes-input"
                      onBlur={(e) => handleReasonBlur(attendance.id, e.target.value)}
                    />
                  </td>
                  <td className="check-actions">
                    {!attendance.checkInTime ? (
                      <button
                        onClick={() => handleStudentCheckIn(attendance)}
                        className="checkin-btn"
                        disabled={checkInMutation.isPending}
                      >
                        출석 체크
                      </button>
                    ) : (
                      <div className="action-buttons">
                        <button
                          onClick={() => handleCancelAttendance(attendance)}
                          className="attendance-cancel-btn"
                          disabled={cancelAttendanceMutation.isPending}
                        >
                          출석 취소
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {(!attendances || attendances.length === 0) && (
            <div className="empty-state">
              <i className="fas fa-calendar-times"></i>
              <p>선택한 날짜에 출석 데이터가 없습니다.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default Attendance;
