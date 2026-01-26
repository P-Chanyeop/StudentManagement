import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import LoadingSpinner from '../components/LoadingSpinner';
import { attendanceAPI, studentAPI } from '../services/api';
import '../styles/Attendance.css';

function Attendance() {
  const queryClient = useQueryClient();
  const [selectedDate, setSelectedDate] = useState(
    new Date().toISOString().split('T')[0]
  );
  const [searchName, setSearchName] = useState('');
  const [tableSearchName, setTableSearchName] = useState('');
  
  // 핸드폰 번호 입력 모달 상태
  const [showPhoneModal, setShowPhoneModal] = useState(false);
  const [selectedAttendance, setSelectedAttendance] = useState(null);
  const [parentPhoneLast4, setParentPhoneLast4] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [selectedStudent, setSelectedStudent] = useState(null);
  
  // 테이블 헤더 정렬 상태
  const [tableSortBy, setTableSortBy] = useState('schedule');
  const [tableSortOrder, setTableSortOrder] = useState('asc'); // 'asc' 또는 'desc'
  
  // 날짜 선택을 위한 분리된 상태
  const [dateComponents, setDateComponents] = useState(() => {
    const today = new Date();
    return {
      year: today.getFullYear().toString(),
      month: (today.getMonth() + 1).toString(),
      day: today.getDate().toString()
    };
  });

  // 출석 상태에 따른 행 클래스 반환
  const getAttendanceRowClass = (attendance) => {
    // 출석 체크가 완료된 경우 실제 상태 표시
    if (attendance.checkInTime) {
      switch (attendance.status) {
        case 'PRESENT':
          return 'present';
        case 'ABSENT':
          return 'absent';
        case 'EXCUSED':
          return 'excused';
        case 'EARLY_LEAVE':
          return 'early-leave';
        default:
          return 'present';
      }
    }
    
    // 미래 시간대 체크
    if (isFutureTime(attendance)) return 'waiting';
    
    if (!attendance.status) return 'absent';
    
    switch (attendance.status) {
      case 'PRESENT':
        return 'present';
      case 'LATE':
        return 'late';
      case 'ABSENT':
        return 'absent';
      case 'EXCUSED':
        return 'excused';
      case 'EARLY_LEAVE':
        return 'early-leave';
      default:
        return 'absent';
    }
  };

  // 출석 상태 텍스트 반환
  const getAttendanceStatusText = (attendance) => {
    // 미래 시간대 체크
    if (isFutureTime(attendance)) return '대기';
    
    // 상태가 없으면 미출석
    if (!attendance.status) return '미출석';
    
    // 상태에 따라 표시
    switch (attendance.status) {
      case 'PRESENT':
        return '출석';
      case 'LATE':
        return '지각';
      case 'NOTYET':
        return '미출석';
      case 'ABSENT':
        return '결석';
      case 'EXCUSED':
        return '사유결석';
      case 'EARLY_LEAVE':
        return '조퇴';
      default:
        return '미출석';
    }
  };

  // 미래 시간대인지 확인하는 함수
  const isFutureTime = (attendance) => {
    const now = new Date();
    const today = now.toISOString().split('T')[0];
    
    // 오늘이 아닌 미래 날짜면 대기
    if (selectedDate > today) return true;
    
    // 오늘 날짜인 경우 수업 시작 시간과 비교
    if (selectedDate === today && attendance.startTime) {
      const currentTime = now.toTimeString().substring(0, 5); // HH:MM
      return currentTime < attendance.startTime;
    }
    
    return false;
  };

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

  // 전화번호로 학생 검색
  const searchStudentMutation = useMutation({
    mutationFn: (phoneLast4) => attendanceAPI.searchByPhone({ phoneLast4 }),
    onSuccess: (response) => {
      setSearchResults(response.data);
      if (response.data.length === 1) {
        setSelectedStudent(response.data[0]);
      }
    },
    onError: (error) => {
      alert(error.response?.data?.error || '학생을 찾을 수 없습니다.');
      setSearchResults([]);
    }
  });

  // 출석 체크인 mutation (전화번호로 통합)
  const checkInMutation = useMutation({
    mutationFn: ({ scheduleId, phoneLast4 }) => {
      return attendanceAPI.checkInByPhone({ 
        scheduleId,
        phoneLast4
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['attendances', selectedDate]);
      alert('출석 체크가 완료되었습니다!');
      setShowPhoneModal(false);
      setParentPhoneLast4('');
      setSearchResults([]);
      setSelectedStudent(null);
      setSelectedAttendance(null);
    },
    onError: (error) => {
      alert(`출석 체크 중 오류가 발생했습니다: ${error.response?.data?.message || error.message}`);
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

  // D/C 체크 업데이트
  const handleDcCheckUpdate = (attendanceId, dcCheck) => {
    if (attendanceId) {
      attendanceAPI.updateDcCheck(attendanceId, dcCheck)
        .then(() => {
          queryClient.invalidateQueries(['attendances', selectedDate]);
        })
        .catch(error => {
          console.error('D/C 업데이트 오류:', error);
        });
    }
  };

  // WR 체크 업데이트
  const handleWrCheckUpdate = (attendanceId, wrCheck) => {
    if (attendanceId) {
      attendanceAPI.updateWrCheck(attendanceId, wrCheck)
        .then(() => {
          queryClient.invalidateQueries(['attendances', selectedDate]);
        })
        .catch(error => {
          console.error('WR 업데이트 오류:', error);
        });
    }
  };

  // 추가 수업 토글
  const handleToggleClass = (attendanceId, classType) => {
    if (attendanceId) {
      const toggleFunction = {
        vocabulary: attendanceAPI.toggleVocabularyClass,
        grammar: attendanceAPI.toggleGrammarClass,
        phonics: attendanceAPI.togglePhonicsClass,
        speaking: attendanceAPI.toggleSpeakingClass
      }[classType];

      toggleFunction(attendanceId)
        .then(() => {
          queryClient.invalidateQueries(['attendances', selectedDate]);
        })
        .catch(error => {
          console.error(`${classType} 수업 토글 오류:`, error);
        });
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

  // 미래 날짜인지 확인하는 함수 (버튼 비활성화용)
  const isFutureDate = (attendance) => {
    const today = new Date().toISOString().split('T')[0];
    return selectedDate > today;
  };

  // 학생 출석 체크인 - 항상 모달 열기
  const handleStudentCheckIn = (attendance) => {
    if (attendance.checkInTime) return; // 이미 체크인된 경우
    
    // 미래 날짜면 체크인 불가
    if (isFutureDate(attendance)) return;
    
    // 항상 핸드폰 번호 확인 모달 열기
    setSelectedAttendance(attendance);
    setShowPhoneModal(true);
    setParentPhoneLast4('');
    setSearchResults([]);
    setSelectedStudent(null);
  };

  // 전화번호 검색
  const handlePhoneSearch = () => {
    if (!parentPhoneLast4 || parentPhoneLast4.length !== 4) {
      alert('부모님 핸드폰 번호 뒷자리 4자리를 정확히 입력해주세요.');
      return;
    }
    searchStudentMutation.mutate(parentPhoneLast4);
  };

  // 핸드폰 번호 확인 후 출석 체크
  const handlePhoneSubmit = () => {
    if (!selectedStudent) {
      alert('학생을 선택해주세요.');
      return;
    }

    if (!selectedAttendance) return;

    checkInMutation.mutate({
      scheduleId: selectedAttendance.scheduleId,
      phoneLast4: parentPhoneLast4
    });
  };

  // 모달 닫기
  const handleCloseModal = () => {
    setShowPhoneModal(false);
    setParentPhoneLast4('');
    setSearchResults([]);
    setSelectedStudent(null);
    setSelectedAttendance(null);
  };

  // 테이블 헤더 클릭 정렬 핸들러
  const handleTableSort = (sortType) => {
    if (tableSortBy === sortType) {
      // 같은 컬럼 클릭 시 정렬 순서 토글
      setTableSortOrder(tableSortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      // 다른 컬럼 클릭 시 해당 컬럼으로 오름차순 정렬
      setTableSortBy(sortType);
      setTableSortOrder('asc');
    }
  };

  // 출석 목록 정렬
  const sortedAttendances = (() => {
    const attendanceList = attendances ? [...attendances] : [];
    
    return attendanceList
      .sort((a, b) => {
        let result = 0;
        
        // 테이블 헤더 정렬이 우선
        if (tableSortBy === 'name') {
          result = a.studentName.localeCompare(b.studentName);
        } else if (tableSortBy === 'arrival') {
          // 등원 시간 정렬
          if (a.checkInTime && b.checkInTime) {
            result = new Date(a.checkInTime) - new Date(b.checkInTime);
          } else if (a.checkInTime && !b.checkInTime) {
            result = -1;
          } else if (!a.checkInTime && b.checkInTime) {
            result = 1;
          } else {
            // 둘 다 미출석이면 예약시간순
            result = (a.startTime || '').localeCompare(b.startTime || '');
          }
        } else if (tableSortBy === 'departure') {
          // 하원 시간 정렬: 출석한 학생 먼저, 실제 하원시간 기준
          const aCheckedIn = !!a.checkInTime;
          const bCheckedIn = !!b.checkInTime;
          
          if (aCheckedIn && !bCheckedIn) {
            result = -1;
          } else if (!aCheckedIn && bCheckedIn) {
            result = 1;
          } else if (a.checkOutTime && b.checkOutTime) {
            result = new Date(a.checkOutTime) - new Date(b.checkOutTime);
          } else if (a.checkOutTime && !b.checkOutTime) {
            result = -1;
          } else if (!a.checkOutTime && b.checkOutTime) {
            result = 1;
          } else {
            // 둘 다 미하원이면 예약시간순
            result = (a.startTime || '').localeCompare(b.startTime || '');
          }
        } else {
          // 기본: 수업 시간순
          const timeA = a.startTime || '';
          const timeB = b.startTime || '';
          
          if (timeA !== timeB) {
            result = timeA.localeCompare(timeB);
          } else {
            // 같은 수업 시간이면 이름 가나다순
            result = a.studentName.localeCompare(b.studentName);
          }
        }
        
        // 정렬 순서 적용
        return tableSortOrder === 'desc' ? -result : result;
      });
  })();

  // 테이블 필터링된 출석 목록
  const filteredAttendances = sortedAttendances.filter(attendance => 
    !tableSearchName || attendance.studentName?.includes(tableSearchName)
  );

  const formatTime = (timeString) => {
    if (!timeString) return '-';
    
    // DateTime 형식 처리 (2026-01-23T14:26:00)
    if (typeof timeString === 'string' && timeString.includes('T')) {
      try {
        const date = new Date(timeString);
        if (!isNaN(date.getTime())) {
          const hour = date.getHours();
          const minute = date.getMinutes();
          const isPM = hour >= 12;
          const displayHour = hour > 12 ? hour - 12 : (hour === 0 ? 12 : hour);
          const period = isPM ? '오후' : '오전';
          return `${period} ${displayHour}:${minute.toString().padStart(2, '0')}`;
        }
      } catch (error) {
        console.error('DateTime format error:', timeString, error);
      }
    }
    
    // LocalTime 형식 (HH:mm:ss 또는 HH:mm:ss.nnnnnnn) 처리
    if (typeof timeString === 'string' && timeString.includes(':')) {
      const parts = timeString.split(':');
      if (parts.length >= 2) {
        const hour = parseInt(parts[0]);
        const minute = parseInt(parts[1]);
        
        if (!isNaN(hour) && !isNaN(minute) && hour >= 0 && hour <= 23) {
          const isPM = hour >= 12;
          const displayHour = hour > 12 ? hour - 12 : (hour === 0 ? 12 : hour);
          const period = isPM ? '오후' : '오전';
          
          return `${period} ${displayHour}:${minute.toString().padStart(2, '0')}`;
        }
      }
    }
    
    return '-';
  };

  // 현재 시간이 예정 시간보다 이전인지 확인
  const isTimeBeforeExpected = (expectedTime) => {
    if (!expectedTime) return false;
    
    const now = new Date();
    const currentTime = now.getHours() * 60 + now.getMinutes();
    
    const timeStr = expectedTime.toString();
    const [hours, minutes] = timeStr.split(':').map(Number);
    const expectedMinutes = hours * 60 + minutes;
    
    return currentTime < expectedMinutes;
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
            <button 
              className="date-nav-btn"
              onClick={() => {
                const d = new Date(selectedDate);
                d.setDate(d.getDate() - 1);
                setSelectedDate(d.toISOString().split('T')[0]);
                setDateComponents({
                  year: d.getFullYear().toString(),
                  month: (d.getMonth() + 1).toString(),
                  day: d.getDate().toString()
                });
              }}
            >◀</button>
            <input
              type="date"
              value={selectedDate}
              onChange={(e) => {
                setSelectedDate(e.target.value);
                const d = new Date(e.target.value);
                setDateComponents({
                  year: d.getFullYear().toString(),
                  month: (d.getMonth() + 1).toString(),
                  day: d.getDate().toString()
                });
              }}
            />
            <button 
              className="date-nav-btn"
              onClick={() => {
                const d = new Date(selectedDate);
                d.setDate(d.getDate() + 1);
                setSelectedDate(d.toISOString().split('T')[0]);
                setDateComponents({
                  year: d.getFullYear().toString(),
                  month: (d.getMonth() + 1).toString(),
                  day: d.getDate().toString()
                });
              }}
            >▶</button>
            <button 
              className="today-btn"
              onClick={() => {
                const today = new Date();
                setSelectedDate(today.toISOString().split('T')[0]);
                setDateComponents({
                  year: today.getFullYear().toString(),
                  month: (today.getMonth() + 1).toString(),
                  day: today.getDate().toString()
                });
              }}
            >오늘</button>
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
                <span className="stat-value notyet">
                  {attendances?.filter(a => !a.checkInTime && a.status === 'NOTYET').length || 0}명
                </span>
              </div>
              <div className="stat">
                <span className="stat-label">결석</span>
                <span className="stat-value absent">
                  {attendances?.filter(a => !a.checkInTime && a.status === 'ABSENT').length || 0}명
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
                <th 
                  className={`sortable ${tableSortBy === 'name' ? 'active' : ''}`}
                  onClick={() => handleTableSort('name')}
                >
                  학생 이름
                  {tableSortBy === 'name' && (
                    <i className={`fas fa-sort-${tableSortOrder === 'asc' ? 'up' : 'down'}`}></i>
                  )}
                </th>
                <th>수업 시간</th>
                <th 
                  className={`sortable ${tableSortBy === 'arrival' ? 'active' : ''}`}
                  onClick={() => handleTableSort('arrival')}
                >
                  등원 시간
                  {tableSortBy === 'arrival' && (
                    <i className={`fas fa-sort-${tableSortOrder === 'asc' ? 'up' : 'down'}`}></i>
                  )}
                </th>
                <th>추가수업</th>
                <th 
                  className={`sortable ${tableSortBy === 'departure' ? 'active' : ''}`}
                  onClick={() => handleTableSort('departure')}
                >
                  하원 시간
                  {tableSortBy === 'departure' && (
                    <i className={`fas fa-sort-${tableSortOrder === 'asc' ? 'up' : 'down'}`}></i>
                  )}
                </th>
                <th>수업 완료</th>
                <th>D/C</th>
                <th>WR</th>
                <th>비고</th>
              </tr>
            </thead>
            <tbody>
              {filteredAttendances.map((attendance, index) => (
                <tr 
                  key={attendance.id || `attendance-${index}`}
                  className={`attendance-row ${getAttendanceRowClass(attendance)}`}
                >
                  <td className="student-name-td">
                    <div className="student-info">
                      <span className="name">{attendance.studentName}</span>
                      {attendance.isNaverBooking && (
                        <span className="naver-badge">네이버 예약</span>
                      )}
                      {!attendance.isNaverBooking && attendance.className && (
                        <span className="class-badge">{attendance.className}</span>
                      )}
                      <span className="status-badge">
                        {getAttendanceStatusText(attendance)}
                      </span>
                    </div>
                  </td>
                  <td className="class-time">
                    <div className="class-info">
                      <span className="course-name">{attendance.courseName}</span>
                      <span className="time-range">{formatTime(attendance.startTime)} - {formatTime(attendance.endTime)}</span>
                    </div>
                  </td>
                  <td className="check-in-time">
                    {attendance.checkInTime ? (
                      <div className="check-in-info">
                        <span>{formatTime(attendance.checkInTime)}</span>
                        {attendance.expectedLeaveTime && !attendance.checkOutTime && (
                          <span className="expected-leave-hint">
                            {formatTime(attendance.expectedLeaveTime)} 하원예정
                          </span>
                        )}
                      </div>
                    ) : '-'}
                  </td>
                  <td className="additional-classes">
                    {attendance.checkInTime && attendance.assignedClassInitials && 
                     attendance.status !== 'ABSENT' && attendance.status !== 'EXCUSED' ? (
                      <span>
                        {formatTime(attendance.additionalClassTime)}
                        <span className="class-initials"> ({attendance.assignedClassInitials})</span>
                      </span>
                    ) : (
                      '-'
                    )}
                  </td>
                  <td className="check-out-time">
                    {attendance.checkOutTime ? formatTime(attendance.checkOutTime) : '-'}
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
                  <td className="dc-check">
                    <input
                      type="text"
                      placeholder="D/C"
                      defaultValue={attendance.dcCheck || ''}
                      className="dc-input"
                      maxLength="10"
                      onBlur={(e) => handleDcCheckUpdate(attendance.id, e.target.value)}
                    />
                  </td>
                  <td className="wr-check">
                    <input
                      type="text"
                      placeholder="WR"
                      defaultValue={attendance.wrCheck || ''}
                      className="wr-input"
                      maxLength="10"
                      onBlur={(e) => handleWrCheckUpdate(attendance.id, e.target.value)}
                    />
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

      {/* 부모님 핸드폰 번호 입력 모달 */}
      {showPhoneModal && (
        <div className="modal-overlay" onClick={handleCloseModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>출석 체크</h3>
              <button className="modal-close" onClick={handleCloseModal}>
                <i className="fas fa-times"></i>
              </button>
            </div>
            <div className="modal-body">
              <div className="form-group">
                <label htmlFor="parentPhone">부모님 핸드폰 번호 뒷자리 4자리</label>
                <div style={{ display: 'flex', gap: '10px' }}>
                  <input
                    type="text"
                    id="parentPhone"
                    value={parentPhoneLast4}
                  onChange={(e) => {
                    const value = e.target.value.replace(/[^0-9]/g, '');
                    if (value.length <= 4) {
                      setParentPhoneLast4(value);
                      setSearchResults([]);
                      setSelectedStudent(null);
                    }
                  }}
                  placeholder="1234"
                  maxLength="4"
                  className="phone-input"
                  autoFocus
                  style={{ flex: 1 }}
                />
                <button 
                  className="btn btn-primary" 
                  onClick={handlePhoneSearch}
                  disabled={searchStudentMutation.isPending || parentPhoneLast4.length !== 4}
                >
                  {searchStudentMutation.isPending ? '검색중...' : '검색'}
                </button>
                </div>
              </div>
              
              {/* 검색 결과 */}
              {searchResults.length > 0 && (
                <div className="search-results" style={{ marginTop: '20px' }}>
                  <h4>검색 결과</h4>
                  {searchResults.map((result, index) => (
                    <div 
                      key={index}
                      className={`student-result ${selectedStudent === result ? 'selected' : ''}`}
                      onClick={() => setSelectedStudent(result)}
                      style={{
                        padding: '15px',
                        border: selectedStudent === result ? '2px solid #00c73c' : '1px solid #ddd',
                        borderRadius: '8px',
                        marginBottom: '10px',
                        cursor: 'pointer',
                        backgroundColor: selectedStudent === result ? '#f0fff4' : 'white'
                      }}
                    >
                      <div><strong>학생:</strong> {result.studentName}</div>
                      <div><strong>부모님:</strong> {result.parentName}</div>
                      <div><strong>전화번호:</strong> {result.parentPhone}</div>
                      {result.school && <div><strong>학교:</strong> {result.school}</div>}
                      {result.courseName && <div><strong>반:</strong> {result.courseName}</div>}
                      {result.isNaverBooking && <div style={{ color: '#00c73c' }}><strong>네이버 예약</strong></div>}
                    </div>
                  ))}
                </div>
              )}
            </div>
            <div className="modal-footer">
              <button 
                className="btn btn-secondary" 
                onClick={handleCloseModal}
                disabled={checkInMutation.isPending}
              >
                취소
              </button>
              <button 
                className="btn btn-primary" 
                onClick={handlePhoneSubmit}
                disabled={checkInMutation.isPending || !selectedStudent}
              >
                {checkInMutation.isPending ? '처리중...' : '출석 체크'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Attendance;
