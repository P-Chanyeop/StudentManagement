import { useState, useEffect } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { publicLevelTestAPI, siteSettingAPI } from '../services/api';
import { holidayService } from '../services/holidayService';
import { getLocalDateString } from '../utils/dateUtils';
import '../styles/ParentReservation.css';

function LevelTest() {
  const [formData, setFormData] = useState({
    parentName: '', parentPhone: '', studentName: '', school: '', studentPhone: '',
    preferredDate: '', preferredTime: '', memo: ''
  });
  const [errors, setErrors] = useState({});
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const [timeSlotStatuses, setTimeSlotStatuses] = useState([]);
  const [success, setSuccess] = useState(false);

  const { data: availableDates } = useQuery({
    queryKey: ['availableDates'],
    queryFn: async () => (await publicLevelTestAPI.getAvailableDates()).data,
  });

  const { data: infoSetting } = useQuery({
    queryKey: ['siteSetting', 'reservation_info_leveltest'],
    queryFn: async () => (await siteSettingAPI.get('reservation_info_leveltest')).data,
  });

  const holidays = holidayService.getHolidaysForYears([new Date().getFullYear(), new Date().getFullYear() + 1]);

  const fetchTimeSlots = async (date) => {
    try {
      const res = await publicLevelTestAPI.getTimeSlotStatus(date);
      setTimeSlotStatuses(res.data);
    } catch { setTimeSlotStatuses([]); }
  };

  useEffect(() => {
    if (formData.preferredDate) fetchTimeSlots(formData.preferredDate);
  }, [formData.preferredDate]);

  const reservationMutation = useMutation({
    mutationFn: (data) => publicLevelTestAPI.create(data),
    onSuccess: () => setSuccess(true),
    onError: (err) => alert(err.response?.data?.message || '예약에 실패했습니다.'),
  });

  const infoData = (() => {
    if (infoSetting?.value) {
      try { return JSON.parse(infoSetting.value); } catch {}
    }
    return {
      title: "레벨테스트",
      content: `리틀베어 리딩클럽이 처음인 학부모님들을 대상으로\n레벨테스트 응시시 체험 수업 1회를 무료로 제공해드리는 이벤트를 진행합니다:)\n\n*레벨테스트와 체험수업은 총 1시간 20분 가량 소요 예정입니다.\n*레벨테스트 비용은 2만원입니다.\n*평일 한정 이벤트 입니다.`
    };
  })();

  const timeSlots = (() => {
    if (!formData.preferredDate) return [];
    const day = new Date(formData.preferredDate + 'T00:00:00').getDay();
    if (day === 0) return [];
    if (day === 6) return ['09:00', '10:00', '11:00', '12:00', '13:00'];
    return ['09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00'];
  })();

  const formatDate = (y, m, d) => `${y}-${String(m + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;

  const isDateSelectable = (year, month, day) => {
    const date = new Date(year, month, day);
    const today = new Date(); today.setHours(0, 0, 0, 0);
    if (date < today || date.getDay() === 0) return false;
    const yearH = holidays[year] || [];
    if (holidayService.isHoliday(date, yearH)) return false;
    if (!availableDates?.startDate || !availableDates?.endDate) return false;
    return date >= new Date(availableDates.startDate + 'T00:00:00') && date <= new Date(availableDates.endDate + 'T23:59:59');
  };

  const handleDateSelect = (year, month, day) => {
    if (!isDateSelectable(year, month, day)) return;
    const d = formatDate(year, month, day);
    setFormData(prev => ({ ...prev, preferredDate: d, preferredTime: '' }));
    fetchTimeSlots(d);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const newErrors = {};
    if (!formData.parentName) newErrors.parentName = '필수';
    if (!formData.parentPhone) newErrors.parentPhone = '필수';
    if (!formData.studentName) newErrors.studentName = '필수';
    if (!formData.preferredDate) newErrors.preferredDate = '날짜를 선택해주세요';
    if (!formData.preferredTime) newErrors.preferredTime = '시간을 선택해주세요';
    if (Object.keys(newErrors).length) { setErrors(newErrors); return; }

    reservationMutation.mutate({
      reservationDate: formData.preferredDate,
      reservationTime: formData.preferredTime + ':00',
      memo: formData.memo,
      consultationType: '레벨테스트',
      parentName: formData.parentName,
      parentPhone: formData.parentPhone,
      students: [{ studentName: formData.studentName, studentPhone: formData.studentPhone, school: formData.school }],
    });
  };

  const renderCalendar = () => {
    const year = currentMonth.getFullYear(), month = currentMonth.getMonth();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const firstDay = new Date(year, month, 1).getDay();
    const days = [];
    for (let i = 0; i < firstDay; i++) days.push(<div key={`e-${i}`} className="calendar-day empty" />);
    for (let day = 1; day <= daysInMonth; day++) {
      const date = new Date(year, month, day);
      const selectable = isDateSelectable(year, month, day);
      const selected = formData.preferredDate === formatDate(year, month, day);
      const yearH = holidays[year] || [];
      const isHol = holidayService.isHoliday(date, yearH);
      const isWknd = holidayService.isWeekend(date);
      days.push(
        <div key={day}
          className={`calendar-day ${!selectable ? 'disabled' : 'available'} ${selected ? 'selected' : ''} ${isHol ? 'holiday' : ''} ${isWknd ? 'weekend' : ''}`}
          onClick={() => selectable && handleDateSelect(year, month, day)}>
          {day}
        </div>
      );
    }
    return days;
  };

  if (success) {
    return (
      <div className="page-wrapper" style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#f5f7fa' }}>
        <div style={{ textAlign: 'center', padding: 40, background: '#fff', borderRadius: 16, boxShadow: '0 2px 12px rgba(0,0,0,0.08)', maxWidth: 400 }}>
          <div style={{ fontSize: 48, marginBottom: 16 }}>✅</div>
          <h2 style={{ marginBottom: 8 }}>레벨테스트 예약이 완료되었습니다!</h2>
          <p style={{ color: '#666' }}>확인 후 연락드리겠습니다.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="page-wrapper" style={{ minHeight: '100vh', background: '#f5f7fa' }}>
      <div style={{ maxWidth: 600, margin: '0 auto', padding: '24px 16px' }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <h1 style={{ fontSize: 22, fontWeight: 700 }}>🐻 리틀베어 리딩클럽</h1>
          <p style={{ color: '#666', fontSize: 14, marginTop: 4 }}>레벨테스트 예약</p>
        </div>

        {infoData && (
          <div className="pr-info-card">
            <h3 className="pr-info-title">{infoData.title}</h3>
            <p className="pr-info-content">{infoData.content}</p>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="pr-form-card">
            <h3 className="pr-card-title">학부모 정보</h3>
            <div className="pr-form-group">
              <label>학부모 성함 *</label>
              <input value={formData.parentName} onChange={e => setFormData(p => ({ ...p, parentName: e.target.value }))} className={errors.parentName ? 'error' : ''} placeholder="성함" />
              {errors.parentName && <span className="error-message">{errors.parentName}</span>}
            </div>
            <div className="pr-form-group">
              <label>학부모 전화번호 *</label>
              <input value={formData.parentPhone} onChange={e => setFormData(p => ({ ...p, parentPhone: e.target.value }))} className={errors.parentPhone ? 'error' : ''} placeholder="010-0000-0000" />
              {errors.parentPhone && <span className="error-message">{errors.parentPhone}</span>}
            </div>
          </div>

          <div className="pr-form-card">
            <h3 className="pr-card-title">학생 정보</h3>
            <div className="pr-form-group">
              <label>학생 이름 *</label>
              <input value={formData.studentName} onChange={e => setFormData(p => ({ ...p, studentName: e.target.value }))} className={errors.studentName ? 'error' : ''} placeholder="이름" />
              {errors.studentName && <span className="error-message">{errors.studentName}</span>}
            </div>
            <div className="pr-form-group">
              <label>학교명</label>
              <input value={formData.school} onChange={e => setFormData(p => ({ ...p, school: e.target.value }))} placeholder="예: 정목초등학교" />
            </div>
            <div className="pr-form-group">
              <label>학생 전화번호</label>
              <input value={formData.studentPhone} onChange={e => setFormData(p => ({ ...p, studentPhone: e.target.value }))} placeholder="010-0000-0000" />
            </div>
          </div>

          <div className="pr-form-card">
            <h3 className="pr-card-title">날짜 선택 *</h3>
            {errors.preferredDate && <span className="error-message">{errors.preferredDate}</span>}
            <div className="calendar-container">
              <div className="calendar-header">
                <button type="button" onClick={() => setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1))}>◀</button>
                <span className="calendar-title">{currentMonth.getFullYear()}년 {currentMonth.getMonth() + 1}월</span>
                <button type="button" onClick={() => setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1))}>▶</button>
              </div>
              <div className="calendar-weekdays">
                {['일', '월', '화', '수', '목', '금', '토'].map(d => <div key={d} className="weekday">{d}</div>)}
              </div>
              <div className="calendar-grid">{renderCalendar()}</div>
            </div>
          </div>

          {formData.preferredDate && (
            <div className="pr-form-card">
              <h3 className="pr-card-title">시간 선택 *</h3>
              {errors.preferredTime && <span className="error-message">{errors.preferredTime}</span>}
              <div className="time-slots-grid">
                {timeSlots.map(time => {
                  const status = timeSlotStatuses.find(s => s.time === time);
                  const isFull = status?.status === 'FULL';
                  const isBlocked = status?.status === 'BLOCKED';
                  const disabled = isFull || isBlocked;
                  return (
                    <button key={time} type="button" disabled={disabled}
                      className={`time-slot-btn ${formData.preferredTime === time ? 'selected' : ''} ${isFull ? 'full' : ''} ${isBlocked ? 'blocked' : ''}`}
                      onClick={() => !disabled && setFormData(p => ({ ...p, preferredTime: time }))}>
                      {time}
                      {isFull && <span className="slot-status">마감</span>}
                      {isBlocked && <span className="slot-status">차단</span>}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          <div className="pr-form-card">
            <h3 className="pr-card-title">메모</h3>
            <div className="pr-form-group">
              <textarea value={formData.memo} onChange={e => setFormData(p => ({ ...p, memo: e.target.value }))}
                placeholder="아이 이름, 학교 및 학년, SR 점수 등" rows={3} />
            </div>
          </div>

          <button type="submit" className="pr-submit-btn" disabled={reservationMutation.isPending}
            style={{ width: '100%', marginTop: 16 }}>
            {reservationMutation.isPending ? '예약 중...' : '레벨테스트 예약하기'}
          </button>
        </form>
      </div>
    </div>
  );
}

export default LevelTest;
