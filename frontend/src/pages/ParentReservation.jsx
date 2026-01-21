import { useState, useEffect } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { reservationAPI, scheduleAPI, authAPI, studentAPI } from '../services/api';
import { holidayService } from '../services/holidayService';
import '../styles/ParentReservation.css';

function ParentReservation() {
  const [reservationStatus, setReservationStatus] = useState(null); // 'success', 'error', null
  // 사용자 프로필 조회
  const { data: profile, isLoading: profileLoading } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
  });

  // 예약 가능한 날짜 범위 조회
  const { data: availableDates } = useQuery({
    queryKey: ['availableDates'],
    queryFn: async () => {
      const response = await reservationAPI.getAvailableDates();
      console.log('availableDates 응답:', response.data);
      return response.data;
    },
    refetchInterval: 60000, // 1분마다 확인
  });

  // 프로필 로딩 중이면 로딩 표시
  if (profileLoading) {
    return <div>로딩 중...</div>;
  }

  // 학부모 자녀 목록 조회
  const { data: myStudents = [] } = useQuery({
    queryKey: ['myStudents'],
    queryFn: async () => {
      try {
        const response = await studentAPI.getMyStudents();
        return response.data;
      } catch (error) {
        console.error('자녀 목록 조회 실패:', error);
        return [];
      }
    },
    enabled: !!profile && profile.role === 'PARENT',
    retry: false,
  });

  // 전체 학생 목록 조회 (관리자/선생님용)
  const { data: allStudents = [] } = useQuery({
    queryKey: ['allStudents'],
    queryFn: async () => {
      const response = await studentAPI.getActive();
      return response.data;
    },
    enabled: !!profile && (profile.role === 'ADMIN' || profile.role === 'TEACHER'),
  });

  const [formData, setFormData] = useState({
    // 선택된 학생 ID
    selectedStudentId: '',
    
    // 예약 정보
    preferredDate: '',
    preferredTime: '',
    consultationType: '',
    
    // 요청사항
    requirements: ''
  });

  const [errors, setErrors] = useState({});
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const [holidays, setHolidays] = useState({});
  const [loadedYears, setLoadedYears] = useState(new Set());
  const [showStudentDropdown, setShowStudentDropdown] = useState(false);
  const [studentSearchQuery, setStudentSearchQuery] = useState('');

  // 특정 년도의 공휴일 로드
  const [selectedDateForTime, setSelectedDateForTime] = useState(null);
  const [showTimeSelector, setShowTimeSelector] = useState(false);
  const [reservedTimes, setReservedTimes] = useState([]);

  // 선택된 날짜의 예약 현황 조회
  const fetchReservedTimes = async (date) => {
    try {
      const response = await reservationAPI.getReservedTimes(date, formData.consultationType);
      console.log('예약된 시간들:', response.data);
      setReservedTimes(response.data);
    } catch (error) {
      console.error('예약 현황 조회 실패:', error);
      setReservedTimes([]);
    }
  };

  const loadHolidaysForYear = async (year) => {
    if (loadedYears.has(year)) return; // 이미 로드된 년도는 스킵

    try {
      const yearHolidays = await holidayService.getHolidays(year);
      setHolidays(prev => ({
        ...prev,
        [year]: yearHolidays
      }));
      setLoadedYears(prev => new Set([...prev, year]));
    } catch (error) {
      console.error(`${year}년 공휴일 조회 실패:`, error);
      setHolidays(prev => ({
        ...prev,
        [year]: holidayService.getDefaultHolidays(year)
      }));
      setLoadedYears(prev => new Set([...prev, year]));
    }
  };

  // 페이지 로딩 시 현재 년도 공휴일 로드
  useEffect(() => {
    const currentYear = new Date().getFullYear();
    loadHolidaysForYear(currentYear);
  }, []);

  // 년도가 변경될 때만 공휴일 로드
  useEffect(() => {
    const year = currentMonth.getFullYear();
    loadHolidaysForYear(year);
  }, [currentMonth.getFullYear()]);

  // 선택된 날짜나 예약 유형이 변경될 때 예약된 시간 조회
  useEffect(() => {
    if (selectedDateForTime && formData.consultationType) {
      fetchReservedTimes(selectedDateForTime);
    }
  }, [selectedDateForTime, formData.consultationType]);

  // 드롭다운 외부 클릭 시 닫기
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (showStudentDropdown && !event.target.closest('.student-select-wrapper')) {
        setShowStudentDropdown(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [showStudentDropdown]);

  // 상담 유형에 따른 예약 가능 날짜 체크
  const isDateAvailable = (date, consultationType) => {
    const dayOfWeek = date.getDay(); // 0: 일요일, 1: 월요일, ..., 6: 토요일
    const dateString = date.toISOString().split('T')[0];
    const year = date.getFullYear();
    const isHoliday = holidays[year] && holidays[year][dateString];

    switch(consultationType) {
      case '재원생상담':
        // 일요일, 공휴일 제외 모두 예약 가능 (토요일 포함)
        return dayOfWeek !== 0 && !isHoliday;
      case '레벨테스트':
        // 평일(월-금, 공휴일 제외)만 예약 가능
        return dayOfWeek >= 1 && dayOfWeek <= 5 && !isHoliday;
      case '입학상담':
        // 토요일만(공휴일 제외) 예약 가능
        return dayOfWeek === 6 && !isHoliday;
      default:
        return true;
    }
  };

  // 캘린더 관련 함수들
  const getDaysInMonth = (date) => {
    return new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate();
  };

  const getFirstDayOfMonth = (date) => {
    return new Date(date.getFullYear(), date.getMonth(), 1).getDay();
  };

  const formatDate = (year, month, day) => {
    return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
  };

  // 날짜가 선택 가능한지 확인 (재원생 예약만 격주 제한 적용)
  const isDateSelectable = (year, month, day) => {
    const date = new Date(year, month, day);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    console.log('isDateSelectable 호출:', { year, month, day, consultationType: formData.consultationType });
    
    // 과거 날짜는 선택 불가
    if (date < today) {
      console.log('과거 날짜로 선택 불가');
      return false;
    }
    
    // 재원생 예약(영어 수업)이 아니면 원래 로직 사용
    if (formData.consultationType !== '재원생상담') {
      console.log('재원생 예약이 아니므로 제한 없음');
      return true; // 레벨테스트 등은 제한 없음
    }
    
    console.log('재원생 예약 - 격주 제한 적용');
    
    // 재원생 예약만 격주 제한 적용
    // 주말은 선택 불가 (토요일=6, 일요일=0)
    const dayOfWeek = date.getDay();
    if (dayOfWeek === 0 || dayOfWeek === 6) {
      console.log('주말로 선택 불가');
      return false;
    }
    
    // 예약 가능한 날짜 범위가 없으면 선택 불가
    if (!availableDates?.startDate || !availableDates?.endDate) {
      console.log('예약 가능한 날짜 범위 없음');
      return false;
    }
    
    const startDate = new Date(availableDates.startDate);
    const endDate = new Date(availableDates.endDate);
    
    console.log('날짜 범위 체크:', { startDate, endDate, date });
    
    // 예약 가능한 날짜 범위 내에서만 선택 가능
    return date >= startDate && date <= endDate;
  };

  const isDateDisabled = (year, month, day) => {
    const date = new Date(year, month, day);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    // 재원생 상담의 경우 특별한 날짜 제한 적용
    if (formData.consultationType === '재원생상담') {
      const now = new Date();
      const currentHour = now.getHours();
      
      // 오늘은 항상 예약 불가
      if (date.toDateString() === today.toDateString()) {
        return true;
      }
      
      // 오늘 18시가 지났다면 내일도 예약 불가
      if (currentHour >= 18) {
        const tomorrow = new Date(today);
        tomorrow.setDate(tomorrow.getDate() + 1);
        if (date.toDateString() === tomorrow.toDateString()) {
          return true;
        }
      }
    } else {
      // 다른 상담 유형의 경우 과거 날짜만 비활성화
      if (date < today) return true;
    }
    
    // 공휴일은 항상 비활성화
    const yearHolidays = holidays[year] || [];
    if (holidayService.isHoliday(date, yearHolidays)) return true;
    
    // 상담 유형이 선택되지 않았으면 기본적으로 주말 비활성화
    if (!formData.consultationType) {
      return holidayService.isWeekend(date);
    }
    
    // 상담 유형에 따른 날짜 제한은 isDateAvailable에서 처리
    return false;
  };

  const handleDateSelect = (year, month, day) => {
    // 선택 불가능한 날짜면 클릭 무시
    if (!isDateSelectable(year, month, day)) {
      return;
    }
    
    const selectedDate = new Date(year, month, day);
    
    // 상담 유형에 따른 예약 가능 날짜 체크
    if (!isDateAvailable(selectedDate, formData.consultationType)) {
      return; // 예약 불가능한 날짜는 선택 불가
    }
    
    const formattedDate = formatDate(year, month, day);
    setFormData(prev => ({
      ...prev,
      preferredDate: formattedDate
    }));
    
    // 해당 날짜의 예약 현황 조회
    fetchReservedTimes(formattedDate);
    
    if (errors.preferredDate) {
      setErrors(prev => ({
        ...prev,
        preferredDate: ''
      }));
    }
  };

  const renderCalendar = () => {
    const year = currentMonth.getFullYear();
    const month = currentMonth.getMonth();
    const daysInMonth = getDaysInMonth(currentMonth);
    const firstDay = getFirstDayOfMonth(currentMonth);
    
    const days = [];
    // 빈 칸 (이전 달)
    for (let i = 0; i < firstDay; i++) {
      days.push(<div key={`empty-${i}`} className="calendar-day empty"></div>);
    }
    
    // 현재 달의 날짜들
    for (let day = 1; day <= daysInMonth; day++) {
      const date = new Date(year, month, day);
      const isDisabled = isDateDisabled(year, month, day);
      const isSelectable = isDateSelectable(year, month, day);
      const isSelected = formData.preferredDate === formatDate(year, month, day);
      const yearHolidays = holidays[year] || [];
      const isHolidayDate = holidayService.isHoliday(date, yearHolidays);
      const isWeekendDate = holidayService.isWeekend(date);
      
      // 상담 유형에 따른 예약 가능 여부 체크
      const isAvailable = isDateAvailable(date, formData.consultationType);
      const finalDisabled = isDisabled || !isAvailable || !isSelectable;
      
      days.push(
        <div
          key={day}
          className={`calendar-day ${finalDisabled ? 'disabled' : ''} ${isSelected ? 'selected' : ''} ${isHolidayDate ? 'holiday' : ''} ${isWeekendDate ? 'weekend' : ''} ${!isAvailable ? 'unavailable' : ''} ${!isSelectable ? 'out-of-range' : ''} ${isAvailable && !finalDisabled ? 'available' : ''}`}
          onClick={() => !finalDisabled && handleDateSelect(year, month, day)}
          title={
            !isSelectable ? '예약 가능한 기간이 아닙니다' :
            isHolidayDate ? yearHolidays.find(h => holidayService.isHoliday(date, [h]))?.name : ''
          }
        >
          {day}
        </div>
      );
    }
    
    return days;
  };

  const navigateMonth = (direction) => {
    setCurrentMonth(prev => {
      const newDate = new Date(prev);
      newDate.setMonth(prev.getMonth() + direction);
      return newDate;
    });
  };

  // 예약 생성 mutation
  const createReservation = useMutation({
    mutationFn: (data) => reservationAPI.create(data),
    onSuccess: () => {
      setReservationStatus('success');
      window.scrollTo({ top: 0, behavior: 'smooth' });
    },
    onError: (error) => {
      setReservationStatus('error');
      window.scrollTo({ top: 0, behavior: 'smooth' });
      console.error('예약 오류:', error);
    },
  });

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    
    // 상담 유형이 변경되면 캘린더 초기화
    if (name === 'consultationType') {
      setSelectedDateForTime(null);
      setShowTimeSelector(false);
      setFormData(prev => ({
        ...prev,
        [name]: value,
        preferredDate: '',
        preferredTime: ''
      }));
      
      // 에러 메시지 제거
      if (errors[name]) {
        setErrors(prev => ({
          ...prev,
          [name]: ''
        }));
      }
      return;
    }
    
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    
    // 에러 메시지 제거
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
  };

  // 학생 선택 핸들러
  const handleStudentSelect = (student) => {
    setFormData(prev => ({
      ...prev,
      selectedStudentId: student.id.toString()
    }));
    setShowStudentDropdown(false);
    setStudentSearchQuery('');
    
    // 에러 메시지 제거
    if (errors.selectedStudentId) {
      setErrors(prev => ({
        ...prev,
        selectedStudentId: ''
      }));
    }
  };

  // 선택된 학생 정보 가져오기
  const getSelectedStudent = () => {
    const students = profile?.role === 'PARENT' ? myStudents : allStudents;
    return students.find(student => student.id.toString() === formData.selectedStudentId);
  };

  // 필터링된 학생 목록
  const getFilteredStudents = () => {
    const students = profile?.role === 'PARENT' ? myStudents : allStudents;
    if (!studentSearchQuery) return students;
    
    return students.filter(student => 
      student.studentName.toLowerCase().includes(studentSearchQuery.toLowerCase()) ||
      student.parentName.toLowerCase().includes(studentSearchQuery.toLowerCase())
    );
  };

  const handleRetryReservation = () => {
    setReservationStatus(null);
    setFormData({
      parentName: '',
      parentPhone: '',
      students: [
        {
          studentName: '',
          studentPhone: '',
          school: ''
        }
      ],
      preferredDate: '',
      preferredTime: '',
      consultationType: '',
      requirements: ''
    });
    setErrors({});
    setSelectedDateForTime(null);
    setShowTimeSelector(false);
  };

  const handleGoHome = () => {
    window.location.href = '/';
  };

  // 성공/실패 컴포넌트 렌더링
  if (reservationStatus === 'success') {
    return (
      <div className="parent-reservation">
        <div className="reservation-result success">
          <div className="result-icon">✓</div>
          <h2>예약이 완료되었습니다!</h2>
          <div className="result-buttons">
            <button onClick={handleRetryReservation} className="btn-retry">
              다시 예약
            </button>
            <button onClick={handleGoHome} className="btn-home">
              홈으로
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (reservationStatus === 'error') {
    return (
      <div className="parent-reservation">
        <div className="reservation-result error">
          <div className="result-icon">✗</div>
          <h2>예약 요청 중 오류가 발생했습니다</h2>
          <p>다시 시도해주세요.</p>
          <div className="result-buttons">
            <button onClick={handleRetryReservation} className="btn-retry">
              다시 예약
            </button>
            <button onClick={handleGoHome} className="btn-home">
              홈으로
            </button>
          </div>
        </div>
      </div>
    );
  }

  const validateForm = () => {
    const newErrors = {};
    
    // 역할별 유효성 검사
    if (profile?.role === 'PARENT') {
      if (!formData.selectedStudentId) {
        newErrors.selectedStudentId = '수업 대상 자녀를 선택해주세요.';
        window.alert('수업 대상 자녀를 선택해주세요.');
        return false;
      }
    } else {
      // 관리자/선생님용 유효성 검사
      if (!formData.selectedStudentId) {
        newErrors.selectedStudentId = '학생을 선택해주세요.';
        window.alert('학생을 선택해주세요.');
        return false;
      }
    }
    
    if (!formData.preferredDate) {
      newErrors.preferredDate = '희망 날짜를 선택해주세요.';
      window.alert('희망 날짜를 선택해주세요.');
      setErrors(newErrors);
      return false;
    }
    
    if (!formData.preferredTime) {
      newErrors.preferredTime = '희망 시간을 선택해주세요.';
      window.alert('희망 시간을 선택해주세요.');
      setErrors(newErrors);
      return false;
    }
    
    if (!formData.consultationType) {
      newErrors.consultationType = '수업 유형을 선택해주세요.';
      window.alert('원하시는 예약을 눌러주세요');
      setErrors(newErrors);
      return false;
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }
    
    try {
      // 선택된 날짜/시간에 해당하는 스케줄 찾기
      const scheduleResponse = await scheduleAPI.getByDate(formData.preferredDate);
      
      // 디버깅용 로그
      console.log('=== 예약 디버깅 ===');
      console.log('선택한 날짜:', formData.preferredDate);
      console.log('선택한 시간:', formData.preferredTime);
      console.log('선택한 상담 유형:', formData.consultationType);
      console.log('조회된 스케줄 수:', scheduleResponse.data.length);
      
      // 시간 형식 맞춰서 비교 (HH:MM vs HH:MM:SS)
      const selectedTime = formData.preferredTime + ":00"; // "14:00" -> "14:00:00"
      console.log('변환된 시간:', selectedTime);
      
      // 매칭되는 스케줄 찾기
      scheduleResponse.data.forEach((schedule, index) => {
        console.log(`스케줄 ${index + 1}:`, {
          id: schedule.id,
          courseName: schedule.courseName,
          startTime: schedule.startTime,
          timeMatch: schedule.startTime === selectedTime,
          courseMatch: schedule.courseName === formData.consultationType
        });
      });
      
      // 09:00~18:00 사이 모든 시간대 예약 가능
      // 스케줄 체크 제거 - 시간대만 선택하면 예약 가능
      
      // 선택된 학생 정보 처리
      const selectedStudent = getSelectedStudent();
      if (!selectedStudent) {
        alert('선택된 학생 정보를 찾을 수 없습니다.');
        return;
      }

      const studentInfo = {
        studentName: selectedStudent.studentName,
        parentName: selectedStudent.parentName,
        phoneNumber: selectedStudent.parentPhone || selectedStudent.phoneNumber
      };

      // 예약 요청 데이터 구성
      const reservationData = {
        studentId: selectedStudent.id,
        studentName: studentInfo.studentName,
        parentName: studentInfo.parentName,
        phoneNumber: studentInfo.phoneNumber,
        reservationDate: formData.preferredDate,
        reservationTime: formData.preferredTime,
        consultationType: formData.consultationType,
        memo: formData.requirements,
        reservationSource: 'WEB'
      };
      
      createReservation.mutate(reservationData);
    } catch (error) {
      console.error('스케줄 조회 실패:', error);
      alert('예약 처리 중 오류가 발생했습니다.');
    }
  };

  // 상담 유형별 안내 문구
  const getConsultationInfo = (type) => {
    switch(type) {
      case '재원생상담':
        return {
          title: "'재원생' 수업 예약 시스템입니다. 메모란에 '아이이름 정목초' 형태로 예약 부탁드립니다.",
          content: `예약 시 확인해 주세요
1. 어머니 성함이 아닌 '아이이름 정목초' 형태로 이름 설정하시어 예약 부탁드립니다.
2. 예약 취소는 전날 오후 12시까지 가능하시며 당일 취소는 횟수가 차감됩니다.
3. 수업 시간보다 늦게 도착하여 시작한 학생은 등록한 시간보다 일찍 끝날 수 있으니 꼭! 시간에 맞춰서 등원 부탁드립니다.
4. 수업 시간보다 일찍 도착시 밖에서 대기할 수 있습니다.

원활한 수업 진행을 위하여 최대한 시간에 맞추어 등원 부탁드립니다:)

주차비 무료
건물 앞 뒤로 무료 주차 가능하십니다 ^^`
        };
      case '레벨테스트':
        return {
          title: "(평일) 레벨테스트& 1회 체험 수업 예약",
          content: `리틀베어 리딩클럽이 처음인 학부모님들을 대상으로
레벨테스트 응시시 체험 수업 1회를 무료로 제공해드리는 이벤트를 진행합니다:)

*레벨테스트와 체험수업은 총 1시간 20분 가량 소요 예정입니다.
*레벨테스트 비용은 2만원입니다.
*평일 한정 이벤트 입니다.

예약 시 확인해 주세요
꼭 확인해 주세요!
>>아이 이름, 학교 및 학년, (SR 점수 있을 경우 기재) 부탁 드립니다.

>> 자세한 수강료 상담은 상담 방문시에만 가능합니다.

>> 개인적인 사유로 인한 취소시 반드시 하루 전에 취소 및 연락 부탁드립니다.

>> 레벨테스트 비용은 2만원입니다.

주차정보
주차비 무료
건물 앞 뒤로 무료 주차 가능하십니다 ^^`
        };
      case '입학상담':
        return {
          title: "(토요일) 레벨테스트 및 상담 예약",
          content: `리틀베어 리딩클럽 레벨테스트 및 상담 예약창입니다:)

* SR test, interview, essay writing 순으로 진행됩니다.
* 약 1시간 소요 예정입니다.
* 비용은 2만원 발생합니다.
* 레벨테스트 후 커리큐럼 및 학습 방향에 대한 자세한 상담 도와드립니다.

예약 시 확인해 주세요
꼭 확인해 주세요!
>>이름, 학교 및 학년, SR 점수(있을 경우) 부탁 드립니다.

>> 자세한 수강료 상담은 상담 방문 시에만 가능합니다.

>>레벨테스트 비용은 2만원이며, 예약금은 1만원입니다.

>> 개인적인 사유로 인한 취소 시 반드시 하루 전에 취소 및 연락 부탁 드립니다. 당일 취소 시, 예약금 환불은 불가합니다.

주차정보
주차비 무료
건물 앞 뒤로 무료 주차 가능하십니다 ^^`
        };
      default:
        return {
          title: "상담 예약",
          content: `상담 예약을 위해 필요한 정보를 입력해주세요.

주차정보
주차비 무료
건물 앞 뒤로 무료 주차 가능하십니다 ^^`
        };
    }
  };

  const consultationTypes = [
    { value: '재원생상담', label: '재원생 예약 시스템' },
    { value: '레벨테스트', label: '(평일) 레벨테스트 & 1회 체험 수업 예약' },
    { value: '입학상담', label: '(토요일) 레벨테스트 및 상담 예약' }
  ];

  const timeSlots = [
    '09:00', '10:00', '11:00', '12:00', '13:00', '14:00', 
    '15:00', '16:00', '17:00', '18:00', '19:00', '20:00'
  ];

  return (
    <div className="page-wrapper">
      <div className="page-header">
        <div className="page-header-content">
          <div className="page-title-section">
            <h1 className="page-title">
              <i className="fas fa-calendar-plus"></i>
              수업 예약
            </h1>
            <p className="page-subtitle">상담을 예약해주세요</p>
          </div>
        </div>
      </div>

      <div className="page-content">
        <form onSubmit={handleSubmit} className="reservation-form">
          {/* 예약 정보 - 가장 위로 이동 */}
          <div className="form-section">
            <h2>예약 정보</h2>
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="consultationType">수업 유형 *</label>
                <select
                  id="consultationType"
                  name="consultationType"
                  value={formData.consultationType}
                  onChange={handleInputChange}
                  className={errors.consultationType ? 'error' : ''}
                >
                  <option value="">수업 유형을 선택해주세요</option>
                  {consultationTypes.map(consultation => (
                    <option key={consultation.value} value={consultation.value}>{consultation.label}</option>
                  ))}
                </select>
                {errors.consultationType && <span className="error-message">{errors.consultationType}</span>}
              </div>
            </div>

            {/* 상담 유형별 안내 문구 */}
            {formData.consultationType && (
              <div className="consultation-info">
                <h3>{getConsultationInfo(formData.consultationType).title}</h3>
                <div className="consultation-content">
                  {getConsultationInfo(formData.consultationType).content.split('\n').map((line, index) => (
                    <div key={index}>
                      {line}
                      {index < getConsultationInfo(formData.consultationType).content.split('\n').length - 1 && <br />}
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* 희망 날짜 선택 */}
          <div className="form-section">
            <h2>희망 날짜 선택</h2>
            <div className="calendar-container">
              <div className="calendar-header-nav">
                <button type="button" onClick={() => navigateMonth(-1)}>
                  <i className="fas fa-chevron-left"></i>
                </button>
                <span className="calendar-month">
                  {currentMonth.getFullYear()}년 {currentMonth.getMonth() + 1}월
                </span>
                <button type="button" onClick={() => navigateMonth(1)}>
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
              {formData.preferredDate && (
                <div className="selected-date">
                  선택된 날짜: {new Date(formData.preferredDate).toLocaleDateString('ko-KR')}
                </div>
              )}
              
              {/* 시간 선택 옵션 - 별도 박스 */}
              {formData.preferredDate && (
                <div className="selected-date time-selector">
                  <h4>시간을 선택해주세요</h4>
                  <div className="time-grid">
                    {timeSlots.map(time => {
                      const isReserved = reservedTimes.includes(time);
                      return (
                        <button
                          key={time}
                          type="button"
                          className={`time-slot ${formData.preferredTime === time ? 'selected' : ''} ${isReserved ? 'reserved' : ''}`}
                          onClick={() => {
                            if (!isReserved) {
                              setFormData(prev => ({ ...prev, preferredTime: time }));
                              if (errors.preferredTime) {
                                setErrors(prev => ({ ...prev, preferredTime: '' }));
                              }
                            }
                          }}
                          disabled={isReserved}
                        >
                          {time}
                          {isReserved && <span className="reserved-text">예약됨</span>}
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}
              {errors.preferredDate && <span className="error-message">{errors.preferredDate}</span>}
            </div>
          </div>

          {/* 자녀 선택 */}
          <div className="form-section">
            {profile?.role === 'PARENT' ? (
              // 학부모용 - 자녀 선택
              <>
                <h2>수업 대상 자녀 선택</h2>
                <div className="form-group">
                  <label htmlFor="selectedStudentId">자녀 선택 *</label>
                  <select
                    id="selectedStudentId"
                    name="selectedStudentId"
                    value={formData.selectedStudentId}
                    onChange={handleInputChange}
                    className={errors.selectedStudentId ? 'error' : ''}
                  >
                    <option value="">자녀를 선택해주세요</option>
                    {myStudents.map((student) => (
                      <option key={student.id} value={student.id}>
                        {student.studentName} ({student.school || '학교 미등록'} {student.grade}학년)
                      </option>
                    ))}
                  </select>
                  {errors.selectedStudentId && <span className="error-message">{errors.selectedStudentId}</span>}
                </div>
                
                {/* 선택된 자녀 정보 표시 */}
                {formData.selectedStudentId && (
                  <div className="selected-student-info">
                    {(() => {
                      const selectedStudent = myStudents.find(s => s.id.toString() === formData.selectedStudentId);
                      return selectedStudent ? (
                        <div className="student-details">
                          <h3>선택된 자녀 정보</h3>
                          <div className="info-grid">
                            <div className="info-item">
                          <span className="label">이름:</span>
                          <span className="value">{selectedStudent.studentName}</span>
                        </div>
                        <div className="info-item">
                          <span className="label">학교:</span>
                          <span className="value">{selectedStudent.school || '미등록'}</span>
                        </div>
                        <div className="info-item">
                          <span className="label">학년:</span>
                          <span className="value">{selectedStudent.grade}학년</span>
                        </div>
                        <div className="info-item">
                          <span className="label">영어 레벨:</span>
                          <span className="value">{selectedStudent.englishLevel}</span>
                        </div>
                      </div>
                    </div>
                  ) : null;
                })()}
              </div>
            )}
              </>
            ) : (
              // 관리자/선생님용 - 학생 선택
              <>
                <h2>학생 선택</h2>
                <div className="form-group">
                  <label htmlFor="studentSelect">학생을 선택해 주세요 *</label>
                  <div className="student-select-wrapper">
                    <div 
                      className={`student-select-input ${errors.selectedStudentId ? 'error' : ''}`}
                      onClick={() => setShowStudentDropdown(!showStudentDropdown)}
                    >
                      {getSelectedStudent() ? (
                        <div className="selected-student-info">
                          <span className="student-name">{getSelectedStudent().studentName}</span>
                          <span className="parent-info">
                            {getSelectedStudent().parentName} · {getSelectedStudent().parentPhone || getSelectedStudent().phoneNumber}
                          </span>
                        </div>
                      ) : (
                        <span className="placeholder">학생을 선택해 주세요</span>
                      )}
                      <i className={`fas fa-chevron-${showStudentDropdown ? 'up' : 'down'}`}></i>
                    </div>
                    
                    {showStudentDropdown && (
                      <div className="student-dropdown">
                        <div className="student-search">
                          <input
                            type="text"
                            placeholder="학생 이름 또는 학부모 이름으로 검색..."
                            value={studentSearchQuery}
                            onChange={(e) => setStudentSearchQuery(e.target.value)}
                            onClick={(e) => e.stopPropagation()}
                          />
                        </div>
                        <div className="student-list">
                          {getFilteredStudents().map(student => (
                            <div
                              key={student.id}
                              className="student-option"
                              onClick={() => handleStudentSelect(student)}
                            >
                              <div className="student-info">
                                <span className="student-name">{student.studentName}</span>
                                <span className="parent-info">
                                  {student.parentName} · {student.parentPhone || student.phoneNumber}
                                </span>
                              </div>
                            </div>
                          ))}
                          {getFilteredStudents().length === 0 && (
                            <div className="no-students">검색 결과가 없습니다.</div>
                          )}
                        </div>
                      </div>
                    )}
                  </div>
                  {errors.selectedStudentId && <span className="error-message">{errors.selectedStudentId}</span>}
                </div>
              </>
            )}
          </div>

          {/* 요청사항 */}
          <div className="form-section">
            <h2>요청사항</h2>
            <div className="form-group">
              <label htmlFor="requirements">추가 요청사항</label>
              <textarea
                id="requirements"
                name="requirements"
                value={formData.requirements}
                onChange={handleInputChange}
                placeholder="수업에 대한 특별한 요청사항이나 학생의 특이사항을 입력해주세요"
                rows="4"
              />
            </div>
          </div>

          {/* 제출 버튼 */}
          <div className="form-actions">
            <button 
              type="submit" 
              className="submit-btn"
              disabled={createReservation.isPending}
            >
              {createReservation.isPending ? '예약 중...' : '예약 하기'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default ParentReservation;
