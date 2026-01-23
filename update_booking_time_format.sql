-- Safe update mode 해제
SET SQL_SAFE_UPDATES = 0;

-- 모든 네이버 예약 bookingTime 형식 변경
-- "26. 1. 23.(금) 오전 9:00" -> "2026-01-23 09:00"

-- 오전 시간 변경
UPDATE naver_bookings SET booking_time = '2026-01-23 09:00' WHERE booking_time LIKE '%1. 23.%오전 9:00%';
UPDATE naver_bookings SET booking_time = '2026-01-23 10:00' WHERE booking_time LIKE '%1. 23.%오전 10:00%';
UPDATE naver_bookings SET booking_time = '2026-01-23 11:00' WHERE booking_time LIKE '%1. 23.%오전 11:00%';
UPDATE naver_bookings SET booking_time = '2026-01-23 12:00' WHERE booking_time LIKE '%1. 23.%오후 12:00%';

-- 오후 시간 변경
UPDATE naver_bookings SET booking_time = '2026-01-23 13:00' WHERE booking_time LIKE '%1. 23.%오후 1:00%';
UPDATE naver_bookings SET booking_time = '2026-01-23 14:00' WHERE booking_time LIKE '%1. 23.%오후 2:00%';
UPDATE naver_bookings SET booking_time = '2026-01-23 15:00' WHERE booking_time LIKE '%1. 23.%오후 3:00%';
UPDATE naver_bookings SET booking_time = '2026-01-23 16:00' WHERE booking_time LIKE '%1. 23.%오후 4:00%';
UPDATE naver_bookings SET booking_time = '2026-01-23 17:00' WHERE booking_time LIKE '%1. 23.%오후 5:00%';
UPDATE naver_bookings SET booking_time = '2026-01-23 18:00' WHERE booking_time LIKE '%1. 23.%오후 6:00%';
UPDATE naver_bookings SET booking_time = '2026-01-23 19:00' WHERE booking_time LIKE '%1. 23.%오후 7:00%';
UPDATE naver_bookings SET booking_time = '2026-01-23 20:00' WHERE booking_time LIKE '%1. 23.%오후 8:00%';

-- 다른 날짜도 필요하면 추가
UPDATE naver_bookings SET booking_time = '2026-01-22 09:00' WHERE booking_time LIKE '%1. 22.%오전 9:00%';
UPDATE naver_bookings SET booking_time = '2026-01-22 10:00' WHERE booking_time LIKE '%1. 22.%오전 10:00%';
UPDATE naver_bookings SET booking_time = '2026-01-22 11:00' WHERE booking_time LIKE '%1. 22.%오전 11:00%';
UPDATE naver_bookings SET booking_time = '2026-01-22 12:00' WHERE booking_time LIKE '%1. 22.%오후 12:00%';
UPDATE naver_bookings SET booking_time = '2026-01-22 13:00' WHERE booking_time LIKE '%1. 22.%오후 1:00%';
UPDATE naver_bookings SET booking_time = '2026-01-22 14:00' WHERE booking_time LIKE '%1. 22.%오후 2:00%';
UPDATE naver_bookings SET booking_time = '2026-01-22 15:00' WHERE booking_time LIKE '%1. 22.%오후 3:00%';
UPDATE naver_bookings SET booking_time = '2026-01-22 16:00' WHERE booking_time LIKE '%1. 22.%오후 4:00%';
UPDATE naver_bookings SET booking_time = '2026-01-22 17:00' WHERE booking_time LIKE '%1. 22.%오후 5:00%';
UPDATE naver_bookings SET booking_time = '2026-01-22 18:00' WHERE booking_time LIKE '%1. 22.%오후 6:00%';
UPDATE naver_bookings SET booking_time = '2026-01-22 19:00' WHERE booking_time LIKE '%1. 22.%오후 7:00%';
UPDATE naver_bookings SET booking_time = '2026-01-22 20:00' WHERE booking_time LIKE '%1. 22.%오후 8:00%';

-- Safe update mode 다시 활성화
SET SQL_SAFE_UPDATES = 1;

-- 변경 후 확인
SELECT id, name, booking_time FROM naver_bookings ORDER BY booking_time;
