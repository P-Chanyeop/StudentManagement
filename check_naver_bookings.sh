#!/bin/bash

# 관리자 로그인
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.accessToken')

echo "Token: $TOKEN"

# 1월 23일 네이버 예약 조회
echo "Fetching Naver bookings for 2026-01-23..."
BOOKINGS=$(curl -s -X GET "http://localhost:8080/api/naver-booking/date?date=2026-01-23" \
  -H "Authorization: Bearer $TOKEN")

echo "Bookings:"
echo "$BOOKINGS" | jq '.'

# 각 예약의 bookingTime을 확인하고 출석 레코드 재생성
echo "$BOOKINGS" | jq -r '.[] | @json' | while read booking; do
  ID=$(echo "$booking" | jq -r '.id')
  NAME=$(echo "$booking" | jq -r '.name')
  BOOKING_TIME=$(echo "$booking" | jq -r '.bookingTime')
  
  echo "Processing: ID=$ID, Name=$NAME, Time=$BOOKING_TIME"
  
  # 출석 레코드가 있는지 확인하고 없으면 생성
  # (수동 동기화를 다시 실행하면 자동으로 생성됨)
done

echo ""
echo "수동 동기화를 실행하여 출석 레코드를 재생성하세요:"
echo "curl -X POST http://localhost:8080/api/naver-booking/sync -H \"Authorization: Bearer $TOKEN\""
