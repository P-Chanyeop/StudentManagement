import requests
import json
from datetime import datetime, timedelta

# 수동으로 복사한 쿠키
cookies = {
    'ASID': 'd2b366190000019bd9e5bf2d0000001d',
    'BUC': 'gP5ePmA1B07HfpDpIcEnL6myX_t_Ner9FH-o5mM7V2I=',
    'csrf_token': 'e5c17d16442f739800ef6faac172c19c3a97c36771b4066a34d1c14016fc87f868fe799659ba7748d314572f970a191f71817eb6123aeffc244a4c93be2eb8cd',
    'JSESSIONID': '4D27F50AD112938686F4493535ACD716',
    'NAC': 'y2iEB4wh3tjY',
    'NACT': '1',
    'NID_AUT': 'gC8Fo10+3VVLsJm0MNdFACCqeuNvgnz1aUdsk+gTUYmMe+HeBTEEn+IjRAJCACYd',
    'nid_inf': '1019430225',
    'NID_SES': 'AAABkV5ysLPB07gWsahp2arIw4570dBQUioLi/OeC1EB4wW5Zn6QzwNBnx5sBNbeAkkcdE79wh5uzIPwfCx7hd3YLjX4efi4KCWbyQPkfURZX0zBjkc+cNrQ/w0YDFxCLjOgNC0Y4Nx52SGZWlZUsQhwe/z2wu15+tDo2zQnkq6Y/FTFrkiHxNhwE+wEONscqDpHI2IVCdLJ2cDCGrd9ruXRUK7bV9nLDMp0sOCldYqEUJyw4mB2sEfDAdmdYTnxu5OLFyJoKXWxwTLLCKGUA/Go4f9d0hycxUT3wP3cTePle6aVzPZOjs9VgBctvbFF1RfSQk3LuNcE4G+tiTwn/OSqO19HhbNgz8ycbD6mDWZXqznTPQSJ7zkttPM3u2mZbT52euzVoe9W5Tn23pxcF13YCQ1hz5gXqH2vkBA4SEw9TbflImgcFZvY0eg71wSJKXqvZOSwFZKGw+Min5J33UDmsc4ytYbCGiIxSHg3LFvy42YmVC/tmFiXpir0X2U8lJn1ygnfjTSdGsU48O4qpDPYg7LRG3a6Gn6bs6R9Im8FSBMn',
    'NNB': 'X4LXIXDIF5FGS',
    'page_uid': 'jUKbNlqVJ5RssOeRQw4-390956',
    'SRT30': '1769145951'
}

# API 호출
now = datetime.now()
start_date = (now - timedelta(days=31)).strftime('%Y-%m-%dT%H:%M:%S.000Z')
end_date = (now + timedelta(days=31)).strftime('%Y-%m-%dT%H:%M:%S.000Z')

api_url = f"https://partner.booking.naver.com/api/businesses/1047988/bookings?bizItemTypes=STANDARD&bookingStatusCodes=&dateDropdownType=MONTH&dateFilter=USEDATE&endDateTime={end_date}&maxDays=31&nPayChargedStatusCodes=&orderBy=&orderByStartDate=ASC&paymentStatusCodes=&searchValue=&startDateTime={start_date}&page=0&size=200&noCache={int(datetime.now().timestamp() * 1000)}"

print(f"API 호출...")

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
