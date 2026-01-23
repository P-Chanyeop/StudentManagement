-- 1월 23일 네이버 예약 bookingTime 형식 변경
-- "26. 1. 23.(금) 오전 9:00" -> "2026-01-23 09:00"

-- 먼저 현재 데이터 확인
SELECT id, name, booking_time FROM naver_bookings WHERE id BETWEEN 66 AND 73;

-- 오전 9시 데이터 일괄 변경
UPDATE naver_bookings SET booking_time = '2026-01-23 09:00' WHERE id IN (66, 67, 68, 69, 70, 71, 72, 73);

-- 변경 후 확인
SELECT id, name, booking_time FROM naver_bookings WHERE id BETWEEN 66 AND 73;
