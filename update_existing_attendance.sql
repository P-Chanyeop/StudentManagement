-- 기존 네이버 예약 출석 데이터에 예상 하원 시간 업데이트
-- 이 스크립트는 수동으로 실행해야 합니다

-- 예시: 황현아 학생 (Basic 90분)
UPDATE attendances a
JOIN naver_bookings nb ON a.naver_booking_id = nb.id
SET 
  a.duration_minutes = 90,
  a.expected_leave_time = ADDTIME(a.attendance_time, '01:30:00'),
  a.original_expected_leave_time = ADDTIME(a.attendance_time, '01:30:00')
WHERE nb.name = '황현아';

-- 모든 학생에 대해 일괄 업데이트는 엑셀 데이터와 매칭 필요
-- 새로 크롤링하는 것을 권장합니다
