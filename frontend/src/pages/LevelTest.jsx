import { useState, useEffect } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { publicLevelTestAPI, siteSettingAPI } from '../services/api';
import { holidayService } from '../services/holidayService';
import '../styles/LevelTest.css';

function LevelTest() {
  const [formData, setFormData] = useState({
    parentName: '', parentPhone: '', studentName: '', school: '', studentPhone: '',
    preferredDate: '', preferredTime: '', memo: ''
  });
  const [errors, setErrors] = useState({});
  const [currentMonth, setCurrentMonth] = useState(new Date());
  const [timeSlotStatuses, setTimeSlotStatuses] = useState([]);
  const [success, setSuccess] = useState(false);
  const [holidays, setHolidays] = useState({});

  const formatPhone = (val) => {
    const v = val.replace(/[^0-9]/g, '');
    if (v.length > 7) return v.slice(0,3)+'-'+v.slice(3,7)+'-'+v.slice(7,11);
    if (v.length > 3) return v.slice(0,3)+'-'+v.slice(3);
    return v;
  };

  const { data: availableDates } = useQuery({
    queryKey: ['ltAvailableDates'],
    queryFn: async () => (await publicLevelTestAPI.getAvailableDates()).data,
  });

  const { data: infoSetting } = useQuery({
    queryKey: ['siteSetting', 'reservation_info_leveltest'],
    queryFn: async () => (await siteSettingAPI.get('reservation_info_leveltest')).data,
  });

  useEffect(() => {
    const load = async (year) => {
      try {
        const h = await holidayService.getHolidays(year);
        setHolidays(prev => ({ ...prev, [year]: h }));
      } catch {
        setHolidays(prev => ({ ...prev, [year]: holidayService.getDefaultHolidays(year) }));
      }
    };
    const y = new Date().getFullYear();
    load(y); load(y + 1);
  }, []);

  useEffect(() => {
    if (formData.preferredDate) {
      publicLevelTestAPI.getTimeSlotStatus(formData.preferredDate)
        .then(res => setTimeSlotStatuses(res.data))
        .catch(() => setTimeSlotStatuses([]));
    }
  }, [formData.preferredDate]);

  const reservationMutation = useMutation({
    mutationFn: (data) => publicLevelTestAPI.create(data),
    onSuccess: () => setSuccess(true),
    onError: (err) => alert(err.response?.data?.message || '예약에 실패했습니다.'),
  });

  const infoData = (() => {
    if (infoSetting?.value) { try { return JSON.parse(infoSetting.value); } catch {} }
    return { title: "레벨테스트", content: "리틀베어 리딩클럽이 처음인 학부모님들을 대상으로\n레벨테스트 응시시 체험 수업 1회를 무료로 제공해드리는 이벤트를 진행합니다:)\n\n*레벨테스트와 체험수업은 총 1시간 20분 가량 소요 예정입니다.\n*레벨테스트 비용은 2만원입니다.\n*평일 한정 이벤트 입니다." };
  })();

  const timeSlots = (() => {
    if (!formData.preferredDate) return [];
    const day = new Date(formData.preferredDate + 'T00:00:00').getDay();
    if (day === 0) return [];
    if (day === 6) return ['09:00', '10:00', '11:00', '12:00', '13:00'];
    return ['09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00'];
  })();

  const fmt = (y, m, d) => `${y}-${String(m + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;

  const isSelectable = (year, month, day) => {
    const date = new Date(year, month, day);
    const today = new Date(); today.setHours(0, 0, 0, 0);
    if (date < today || date.getDay() === 0) return false;
    const yh = holidays[year] || [];
    if (holidayService.isHoliday(date, yh)) return false;
    if (!availableDates?.startDate || !availableDates?.endDate) return false;
    return date >= new Date(availableDates.startDate + 'T00:00:00') && date <= new Date(availableDates.endDate + 'T23:59:59');
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const ne = {};
    if (!formData.parentName) ne.parentName = '필수';
    if (!formData.parentPhone) ne.parentPhone = '필수';
    if (!formData.studentName) ne.studentName = '필수';
    if (!formData.preferredDate) ne.preferredDate = '날짜를 선택해주세요';
    if (!formData.preferredTime) ne.preferredTime = '시간을 선택해주세요';
    if (Object.keys(ne).length) { setErrors(ne); return; }
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
    for (let i = 0; i < firstDay; i++) days.push(<div key={`e-${i}`} className="lt-cal-day empty" />);
    for (let day = 1; day <= daysInMonth; day++) {
      const date = new Date(year, month, day);
      const ok = isSelectable(year, month, day);
      const sel = formData.preferredDate === fmt(year, month, day);
      const yh = holidays[year] || [];
      const hol = holidayService.isHoliday(date, yh);
      const wknd = date.getDay() === 0 || date.getDay() === 6;
      days.push(
        <div key={day}
          className={`lt-cal-day ${!ok ? 'disabled' : 'available'} ${sel ? 'selected' : ''} ${hol ? 'holiday' : ''} ${wknd ? 'weekend' : ''}`}
          onClick={() => ok && setFormData(p => ({ ...p, preferredDate: fmt(year, month, day), preferredTime: '' }))}>
          {day}
        </div>
      );
    }
    return days;
  };

  if (success) {
    return (
      <div className="lt-success">
        <div className="lt-success-card">
          <div className="lt-success-icon">✅</div>
          <h2 className="lt-success-title">레벨테스트 예약이 완료되었습니다!</h2>
          <p className="lt-success-desc">확인 후 연락드리겠습니다.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="lt-page">
      <div className="lt-container">
        <div className="lt-header">
          <div className="lt-logo">🐻</div>
          <h1 className="lt-title">리틀베어 리딩클럽</h1>
          <p className="lt-subtitle">레벨테스트 예약</p>
        </div>

        {infoData && (
          <div className="lt-info-card">
            <h3 className="lt-info-title">{infoData.title}</h3>
            <p className="lt-info-content">{infoData.content}</p>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="lt-card">
            <h3 className="lt-card-title">학부모 정보</h3>
            <div className="lt-form-group">
              <label className="lt-label">학부모 성함 *</label>
              <input className={`lt-input ${errors.parentName ? 'error' : ''}`} value={formData.parentName} onChange={e => setFormData(p => ({ ...p, parentName: e.target.value }))} placeholder="성함" />
              {errors.parentName && <div className="lt-error">{errors.parentName}</div>}
            </div>
            <div className="lt-form-group">
              <label className="lt-label">학부모 전화번호 *</label>
              <input className={`lt-input ${errors.parentPhone ? 'error' : ''}`} value={formData.parentPhone} onChange={e => setFormData(p => ({ ...p, parentPhone: formatPhone(e.target.value) }))} placeholder="010-0000-0000" />
              {errors.parentPhone && <div className="lt-error">{errors.parentPhone}</div>}
            </div>
          </div>

          <div className="lt-card">
            <h3 className="lt-card-title">학생 정보</h3>
            <div className="lt-form-group">
              <label className="lt-label">학생 이름 *</label>
              <input className={`lt-input ${errors.studentName ? 'error' : ''}`} value={formData.studentName} onChange={e => setFormData(p => ({ ...p, studentName: e.target.value }))} placeholder="이름" />
              {errors.studentName && <div className="lt-error">{errors.studentName}</div>}
            </div>
            <div className="lt-form-group">
              <label className="lt-label">학교명</label>
              <input className="lt-input" value={formData.school} onChange={e => setFormData(p => ({ ...p, school: e.target.value }))} placeholder="예: 정목초등학교" />
            </div>
            <div className="lt-form-group">
              <label className="lt-label">학생 전화번호</label>
              <input className="lt-input" value={formData.studentPhone} onChange={e => setFormData(p => ({ ...p, studentPhone: formatPhone(e.target.value) }))} placeholder="010-0000-0000" />
            </div>
          </div>

          <div className="lt-card">
            <h3 className="lt-card-title">날짜 선택 *</h3>
            {errors.preferredDate && <div className="lt-error" style={{ marginBottom: 8 }}>{errors.preferredDate}</div>}
            <div className="lt-calendar">
              <div className="lt-cal-header">
                <button type="button" className="lt-cal-nav" onClick={() => setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() - 1))}>◀</button>
                <span className="lt-cal-title">{currentMonth.getFullYear()}년 {currentMonth.getMonth() + 1}월</span>
                <button type="button" className="lt-cal-nav" onClick={() => setCurrentMonth(new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1))}>▶</button>
              </div>
              <div className="lt-cal-weekdays">
                {['일', '월', '화', '수', '목', '금', '토'].map(d => <div key={d} className="lt-cal-weekday">{d}</div>)}
              </div>
              <div className="lt-cal-grid">{renderCalendar()}</div>
            </div>
          </div>

          {formData.preferredDate && (
            <div className="lt-card">
              <h3 className="lt-card-title">시간 선택 *</h3>
              {errors.preferredTime && <div className="lt-error" style={{ marginBottom: 8 }}>{errors.preferredTime}</div>}
              <div className="lt-time-grid">
                {timeSlots.map(time => {
                  const st = timeSlotStatuses.find(s => s.time === time);
                  const full = st?.status === 'FULL';
                  const blocked = st?.status === 'BLOCKED';
                  const dis = full || blocked;
                  return (
                    <button key={time} type="button" disabled={dis}
                      className={`lt-time-btn ${formData.preferredTime === time ? 'selected' : ''}`}
                      onClick={() => !dis && setFormData(p => ({ ...p, preferredTime: time }))}>
                      {time}
                      {full && <span className="lt-slot-status">마감</span>}
                      {blocked && <span className="lt-slot-status">차단</span>}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          <div className="lt-card">
            <h3 className="lt-card-title">메모</h3>
            <div className="lt-form-group">
              <textarea className="lt-textarea" value={formData.memo} onChange={e => setFormData(p => ({ ...p, memo: e.target.value }))}
                placeholder="아이 이름, 학교 및 학년, SR 점수 등" rows={3} />
            </div>
          </div>

          <button type="submit" className="lt-submit-btn" disabled={reservationMutation.isPending}>
            {reservationMutation.isPending ? '예약 중...' : '레벨테스트 예약하기'}
          </button>

          <a href="/login" style={{ display: 'block', textAlign: 'center', marginTop: 12, color: '#888', fontSize: 14, textDecoration: 'none' }}>
            ← 로그인으로 돌아가기
          </a>
        </form>
      </div>
    </div>
  );
}

export default LevelTest;
