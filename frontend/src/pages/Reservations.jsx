import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import LoadingSpinner from '../components/LoadingSpinner';
import { reservationAPI, scheduleAPI, enrollmentAPI, authAPI, naverBookingAPI } from '../services/api';
import '../styles/Reservations.css';

function Reservations() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showNaverDetailModal, setShowNaverDetailModal] = useState(false);
  const [naverBookings, setNaverBookings] = useState([]);
  const [showAllNaverBookings, setShowAllNaverBookings] = useState(false);
  const [showAllSystemReservations, setShowAllSystemReservations] = useState(false);
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

  // 네이버 예약 조회
  const { data: naverBookingsData } = useQuery({
    queryKey: ['naverBookings'],
    queryFn: () => naverBookingAPI.getToday(),
    enabled: !isParent,
  });
  
  // 네이버 예약 데이터 업데이트 (선택된 날짜로 필터링)
  React.useEffect(() => {
    if (naverBookingsData?.data) {
      const filtered = naverBookingsData.data.filter(booking => {
        const bookingDate = booking.orderDate?.split(' ')[0]; // "2026-01-15 09:00" -> "2026-01-15"
        return bookingDate === selectedDate;
      });
      setNaverBookings(filtered);
    }
  }, [naverBookingsData, selectedDate]);
  
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
        const response = await reservationAPI.getMyReservations();
        const year = currentMonth.getFullYear();
        const month = currentMonth.getMonth() + 1;
        const startDate = `${year}-${String(month).padStart(2, '0')}-01`;
        const endDate = `${year}-${String(month).padStart(2, '0')}-${new Date(year, month, 0).getDate()}`;
        
        return response.data.filter(r => {
          const reservationDate = r.scheduleDate || r.schedule?.scheduleDate;
          return reservationDate >= startDate && reservationDate <= endDate;
        });
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
        const response = await reservationAPI.getMyReservations();
        return response.data.filter(r => {
          const reservationDate = r.scheduleDate || r.schedule?.scheduleDate;
          return reservationDate === selectedDate;
        });
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
    mutationFn: () => naverBookingAPI.sync(),
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
      cancelMutation.mutate(reservationId);
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
    const scheduleDate = new Date(reservation.schedule?.scheduleDate || reservation.scheduleDate);
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
            <div className="selected-date-header">
              <h2>{new Date(selectedDate).toLocaleDateString('ko-KR', { 
                year: 'numeric', 
                month: 'long', 
                day: 'numeric',
                weekday: 'long'
              })} 예약 현황</h2>
              <span className="count-badge">{reservations.length}건</span>
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
                    <span className="stat-value">0건</span>
                  </div>
                  <div className="stat-divider"></div>
                  <div className="stat-item total">
                    <span className="stat-label">총</span>
                    <span className="stat-value">{reservations.length}건</span>
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
                    <div className="reservation-header">
                      <div className="student-info">
                        <h3>{reservation.student?.name || reservation.studentName}</h3>
                        <span className="student-contact">{reservation.student?.phone}</span>
                      </div>
                      {getStatusBadge(reservation.status)}
                    </div>

                    <div className="reservation-details">
                      <div className="detail-row">
                        <span className="label">수업:</span>
                        <span className="value">{reservation.schedule?.course?.name || reservation.courseName}</span>
                      </div>
                      <div className="detail-row">
                        <span className="label">날짜:</span>
                        <span className="value">{reservation.schedule?.scheduleDate || reservation.scheduleDate}</span>
                      </div>
                      <div className="detail-row">
                        <span className="label">시간:</span>
                        <span className="value">
                          {reservation.schedule?.startTime || reservation.startTime} - {reservation.schedule?.endTime || reservation.endTime}
                        </span>
                      </div>
                      <div className="detail-row">
                        <span className="label">수강권:</span>
                        <span className="value">
                          {reservation.enrollment?.course?.name || '수강권 정보 없음'}
                          {reservation.enrollment?.type === 'COUNT_BASED' && (
                            <span className="remaining-count">
                              ({reservation.enrollment.remainingCount}회 남음)
                            </span>
                          )}
                        </span>
                      </div>
                      {reservation.notes && (
                        <div className="detail-row">
                          <span className="label">메모:</span>
                          <span className="value">{reservation.notes}</span>
                        </div>
                      )}
                    </div>

                    <div className="reservation-actions">
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
                          <span className="student-name">{booking.name}</span>
                          <span className={`status-badge ${booking.status === '확정' ? 'confirmed' : 'cancelled'}`}>
                            {booking.status}
                          </span>
                        </div>
                        <span className="time">{booking.bookingTime}</span>
                      </div>
                      <div className="card-body">
                        <div className="info-row">
                          <i className="fas fa-phone"></i>
                          <span>{booking.phone}</span>
                        </div>
                        <div className="info-row">
                          <i className="fas fa-tag"></i>
                          <span>{booking.product}</span>
                        </div>
                        <div className="info-row">
                          <i className="fas fa-users"></i>
                          <span>{booking.quantity}명</span>
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
                        </div>
                        {getStatusBadge(reservation.status)}
                      </div>
                      <div className="reservation-details">
                        <div className="detail-row">
                          <span className="label">수업:</span>
                          <span className="value">{reservation.schedule?.course?.name || reservation.courseName}</span>
                        </div>
                        <div className="detail-row">
                          <span className="label">시간:</span>
                          <span className="value">
                            {reservation.schedule?.startTime || reservation.startTime} - {reservation.schedule?.endTime || reservation.endTime}
                          </span>
                        </div>
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
                      <th>이름</th>
                      <th>전화번호</th>
                      <th>예약번호</th>
                      <th>이용일시</th>
                      <th>상품</th>
                      <th>인원</th>
                      <th>옵션</th>
                      <th>요청사항</th>
                      <th>예약금</th>
                      <th>결제금액</th>
                      <th>신청일시</th>
                      <th>확정일시</th>
                      <th>취소일시</th>
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
                            <span className={`status-label ${booking.status === '확정' ? 'confirmed' : 'cancelled'}`}>
                              {booking.status}
                            </span>
                          </td>
                          <td>{booking.name}</td>
                          <td>{booking.phone}</td>
                          <td>{booking.bookingNumber}</td>
                          <td>{booking.bookingTime}</td>
                          <td>{booking.product}</td>
                          <td>{booking.quantity}</td>
                          <td>{booking.option || '-'}</td>
                          <td>{booking.comment || '-'}</td>
                          <td>{booking.deposit || '-'}</td>
                          <td>{booking.totalPrice}</td>
                          <td>{booking.orderDate}</td>
                          <td>{booking.confirmDate}</td>
                          <td>{booking.cancelDate || '-'}</td>
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
        </div>
      </div>
    </div>
  );
}

export default Reservations;
