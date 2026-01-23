-- 1월 23일 네이버 예약 bookingTime 형식 변경
-- "26. 1. 23.(목) 오전 10:00" -> "2026-01-23 10:00"
-- "26. 1. 23.(목) 오후 2:00" -> "2026-01-23 14:00"

-- 먼저 현재 데이터 확인
SELECT id, name, booking_time FROM naver_bookings WHERE booking_time LIKE '%1. 23.%';

-- 오전 시간 변경 (예시)
UPDATE naver_bookings SET booking_time = '2026-01-23 09:00' WHERE booking_time LIKE '%1. 23.%오전 9:00%';
UPDATE naver_bookings SET booking_time = '2026-01-23 10:00' WHERE booking_time LIKE '%1. 23.%오전 10:00%';
UPDATE naver_bookings SET booking_time = '2026-01-23 11:00' WHERE booking_time LIKE '%1. 23.%오전 11:00%';

-- 오후 시간 변경 (예시)
UPDATE naver_bookings SET booking_time = '2026-01-23 13:00' WHERE booking_time LIKE '%1. 23.%오후 1:00%';
UPDATE naver_bookings SET booking_time = '2026-01-23 14:00' WHERE booking_time LIKE '%1. 23.%오후 2:00%';
UPDATE naver_bookings SET booking_time = '2026-01-23 15:00' WHERE booking_time LIKE '%1. 23.%오후 3:00%';
UPDATE naver_bookings SET booking_time = '2026-01-23 16:00' WHERE booking_time LIKE '%1. 23.%오후 4:00%';
UPDATE naver_bookings SET booking_time = '2026-01-23 17:00' WHERE booking_time LIKE '%1. 23.%오후 5:00%';
UPDATE naver_bookings SET booking_time = '2026-01-23 18:00' WHERE booking_time LIKE '%1. 23.%오후 6:00%';

-- 변경 후 확인
SELECT id, name, booking_time FROM naver_bookings WHERE booking_time LIKE '2026-01-23%';
