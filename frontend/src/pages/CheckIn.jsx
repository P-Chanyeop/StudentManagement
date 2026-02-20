import React, { useState, useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import './CheckIn.css';

const CheckIn = () => {
  const [phoneNumber, setPhoneNumber] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [currentTime, setCurrentTime] = useState(new Date());
  const [mode, setMode] = useState('checkIn');
  const [page, setPage] = useState(1); // 1: 번호입력, 2: 학생선택
  const queryClient = useQueryClient();

  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  const searchMutation = useMutation({
    mutationFn: async (phone) => {
      const studentRes = await axios.post('/api/attendances/search-by-phone', { phoneLast4: phone });
      const teacherRes = await axios.post('/api/teacher-attendance/search', { phoneLast4: phone });
      return { students: studentRes.data, teachers: teacherRes.data };
    },
    onSuccess: (data) => {
      let results = [];
      data.teachers.forEach(t => {
        results.push({ ...t, isTeacher: true, studentName: t.name + ' 선생님', courseName: '선생님' });
      });
      if (mode === 'checkOut') {
        results = [...results, ...data.students.filter(s => s.checkInTime && !s.checkOutTime)];
      } else {
        results = [...results, ...data.students.filter(s => !s.checkInTime)];
      }
      setSearchResults(results);
      setSelectedStudent(null);
      if (results.length > 0) {
        setPage(2);
      }
    },
    onError: () => {
      setSearchResults([]);
    }
  });

  const checkInMutation = useMutation({
    mutationFn: async (studentData) => {
      const isNaver = studentData.isNaverBooking || studentData.naverBooking;
      if (isNaver) {
        return axios.post(`/api/attendances/naver-booking/${studentData.naverBookingId}/check-in`);
      } else {
        return axios.post(`/api/attendances/${studentData.studentId}/check-in`);
      }
    },
    onSuccess: () => {
      alert('출석 체크가 완료되었습니다.');
      resetState();
      queryClient.invalidateQueries(['attendances']);
    },
    onError: (error) => {
      alert(error.response?.data?.message || '출석 체크 중 오류가 발생했습니다.');
      resetState();
    }
  });

  const checkOutMutation = useMutation({
    mutationFn: async (studentData) => {
      return axios.post(`/api/attendances/${studentData.attendanceId}/checkout`);
    },
    onSuccess: () => {
      alert('하원 체크가 완료되었습니다.');
      resetState();
      queryClient.invalidateQueries(['attendances']);
    },
    onError: (error) => {
      alert(error.response?.data?.message || '하원 체크 중 오류가 발생했습니다.');
      resetState();
    }
  });

  const teacherCheckInMutation = useMutation({
    mutationFn: async (teacherData) => {
      return axios.post('/api/teacher-attendance/check-in', { teacherId: teacherData.id });
    },
    onSuccess: (response) => {
      alert(response.data.message);
      resetState();
    },
    onError: (error) => {
      alert(error.response?.data?.message || '출근 체크 중 오류가 발생했습니다.');
      resetState();
    }
  });

  const teacherCheckOutMutation = useMutation({
    mutationFn: async (teacherData) => {
      return axios.post('/api/teacher-attendance/check-out', { teacherId: teacherData.id });
    },
    onSuccess: (response) => {
      alert(response.data.message);
      resetState();
    },
    onError: (error) => {
      alert(error.response?.data?.message || '퇴근 체크 중 오류가 발생했습니다.');
      resetState();
    }
  });

  const resetState = () => {
    setPhoneNumber('');
    setSearchResults([]);
    setSelectedStudent(null);
    setPage(1);
  };

  const handleNumberClick = (num) => {
    if (phoneNumber.length < 4) {
      const newPhone = phoneNumber + num;
      setPhoneNumber(newPhone);
      if (newPhone.length === 4) {
        searchMutation.mutate(newPhone);
      }
    }
  };

  const handleDelete = () => {
    setPhoneNumber(phoneNumber.slice(0, -1));
    setSearchResults([]);
    setSelectedStudent(null);
  };

  const handleClear = () => {
    resetState();
  };

  const handleStudentCheckIn = (student) => {
    if (student.isTeacher) {
      teacherCheckInMutation.mutate(student);
    } else {
      checkInMutation.mutate(student);
    }
  };

  const handleStudentCheckOut = (student) => {
    if (student.isTeacher) {
      teacherCheckOutMutation.mutate(student);
    } else {
      checkOutMutation.mutate(student);
    }
  };

  const handleModeChange = (newMode) => {
    setMode(newMode);
    resetState();
  };

  const days = ['일', '월', '화', '수', '목', '금', '토'];
  const dateStr = `${currentTime.getFullYear()}년 ${currentTime.getMonth() + 1}월 ${currentTime.getDate()}일 ${days[currentTime.getDay()]}요일`;
  const timeStr = `${currentTime.getHours() >= 12 ? 'PM' : 'AM'} ${String(currentTime.getHours() % 12 || 12).padStart(2, '0')}:${String(currentTime.getMinutes()).padStart(2, '0')}:${String(currentTime.getSeconds()).padStart(2, '0')}`;

  return (
    <div className="checkin-fullscreen">
      <div className={`checkin-page checkin-page1 ${page === 1 ? 'active' : 'exit'}`}>
        <div className="checkin-left">
          <h1 className="academy-name">리틀베어 리딩클럽</h1>
          <div className="mode-tabs">
            <button className={`mode-tab ${mode === 'checkIn' ? 'active' : ''}`} onClick={() => handleModeChange('checkIn')}>등원</button>
            <button className={`mode-tab ${mode === 'checkOut' ? 'active' : ''}`} onClick={() => handleModeChange('checkOut')}>하원</button>
          </div>
          <div className="datetime-center">
            <div className="date">{dateStr}</div>
            <div className="time">{timeStr}</div>
          </div>
          <div className="left-content">
            <p className="checkin-instruction">
              <span className="highlight">부모님 휴대폰번호 뒤 4자리</span>를 입력해주세요.
            </p>
            <div className="digit-boxes">
              {[0, 1, 2, 3].map((i) => (
                <div key={i} className={`digit-box ${phoneNumber[i] ? 'filled' : ''}`}>
                  {phoneNumber[i] ? '*' : ''}
                </div>
              ))}
            </div>
            {searchResults.length === 0 && phoneNumber.length === 4 && !searchMutation.isPending && (
              <div className="no-results">
                {mode === 'checkIn' ? '출석 가능한 학생이 없습니다.' : '하원 가능한 학생이 없습니다.'}
              </div>
            )}
          </div>
        </div>
        <div className="checkin-right">
          <div className="number-pad">
            {[1, 2, 3, 4, 5, 6, 7, 8, 9, '←', 0, 'C'].map((num) => (
              <button
                key={num}
                className={`number-btn ${num === '←' || num === 'C' ? 'func-btn' : ''}`}
                onClick={() => {
                  if (num === '←') handleDelete();
                  else if (num === 'C') handleClear();
                  else handleNumberClick(num.toString());
                }}
              >
                {num}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className={`checkin-page checkin-page2 ${page === 2 ? 'active' : ''}`}>
        <div className="checkin-left page2-left">
          <h1 className="academy-name">리틀베어 리딩클럽</h1>
          <div className="page2-guide">
            <p className="page2-title">출석할 학생을 선택해주세요.</p>
            <p className="page2-sub">수업종료(하원)시 하원탭에서<br/>번호를 다시 입력해주세요.</p>
            <button className="page2-back-btn" onClick={resetState}>
              ← 다시 입력하기
            </button>
          </div>
        </div>
        <div className="checkin-right page2-right">
          <div className="student-card-list">
            {searchResults.map((result, index) => (
              <div key={index} className="student-card" style={{ animationDelay: `${index * 0.08}s` }}>
                <div className="student-card-info">
                  <div className="student-card-name">{result.studentName}</div>
                  <div className="student-card-class">{result.courseName || result.school || ''}</div>
                </div>
                <div className="student-card-actions">
                  <button
                    className={`card-btn ${mode === 'checkIn' ? 'card-btn-checkin' : 'card-btn-checkout'}`}
                    onClick={() => mode === 'checkIn' ? handleStudentCheckIn(result) : handleStudentCheckOut(result)}
                    disabled={checkInMutation.isPending || checkOutMutation.isPending || teacherCheckInMutation.isPending || teacherCheckOutMutation.isPending}
                  >
                    {result.isTeacher
                      ? (mode === 'checkIn' ? '출근' : '퇴근')
                      : (mode === 'checkIn' ? '출석체크' : '하원체크')}
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );

  // 페이지 1: 번호 입력
  if (page === 1) {
    return (
      <div className="checkin-fullscreen">
        <div className="checkin-left">
          <h1 className="academy-name">리틀베어 리딩클럽</h1>
          <div className="mode-tabs">
            <button className={`mode-tab ${mode === 'checkIn' ? 'active' : ''}`} onClick={() => handleModeChange('checkIn')}>등원</button>
            <button className={`mode-tab ${mode === 'checkOut' ? 'active' : ''}`} onClick={() => handleModeChange('checkOut')}>하원</button>
          </div>
          <div className="datetime-center">
            <div className="date">{dateStr}</div>
            <div className="time">{timeStr}</div>
          </div>
          <div className="left-content">
            <p className="checkin-instruction">
              <span className="highlight">부모님 휴대폰번호 뒤 4자리</span>를 입력해주세요.
            </p>
            <div className="digit-boxes">
              {[0, 1, 2, 3].map((i) => (
                <div key={i} className={`digit-box ${phoneNumber[i] ? 'filled' : ''}`}>
                  {phoneNumber[i] ? '*' : ''}
                </div>
              ))}
            </div>
            {searchResults.length === 0 && phoneNumber.length === 4 && !searchMutation.isPending && (
              <div className="no-results">
                {mode === 'checkIn' ? '출석 가능한 학생이 없습니다.' : '하원 가능한 학생이 없습니다.'}
              </div>
            )}
          </div>
        </div>
        <div className="checkin-right">
          <div className="number-pad">
            {[1, 2, 3, 4, 5, 6, 7, 8, 9, '←', 0, 'C'].map((num) => (
              <button
                key={num}
                className={`number-btn ${num === '←' || num === 'C' ? 'func-btn' : ''}`}
                onClick={() => {
                  if (num === '←') handleDelete();
                  else if (num === 'C') handleClear();
                  else handleNumberClick(num.toString());
                }}
              >
                {num}
              </button>
            ))}
          </div>
        </div>
      </div>
    );
  }

  // 페이지 2: 학생 선택
  return (
    <div className="checkin-fullscreen">
      <div className="checkin-left page2-left">
        <h1 className="academy-name">리틀베어 리딩클럽</h1>
        <div className="page2-guide">
          <p className="page2-title">출석할 학생을 선택해주세요.</p>
          <p className="page2-sub">수업종료(하원)시 하원탭에서<br/>번호를 다시 입력해주세요.</p>
          <button className="page2-back-btn" onClick={resetState}>
            ← 다시 입력하기
          </button>
        </div>
      </div>
      <div className="checkin-right page2-right">
        <div className="student-card-list">
          {searchResults.map((result, index) => (
            <div key={index} className="student-card">
              <div className="student-card-info">
                <div className="student-card-name">{result.studentName}</div>
                <div className="student-card-class">{result.courseName || result.school || ''}</div>
              </div>
              <div className="student-card-actions">
                <button
                  className={`card-btn ${mode === 'checkIn' ? 'card-btn-checkin' : 'card-btn-checkout'}`}
                  onClick={() => mode === 'checkIn' ? handleStudentCheckIn(result) : handleStudentCheckOut(result)}
                  disabled={checkInMutation.isPending || checkOutMutation.isPending || teacherCheckInMutation.isPending || teacherCheckOutMutation.isPending}
                >
                  {result.isTeacher
                    ? (mode === 'checkIn' ? '출근' : '퇴근')
                    : (mode === 'checkIn' ? '출석체크' : '하원체크')}
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default CheckIn;
