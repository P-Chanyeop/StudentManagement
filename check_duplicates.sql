-- 예약번호 중복 확인
SELECT booking_number, COUNT(*) as count
FROM naver_bookings
GROUP BY booking_number
HAVING COUNT(*) > 1;

-- 전체 데이터 개수
SELECT COUNT(*) as total_count FROM naver_bookings;

-- 고유 예약번호 개수
SELECT COUNT(DISTINCT booking_number) as unique_count FROM naver_bookings;
