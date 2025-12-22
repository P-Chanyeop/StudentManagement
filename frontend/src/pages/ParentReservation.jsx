import { useState, useEffect } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import LoadingSpinner from '../components/LoadingSpinner';
import { reservationAPI, scheduleAPI, enrollmentAPI, studentAPI } from '../services/api';
import '../styles/ParentReservation.css';

function ParentReservation() {
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
  const [selectedStudent, setSelectedStudent] = useState('');
  const [selectedSchedule, setSelectedSchedule] = useState(null);

  // 학생 목록 조회 (학부모는 자신의 자녀만)
  const { data: students = [] } = useQuery({
    queryKey: ['students'],
    queryFn: async () => {
      const response = await studentAPI.getAll();
      return response.data;
    },
  });

  // 선택된 학생의 활성 수강권 조회
  const { data: enrollments = [] } = useQuery({
    queryKey: ['enrollments', selectedStudent],
    queryFn: async () => {
      if (!selectedStudent) return [];
      const response = await enrollmentAPI.getActiveByStudent(selectedStudent);
      return response.data;
    },
    enabled: !!selectedStudent,
  });

  // 날짜별 예약 가능한 스케줄 조회
  const { data: availableSchedules = [], isLoading } = useQuery({
    queryKey: ['available-schedules', selectedDate, selectedStudent],
    queryFn: async () => {
      if (!selectedDate || !selectedStudent) return [];
      const response = await scheduleAPI.getAvailableByDate(selectedDate);
      return response.data;
    },
    enabled: !!selectedDate && !!selectedStudent,
  });

  // 예약 생성 mutation
  const createReservation = useMutation({
    mutationFn: (data) => reservationAPI.create(data),
    onSuccess: () => {
      alert('예약이 완료되었습니다!');
      setSelectedSchedule(null);
    },
    onError: (error) => {
      alert(`예약 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    },
  });

  const handleReservation = (schedule) => {
    if (!selectedStudent) {
      alert('학생을 선택해주세요.');
      return;
    }

    const activeEnrollment = enrollments.find(e => 
      e.courseId === schedule.courseId && e.remainingCount > 0
    );

    if (!activeEnrollment) {
      alert('해당 수업의 활성 수강권이 없습니다.');
      return;
    }

    if (window.confirm(`${schedule.courseName} 수업을 예약하시겠습니까?\n시간: ${schedule.startTime} - ${schedule.endTime}`)) {
      createReservation.mutate({
        studentId: selectedStudent,
        scheduleId: schedule.id,
        enrollmentId: activeEnrollment.id,
        reservationSource: 'WEB'
      });
    }
  };

  // 오늘 이후 날짜만 선택 가능
  const today = new Date().toISOString().split('T')[0];
  const maxDate = new Date();
  maxDate.setMonth(maxDate.getMonth() + 2); // 2개월 후까지
  const maxDateStr = maxDate.toISOString().split('T')[0];

  if (isLoading) return <LoadingSpinner />;

  return (
    <div className="parent-reservation-container">
      <div className="reservation-header">
        <h1>수업 예약</h1>
        <p>원하는 날짜와 시간을 선택하여 수업을 예약하세요</p>
      </div>

      <div className="reservation-filters">
        <div className="filter-group">
          <label>학생 선택</label>
          <select 
            value={selectedStudent} 
            onChange={(e) => setSelectedStudent(e.target.value)}
          >
            <option value="">학생을 선택하세요</option>
            {students.map(student => (
              <option key={student.id} value={student.id}>
                {student.name}
              </option>
            ))}
          </select>
        </div>

        <div className="filter-group">
          <label>날짜 선택</label>
          <input
            type="date"
            value={selectedDate}
            min={today}
            max={maxDateStr}
            onChange={(e) => setSelectedDate(e.target.value)}
          />
        </div>
      </div>

      {selectedStudent && enrollments.length > 0 && (
        <div className="active-enrollments">
          <h3>보유 수강권</h3>
          <div className="enrollment-cards">
            {enrollments.map(enrollment => (
              <div key={enrollment.id} className="enrollment-card">
                <div className="course-name">{enrollment.courseName}</div>
                <div className="remaining-count">
                  남은 횟수: <span className="count">{enrollment.remainingCount}회</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {selectedStudent && selectedDate && (
        <div className="available-schedules">
          <h3>{new Date(selectedDate).toLocaleDateString()} 예약 가능한 수업</h3>
          
          {availableSchedules.length === 0 ? (
            <div className="no-schedules">
              선택한 날짜에 예약 가능한 수업이 없습니다.
            </div>
          ) : (
            <div className="schedule-grid">
              {availableSchedules.map(schedule => {
                const hasEnrollment = enrollments.some(e => 
                  e.courseId === schedule.courseId && e.remainingCount > 0
                );
                
                return (
                  <div 
                    key={schedule.id} 
                    className={`schedule-card ${!hasEnrollment ? 'disabled' : ''}`}
                  >
                    <div className="schedule-header">
                      <h4>{schedule.courseName}</h4>
                      <span className="level">{schedule.courseLevel}</span>
                    </div>
                    
                    <div className="schedule-time">
                      <i className="fas fa-clock"></i>
                      {schedule.startTime} - {schedule.endTime}
                    </div>
                    
                    <div className="schedule-info">
                      <div className="teacher">
                        <i className="fas fa-user"></i>
                        {schedule.teacherName}
                      </div>
                      <div className="capacity">
                        <i className="fas fa-users"></i>
                        {schedule.currentCount}/{schedule.maxCapacity}명
                      </div>
                    </div>

                    {hasEnrollment ? (
                      <button 
                        className="btn-reserve"
                        onClick={() => handleReservation(schedule)}
                        disabled={schedule.currentCount >= schedule.maxCapacity}
                      >
                        {schedule.currentCount >= schedule.maxCapacity ? '정원 마감' : '예약하기'}
                      </button>
                    ) : (
                      <div className="no-enrollment">
                        해당 수업의 수강권이 없습니다
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default ParentReservation;
