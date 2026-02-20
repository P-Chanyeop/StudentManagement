from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.options import Options
import time
import requests
import json
from datetime import datetime, timedelta

# Chrome 옵션 설정
chrome_options = Options()
# chrome_options.add_argument('--headless')
chrome_options.add_argument('--no-sandbox')
chrome_options.add_argument('--disable-dev-shm-usage')
chrome_options.add_argument('user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36')

# 프로필 사용
import os
user_data_dir = os.path.expanduser('~/.chrome-naver-profile')
chrome_options.add_argument(f'--user-data-dir={user_data_dir}')
chrome_options.add_argument('--profile-directory=Default')

print("Chrome 시작...")
driver = webdriver.Chrome(options=chrome_options)

try:
    # 네이버 로그인 확인
    driver.get('https://www.naver.com/')
    time.sleep(1)
    
    # 로그인 필요한지 확인
    try:
        driver.find_element(By.LINK_TEXT, '로그인')
        print("로그인 필요 - 수동으로 로그인해주세요")
        input("로그인 완료 후 Enter를 누르세요...")
    except:
        print("이미 로그인되어 있음")
    
    # 네이버 예약 페이지로 이동
    print("네이버 예약 페이지 접속...")
    driver.get('https://partner.booking.naver.com/')
    time.sleep(3)
    
    # 쿠키 가져오기
    cookies = driver.get_cookies()
    cookie_dict = {cookie['name']: cookie['value'] for cookie in cookies}
    cookie_string = '; '.join([f"{name}={value}" for name, value in cookie_dict.items()])
    
    print(f"쿠키 개수: {len(cookies)}")
    
    # API 호출
    now = datetime.now()
    start_date = (now - timedelta(days=31)).strftime('%Y-%m-%dT%H:%M:%S.000Z')
    end_date = (now + timedelta(days=31)).strftime('%Y-%m-%dT%H:%M:%S.000Z')
    
    api_url = f"https://partner.booking.naver.com/api/businesses/1047988/bookings?bizItemTypes=STANDARD&bookingStatusCodes=&dateDropdownType=MONTH&dateFilter=USEDATE&endDateTime={end_date}&maxDays=31&nPayChargedStatusCodes=&orderBy=&orderByStartDate=ASC&paymentStatusCodes=&searchValue=&startDateTime={start_date}&page=0&size=50&noCache={int(time.time() * 1000)}"
    
    print(f"\nAPI 호출: {api_url[:100]}...")
    
    headers = {
        'Cookie': cookie_string,
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Accept': 'application/json',
        'Referer': 'https://partner.booking.naver.com/'
    }
    
    response = requests.get(api_url, headers=headers)
    
    print(f"\n응답 코드: {response.status_code}")
    
    if response.status_code == 200:
        data = response.json()
        print(f"\n응답 데이터 키: {data.keys()}")
        
        if 'content' in data:
            bookings = data['content']
            print(f"\n총 예약 건수: {len(bookings)}")
            
            if bookings:
                print("\n첫 번째 예약 데이터:")
                first = bookings[0]
                print(json.dumps(first, indent=2, ensure_ascii=False))
                
                print("\n\n처음 5개 예약 요약:")
                for i, booking in enumerate(bookings[:5], 1):
                    print(f"{i}. {booking.get('visitor', {}).get('name')} - {booking.get('useDateTime')} - {booking.get('bookingStatus', {}).get('code')}")
    else:
        print(f"에러: {response.text[:500]}")

finally:
    print("\n\nEnter를 누르면 종료...")
    input()
    driver.quit()
