import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import LoadingSpinner from '../components/LoadingSpinner';
import { reservationAPI, scheduleAPI, enrollmentAPI, authAPI, naverBookingAPI, consultationAPI, blockedTimeSlotAPI } from '../services/api';
import { getTodayString } from '../utils/dateUtils';
import '../styles/Reservations.css';

function Reservations() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [selectedDate, setSelectedDate] = useState(getTodayString());
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showNaverDetailModal, setShowNaverDetailModal] = useState(false);
  const [showBlockModal, setShowBlockModal] = useState(false);
  const [blockForm, setBlockForm] = useState({
    blockType: 'SINGLE', blockDate: '', startDate: '', endDate: '',
    dayOfWeek: '', blockTime: '', reason: '', targetType: 'CLASS'
  });
  const [blockTab, setBlockTab] = useState('CLASS');
  const [naverBookings, setNaverBookings] = useState([]);
  const [showAllNaverBookings, setShowAllNaverBookings] = useState(false);
  const [showAllSystemReservations, setShowAllSystemReservations] = useState(false);
  const [selectedConsultation, setSelectedConsultation] = useState(null);
  const [selectedSchedule, setSelectedSchedule] = useState(null);
  const [newReservation, setNewReservation] = useState({
    studentId: '',
    scheduleId: '',
    enrollmentId: '',
  });

  // 사용자 프로필 조회
  const { data: profile } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
  });

  const isParent = profile?.role === 'PARENT';

  // 네이버 예약 조회 (선택된 날짜로)
  const { data: naverBookingsData } = useQuery({
    queryKey: ['naverBookings', selectedDate],
    queryFn: () => {
      console.log('네이버 예약 API 호출:', selectedDate);
      return naverBookingAPI.getByDate(selectedDate);
    },
    enabled: !isParent,
  });
  
  // 네이버 예약 데이터 업데이트
  React.useEffect(() => {
    console.log('네이버 예약 데이터:', naverBookingsData);
    if (naverBookingsData?.data) {
      console.log('실제 데이터:', naverBookingsData.data);
      setNaverBookings(naverBookingsData.data);
    }
  }, [naverBookingsData]);
  
  // SSE로 크롤링 완료 알림 받기
  React.useEffect(() => {
    if (isParent) return;
    
    const eventSource = new EventSource('/api/naver-booking/events');
    
    eventSource.addEventListener('crawling-complete', () => {
      console.log('크롤링 완료 알림 받음 - 데이터 갱신');
      queryClient.invalidateQueries(['naverBookings']);
    });
    
    eventSource.onerror = (error) => {
      console.error('SSE 연결 오류', error);
    };
    
    return () => {
      eventSource.close();
    };
  }, [isParent, queryClient]);

  // 월별 예약 조회 (캘린더용) - 간소화
  const { data: monthlyReservations = [] } = useQuery({
    queryKey: ['monthlyReservations', currentMonth.getFullYear(), currentMonth.getMonth(), profile?.role],
    queryFn: async () => {
      if (!profile) return [];
      
      if (isParent) {
        const reservationResponse = await reservationAPI.getMyReservations();
        const consultationResponse = await consultationAPI.getMyChildren();
        
        const year = currentMonth.getFullYear();
        const month = currentMonth.getMonth() + 1;
        const startDate = `${year}-${String(month).padStart(2, '0')}-01`;
        const endDate = `${year}-${String(month).padStart(2, '0')}-${new Date(year, month, 0).getDate()}`;
        
        // 수업 예약 필터링
        const filteredReservations = reservationResponse.data.filter(r => {
          return r.reservationDate >= startDate && r.reservationDate <= endDate;
        });
        
        // 상담 예약 필터링 및 변환
        const consultations = consultationResponse.data
          .filter(c => c.consultationDate >= startDate && c.consultationDate <= endDate)
          .map(c => ({
            id: `consultation-${c.id}`,
            reservationDate: c.consultationDate,
            isConsultation: true
          }));
        
        return [...filteredReservations, ...consultations];
      } else {
        // 관리자/선생님: 선택된 날짜의 예약만 조회 (월별 조회는 생략)
        return [];
      }
    },
    enabled: !!profile,
  });
  // 날짜별 예약 조회 (역할별 분기)
  const { data: reservations = [], isLoading: reservationsLoading } = useQuery({
    queryKey: ['reservations', selectedDate, profile?.role],
    queryFn: async () => {
      if (isParent) {
        // 학부모는 본인 자녀 예약만 조회
        const reservationResponse = await reservationAPI.getMyReservations();
        const consultationResponse = await consultationAPI.getMyChildren();
        
        // 수업 예약 필터링
        const filteredReservations = reservationResponse.data.filter(r => {
          return r.reservationDate === selectedDate;
        });
        
        // 상담 예약을 예약 형식으로 변환하여 병합
        const consultations = consultationResponse.data
          .filter(c => c.consultationDate === selectedDate)
          .map(c => ({
            id: `consultation-${c.id}`,
            studentId: c.studentId,
            studentName: c.studentName,
            student: { name: c.studentName, phone: c.studentPhone },
            reservationDate: c.consultationDate,
            reservationTime: c.consultationTime,
            consultationType: c.consultationType,
            notes: c.content || c.memo,
            status: 'CONFIRMED',
            isConsultation: true
          }));
        
        return [...filteredReservations, ...consultations];
      } else {
        // 관리자/선생님은 전체 예약 조회
        console.log('관리자 예약 조회 - 날짜:', selectedDate);
        const response = await reservationAPI.getByDate(selectedDate);
        console.log('관리자 예약 조회 결과:', response.data);
        return response.data;
      }
    },
    enabled: !!profile,
  });

  // 날짜별 스케줄 조회 (관리자/선생님만)
  const { data: schedules = [], isLoading: schedulesLoading } = useQuery({
    queryKey: ['schedules', selectedDate],
    queryFn: async () => {
      if (isParent) return []; // 학부모는 스케줄 조회 불필요
      const response = await scheduleAPI.getByDate(selectedDate);
      return response.data;
    },
    enabled: !!profile && !isParent,
  });

  // 학생별 활성 수강권 조회
  const { data: enrollments = [], isLoading: enrollmentsLoading } = useQuery({
    queryKey: ['enrollments', newReservation.studentId],
    queryFn: async () => {
      if (!newReservation.studentId) return [];
      const response = await enrollmentAPI.getByStudent(newReservation.studentId);
      return response.data.filter(e => e.status === 'ACTIVE');
    },
    enabled: !!newReservation.studentId,
  });

  // 차단 시간 목록
  const { data: blockedSlots = [] } = useQuery({
    queryKey: ['blockedTimeSlots'],
    queryFn: async () => (await blockedTimeSlotAPI.getAll()).data,
    enabled: !isParent,
  });

  const createBlockMutation = useMutation({
    mutationFn: (data) => blockedTimeSlotAPI.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries(['blockedTimeSlots']);
      alert('차단이 추가되었습니다.');
      setBlockForm(f => ({ ...f, blockTime: '', reason: '' }));
    },
    onError: (error) => alert(`차단 실패: ${error.response?.data?.message || '오류'}`),
  });

  const deleteBlockMutation = useMutation({
    mutationFn: (id) => blockedTimeSlotAPI.delete(id),
    onSuccess: () => queryClient.invalidateQueries(['blockedTimeSlots']),
  });

  const TIME_SLOTS = ['09:00','10:00','11:00','12:00','13:00','14:00','15:00','16:00','17:00','18:00','19:00','20:00'];
  const DAY_NAMES = { MONDAY:'월', TUESDAY:'화', WEDNESDAY:'수', THURSDAY:'목', FRIDAY:'금', SATURDAY:'토', SUNDAY:'일' };

  // 예약 생성 mutation
  const createMutation = useMutation({
    mutationFn: (data) => reservationAPI.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries(['reservations', selectedDate]);
      setShowCreateModal(false);
      setNewReservation({ studentId: '', scheduleId: '', enrollmentId: '' });
    },
    onError: (error) => {
      alert(`예약 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    },
  });

  // 예약 취소 mutation
  const cancelMutation = useMutation({
    mutationFn: (id) => reservationAPI.cancel(id),
    onSuccess: () => {
      queryClient.invalidateQueries(['reservations', selectedDate]);
      alert('예약이 취소되었습니다.');
    },
    onError: (error) => {
      alert(`취소 실패: ${error.response?.data?.message || '취소 가능 시간이 지났습니다.'}`);
    },
  });

  // 관리자 강제 취소 mutation
  const forceCancelMutation = useMutation({
    mutationFn: (id) => reservationAPI.forceCancel(id),
    onSuccess: () => {
      queryClient.invalidateQueries(['reservations', selectedDate]);
      alert('예약이 강제 취소되었습니다.');
    },
    onError: (error) => {
      alert(`강제 취소 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    },
  });

  // 예약 확정 mutation
  const confirmMutation = useMutation({
    mutationFn: (id) => reservationAPI.confirm(id),
    onSuccess: () => {
      queryClient.invalidateQueries(['reservations', selectedDate]);
      alert('예약이 확정되었습니다.');
    },
    onError: (error) => {
      alert(`확정 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    },
  });

  // 네이버 예약 동기화 mutation
  const syncNaverMutation = useMutation({
    mutationFn: () => naverBookingAPI.sync(selectedDate),
    onSuccess: (response) => {
      console.log('네이버 예약 동기화 성공:', response);
      const data = response.data || response;
      console.log('실제 데이터:', data);
      setNaverBookings(Array.isArray(data) ? data : []);
      queryClient.invalidateQueries(['naverBookings']);
      queryClient.invalidateQueries(['reservations', selectedDate]);
    },
    onError: (error) => {
      console.error('네이버 예약 동기화 실패:', error);
      alert(`동기화 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    },
  });

  // 학생 목록 엑셀 업로드 mutation
  const uploadStudentListMutation = useMutation({
    mutationFn: async (file) => {
      const formData = new FormData();
      formData.append('file', file);
      const token = localStorage.getItem('accessToken');
      const response = await axios.post('/api/student-course/upload', formData, {
        headers: { 
          'Content-Type': 'multipart/form-data',
          'Authorization': `Bearer ${token}`
        }
      });
      return response;
    },
    onSuccess: (response) => {
      alert(`학생 목록이 업데이트되었습니다. (${response.data.studentCount}명)`);
    },
    onError: (error) => {
      alert(`업로드 실패: ${error.response?.data?.message || error.message || '오류가 발생했습니다.'}`);
    },
  });

  const handleStudentListUpload = (e) => {
    const file = e.target.files[0];
    console.log('파일 선택:', file);
    if (file) {
      console.log('업로드 시작:', file.name);
      uploadStudentListMutation.mutate(file);
    }
    e.target.value = '';
  };

  const handleSyncNaver = () => {
    syncNaverMutation.mutate();
  };

  const handleCreateReservation = () => {
    if (!newReservation.studentId || !newReservation.scheduleId || !newReservation.enrollmentId) {
      alert('모든 항목을 선택해주세요.');
      return;
    }
    createMutation.mutate(newReservation);
  };

  const handleCancel = (reservationId) => {
    if (window.confirm('예약을 취소하시겠습니까?')) {
      // 상담 예약인 경우 (ID가 'consultation-'로 시작)
      if (typeof reservationId === 'string' && reservationId.startsWith('consultation-')) {
        const consultationId = reservationId.replace('consultation-', '');
        consultationAPI.delete(consultationId)
          .then(() => {
            queryClient.invalidateQueries(['reservations', selectedDate]);
            alert('상담 예약이 취소되었습니다.');
          })
          .catch((error) => {
            alert(`취소 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
          });
      } else {
        // 일반 수업 예약
        cancelMutation.mutate(reservationId);
      }
    }
  };

  const handleForceCancel = (reservationId) => {
    if (window.confirm('관리자 권한으로 예약을 강제 취소하시겠습니까?')) {
      forceCancelMutation.mutate(reservationId);
    }
  };

  const handleConfirm = (reservationId) => {
    if (window.confirm('예약을 확정하시겠습니까?')) {
      confirmMutation.mutate(reservationId);
    }
  };

  const openCreateModal = (schedule) => {
    setSelectedSchedule(schedule);
    setNewReservation({ ...newReservation, scheduleId: schedule.id });
    setShowCreateModal(true);
  };

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
      const dateStr = `${prevYear}-${String(prevMonth + 1).padStart(2, '0')}-${String(prevDay).padStart(2, '0')}`;
      
      const hasReservations = monthlyReservations.some(reservation => 
        (reservation.scheduleDate || reservation.schedule?.scheduleDate) === dateStr
      );
      
      days.push(
        <div
          key={`prev-${i}`}
          className={`calendar-day other-month ${dateStr === selectedDate ? 'selected' : ''} ${hasReservations ? 'has-reservations' : ''}`}
          onClick={() => setSelectedDate(dateStr)}
        >
          <span className="day-number">{prevDay}</span>
          {hasReservations && <div className="reservation-indicator"></div>}
        </div>
      );
    }
    
    // 현재 달의 날짜들
    for (let day = 1; day <= daysInMonth; day++) {
      const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      
      const hasReservations = monthlyReservations.some(reservation => 
        (reservation.scheduleDate || reservation.schedule?.scheduleDate) === dateStr
      );
      
      days.push(
        <div
          key={day}
          className={`calendar-day ${dateStr === selectedDate ? 'selected' : ''} ${hasReservations ? 'has-reservations' : ''}`}
          onClick={() => setSelectedDate(dateStr)}
        >
          <span className="day-number">{day}</span>
          {hasReservations && <div className="reservation-indicator"></div>}
        </div>
      );
    }
    
    // 다음 달 날짜들 (6주 완성을 위해)
    const totalCells = Math.ceil((firstDay + daysInMonth) / 7) * 7;
    const remainingCells = totalCells - (firstDay + daysInMonth);
    
    for (let day = 1; day <= remainingCells; day++) {
      const nextMonth = month === 11 ? 0 : month + 1;
      const nextYear = month === 11 ? year + 1 : year;
      const dateStr = `${nextYear}-${String(nextMonth + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      
      const hasReservations = monthlyReservations.some(reservation => 
        (reservation.scheduleDate || reservation.schedule?.scheduleDate) === dateStr
      );
      
      days.push(
        <div
          key={`next-${day}`}
          className={`calendar-day other-month ${dateStr === selectedDate ? 'selected' : ''} ${hasReservations ? 'has-reservations' : ''}`}
          onClick={() => setSelectedDate(dateStr)}
        >
          <span className="day-number">{day}</span>
          {hasReservations && <div className="reservation-indicator"></div>}
        </div>
      );
    }

    return days;
  };

  // 취소 가능 여부 확인 (수업 전날 오후 6시까지)
  const canCancelReservation = (reservation) => {
    const scheduleDate = new Date(reservation.reservationDate);
    const cancelDeadline = new Date(scheduleDate);
    cancelDeadline.setDate(cancelDeadline.getDate() - 1);
    cancelDeadline.setHours(18, 0, 0, 0);

    return new Date() < cancelDeadline && reservation.status === 'CONFIRMED';
  };

  // 상태별 배지 색상
  const getStatusBadge = (status) => {
    const statusMap = {
      PENDING: { text: '대기', color: '#FFA500' },
      CONFIRMED: { text: '확정', color: '#03C75A' },
      CANCELLED: { text: '취소', color: '#999' },
      COMPLETED: { text: '완료', color: '#0066FF' },
      NO_SHOW: { text: '노쇼', color: '#FF0000' },
    };
    const { text, color } = statusMap[status] || { text: status, color: '#999' };
    return <span className="status-badge" style={{ backgroundColor: color }}>{text}</span>;
  };

  if (reservationsLoading || schedulesLoading) {
    return (
      <div className="page-wrapper">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="page-wrapper">
      <div className="page-header">
        <div className="page-header-content">
          <div className="page-title-section">
            <h1 className="page-title">
              <i className="fas fa-calendar-alt"></i>
              {isParent ? '예약 내역' : '예약 관리'}
            </h1>
            <p className="page-subtitle">
              {isParent ? '내 자녀의 예약 현황을 확인합니다' : '수업 예약 현황을 관리합니다'}
            </p>
          </div>
          <div className="date-selector">
            <input
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
            />
          </div>
        </div>
      </div>

      <div className="page-content">
        <div className="reservations-layout">
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
            <div className="rsv-date-header">
              <h2 className="rsv-date-title">{new Date(selectedDate).toLocaleDateString('ko-KR', { 
                year: 'numeric', 
                month: 'long', 
                day: 'numeric',
                weekday: 'long'
              })} 예약 현황</h2>
              <span className="rsv-date-count">{reservations.length}건</span>
            </div>

            {/* 통계 및 동기화 버튼 */}
            {!isParent && (
              <div className="reservation-stats">
                <div className="stats-row">
                  <div className="stat-item">
                    <span className="stat-label">시스템 예약</span>
                    <span className="stat-value">{reservations.length}건</span>
                  </div>
                  <div className="stat-divider"></div>
                  <div className="stat-item">
                    <span className="stat-label">네이버 예약</span>
                    <span className="stat-value">{naverBookings.length}건</span>
                  </div>
                  <div className="stat-divider"></div>
                  <div className="stat-item total">
                    <span className="stat-label">총</span>
                    <span className="stat-value">{reservations.length + naverBookings.length}건</span>
                  </div>
                </div>
                <button 
                  className="sync-button"
                  onClick={handleSyncNaver}
                  disabled={syncNaverMutation.isPending}
                >
                  <i className={`fas fa-sync-alt ${syncNaverMutation.isPending ? 'fa-spin' : ''}`}></i>
                  {syncNaverMutation.isPending ? '동기화 중...' : '네이버 예약 동기화'}
                </button>

                <label htmlFor="student-list-upload" className="sync-button" style={{ cursor: 'pointer' }}>
                  <i className={`fas fa-file-excel ${uploadStudentListMutation.isPending ? 'fa-spin' : ''}`}></i>
                  {uploadStudentListMutation.isPending ? '업로드 중...' : '기존 학생 목록 업데이트'}
                </label>
                <button className="sync-button" onClick={() => setShowBlockModal(true)} style={{ background: '#FF6B6B', color: '#fff' }}>
                  <i className="fas fa-ban"></i> 시간대 차단 관리
                </button>
                <input
                  id="student-list-upload"
                  type="file"
                  accept=".xlsx"
                  onChange={handleStudentListUpload}
                  disabled={uploadStudentListMutation.isPending}
                  style={{ display: 'none' }}
                />
              </div>
            )}

        <div className="reservations-content">
          {/* 관리자/선생님용: 시스템 예약 + 네이버 예약 */}
          {!isParent ? (
            <div className="dual-reservations-layout">
              {/* 왼쪽: 시스템 예약 */}
              <div className="system-reservations-section">
                <div className="section-header">
                  <h2><i className="fas fa-calendar-check"></i> 시스템 예약</h2>
                  <span className="count-badge">{reservations.length}건</span>
                </div>

            <div className="reservations-list">
              {reservations.length === 0 ? (
                <div className="empty-state">
                  <i className="fas fa-calendar-alt"></i>
                  <p>{isParent ? '예약 내역이 없습니다.' : '등록된 예약이 없습니다.'}</p>
                </div>
              ) : (
                <>
                  {(showAllSystemReservations ? reservations : reservations.slice(0, 2)).map((reservation) => (
                  <div key={reservation.id} className="reservation-card">
                    <div className="rsv-card-header">
                      <div className="rsv-card-student">
                        <span className="rsv-card-name">{reservation.student?.name || reservation.studentName}</span>
                        {reservation.student?.phone && (
                          <span className="rsv-card-phone">{reservation.student.phone}</span>
                        )}
                      </div>
                      <span className={`rsv-card-status rsv-status-${(reservation.status || '').toLowerCase()}`}>
                        {({PENDING:'대기', CONFIRMED:'확정', CANCELLED:'취소', COMPLETED:'완료', NO_SHOW:'노쇼'})[reservation.status] || reservation.status}
                      </span>
                    </div>

                    <div className="reservation-details">
                      {(() => {
                        const isClassReservation = reservation.consultationType === '재원생수업' || reservation.consultationType === '레벨테스트';
                        const isConsultationReservation = reservation.consultationType === '상담';
                        return (
                          <>
                            <div className="detail-row">
                              <span className="label">유형:</span>
                              <span className={`value ${isClassReservation ? 'class-badge' : 'consultation-badge'}`}>
                                {isClassReservation ? '수업 예약' : isConsultationReservation ? '상담 예약' : (reservation.consultationType || '수업 예약')}
                              </span>
                            </div>
                            {reservation.consultationType && (
                              <div className="detail-row">
                                <span className="label">{isClassReservation ? '수업 유형:' : '상담 유형:'}</span>
                                <span className="value">{reservation.consultationType}</span>
                              </div>
                            )}
                            <div className="detail-row">
                              <span className="label">날짜:</span>
                              <span className="value">{reservation.reservationDate}</span>
                            </div>
                            <div className="detail-row">
                              <span className="label">시간:</span>
                              <span className="value">{reservation.reservationTime}</span>
                            </div>
                            {reservation.notes && (
                              <div className="detail-row">
                                <span className="label">메모:</span>
                                <span className="value">{reservation.notes}</span>
                              </div>
                            )}
                          </>
                        );
                      })()}
                    </div>

                    <div className="reservation-actions">
                      {/* 상담 예약 상세보기 */}
                      {reservation.consultationType === '상담' && reservation.memo && (
                        <button className="btn-secondary" onClick={() => setSelectedConsultation(reservation)}>
                          <i className="fas fa-eye"></i> 상세보기
                        </button>
                      )}
                      {/* 관리자/선생님 전용 액션 */}
                      {!isParent && (
                        <>
                          {reservation.status === 'PENDING' && (
                            <button
                              className="btn-primary"
                              onClick={() => handleConfirm(reservation.id)}
                            >
                              <i className="fas fa-check"></i> 확정
                            </button>
                          )}
                          {reservation.status === 'CONFIRMED' && (
                            <>
                              {canCancelReservation(reservation) ? (
                                <button
                                  className="btn-table-delete"
                                  onClick={() => handleCancel(reservation.id)}
                                >
                                  <i className="fas fa-times"></i> 취소
                                </button>
                              ) : (
                                <>
                                  <span className="cancel-deadline-notice">
                                    취소 마감 (전날 18:00)
                                  </span>
                                  <button
                                    className="btn-secondary"
                                    onClick={() => handleForceCancel(reservation.id)}
                                  >
                                    <i className="fas fa-ban"></i> 관리자 취소
                                  </button>
                                </>
                              )}
                            </>
                          )}
                        </>
                      )}

                      {/* 학부모 전용 액션 */}
                      {isParent && (
                        <>
                          {reservation.status === 'CONFIRMED' && canCancelReservation(reservation) && (
                            <button
                              className="btn-table-delete"
                              onClick={() => handleCancel(reservation.id)}
                            >
                              <i className="fas fa-times"></i> 예약 취소
                            </button>
                          )}
                          {reservation.status === 'CONFIRMED' && !canCancelReservation(reservation) && (
                            <span className="cancel-deadline-notice">
                              취소 불가 (전날 18:00 마감)
                            </span>
                          )}
                        </>
                      )}
                    </div>
                  </div>
                  ))}
                  {reservations.length > 2 && !showAllSystemReservations && (
                    <button 
                      className="show-more-button"
                      onClick={() => setShowAllSystemReservations(true)}
                    >
                      더보기 ({reservations.length - 2}개 더)
                    </button>
                  )}
                  {showAllSystemReservations && (
                    <button 
                      className="show-more-button"
                      onClick={() => setShowAllSystemReservations(false)}
                    >
                      접기
                    </button>
                  )}
                </>
              )}
            </div>
          </div>

          {/* 오른쪽: 네이버 예약 */}
          <div className="naver-reservations-section">
            <div className="section-header">
              <h2><i className="fas fa-globe"></i> 네이버 예약</h2>
              <span className="count-badge">{Array.isArray(naverBookings) ? naverBookings.length : 0}건</span>
            </div>

            <div className="reservations-list">
              {!Array.isArray(naverBookings) || naverBookings.length === 0 ? (
                <div className="empty-state">
                  <i className="fas fa-calendar-alt"></i>
                  <p>네이버 예약 내역이 없습니다</p>
                  <div className="naver-info-badge">
                    <i className="fas fa-info-circle"></i>
                    상단의 동기화 버튼을 눌러 네이버 예약을 가져오세요
                  </div>
                  <button 
                    className="detail-view-button"
                    onClick={() => setShowNaverDetailModal(true)}
                  >
                    자세히 보기
                  </button>
                </div>
              ) : (
                <React.Fragment>
                  {(showAllNaverBookings ? naverBookings : naverBookings.slice(0, 2)).map((booking, index) => (
                    <div key={index} className="reservation-card">
                      <div className="card-header">
                        <div className="student-info">
                          <span className="student-name">{booking.studentName || booking.name}</span>
                          {booking.school && <span className="school-badge">{booking.school}</span>}
                          <span className={`status-badge ${booking.status === 'RC03' ? 'confirmed' : 'cancelled'}`}>
                            {booking.status === 'RC03' ? '확정' : '취소'}
                          </span>
                        </div>
                        <span className="time">{booking.bookingTime}</span>
                      </div>
                      <div className="card-body">
                        <div className="info-row">
                          <i className="fas fa-user"></i>
                          <span>예약자: {booking.name}</span>
                        </div>
                        <div className="info-row">
                          <i className="fas fa-phone"></i>
                          <span>{booking.phone}</span>
                        </div>
                        <div className="info-row">
                          <i className="fas fa-tag"></i>
                          <span>{booking.product}</span>
                        </div>
                      </div>
                    </div>
                  ))}
                  {naverBookings.length > 2 && !showAllNaverBookings && (
                    <button 
                      className="show-more-button"
                      onClick={() => setShowAllNaverBookings(true)}
                    >
                      더보기 ({naverBookings.length - 2}개 더)
                    </button>
                  )}
                  {showAllNaverBookings && (
                    <button 
                      className="show-more-button"
                      onClick={() => setShowAllNaverBookings(false)}
                    >
                      접기
                    </button>
                  )}
                  <button 
                    className="detail-view-button"
                    onClick={() => setShowNaverDetailModal(true)}
                    style={{ marginTop: '16px', width: '100%' }}
                  >
                    자세히 보기
                  </button>
                </React.Fragment>
              )}
            </div>
          </div>
        </div>
          ) : (
            /* 학부모용: 기존 레이아웃 유지 */
            <div className="reservations-section full-width">
              <div className="section-header">
                <h2>내 자녀 예약 현황</h2>
                <span className="count-badge">{reservations.length}건</span>
              </div>

              <div className="reservations-list">
                {reservations.length === 0 ? (
                  <div className="empty-state">
                    <i className="fas fa-calendar-alt"></i>
                    <p>예약 내역이 없습니다.</p>
                  </div>
                ) : (
                  reservations.map((reservation) => (
                    <div key={reservation.id} className="reservation-card">
                      <div className="reservation-header">
                        <div className="student-info">
                          <h3>{reservation.student?.name || reservation.studentName}</h3>
                          {reservation.isConsultation ? (
                            <span className="consultation-type-badge">상담</span>
                          ) : (
                            <span className="class-type-badge">수업</span>
                          )}
                        </div>
                        {getStatusBadge(reservation.status)}
                      </div>
                      <div className="reservation-details">
                        {reservation.isConsultation ? (
                          <>
                            <div className="detail-row">
                              <span className="label">유형:</span>
                              <span className="value consultation-text">
                                <i className="fas fa-comments"></i> {reservation.consultationType || '상담'}
                              </span>
                            </div>
                            <div className="detail-row">
                              <span className="label">날짜:</span>
                              <span className="value">{reservation.reservationDate}</span>
                            </div>
                            <div className="detail-row">
                              <span className="label">시간:</span>
                              <span className="value">{reservation.reservationTime}</span>
                            </div>
                            {reservation.notes && (
                              <div className="detail-row">
                                <span className="label">내용:</span>
                                <span className="value">{reservation.notes}</span>
                              </div>
                            )}
                          </>
                        ) : (
                          <>
                            <div className="detail-row">
                              <span className="label">유형:</span>
                              <span className="value class-text">
                                <i className="fas fa-book"></i> 수업
                              </span>
                            </div>
                            <div className="detail-row">
                              <span className="label">반:</span>
                              <span className="value">{reservation.courseName || "미지정"}</span>
                            </div>
                            <div className="detail-row">
                              <span className="label">날짜:</span>
                              <span className="value">{reservation.reservationDate}</span>
                            </div>
                            <div className="detail-row">
                              <span className="label">시간:</span>
                              <span className="value">{reservation.reservationTime}</span>
                            </div>
                          </>
                        )}
                      </div>
                      <div className="reservation-actions">
                        {reservation.status === 'CONFIRMED' && canCancelReservation(reservation) && (
                          <button
                            className="btn-table-delete"
                            onClick={() => handleCancel(reservation.id)}
                          >
                            <i className="fas fa-times"></i> 예약 취소
                          </button>
                        )}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* 예약 생성 모달 */}
      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>예약 등록</h2>
              <button className="modal-close" onClick={() => setShowCreateModal(false)}>
                ×
              </button>
            </div>

            <div className="modal-body">
              {selectedSchedule && (
                <div className="selected-schedule-info">
                  <h3>{selectedSchedule.course.name}</h3>
                  <p>{selectedSchedule.startTime} - {selectedSchedule.endTime}</p>
                </div>
              )}

              <div className="form-group">
                <label>학생 ID</label>
                <input
                  type="number"
                  value={newReservation.studentId}
                  onChange={(e) =>
                    setNewReservation({ ...newReservation, studentId: e.target.value })
                  }
                  placeholder="학생 ID를 입력하세요"
                />
              </div>

              {newReservation.studentId && (
                <div className="form-group">
                  <label>수강권 선택</label>
                  {enrollmentsLoading ? (
                    <p>수강권 조회 중...</p>
                  ) : enrollments.length === 0 ? (
                    <p className="error-message">사용 가능한 수강권이 없습니다.</p>
                  ) : (
                    <select
                      value={newReservation.enrollmentId}
                      onChange={(e) =>
                        setNewReservation({ ...newReservation, enrollmentId: e.target.value })
                      }
                    >
                      <option value="">수강권을 선택하세요</option>
                      {enrollments.map((enrollment) => (
                        <option key={enrollment.id} value={enrollment.id}>
                          {enrollment.course.name}
                          {enrollment.type === 'COUNT_BASED' &&
                            ` (${enrollment.remainingCount}회 남음)`}
                          {enrollment.type === 'PERIOD_BASED' &&
                            ` (${new Date(enrollment.endDate).toLocaleDateString()}까지)`}
                        </option>
                      ))}
                    </select>
                  )}
                </div>
              )}
            </div>

            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setShowCreateModal(false)}>
                취소
              </button>
              <button
                className="btn-primary"
                onClick={handleCreateReservation}
                disabled={!newReservation.studentId || !newReservation.enrollmentId}
              >
                예약 등록
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 상담 예약 상세 모달 */}
      {selectedConsultation && (
        <div className="modal-overlay" onClick={() => setSelectedConsultation(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{maxWidth: '500px'}}>
            <div className="modal-header">
              <h2><i className="fas fa-comments"></i> 상담 예약 상세</h2>
              <button className="close-button" onClick={() => setSelectedConsultation(null)}>
                <i className="fas fa-times"></i>
              </button>
            </div>
            <div className="modal-body">
              <div className="consult-detail-grid">
                <div className="consult-detail-item"><span className="consult-detail-label">학생명</span><span className="consult-detail-value">{selectedConsultation.student?.name || selectedConsultation.studentName}</span></div>
                <div className="consult-detail-item"><span className="consult-detail-label">상태</span><span className="consult-detail-value">{({PENDING:'대기', CONFIRMED:'확정', CANCELLED:'취소', COMPLETED:'완료', NO_SHOW:'노쇼'})[selectedConsultation.status]}</span></div>
                <div className="consult-detail-item"><span className="consult-detail-label">날짜</span><span className="consult-detail-value">{selectedConsultation.reservationDate}</span></div>
                <div className="consult-detail-item"><span className="consult-detail-label">시간</span><span className="consult-detail-value">{selectedConsultation.reservationTime}</span></div>
              </div>
              <div className="consult-detail-content">
                <span className="consult-detail-label">상담 내용</span>
                <div className="consult-detail-memo">{selectedConsultation.memo}</div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 네이버 예약 상세 모달 */}
      {showNaverDetailModal && (
        <div className="modal-overlay" onClick={() => setShowNaverDetailModal(false)}>
          <div className="modal-content naver-detail-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2><i className="fas fa-globe"></i> 네이버 예약 상세</h2>
              <button className="close-button" onClick={() => setShowNaverDetailModal(false)}>
                <i className="fas fa-times"></i>
              </button>
            </div>
            <div className="modal-body">
              <div className="naver-table-container">
                <table className="naver-booking-table">
                  <thead>
                    <tr>
                      <th>상태</th>
                      <th>학생이름</th>
                      <th>학교</th>
                      <th>예약자</th>
                      <th>전화번호</th>
                      <th>예약번호</th>
                      <th>이용일시</th>
                      <th>상품</th>
                    </tr>
                  </thead>
                  <tbody>
                    {!Array.isArray(naverBookings) || naverBookings.length === 0 ? (
                      <tr>
                        <td colSpan="14" className="empty-data">
                          <i className="fas fa-inbox"></i>
                          <p>네이버 예약 데이터가 없습니다</p>
                        </td>
                      </tr>
                    ) : (
                      naverBookings.map((booking, index) => (
                        <tr key={index}>
                          <td>
                            <span className={`status-label ${booking.status === 'RC03' ? 'confirmed' : 'cancelled'}`}>
                              {booking.status === 'RC03' ? '확정' : '취소'}
                            </span>
                          </td>
                          <td>{booking.studentName || booking.name}</td>
                          <td>{booking.school || '-'}</td>
                          <td>{booking.name}</td>
                          <td>{booking.phone}</td>
                          <td>{booking.bookingNumber}</td>
                          <td>{booking.bookingTime}</td>
                          <td>{booking.product}</td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 시간대 차단 관리 모달 */}
      {showBlockModal && (
        <div className="modal-overlay" onClick={() => setShowBlockModal(false)}>
          <div className="modal-content" style={{ maxWidth: 640 }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>시간대 차단 관리</h2>
              <button className="modal-close" onClick={() => setShowBlockModal(false)}>×</button>
            </div>
            <div className="modal-body">
              {/* 탭 */}
              <div style={{ display: 'flex', gap: 0, marginBottom: 20, borderBottom: '2px solid #eee' }}>
                {[['CLASS','수업'], ['CONSULTATION','상담']].map(([key, label]) => (
                  <button key={key} type="button"
                    style={{ flex: 1, padding: '10px 0', border: 'none', borderBottom: blockTab === key ? '2px solid #03C75A' : '2px solid transparent', background: 'none', fontWeight: blockTab === key ? 700 : 400, color: blockTab === key ? '#03C75A' : '#999', fontSize: 15, cursor: 'pointer', marginBottom: -2 }}
                    onClick={() => { setBlockTab(key); setBlockForm(f => ({ ...f, targetType: key })); }}
                  >{label}</button>
                ))}
              </div>
              {/* 새 차단 추가 */}
              <div style={{ marginBottom: 24, padding: 16, background: '#f9f9f9', borderRadius: 8 }}>
                <h3 style={{ margin: '0 0 12px', fontSize: 15 }}>새 차단 추가</h3>
                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 12 }}>
                  {['SINGLE','RANGE','WEEKLY'].map(t => (
                    <button key={t} type="button"
                      style={{ padding: '6px 14px', borderRadius: 6, border: blockForm.blockType === t ? '2px solid #03C75A' : '1px solid #ddd', background: blockForm.blockType === t ? '#E8F8EE' : '#fff', fontWeight: 600, fontSize: 13, cursor: 'pointer' }}
                      onClick={() => setBlockForm(f => ({ ...f, blockType: t }))}
                    >
                      {t === 'SINGLE' ? '특정 날짜' : t === 'RANGE' ? '기간' : '매주 반복'}
                    </button>
                  ))}
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginBottom: 12 }}>
                  {blockForm.blockType === 'SINGLE' && (
                    <div>
                      <label style={{ fontSize: 12, color: '#888' }}>날짜</label>
                      <input type="date" value={blockForm.blockDate} onChange={e => setBlockForm(f => ({ ...f, blockDate: e.target.value }))} style={{ width: '100%', padding: 8, border: '1px solid #ddd', borderRadius: 6 }} />
                    </div>
                  )}
                  {blockForm.blockType === 'RANGE' && (
                    <>
                      <div>
                        <label style={{ fontSize: 12, color: '#888' }}>시작일</label>
                        <input type="date" value={blockForm.startDate} onChange={e => setBlockForm(f => ({ ...f, startDate: e.target.value }))} style={{ width: '100%', padding: 8, border: '1px solid #ddd', borderRadius: 6 }} />
                      </div>
                      <div>
                        <label style={{ fontSize: 12, color: '#888' }}>종료일</label>
                        <input type="date" value={blockForm.endDate} onChange={e => setBlockForm(f => ({ ...f, endDate: e.target.value }))} style={{ width: '100%', padding: 8, border: '1px solid #ddd', borderRadius: 6 }} />
                      </div>
                    </>
                  )}
                  {blockForm.blockType === 'WEEKLY' && (
                    <div>
                      <label style={{ fontSize: 12, color: '#888' }}>요일</label>
                      <select value={blockForm.dayOfWeek} onChange={e => setBlockForm(f => ({ ...f, dayOfWeek: e.target.value }))} style={{ width: '100%', padding: 8, border: '1px solid #ddd', borderRadius: 6 }}>
                        <option value="">선택</option>
                        {Object.entries(DAY_NAMES).map(([k, v]) => <option key={k} value={k}>{v}요일</option>)}
                      </select>
                    </div>
                  )}
                  <div>
                    <label style={{ fontSize: 12, color: '#888' }}>시간</label>
                    <select value={blockForm.blockTime} onChange={e => setBlockForm(f => ({ ...f, blockTime: e.target.value }))} style={{ width: '100%', padding: 8, border: '1px solid #ddd', borderRadius: 6 }}>
                      <option value="">선택</option>
                      {TIME_SLOTS.map(t => <option key={t} value={t}>{t}</option>)}
                    </select>
                  </div>
                </div>
                <input placeholder="차단 사유 (선택)" value={blockForm.reason} onChange={e => setBlockForm(f => ({ ...f, reason: e.target.value }))} style={{ width: '100%', padding: 8, border: '1px solid #ddd', borderRadius: 6, marginBottom: 12 }} />
                <button onClick={() => {
                  if (!blockForm.blockTime) return alert('시간을 선택해주세요');
                  if (blockForm.blockType === 'SINGLE' && !blockForm.blockDate) return alert('날짜를 선택해주세요');
                  if (blockForm.blockType === 'RANGE' && (!blockForm.startDate || !blockForm.endDate)) return alert('기간을 선택해주세요');
                  if (blockForm.blockType === 'WEEKLY' && !blockForm.dayOfWeek) return alert('요일을 선택해주세요');
                  // 중복 체크
                  const dup = blockedSlots.filter(s => (s.targetType || 'CLASS') === blockTab).some(s => {
                    if (s.blockTime?.substring(0,5) !== blockForm.blockTime) return false;
                    if (s.blockType !== blockForm.blockType) return false;
                    if (blockForm.blockType === 'SINGLE') return s.blockDate === blockForm.blockDate;
                    if (blockForm.blockType === 'WEEKLY') return s.dayOfWeek === blockForm.dayOfWeek;
                    if (blockForm.blockType === 'RANGE') return s.startDate === blockForm.startDate && s.endDate === blockForm.endDate;
                    return false;
                  });
                  if (dup) return alert('이미 동일한 차단이 등록되어 있습니다.');
                  createBlockMutation.mutate(blockForm);
                }} style={{ padding: '8px 20px', background: '#FF6B6B', color: '#fff', border: 'none', borderRadius: 6, fontWeight: 600, cursor: 'pointer' }}>
                  차단 추가
                </button>
              </div>

              {/* 차단 목록 */}
              <h3 style={{ margin: '0 0 12px', fontSize: 15 }}>현재 차단 목록 ({blockedSlots.filter(s => (s.targetType || 'CLASS') === blockTab).length}건)</h3>
              {blockedSlots.filter(s => (s.targetType || 'CLASS') === blockTab).length === 0 ? (
                <p style={{ color: '#999', textAlign: 'center', padding: 20 }}>차단된 시간이 없습니다</p>
              ) : (
                <div style={{ maxHeight: 300, overflowY: 'auto' }}>
                  {blockedSlots.filter(s => (s.targetType || 'CLASS') === blockTab).map(slot => (
                    <div key={slot.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 12px', borderBottom: '1px solid #f0f0f0' }}>
                      <div>
                        <span style={{ fontWeight: 600, marginRight: 8 }}>{slot.blockTime}</span>
                        <span style={{ fontSize: 13, color: '#666' }}>
                          {slot.blockType === 'SINGLE' && slot.blockDate}
                          {slot.blockType === 'RANGE' && `${slot.startDate} ~ ${slot.endDate}`}
                          {slot.blockType === 'WEEKLY' && `매주 ${DAY_NAMES[slot.dayOfWeek]}요일`}
                        </span>
                        {slot.reason && <span style={{ fontSize: 12, color: '#999', marginLeft: 8 }}>({slot.reason})</span>}
                      </div>
                      <button onClick={() => { if (window.confirm('차단을 해제하시겠습니까?')) deleteBlockMutation.mutate(slot.id); }}
                        style={{ padding: '4px 10px', background: '#fff', border: '1px solid #ddd', borderRadius: 4, fontSize: 12, cursor: 'pointer', color: '#FF3B30' }}>
                        해제
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
        </div>
      </div>
    </div>
  );
}

export default Reservations;
