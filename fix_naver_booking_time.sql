-- 1월 23일 네이버 예약 데이터 조회
SELECT id, name, booking_time FROM naver_bookings WHERE booking_time LIKE '%23.%';

-- bookingTime 형식 변경 예시:
-- "26. 1. 23.(목) 오전 10:00" -> "2026-01-23 10:00"
-- "26. 1. 23.(목) 오후 2:00" -> "2026-01-23 14:00"

-- 실제 데이터 확인 후 수동으로 업데이트
-- UPDATE naver_bookings SET booking_time = '2026-01-23 10:00' WHERE id = ?;
