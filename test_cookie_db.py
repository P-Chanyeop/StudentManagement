import sqlite3
import requests
import json
from datetime import datetime, timedelta

# Chrome 쿠키 DB 경로
cookie_db_path = "/mnt/c/Users/user/.chrome-naver-profile/Default/Network/Cookies"

# 쿠키 읽기
conn = sqlite3.connect(cookie_db_path)
cursor = conn.cursor()

# naver.com 도메인의 쿠키 가져오기
cursor.execute("SELECT name, value, host_key FROM cookies WHERE host_key LIKE '%naver.com%'")
cookies = {}
for name, value, host in cursor.fetchall():
    cookies[name] = value
    print(f"쿠키: {name} = {value[:20]}... (도메인: {host})")

conn.close()

print(f"\n총 {len(cookies)}개 쿠키 로드")

# API 호출
now = datetime.now()
start_date = (now - timedelta(days=31)).strftime('%Y-%m-%dT%H:%M:%S.000Z')
end_date = (now + timedelta(days=31)).strftime('%Y-%m-%dT%H:%M:%S.000Z')

api_url = f"https://partner.booking.naver.com/api/businesses/1047988/bookings?bizItemTypes=STANDARD&bookingStatusCodes=&dateDropdownType=MONTH&dateFilter=USEDATE&endDateTime={end_date}&maxDays=31&nPayChargedStatusCodes=&orderBy=&orderByStartDate=ASC&paymentStatusCodes=&searchValue=&startDateTime={start_date}&page=0&size=200&noCache={int(datetime.now().timestamp() * 1000)}"

print(f"\nAPI 호출...")

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Accept': 'application/json',
    'Referer': 'https://partner.booking.naver.com/'
}

response = requests.get(api_url, headers=headers, cookies=cookies)

print(f"응답 코드: {response.status_code}")

if response.status_code == 200:
    data = response.json()
    print(f"\n응답 데이터 키: {list(data.keys())}")
    
    if 'content' in data:
        bookings = data['content']
        print(f"\n총 예약 건수: {len(bookings)}")
        
        if bookings:
            print("\n첫 번째 예약 전체 데이터:")
            first = bookings[0]
            print(json.dumps(first, indent=2, ensure_ascii=False))
            
            print("\n\n처음 5개 예약 요약:")
            for i, booking in enumerate(bookings[:5], 1):
                visitor = booking.get('visitor', {})
                print(f"{i}. {visitor.get('name')} - {booking.get('useDateTime')} - {booking.get('bookingStatus', {}).get('code')}")
else:
    print(f"에러 응답: {response.text[:500]}")
