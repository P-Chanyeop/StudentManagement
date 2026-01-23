import React, { useState, useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import './CheckIn.css';

const CheckIn = () => {
  const [phoneNumber, setPhoneNumber] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [currentTime, setCurrentTime] = useState(new Date());
  const queryClient = useQueryClient();

  useEffect(() => {
    const timer = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  const searchMutation = useMutation({
    mutationFn: async (phone) => {
      const response = await axios.post('/api/attendances/search-by-phone', { phoneLast4: phone });
      return response.data;
    },
    onSuccess: (data) => {
      setSearchResults(data);
      setSelectedStudent(null);
    },
    onError: () => {
      setSearchResults([]);
    }
  });

  const checkInMutation = useMutation({
    mutationFn: async (studentData) => {
      // isNaverBooking 또는 naverBooking 둘 다 체크 (Java boolean getter 직렬화 이슈)
      const isNaver = studentData.isNaverBooking || studentData.naverBooking;
      if (isNaver) {
        return axios.post(`/api/attendances/naver-booking/${studentData.naverBookingId}/check-in`);
      } else {
        return axios.post(`/api/attendances/${studentData.studentId}/check-in`);
      }
    },
    onSuccess: () => {
      alert('출석 체크가 완료되었습니다.');
      setPhoneNumber('');
      setSearchResults([]);
      setSelectedStudent(null);
      queryClient.invalidateQueries(['attendances']);
    },
    onError: (error) => {
      alert(error.response?.data?.message || '출석 체크 중 오류가 발생했습니다.');
      setPhoneNumber('');
      setSearchResults([]);
      setSelectedStudent(null);
    }
  });

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
    setPhoneNumber('');
    setSearchResults([]);
    setSelectedStudent(null);
  };

  const handleCheckIn = () => {
    if (!selectedStudent) return;
    checkInMutation.mutate(selectedStudent);
  };

  const days = ['일', '월', '화', '수', '목', '금', '토'];
  const dateStr = `${currentTime.getFullYear()}년 ${currentTime.getMonth() + 1}월 ${currentTime.getDate()}일 ${days[currentTime.getDay()]}요일`;
  const timeStr = `${currentTime.getHours() >= 12 ? 'PM' : 'AM'} ${String(currentTime.getHours() % 12 || 12).padStart(2, '0')}:${String(currentTime.getMinutes()).padStart(2, '0')}:${String(currentTime.getSeconds()).padStart(2, '0')}`;

  return (
    <div className="checkin-fullscreen">
      {/* 왼쪽: 입력 영역 */}
      <div className="checkin-left">
        <h1 className="academy-name">리딩베어 리딩클럽</h1>
        
        <div className="left-content">
          <p className="checkin-instruction">
            <span className="highlight">휴대폰번호 뒤 4자리</span>를 입력해주세요.
          </p>
          <p className="checkin-sub">휴대폰이 없는 경우 부모님 휴대폰번호를 입력해주세요.</p>

          <div className="digit-boxes">
            {[0, 1, 2, 3].map((i) => (
              <div key={i} className={`digit-box ${phoneNumber[i] ? 'filled' : ''}`}>
                {phoneNumber[i] ? '*' : ''}
              </div>
            ))}
          </div>

          {searchResults.length > 0 && (
            <div className="search-results">
              {searchResults.map((result, index) => (
                <div
                  key={index}
                  className={`result-card ${selectedStudent === result ? 'selected' : ''}`}
                  onClick={() => setSelectedStudent(result)}
                >
                  <span className="student-name">{result.studentName}</span>
                  <span className="student-info">{result.courseName || result.school}</span>
                </div>
              ))}
              {selectedStudent && (
                <button
                  className="checkin-confirm-btn"
                  onClick={handleCheckIn}
                  disabled={checkInMutation.isPending}
                >
                  출석 체크
                </button>
              )}
            </div>
          )}
        </div>

        <div className="datetime">
          <div className="date">{dateStr}</div>
          <div className="time">{timeStr}</div>
        </div>
      </div>

      {/* 오른쪽: 숫자 키패드 */}
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
};

export default CheckIn;
