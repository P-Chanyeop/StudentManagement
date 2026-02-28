import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import LoadingSpinner from '../components/LoadingSpinner';
import { attendanceAPI, studentAPI, teacherAttendanceAPI } from '../services/api';
import { getLocalDateString, getTodayString } from '../utils/dateUtils';
import '../styles/Attendance.css';

function Attendance() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState('student');
  const [selectedDate, setSelectedDate] = useState(getTodayString());
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
  const [tableSortOrder, setTableSortOrder] = useState('asc');

  // 행 추가 모달 상태
  const [showAddModal, setShowAddModal] = useState(false);
  // 학생 상세 모달 상태
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [detailAttendance, setDetailAttendance] = useState(null);
  const [editCheckInTime, setEditCheckInTime] = useState('');
  const [editCheckOutTime, setEditCheckOutTime] = useState('');
  const [editActualCheckInTime, setEditActualCheckInTime] = useState('');
  const [editActualCheckOutTime, setEditActualCheckOutTime] = useState('');
  const [editExpectedLeaveTime, setEditExpectedLeaveTime] = useState('');
  const [hideCheckedOut, setHideCheckedOut] = useState(false);
  const [addType, setAddType] = useState('naver'); // 'naver' or 'system'
  const [addStudentSearch, setAddStudentSearch] = useState('');
  const [addSelectedStudent, setAddSelectedStudent] = useState(null);
  const [addStartTime, setAddStartTime] = useState('09:00');
  
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
    // 하원 완료된 학생은 gray out
    if (attendance.checkOutTime) return 'checked-out';

    // 상태가 있으면 상태에 따라 클래스 반환
    if (attendance.status) {
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
        case 'NOTYET':
          return 'notyet';
        default:
          return 'absent';
      }
    }
    
    // 상태가 없으면 미래 시간대 체크
    if (isFutureTime(attendance)) return 'waiting';
    
    return 'absent';
  };

  // 출석 상태 텍스트 반환
  const getAttendanceStatusText = (attendance) => {
    // 상태가 있으면 상태에 따라 표시
    if (attendance.status) {
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
    }
    
    // 상태가 없으면 미래 시간대 체크
    if (isFutureTime(attendance)) return '대기';
    
    return '미출석';
  };

  // 미래 시간대인지 확인하는 함수
  const isFutureTime = (attendance) => {
    const now = new Date();
    const today = getLocalDateString(now);
    
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

  // 엑셀 학생 목록 (네이버 예약용)
  const { data: excelStudents } = useQuery({
    queryKey: ['excelStudents'],
    queryFn: async () => {
      const response = await studentAPI.getExcelList();
      return response.data;
    },
  });

  // 선생님 출석 데이터
  const { data: teacherAttendances = [], isLoading: teacherLoading } = useQuery({
    queryKey: ['teacherAttendances', selectedDate],
    queryFn: async () => {
      const response = await teacherAttendanceAPI.getByDate(selectedDate);
      return response.data;
    },
    enabled: activeTab === 'teacher',
  });

  // 시간 수정 mutation
  const updateTimeMutation = useMutation({
    mutationFn: ({ attendanceId, startTime, endTime, checkInTime, checkOutTime, expectedLeaveTime }) =>
      attendanceAPI.updateTime(attendanceId, { startTime, endTime, checkInTime, checkOutTime, expectedLeaveTime }),
    onSuccess: () => {
      queryClient.invalidateQueries(['attendances', selectedDate]);
      alert('시간이 수정되었습니다.');
      setShowDetailModal(false);
    },
    onError: (error) => alert(error.response?.data?.message || '시간 수정 실패'),
  });

  // 출석 삭제 mutation
  const deleteMutation = useMutation({
    mutationFn: (id) => attendanceAPI.deleteAttendance(id),
    onSuccess: () => {
      queryClient.invalidateQueries(['attendances', selectedDate]);
      alert('삭제되었습니다.');
      setShowDetailModal(false);
    },
    onError: (error) => alert(error.response?.data?.message || '삭제 실패'),
  });

  // 수동 출석 추가 mutation
  const addManualMutation = useMutation({
    mutationFn: (data) => attendanceAPI.addManual(data),
    onSuccess: () => {
      queryClient.invalidateQueries(['attendances', selectedDate]);
      alert('출석부에 추가되었습니다.');
      setShowAddModal(false);
      setAddSelectedStudent(null);
      setAddStudentSearch('');
      setAddStartTime('09:00');
    },
    onError: (error) => {
      alert(error.response?.data?.message || '추가 중 오류가 발생했습니다.');
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
    return selectedDate > getTodayString();
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
      if (tableSortOrder === 'asc') {
        setTableSortOrder('desc');
      } else {
        // 내림차순 → 해제 (기본 정렬로)
        setTableSortBy('schedule');
        setTableSortOrder('asc');
      }
    } else {
      setTableSortBy(sortType);
      setTableSortOrder('asc');
    }
  };

  // 출석 목록 정렬
  const sortedAttendances = (() => {
    const attendanceList = attendances ? [...attendances] : [];
    
    return attendanceList
      .sort((a, b) => {
        // 출석한 학생 우선 (PRESENT, LATE → 나머지)
        const aPresent = a.checkInTime ? 0 : 1;
        const bPresent = b.checkInTime ? 0 : 1;
        if (aPresent !== bPresent) return aPresent - bPresent;

        let result = 0;
        
        // 테이블 헤더 정렬이 우선
        if (tableSortBy === 'name') {
          result = a.studentName.localeCompare(b.studentName);
        } else if (tableSortBy === 'arrival') {
          if (a.checkInTime && b.checkInTime) {
            result = new Date(a.checkInTime) - new Date(b.checkInTime);
          } else {
            result = (a.startTime || '').localeCompare(b.startTime || '');
          }
        } else if (tableSortBy === 'departure') {
          const aTime = a.expectedLeaveTime || '';
          const bTime = b.expectedLeaveTime || '';
          result = aTime.localeCompare(bTime);
        } else if (tableSortBy === 'actualDeparture') {
          if (a.checkOutTime && b.checkOutTime) {
            result = new Date(a.checkOutTime) - new Date(b.checkOutTime);
          } else if (a.checkOutTime && !b.checkOutTime) {
            result = -1;
          } else if (!a.checkOutTime && b.checkOutTime) {
            result = 1;
          } else {
            result = (a.startTime || '').localeCompare(b.startTime || '');
          }
        } else {
          // 기본: 수업 시간순
          const timeA = a.startTime || '';
          const timeB = b.startTime || '';
          if (timeA !== timeB) {
            result = timeA.localeCompare(timeB);
          } else {
            result = a.studentName.localeCompare(b.studentName);
          }
        }
        
        // 정렬 순서 적용
        return tableSortOrder === 'desc' ? -result : result;
      });
  })();

  // 테이블 필터링된 출석 목록
  const filteredAttendances = sortedAttendances.filter(attendance => 
    (!tableSearchName || attendance.studentName?.includes(tableSearchName)) &&
    (!hideCheckedOut || !attendance.checkOutTime)
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
        {/* 탭 */}
        <div className="att-tab-bar">
          <button className={`att-tab-btn ${activeTab === 'student' ? 'active' : ''}`} onClick={() => setActiveTab('student')}>
            <i className="fas fa-user-graduate"></i> 학생 출석부
          </button>
          <button className={`att-tab-btn ${activeTab === 'teacher' ? 'active' : ''}`} onClick={() => setActiveTab('teacher')}>
            <i className="fas fa-chalkboard-teacher"></i> 선생님 출석부
          </button>
        </div>

        {activeTab === 'student' ? (
        <div>
        <div className="attendance-controls">
          <div className="date-selector">
            <button 
              className="date-nav-btn"
              onClick={() => {
                const d = new Date(selectedDate);
                d.setDate(d.getDate() - 1);
                setSelectedDate(getLocalDateString(d));
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
                setSelectedDate(getLocalDateString(d));
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
                setSelectedDate(getLocalDateString(today));
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
            <button 
              className="add-attendance-btn"
              onClick={() => { setShowAddModal(true); setAddType('naver'); setAddSelectedStudent(null); setAddStudentSearch(''); }}
            >
              + 행 추가
            </button>
            <label className="hide-checkout-label" style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '14px', cursor: 'pointer', whiteSpace: 'nowrap' }}>
              <input
                type="checkbox"
                checked={hideCheckedOut}
                onChange={(e) => setHideCheckedOut(e.target.checked)}
              />
              하원 숨기기
            </label>
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
                  하원 예정
                  {tableSortBy === 'departure' && (
                    <i className={`fas fa-sort-${tableSortOrder === 'asc' ? 'up' : 'down'}`}></i>
                  )}
                </th>
                <th
                  className={`sortable ${tableSortBy === 'actualDeparture' ? 'active' : ''}`}
                  onClick={() => handleTableSort('actualDeparture')}
                >
                  실제 하원
                  {tableSortBy === 'actualDeparture' && (
                    <i className={`fas fa-sort-${tableSortOrder === 'asc' ? 'up' : 'down'}`}></i>
                  )}
                </th>
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
                  onClick={() => {
                    setDetailAttendance(attendance);
                    setEditCheckInTime(attendance.startTime || '');
                    setEditCheckOutTime(attendance.endTime || '');
                    // DateTime → HH:mm 변환 헬퍼
                    const toTimeStr = (dt) => {
                      if (!dt) return '';
                      if (typeof dt === 'string' && dt.includes('T')) {
                        return dt.substring(11, 16);
                      }
                      if (typeof dt === 'string' && dt.includes(':')) {
                        return dt.substring(0, 5);
                      }
                      return '';
                    };
                    setEditActualCheckInTime(toTimeStr(attendance.checkInTime));
                    setEditActualCheckOutTime(toTimeStr(attendance.checkOutTime));
                    setEditExpectedLeaveTime(attendance.expectedLeaveTime ? attendance.expectedLeaveTime.substring(0, 5) : '');
                    setShowDetailModal(true);
                  }}
                  style={{ cursor: 'pointer' }}
                >
                  <td className="student-name-td">
                    <div className="student-info">
                      <span className="name">{attendance.studentName}</span>
                      {(attendance.isNaverBooking || attendance.className === '네이버 예약') && (
                        <span className="naver-badge">네이버 예약</span>
                      )}
                      {!attendance.isNaverBooking && attendance.className && attendance.className !== '네이버 예약' && (
                        <span className={`naver-badge ${attendance.className === '관리자 예약' ? 'badge-manual' : 'badge-system'}`}>{attendance.className}</span>
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
                    {attendance.checkInTime && attendance.expectedLeaveTime ? formatTime(attendance.expectedLeaveTime) : '-'}
                  </td>
                  <td className="class-complete">
                    {attendance.checkOutTime ? formatTime(attendance.checkOutTime) : '-'}
                  </td>
                  <td className="dc-check" onClick={(e) => e.stopPropagation()}>
                    <input
                      type="text"
                      placeholder="D/C"
                      defaultValue={attendance.dcCheck || ''}
                      className="dc-input"
                      maxLength="10"
                      onBlur={(e) => handleDcCheckUpdate(attendance.id, e.target.value)}
                    />
                  </td>
                  <td className="wr-check" onClick={(e) => e.stopPropagation()}>
                    <input
                      type="text"
                      placeholder="WR"
                      defaultValue={attendance.wrCheck || ''}
                      className="wr-input"
                      maxLength="10"
                      onBlur={(e) => handleWrCheckUpdate(attendance.id, e.target.value)}
                    />
                  </td>
                  <td className="notes" onClick={(e) => e.stopPropagation()}>
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

      {/* 학생 상세 모달 */}
      {showDetailModal && detailAttendance && (
        <div className="modal-overlay" onClick={() => setShowDetailModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '500px' }}>
            <div className="modal-header">
              <h3>{detailAttendance.studentName} 상세</h3>
              <button className="modal-close" onClick={() => setShowDetailModal(false)}>
                <i className="fas fa-times"></i>
              </button>
            </div>
            <div className="modal-body">
              <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                <div><strong>반:</strong> {detailAttendance.courseName || '-'}</div>
                <div><strong>수업시간:</strong> {formatTime(detailAttendance.startTime)} - {formatTime(detailAttendance.endTime)}</div>
                <div><strong>상태:</strong> {getAttendanceStatusText(detailAttendance)}</div>

                <div className="form-group">
                  <label><strong>등원 시간</strong></label>
                  <input
                    type="time"
                    value={editActualCheckInTime}
                    onChange={(e) => {
                      const newVal = e.target.value;
                      setEditActualCheckInTime(newVal);
                      // 등원시간 변경 시 duration 기반 하원예정시간 자동 계산
                      if (newVal && detailAttendance) {
                        const [h, m] = newVal.split(':').map(Number);
                        const startParts = (detailAttendance.startTime || '').split(':').map(Number);
                        const endParts = (detailAttendance.endTime || '').split(':').map(Number);
                        let duration = 120;
                        if (startParts.length >= 2 && endParts.length >= 2) {
                          duration = (endParts[0] * 60 + endParts[1]) - (startParts[0] * 60 + startParts[1]);
                          if (duration <= 0) duration = 120;
                        }
                        const totalMin = h * 60 + m + duration;
                        const newH = Math.floor(totalMin / 60) % 24;
                        const newM = totalMin % 60;
                        setEditExpectedLeaveTime(`${String(newH).padStart(2, '0')}:${String(newM).padStart(2, '0')}`);
                      }
                    }}
                    className="phone-input"
                  />
                </div>
                <div className="form-group">
                  <label><strong>하원 예정 시간</strong></label>
                  <input
                    type="time"
                    value={editExpectedLeaveTime}
                    onChange={(e) => setEditExpectedLeaveTime(e.target.value)}
                    className="phone-input"
                  />
                </div>
                <div className="form-group">
                  <label><strong>하원 시간</strong></label>
                  <input
                    type="time"
                    value={editActualCheckOutTime}
                    onChange={(e) => setEditActualCheckOutTime(e.target.value)}
                    className="phone-input"
                  />
                </div>

                <hr style={{ border: 'none', borderTop: '1px solid #eee', margin: '4px 0' }} />

                <div className="form-group">
                  <label><strong>수업 시작 시간</strong></label>
                  <input
                    type="time"
                    value={editCheckInTime}
                    onChange={(e) => setEditCheckInTime(e.target.value)}
                    className="phone-input"
                  />
                </div>
                <div className="form-group">
                  <label><strong>수업 종료 시간</strong></label>
                  <input
                    type="time"
                    value={editCheckOutTime}
                    onChange={(e) => setEditCheckOutTime(e.target.value)}
                    className="phone-input"
                  />
                </div>

                <div className="form-group">
                  <label><strong>비고 (사유)</strong></label>
                  <textarea
                    defaultValue={detailAttendance.reason || ''}
                    onBlur={(e) => handleReasonBlur(detailAttendance.id, e.target.value)}
                    placeholder="결석/지각 사유 입력..."
                    style={{ width: '100%', minHeight: '80px', padding: '10px', borderRadius: '8px', border: '1px solid #ddd', resize: 'vertical', boxSizing: 'border-box' }}
                  />
                </div>
              </div>
            </div>
            <div className="modal-footer">
              {detailAttendance.className === '관리자 예약' && (
                <button
                  className="btn btn-danger"
                  style={{ marginRight: 'auto', background: '#ef4444', color: 'white' }}
                  onClick={() => {
                    if (window.confirm(`${detailAttendance.studentName} 행을 삭제하시겠습니까?`)) {
                      deleteMutation.mutate(detailAttendance.id);
                    }
                  }}
                  disabled={deleteMutation?.isPending}
                >
                  <i className="fas fa-trash"></i> 행 삭제
                </button>
              )}
              <button className="btn btn-secondary" onClick={() => setShowDetailModal(false)}>닫기</button>
              <button
                className="btn btn-primary"
                disabled={updateTimeMutation.isPending}
                onClick={() => {
                  updateTimeMutation.mutate({
                    attendanceId: detailAttendance.id,
                    startTime: editCheckInTime || null,
                    endTime: editCheckOutTime || null,
                    checkInTime: editActualCheckInTime || null,
                    checkOutTime: editActualCheckOutTime || null,
                    expectedLeaveTime: editExpectedLeaveTime || null,
                  });
                }}
              >
                {updateTimeMutation.isPending ? '저장중...' : '시간 저장'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 행 추가 모달 */}
      {showAddModal && (
        <div className="modal-overlay" onClick={() => setShowAddModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>출석부 행 추가</h3>
              <button className="modal-close" onClick={() => setShowAddModal(false)}>
                <i className="fas fa-times"></i>
              </button>
            </div>
            <div className="modal-body">
              <div className="form-group">
                <label>예약 유형</label>
                <div style={{ display: 'flex', gap: '10px', marginBottom: '15px' }}>
                  <button
                    className={`add-modal-type-btn ${addType === 'naver' ? 'active' : ''}`}
                    onClick={() => { setAddType('naver'); setAddSelectedStudent(null); setAddStudentSearch(''); }}
                  >
                    네이버 예약
                  </button>
                  <button
                    className={`add-modal-type-btn ${addType === 'system' ? 'active' : ''}`}
                    onClick={() => { setAddType('system'); setAddSelectedStudent(null); setAddStudentSearch(''); }}
                  >
                    시스템 학생
                  </button>
                </div>
              </div>

              <div className="form-group">
                <label>학생 검색</label>
                <input
                  type="text"
                  value={addStudentSearch}
                  onChange={(e) => setAddStudentSearch(e.target.value)}
                  placeholder="학생 이름 검색..."
                  className="phone-input"
                  autoFocus
                />
              </div>

              {addStudentSearch && (
                <div className="search-results" style={{ maxHeight: '200px', overflowY: 'auto', marginBottom: '15px' }}>
                  {addType === 'naver' 
                    ? (excelStudents || [])
                        .filter(s => s.studentName.includes(addStudentSearch))
                        .map((s, i) => (
                          <div key={i}
                            className={`student-result ${addSelectedStudent?.studentName === s.studentName ? 'selected' : ''}`}
                            onClick={() => setAddSelectedStudent(s)}
                            style={{
                              padding: '10px', border: addSelectedStudent?.studentName === s.studentName ? '2px solid #00c73c' : '1px solid #ddd',
                              borderRadius: '8px', marginBottom: '5px', cursor: 'pointer',
                              backgroundColor: addSelectedStudent?.studentName === s.studentName ? '#f0fff4' : 'white'
                            }}
                          >
                            <strong>{s.studentName}</strong> - {s.courseName}
                          </div>
                        ))
                    : (allStudents || [])
                        .filter(s => s.studentName?.includes(addStudentSearch))
                        .map((s, i) => (
                          <div key={i}
                            className={`student-result ${addSelectedStudent?.id === s.id ? 'selected' : ''}`}
                            onClick={() => setAddSelectedStudent(s)}
                            style={{
                              padding: '10px', border: addSelectedStudent?.id === s.id ? '2px solid #00c73c' : '1px solid #ddd',
                              borderRadius: '8px', marginBottom: '5px', cursor: 'pointer',
                              backgroundColor: addSelectedStudent?.id === s.id ? '#f0fff4' : 'white'
                            }}
                          >
                            <strong>{s.studentName}</strong> {s.parentPhone && `- ${s.parentPhone}`}
                          </div>
                        ))
                  }
                </div>
              )}

              {addSelectedStudent && (
                <div className="form-group">
                  <label>수업 시작 시간</label>
                  <input
                    type="time"
                    value={addStartTime}
                    onChange={(e) => setAddStartTime(e.target.value)}
                    className="phone-input"
                  />
                </div>
              )}
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setShowAddModal(false)}>취소</button>
              <button
                className="btn btn-primary"
                disabled={!addSelectedStudent || addManualMutation.isPending}
                onClick={() => {
                  const courseName = addType === 'naver' ? addSelectedStudent.courseName : null;
                  const durationMap = { Able: 60, Basic: 90, Core: 120, Development: 150 };
                  const duration = courseName ? (durationMap[courseName] || 120) : 120;
                  addManualMutation.mutate({
                    type: addType,
                    studentId: addType === 'system' ? addSelectedStudent.id : null,
                    studentName: addSelectedStudent.studentName,
                    date: selectedDate,
                    startTime: addStartTime,
                    durationMinutes: duration,
                    courseName: courseName,
                  });
                }}
              >
                {addManualMutation.isPending ? '추가중...' : '추가'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
    ) : (
      /* 선생님 출석부 탭 */
      <div className="tch-att-section">
        <div className="tch-att-controls">
          <div className="tch-att-date-selector">
            <button className="tch-att-nav-btn" onClick={() => {
              const d = new Date(selectedDate);
              d.setDate(d.getDate() - 1);
              setSelectedDate(getLocalDateString(d));
            }}>◀</button>
            <input type="date" value={selectedDate} onChange={(e) => setSelectedDate(e.target.value)} className="tch-att-date-input" />
            <button className="tch-att-nav-btn" onClick={() => {
              const d = new Date(selectedDate);
              d.setDate(d.getDate() + 1);
              setSelectedDate(getLocalDateString(d));
            }}>▶</button>
            <button className="tch-att-today-btn" onClick={() => setSelectedDate(getTodayString())}>오늘</button>
          </div>
          <div className="tch-att-actions">
            <button className="tch-att-action-btn" onClick={() => {
              const rows = teacherAttendances || [];
              if (rows.length === 0) return alert('데이터가 없습니다');
              const BOM = '\uFEFF';
              const header = '이름,출근시간,퇴근시간,근무시간,날짜\n';
              const csv = rows.map(r => {
                const inTime = r.checkInTime ? new Date(r.checkInTime).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }) : '';
                const outTime = r.checkOutTime ? new Date(r.checkOutTime).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }) : '';
                const inD = r.checkInTime ? new Date(r.checkInTime) : null;
                const outD = r.checkOutTime ? new Date(r.checkOutTime) : null;
                const mins = inD && outD ? Math.round((outD - inD) / 60000) : null;
                const work = mins !== null ? `${Math.floor(mins / 60)}시간 ${mins % 60}분` : '';
                return `${r.teacherName},${inTime},${outTime},${work},${r.attendanceDate}`;
              }).join('\n');
              const blob = new Blob([BOM + header + csv], { type: 'text/csv;charset=utf-8;' });
              const link = document.createElement('a');
              link.href = URL.createObjectURL(blob);
              link.download = `선생님출석부_${selectedDate}.csv`;
              link.click();
            }}>
              <i className="fas fa-file-excel"></i> 엑셀 다운로드
            </button>
          </div>
        </div>

        {teacherLoading ? <LoadingSpinner /> : (
          <div className="tch-att-table-container">
            <table className="tch-att-table">
              <thead>
                <tr>
                  <th>이름</th>
                  <th>출근 시간</th>
                  <th>퇴근 시간</th>
                  <th>근무 시간</th>
                  <th>상태</th>
                </tr>
              </thead>
              <tbody>
                {teacherAttendances.length === 0 ? (
                  <tr><td colSpan="5" className="tch-att-empty">
                    <i className="fas fa-calendar-times"></i>
                    <p>출근 기록이 없습니다</p>
                  </td></tr>
                ) : teacherAttendances.map((ta) => {
                  const inTime = ta.checkInTime ? new Date(ta.checkInTime) : null;
                  const outTime = ta.checkOutTime ? new Date(ta.checkOutTime) : null;
                  const workMinutes = inTime && outTime ? Math.round((outTime - inTime) / 60000) : null;
                  const workHours = workMinutes !== null ? `${Math.floor(workMinutes / 60)}시간 ${workMinutes % 60}분` : '-';
                  const status = outTime ? '퇴근' : inTime ? '근무중' : '-';
                  return (
                    <tr key={ta.id}>
                      <td style={{ fontWeight: 600 }}>{ta.teacherName}</td>
                      <td>{inTime ? inTime.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }) : '-'}</td>
                      <td>{outTime ? outTime.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' }) : '-'}</td>
                      <td>{workHours}</td>
                      <td><span className={`tch-att-status ${outTime ? 'done' : inTime ? 'working' : ''}`}>{status}</span></td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

      </div>
    )}
    </div>
    </div>
  );
}

export default Attendance;
