import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { reservationAPI, scheduleAPI, enrollmentAPI } from '../services/api';
import '../styles/Reservations.css';

function Reservations() {
  const queryClient = useQueryClient();
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedSchedule, setSelectedSchedule] = useState(null);
  const [newReservation, setNewReservation] = useState({
    studentId: '',
    scheduleId: '',
    enrollmentId: '',
  });

  // 날짜별 예약 조회
  const { data: reservations = [], isLoading: reservationsLoading } = useQuery({
    queryKey: ['reservations', selectedDate],
    queryFn: async () => {
      const response = await reservationAPI.getByDate(selectedDate);
      return response.data;
    },
  });

  // 날짜별 스케줄 조회
  const { data: schedules = [], isLoading: schedulesLoading } = useQuery({
    queryKey: ['schedules', selectedDate],
    queryFn: async () => {
      const response = await scheduleAPI.getByDate(selectedDate);
      return response.data;
    },
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
      alert('예약이 완료되었습니다.');
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

  // 취소 가능 여부 확인 (수업 전날 오후 6시까지)
  const canCancelReservation = (reservation) => {
    const scheduleDate = new Date(reservation.schedule.scheduleDate);
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
    return <div className="reservations-container">로딩 중...</div>;
  }

  return (
    <div className="reservations-container">
      <div className="reservations-header">
        <h1>예약 관리</h1>
        <div className="date-selector">
          <input
            type="date"
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
          />
        </div>
      </div>

      <div className="reservations-content">
        {/* 왼쪽: 스케줄 목록 */}
        <div className="schedules-section">
          <div className="section-header">
            <h2>수업 스케줄</h2>
            <span className="count-badge">{schedules.length}개</span>
          </div>

          <div className="schedules-grid">
            {schedules.length === 0 ? (
              <div className="empty-state">오늘 예정된 수업이 없습니다.</div>
            ) : (
              schedules.map((schedule) => {
                const reservationCount = reservations.filter(
                  (r) => r.schedule.id === schedule.id && r.status !== 'CANCELLED'
                ).length;
                const isAvailable = reservationCount < schedule.capacity;

                return (
                  <div key={schedule.id} className="schedule-card">
                    <div className="schedule-info">
                      <h3>{schedule.course.name}</h3>
                      <p className="schedule-time">
                        {schedule.startTime} - {schedule.endTime}
                      </p>
                      <p className="schedule-teacher">
                        강사: {schedule.teacher.name}
                      </p>
                      <div className="schedule-capacity">
                        <span className={`capacity-badge ${isAvailable ? 'available' : 'full'}`}>
                          {reservationCount} / {schedule.capacity}
                        </span>
                        {isAvailable ? (
                          <span className="availability">예약 가능</span>
                        ) : (
                          <span className="availability full">정원 마감</span>
                        )}
                      </div>
                    </div>
                    {isAvailable && (
                      <button
                        className="btn-create"
                        onClick={() => openCreateModal(schedule)}
                      >
                        예약 등록
                      </button>
                    )}
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* 오른쪽: 예약 목록 */}
        <div className="reservations-section">
          <div className="section-header">
            <h2>예약 현황</h2>
            <span className="count-badge">{reservations.length}건</span>
          </div>

          <div className="reservations-list">
            {reservations.length === 0 ? (
              <div className="empty-state">등록된 예약이 없습니다.</div>
            ) : (
              reservations.map((reservation) => (
                <div key={reservation.id} className="reservation-card">
                  <div className="reservation-header">
                    <div className="student-info">
                      <h3>{reservation.student.name}</h3>
                      <span className="student-contact">{reservation.student.phone}</span>
                    </div>
                    {getStatusBadge(reservation.status)}
                  </div>

                  <div className="reservation-details">
                    <div className="detail-row">
                      <span className="label">수업:</span>
                      <span className="value">{reservation.schedule.course.name}</span>
                    </div>
                    <div className="detail-row">
                      <span className="label">시간:</span>
                      <span className="value">
                        {reservation.schedule.startTime} - {reservation.schedule.endTime}
                      </span>
                    </div>
                    <div className="detail-row">
                      <span className="label">강사:</span>
                      <span className="value">{reservation.schedule.teacher.name}</span>
                    </div>
                    <div className="detail-row">
                      <span className="label">수강권:</span>
                      <span className="value">
                        {reservation.enrollment.course.name}
                        {reservation.enrollment.type === 'COUNT_BASED' && (
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
                    {reservation.status === 'PENDING' && (
                      <button
                        className="btn-confirm"
                        onClick={() => handleConfirm(reservation.id)}
                      >
                        확정
                      </button>
                    )}
                    {reservation.status === 'CONFIRMED' && (
                      <>
                        {canCancelReservation(reservation) ? (
                          <button
                            className="btn-cancel"
                            onClick={() => handleCancel(reservation.id)}
                          >
                            취소
                          </button>
                        ) : (
                          <>
                            <span className="cancel-deadline-notice">
                              취소 마감 (전날 18:00)
                            </span>
                            <button
                              className="btn-force-cancel"
                              onClick={() => handleForceCancel(reservation.id)}
                            >
                              관리자 취소
                            </button>
                          </>
                        )}
                      </>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
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
                  <p>강사: {selectedSchedule.teacher.name}</p>
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
    </div>
  );
}

export default Reservations;
