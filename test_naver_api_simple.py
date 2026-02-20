import requests
import json
from datetime import datetime, timedelta
import browser_cookie3

# Chrome 프로필에서 쿠키 가져오기
print("Chrome 쿠키 로드 중...")
cookies = browser_cookie3.chrome(domain_name='naver.com')

# API 호출
now = datetime.now()
start_date = (now - timedelta(days=31)).strftime('%Y-%m-%dT%H:%M:%S.000Z')
end_date = (now + timedelta(days=31)).strftime('%Y-%m-%dT%H:%M:%S.000Z')

api_url = f"https://partner.booking.naver.com/api/businesses/1047988/bookings?bizItemTypes=STANDARD&bookingStatusCodes=&dateDropdownType=MONTH&dateFilter=USEDATE&endDateTime={end_date}&maxDays=31&nPayChargedStatusCodes=&orderBy=&orderByStartDate=ASC&paymentStatusCodes=&searchValue=&startDateTime={start_date}&page=0&size=50&noCache={int(datetime.now().timestamp() * 1000)}"

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
            print("\n첫 번째 예약 데이터:")
            first = bookings[0]
            print(json.dumps(first, indent=2, ensure_ascii=False))
            
            print("\n\n처음 5개 예약 요약:")
            for i, booking in enumerate(bookings[:5], 1):
                visitor = booking.get('visitor', {})
                print(f"{i}. {visitor.get('name')} - {booking.get('useDateTime')} - {booking.get('bookingStatus', {}).get('code')}")
else:
    print(f"에러: {response.text[:500]}")
