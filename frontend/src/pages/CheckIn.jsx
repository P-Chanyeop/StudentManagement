import React, { useState, useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import './CheckIn.css';

const CheckIn = () => {
  const [phoneNumber, setPhoneNumber] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [currentTime, setCurrentTime] = useState(new Date());
  const [page, setPage] = useState(1);
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
        const checkedIn = !!t.checkInTime;
        const checkedOut = !!t.checkOutTime;
        if (!checkedOut) {
          results.push({ ...t, isTeacher: true, studentName: t.name + ' 선생님', courseName: '선생님', needsCheckIn: !checkedIn, needsCheckOut: checkedIn });
        }
      });
      data.students.forEach(s => {
        if (s.checkInTime && !s.checkOutTime) {
          results.push({ ...s, needsCheckIn: false, needsCheckOut: true });
        } else if (!s.checkInTime) {
          results.push({ ...s, needsCheckIn: true, needsCheckOut: false });
        }
      });
      setSearchResults(results);
      if (results.length > 0) setPage(2);
    },
    onError: () => setSearchResults([])
  });

  const checkInMutation = useMutation({
    mutationFn: async (studentData) => {
      if (studentData.isManualExcel && studentData.attendanceId) {
        return axios.post(`/api/attendances/${studentData.attendanceId}/manual-check-in`);
      }
      const isNaver = studentData.isNaverBooking || studentData.naverBooking;
      if (isNaver) {
        return axios.post(`/api/attendances/naver-booking/${studentData.naverBookingId}/check-in`);
      } else {
        return axios.post(`/api/attendances/${studentData.studentId}/check-in`);
      }
    },
    onSuccess: () => { alert('등원 체크가 완료되었습니다.'); resetState(); queryClient.invalidateQueries(['attendances']); },
    onError: (error) => { alert(error.response?.data?.message || '등원 체크 중 오류가 발생했습니다.'); resetState(); }
  });

  const checkOutMutation = useMutation({
    mutationFn: async (studentData) => axios.post(`/api/attendances/${studentData.attendanceId}/checkout`),
    onSuccess: () => { alert('하원 체크가 완료되었습니다.'); resetState(); queryClient.invalidateQueries(['attendances']); },
    onError: (error) => { alert(error.response?.data?.message || '하원 체크 중 오류가 발생했습니다.'); resetState(); }
  });

  const teacherCheckInMutation = useMutation({
    mutationFn: async (teacherData) => axios.post('/api/teacher-attendance/check-in', { teacherId: teacherData.id }),
    onSuccess: (response) => { alert(response.data.message); resetState(); },
    onError: (error) => { alert(error.response?.data?.message || '출근 체크 중 오류가 발생했습니다.'); resetState(); }
  });

  const teacherCheckOutMutation = useMutation({
    mutationFn: async (teacherData) => axios.post('/api/teacher-attendance/check-out', { teacherId: teacherData.id }),
    onSuccess: (response) => { alert(response.data.message); resetState(); },
    onError: (error) => { alert(error.response?.data?.message || '퇴근 체크 중 오류가 발생했습니다.'); resetState(); }
  });

  const resetState = () => { setPhoneNumber(''); setSearchResults([]); setPage(1); };

  const handleNumberClick = (num) => {
    if (phoneNumber.length < 4) {
      const newPhone = phoneNumber + num;
      setPhoneNumber(newPhone);
      if (newPhone.length === 4) searchMutation.mutate(newPhone);
    }
  };

  const handleDelete = () => { setPhoneNumber(phoneNumber.slice(0, -1)); setSearchResults([]); };

  const handleAction = (result) => {
    if (result.isTeacher) {
      result.needsCheckIn ? teacherCheckInMutation.mutate(result) : teacherCheckOutMutation.mutate(result);
    } else {
      result.needsCheckIn ? checkInMutation.mutate(result) : checkOutMutation.mutate(result);
    }
  };

  const isLoading = checkInMutation.isPending || checkOutMutation.isPending || teacherCheckInMutation.isPending || teacherCheckOutMutation.isPending;

  const days = ['일', '월', '화', '수', '목', '금', '토'];
  const dateStr = `${currentTime.getFullYear()}년 ${currentTime.getMonth() + 1}월 ${currentTime.getDate()}일 ${days[currentTime.getDay()]}요일`;
  const timeStr = `${currentTime.getHours() >= 12 ? 'PM' : 'AM'} ${String(currentTime.getHours() % 12 || 12).padStart(2, '0')}:${String(currentTime.getMinutes()).padStart(2, '0')}:${String(currentTime.getSeconds()).padStart(2, '0')}`;

  return (
    <div className="checkin-fullscreen">
      <div className={`checkin-page checkin-page1 ${page === 1 ? 'active' : 'exit'}`}>
        <div className="checkin-left">
          <h1 className="academy-name">리틀베어 리딩클럽</h1>
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
              <div className="no-results">등록된 학생이 없습니다.</div>
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
                  else if (num === 'C') resetState();
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
            <p className="page2-title">이름을 확인하고 버튼을 눌러주세요.</p>
            <button className="page2-back-btn" onClick={resetState}>
              ← 다시 입력하기
            </button>
          </div>
        </div>
        <div className="checkin-right page2-right page2-split">
          <div className="ci-half ci-half--in">
            <div className="ci-half__label ci-half__label--in">🏫 학원 왔을때</div>
            <div className="ci-half__cards">
              {searchResults.filter(r => r.needsCheckIn).length === 0
                ? <div className="ci-half__empty">등원 대상이 없습니다</div>
                : searchResults.filter(r => r.needsCheckIn).map((result, index) => (
                <div key={'in-' + index} className="student-card" style={{ animationDelay: `${index * 0.08}s` }}>
                  <div className="student-card-info">
                    <div className="student-card-name">{result.studentName}</div>
                    <div className="student-card-class">{result.courseName || result.school || ''}</div>
                  </div>
                  <div className="student-card-actions">
                    <button
                      className="card-btn card-btn-checkin"
                      onClick={() => handleAction(result)}
                      disabled={isLoading}
                    >
                      {result.isTeacher ? '출근' : '등원'}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
          <div className="ci-half ci-half--out">
            <div className="ci-half__label ci-half__label--out">🏠 집에 갈때</div>
            <div className="ci-half__cards">
              {searchResults.filter(r => r.needsCheckOut).length === 0
                ? <div className="ci-half__empty">하원 대상이 없습니다</div>
                : searchResults.filter(r => r.needsCheckOut).map((result, index) => (
                <div key={'out-' + index} className="student-card" style={{ animationDelay: `${index * 0.08}s` }}>
                  <div className="student-card-info">
                    <div className="student-card-name">{result.studentName}</div>
                    <div className="student-card-class">{result.courseName || result.school || ''}</div>
                  </div>
                  <div className="student-card-actions">
                    <button
                      className="card-btn card-btn-checkout"
                      onClick={() => handleAction(result)}
                      disabled={isLoading}
                    >
                      {result.isTeacher ? '퇴근' : '하원'}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CheckIn;
