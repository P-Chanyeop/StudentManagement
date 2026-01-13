import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { attendanceAPI, authAPI, scheduleAPI, reservationAPI, enrollmentAPI } from '../services/api';
import LoadingSpinner from '../components/LoadingSpinner';
import '../styles/ClassInfo.css';

function ClassInfo() {
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const navigate = useNavigate();

  // 사용자 프로필 조회
  const { data: profile, error: profileError } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
    retry: false,
    onError: (error) => {
      if (error.response?.status === 401) {
        localStorage.clear();
        navigate('/login');
      }
    }
  });

  const isParent = profile?.role === 'PARENT';
  const isTeacher = profile?.role === 'TEACHER';
  const isAdmin = profile?.role === 'ADMIN';

  // 학부모용 출석 데이터 조회 (학부모 계정에서만)
  const { data: attendanceData = [], isLoading: attendanceLoading, error: attendanceError } = useQuery({
    queryKey: ['parentAttendance', profile?.studentId],
    queryFn: async () => {
      if (!isParent || !profile?.studentId) {
        console.log('Attendance query skipped - not parent or no studentId');
        return [];
      }
      
      console.log('Fetching attendance data for studentId:', profile.studentId);
      
      try {
        const response = await attendanceAPI.getByStudent(profile.studentId);
        const attendances = response.data || [];
        
        console.log('Attendance API response:', response);
        console.log('Attendance data received:', attendances);
        console.log('First attendance detail:', attendances[0]);
        return attendances;
      } catch (error) {
        console.error('Error fetching attendance:', error);
        console.error('Error details:', error.response?.data);
        return [];
      }
    },
    enabled: isParent && !!profile?.studentId,
    refetchInterval: 5000,
    refetchOnWindowFocus: true,
    refetchOnMount: true,
    staleTime: 0,
    cacheTime: 0,
    refetchIntervalInBackground: true, // 백그라운드에서도 새로고침
  });
  
  // 디버깅용 로그
  console.log('=== ATTENDANCE DEBUG ===');
  console.log('Attendance query enabled:', isParent && !!profile?.studentId);
  console.log('isParent:', isParent);
  console.log('profile?.studentId:', profile?.studentId);
  console.log('attendanceLoading:', attendanceLoading);
  console.log('attendanceError:', attendanceError);
  console.log('attendanceData length:', attendanceData.length);
  console.log('========================');

  // 선택된 날짜의 수업 정보 조회 (역할별)
  const { data: classData = [], isLoading, error: classDataError } = useQuery({
    queryKey: ['classInfo', selectedDate, profile?.role],
    queryFn: async () => {
      if (!profile) return [];
      
      console.log('Fetching class data for:', { selectedDate, role: profile.role, studentId: profile.studentId });
      
      if (profile.role === 'PARENT') {
        const today = new Date().toISOString().split('T')[0];
        const isToday = selectedDate === today;
        const isFuture = selectedDate > today;
        
        if (isFuture) {
          // 미래 날짜: 스케줄 정보(수업 예정) + 예약 정보 모두 표시
          try {
            const results = [];
            
            // 1. 스케줄 정보 가져오기 (수업 예정)
            const allSchedulesResponse = await scheduleAPI.getByDate(selectedDate);
            const allSchedules = allSchedulesResponse.data || [];
            
            // 학생이 수강하는 코스의 스케줄만 필터링
            const enrollmentsResponse = await enrollmentAPI.getByStudent(profile.studentId);
            const enrollments = enrollmentsResponse.data || [];
            const activeEnrollments = enrollments.filter(e => e.isActive);
            const studentCourseIds = activeEnrollments.map(e => e.courseId);
            
            const studentSchedules = allSchedules.filter(schedule => 
              studentCourseIds.includes(schedule.courseId)
            );
            
            // 스케줄을 "수업 예정" 형태로 변환
            studentSchedules.forEach(schedule => {
              results.push({
                id: `schedule-${schedule.id}`,
                type: 'scheduled',
                courseName: schedule.courseName,
                courseLevel: schedule.courseLevel,
                startTime: schedule.startTime,
                endTime: schedule.endTime,
                teacherName: schedule.teacherName || '미배정',
                status: '수업 예정',
                student: { 
                  id: profile.studentId,
                  studentName: profile.name 
                }
              });
            });
            
            // 2. 예약 정보 가져오기
            const reservationsResponse = await reservationAPI.getByDate(selectedDate);
            const reservations = reservationsResponse.data || [];
            
            // 예약을 "예약" 형태로 변환
            reservations.forEach(reservation => {
              results.push({
                id: `reservation-${reservation.id}`,
                type: 'reservation',
                courseName: reservation.courseName,
                startTime: reservation.startTime,
                endTime: reservation.endTime,
                teacherName: '미배정',
                status: reservation.status,
                reservationStatus: reservation.status,
                memo: reservation.memo,
                student: reservation.student || { 
                  name: reservation.studentName, 
                  studentName: reservation.studentName 
                }
              });
            });
            
            console.log('Future date results (schedules + reservations):', results);
            return results;
          } catch (error) {
            console.error('Failed to fetch future date info:', error);
            return [];
          }
        } else {
          // 과거/현재 날짜: 학생의 수강권 기반으로 수업 정보 표시
          try {
            // 1. 학생의 활성 수강권 조회
            const enrollmentsResponse = await enrollmentAPI.getByStudent(profile.studentId);
            const enrollments = enrollmentsResponse.data || [];
            const activeEnrollments = enrollments.filter(e => e.isActive);
            
            console.log('Student active enrollments:', activeEnrollments);
            
            if (activeEnrollments.length === 0) {
              return [];
            }
            
            // 2. 해당 날짜의 모든 스케줄 조회
            console.log('Calling scheduleAPI.getByDate with:', selectedDate);
            const allSchedulesResponse = await scheduleAPI.getByDate(selectedDate);
            const allSchedules = allSchedulesResponse.data || [];
            
            console.log('API Response status:', allSchedulesResponse.status);
            console.log('API Response data:', allSchedulesResponse.data);
            console.log('All schedules for selected date:', selectedDate, allSchedules);
            
            // 3. 학생이 수강하는 코스의 스케줄만 필터링
            const studentCourseIds = activeEnrollments.map(e => e.courseId);
            const studentSchedules = allSchedules.filter(schedule => {
              console.log('Checking schedule courseId:', schedule.courseId, 'vs student courseIds:', studentCourseIds);
              return studentCourseIds.includes(schedule.courseId);
            });
            
            console.log('Student course IDs:', studentCourseIds);
            console.log('Filtered student schedules:', studentSchedules);
            
            // 4. 출석 데이터 가져오기
            const attendancesResponse = await attendanceAPI.getByStudent(profile.studentId);
            const attendances = attendancesResponse.data || [];
            
            // 5. 스케줄에 출석 정보와 수강권 정보 추가
            const schedulesWithData = studentSchedules.map(schedule => {
              const attendance = attendances.find(att => 
                att.schedule && att.schedule.id === schedule.id
              );
              
              const enrollment = activeEnrollments.find(e => e.courseId === schedule.courseId);
              
              return {
                ...schedule,
                attendance: attendance,
                enrollment: enrollment,
                type: 'schedule',
                student: { 
                  id: profile.studentId,
                  studentName: profile.name 
                }
              };
            });
            
            console.log('Final schedules with data:', schedulesWithData);
            return schedulesWithData;
          } catch (error) {
            console.error('Failed to fetch student class info:', error);
            return [];
          }
        }
      } else if (profile.role === 'TEACHER') {
        const response = await scheduleAPI.getMySchedules(selectedDate);
        return response.data;
      } else if (profile.role === 'ADMIN') {
        const response = await scheduleAPI.getAllSchedules(selectedDate);
        return response.data;
      }
      return [];
    },
    enabled: !!profile,
    retry: false
  });

  // 월별 스케줄 조회 (캘린더용)
  const { data: monthlySchedules = [] } = useQuery({
    queryKey: ['monthlySchedules', currentMonth.getFullYear(), currentMonth.getMonth(), profile?.role],
    queryFn: async () => {
      if (!profile) return [];
      
      const year = currentMonth.getFullYear();
      const month = currentMonth.getMonth() + 1;
      
      console.log('=== MONTHLY SCHEDULES DEBUG ===');
      console.log('Fetching monthly schedules for:', { year, month, role: profile.role });
      
      if (profile.role === 'PARENT') {
        try {
          // 학생의 활성 수강권 조회
          const enrollmentsResponse = await enrollmentAPI.getByStudent(profile.studentId);
          const enrollments = enrollmentsResponse.data || [];
          const activeEnrollments = enrollments.filter(e => e.isActive);
          
          console.log('Active enrollments:', activeEnrollments);
          
          if (activeEnrollments.length === 0) {
            console.log('No active enrollments found');
            return [];
          }
          
          // 해당 월의 모든 스케줄 조회
          const startDate = `${year}-${String(month).padStart(2, '0')}-01`;
          const endDate = `${year}-${String(month).padStart(2, '0')}-31`;
          console.log('Fetching schedules for range:', startDate, 'to', endDate);
          
          const allSchedulesResponse = await scheduleAPI.getByRange(startDate, endDate);
          const allSchedules = allSchedulesResponse.data || [];
          
          console.log('All schedules in range:', allSchedules.length);
          
          // 학생이 수강하는 코스의 스케줄만 필터링
          const studentCourseIds = activeEnrollments.map(e => e.courseId);
          const monthlyData = allSchedules.filter(schedule => 
            studentCourseIds.includes(schedule.courseId)
          );
          
          console.log('Student course IDs:', studentCourseIds);
          console.log('Filtered monthly schedules:', monthlyData.length);
          console.log('Monthly schedules data:', monthlyData);
          return monthlyData;
        } catch (error) {
          console.error('Failed to fetch monthly schedules:', error);
          return [];
        }
      } else if (profile.role === 'TEACHER') {
        const response = await scheduleAPI.getMyMonthlySchedules(year, month);
        return response.data;
      } else if (profile.role === 'ADMIN') {
        const response = await scheduleAPI.getAllMonthlySchedules(year, month);
        return response.data;
      }
      return [];
    },
    enabled: !!profile,
    retry: false
  });

  // 캘린더 렌더링
  const renderCalendar = () => {
    const year = currentMonth.getFullYear();
    const month = currentMonth.getMonth();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const firstDay = new Date(year, month, 1).getDay();
    
    const days = [];
    // 빈 칸 (이전 달)
    for (let i = 0; i < firstDay; i++) {
      const prevMonth = month === 0 ? 11 : month - 1;
      const prevYear = month === 0 ? year - 1 : year;
      const prevDaysInMonth = new Date(prevYear, prevMonth + 1, 0).getDate();
      const prevDay = prevDaysInMonth - firstDay + i + 1;
      const prevDate = new Date(prevYear, prevMonth, prevDay);
      // 시간대 문제 해결을 위해 로컬 날짜 문자열 생성
      const dateStr = `${prevYear}-${String(prevMonth + 1).padStart(2, '0')}-${String(prevDay).padStart(2, '0')}`;
      
      const hasSchedule = monthlySchedules.some(schedule => 
        schedule.scheduleDate === dateStr || schedule.date === dateStr
      );
      
      days.push(
        <div
          key={`prev-${i}`}
          className={`calendar-day other-month ${dateStr === selectedDate ? 'selected' : ''} ${hasSchedule ? 'has-schedule' : ''}`}
          onClick={() => setSelectedDate(dateStr)}
        >
          <span className="day-number">{prevDay}</span>
          {hasSchedule && <div className="schedule-indicator"></div>}
        </div>
      );
    }
    
    // 현재 달의 날짜들
    for (let day = 1; day <= daysInMonth; day++) {
      // 시간대 문제 해결을 위해 로컬 날짜 문자열 생성
      const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      
      const hasSchedule = monthlySchedules.some(schedule => 
        schedule.scheduleDate === dateStr || schedule.date === dateStr
      );

      // 학부모용 출석 상태 확인 (학부모 계정에서만, 오늘 이전 날짜만)
      let attendanceStatus = null;
      if (isParent && attendanceData.length > 0) {
        const today = new Date().toISOString().split('T')[0];
        
        // 오늘 이전 날짜만 출석 상태 표시
        if (dateStr <= today) {
          // 출석 데이터에서 직접 날짜 매칭 (최신 데이터 우선)
          const attendance = attendanceData
            .filter(att => {
              // 만약 checkInTime이 있으면 그 날짜로 매칭
              if (att.checkInTime) {
                const attDate = att.checkInTime.split('T')[0];
                return attDate === dateStr;
              }
              // checkInTime이 없으면 스케줄 ID로 월별 스케줄과 매칭
              const daySchedule = monthlySchedules.find(schedule => {
                const scheduleDate = schedule.scheduleDate || schedule.date;
                return scheduleDate === dateStr && schedule.id === att.scheduleId;
              });
              return !!daySchedule;
            })
            .sort((a, b) => b.id - a.id)[0]; // ID 내림차순으로 정렬해서 최신 데이터 선택
          
          if (attendance) {
            attendanceStatus = attendance.status;
            console.log('Found attendance for', dateStr, ':', attendanceStatus);
          }
        }
      }
      
      days.push(
        <div
          key={day}
          className={`calendar-day ${dateStr === selectedDate ? 'selected' : ''} ${attendanceStatus ? `attendance-${attendanceStatus.toLowerCase()}` : ''}`}
          onClick={() => setSelectedDate(dateStr)}
        >
          <span className="day-number">{day}</span>
          {!isParent && hasSchedule && <div className="schedule-indicator"></div>}
          {isParent && attendanceStatus && (
            <div className={`attendance-indicator ${attendanceStatus.toLowerCase()}`}>
              <div className="attendance-dot"></div>
            </div>
          )}
        </div>
      );
    }
    
    // 다음 달 날짜들 (6주 완성을 위해)
    const totalCells = Math.ceil((firstDay + daysInMonth) / 7) * 7;
    const remainingCells = totalCells - (firstDay + daysInMonth);
    
    for (let day = 1; day <= remainingCells; day++) {
      const nextMonth = month === 11 ? 0 : month + 1;
      const nextYear = month === 11 ? year + 1 : year;
      // 시간대 문제 해결을 위해 로컬 날짜 문자열 생성
      const dateStr = `${nextYear}-${String(nextMonth + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      
      const hasSchedule = monthlySchedules.some(schedule => 
        schedule.scheduleDate === dateStr || schedule.date === dateStr
      );
      
      days.push(
        <div
          key={`next-${day}`}
          className={`calendar-day other-month ${dateStr === selectedDate ? 'selected' : ''} ${hasSchedule ? 'has-schedule' : ''}`}
          onClick={() => setSelectedDate(dateStr)}
        >
          <span className="day-number">{day}</span>
          {hasSchedule && <div className="schedule-indicator"></div>}
        </div>
      );
    }

    return days;
  };

  // 시간 포맷팅
  const formatTime = (timeString) => {
    if (!timeString) return '-';
    return timeString.substring(0, 5); // HH:MM 형식
  };

  // 출석 상태별 배지
  const getStatusBadge = (status) => {
    const statusMap = {
      PRESENT: { text: '출석', color: '#03C75A' },
      LATE: { text: '지각', color: '#FFA500' },
      ABSENT: { text: '결석', color: '#FF0000' },
      EXCUSED: { text: '사유결석', color: '#0066FF' },
      EARLY_LEAVE: { text: '조퇴', color: '#9C27B0' },
      PENDING: { text: '예약대기', color: '#6c757d' },
      CONFIRMED: { text: '예약확정', color: '#28a745' },
      CANCELLED: { text: '취소됨', color: '#dc3545' },
    };
    const { text, color } = statusMap[status] || { text: status, color: '#999' };
    return <span className="status-badge" style={{ backgroundColor: color }}>{text}</span>;
  };

  // 페이지 제목 (역할별)
  const getPageTitle = () => {
    if (isParent) return '자녀 수업 정보';
    if (isTeacher) return '수업 스케줄';
    return '수업 스케줄';
  };

  const getPageSubtitle = () => {
    if (isParent) return '자녀의 수업 출석 현황을 확인합니다';
    if (isTeacher) return '담당 클래스 스케줄과 수업 현황을 관리합니다';
    return '전체 클래스 스케줄과 수업 현황을 관리합니다';
  };

  // 데이터 렌더링 (역할별)
  const renderClassData = () => {
    if (classData.length === 0) {
      return (
        <div className="empty-state">
          <i className="fas fa-calendar-times"></i>
          <h3>해당 날짜에 수업이 없습니다</h3>
          <p>다른 날짜를 선택해보세요.</p>
        </div>
      );
    }

    if (isParent) {
      return (
        <div className="schedule-list">
          {classData.map((schedule) => (
            <div key={schedule.id} className="schedule-card">
              <div className="card-header">
                <div className="course-info">
                  <h3>{schedule.courseName}</h3>
                  <span className="course-level">{schedule.courseLevel}</span>
                </div>
                {schedule.attendance && getStatusBadge(schedule.attendance.status)}
              </div>
              <div className="card-body">
                <div className="info-grid">
                  <div className="info-item">
                    <span className="label"><i className="fas fa-clock"></i>수업 시간</span>
                    <span className="value">{formatTime(schedule.startTime)} - {formatTime(schedule.endTime)}</span>
                  </div>
                  <div className="info-item">
                    <span className="label"><i className="fas fa-user-tie"></i>담당 강사</span>
                    <span className="value">{schedule.teacherName || '미배정'}</span>
                  </div>
                  <div className="info-item">
                    <span className="label"><i className="fas fa-users"></i>수강 인원</span>
                    <span className="value">{schedule.currentStudents || 0}/{schedule.maxStudents || '무제한'}</span>
                  </div>
                  {schedule.attendance && (
                    <>
                      <div className="info-item">
                        <span className="label"><i className="fas fa-sign-in-alt"></i>출석 시간</span>
                        <span className="value">{schedule.attendance.checkInTime ? formatDateTime(schedule.attendance.checkInTime) : '-'}</span>
                      </div>
                      <div className="info-item">
                        <span className="label"><i className="fas fa-sign-out-alt"></i>퇴실 시간</span>
                        <span className="value">{schedule.attendance.checkOutTime ? formatDateTime(schedule.attendance.checkOutTime) : '-'}</span>
                      </div>
                    </>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      );
    }

    // 관리자/선생님용 렌더링
    return (
      <div className="schedule-list">
        {classData.map((schedule) => (
          <div key={schedule.id} className="schedule-card">
            <div className="card-header">
              <div className="course-info">
                <h3>{schedule.course?.name || schedule.courseName || '수업'}</h3>
                <span className="time-info">{formatTime(schedule.startTime)} - {formatTime(schedule.endTime)}</span>
              </div>
              <span className="capacity-badge">{schedule.currentStudents || 0}/{schedule.maxCapacity || schedule.maxStudents || 0}</span>
            </div>
            <div className="card-body">
              {isTeacher && (
                <div className="teacher-info">
                  <span className="label"><i className="fas fa-user-tie"></i>담당 강사</span>
                  <span className="value">{schedule.teacherName || '미배정'}</span>
                </div>
              )}
              {schedule.currentStudents > 0 && (
                <div className="students-list">
                  <span className="label"><i className="fas fa-users"></i>수강생</span>
                  <div className="students-info">
                    <span className="student-count">{schedule.currentStudents}명 수강 중</span>
                  </div>
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
    );
  };

  if (profileError?.response?.status === 401) {
    return null; // 이미 리다이렉트 처리됨
  }

  if (isLoading) {
    return (
      <div className="page-wrapper">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="page-wrapper">
      {/* 페이지 헤더 */}
      <div className="page-header">
        <div className="page-header-content">
          <div className="page-title-section">
            <h1 className="page-title">
              <i className="fas fa-calendar-check"></i>
              {getPageTitle()}
            </h1>
            <p className="page-subtitle">{getPageSubtitle()}</p>
          </div>
        </div>
      </div>

      {/* 메인 컨텐츠 */}
      <div className="page-content">
        <div className="class-info-layout">
          {/* 캘린더 섹션 */}
          <div className="calendar-section">
            <div className="calendar-header">
              <button 
                className="nav-button"
                onClick={() => setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1))}
              >
                <i className="fas fa-chevron-left"></i>
              </button>
              <h3>{currentMonth.getFullYear()}년 {currentMonth.getMonth() + 1}월</h3>
              <button 
                className="nav-button"
                onClick={() => setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1))}
              >
                <i className="fas fa-chevron-right"></i>
              </button>
            </div>
            
            <div className="calendar-weekdays">
              <div className="weekday">일</div>
              <div className="weekday">월</div>
              <div className="weekday">화</div>
              <div className="weekday">수</div>
              <div className="weekday">목</div>
              <div className="weekday">금</div>
              <div className="weekday">토</div>
            </div>
            <div className="calendar-grid">
              {renderCalendar()}
            </div>
          </div>

          {/* 선택된 날짜 정보 섹션 */}
          <div className="selected-info-section">
            <div className="selected-date-header">
              <h2>{new Date(selectedDate).toLocaleDateString('ko-KR', { 
                year: 'numeric', 
                month: 'long', 
                day: 'numeric',
                weekday: 'long'
              })} 수업 현황</h2>
              <span className="count-badge">{classData.length}건</span>
            </div>

            {renderClassData()}
          </div>
        </div>
      </div>
    </div>
  );
}

export default ClassInfo;
