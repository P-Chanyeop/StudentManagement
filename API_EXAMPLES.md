# API 테스트 예제

이 문서는 Postman, curl 또는 기타 HTTP 클라이언트를 사용하여 API를 테스트하는 방법을 안내합니다.

## 기본 정보
- Base URL: `http://localhost:8080`
- Content-Type: `application/json`
- Authorization: `Bearer {accessToken}` (로그인 후 받은 토큰)

## 1. 인증 API

### 1.1 로그인
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

**응답 예시:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "id": 1,
  "username": "admin",
  "name": "관리자",
  "role": "ADMIN"
}
```

### 1.2 회원가입
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "student01",
    "password": "password123",
    "name": "김학생",
    "phoneNumber": "010-1111-2222",
    "email": "student@example.com",
    "role": "STUDENT"
  }'
```

### 1.3 현재 사용자 정보 조회
```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 1.4 토큰 갱신
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "YOUR_REFRESH_TOKEN"
  }'
```

### 1.5 로그아웃
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## 2. 학생 관리 API

### 2.1 학생 등록
```bash
curl -X POST http://localhost:8080/api/students \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "studentName": "이학생",
    "studentPhone": "010-1234-5678",
    "birthDate": "2010-05-15",
    "gender": "MALE",
    "address": "서울시 강남구",
    "school": "강남초등학교",
    "grade": "5학년",
    "englishLevel": "Intermediate",
    "memo": "영어 말하기에 자신감 부족",
    "parentName": "이부모",
    "parentPhone": "010-9999-8888",
    "parentEmail": "parent@example.com"
  }'
```

### 2.2 학생 조회
```bash
# 특정 학생 조회
curl -X GET http://localhost:8080/api/students/1 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# 모든 학생 조회
curl -X GET http://localhost:8080/api/students \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# 활성 학생만 조회
curl -X GET http://localhost:8080/api/students/active \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# 학생 검색 (이름 또는 학부모 이름)
curl -X GET "http://localhost:8080/api/students/search?keyword=이학생" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 2.3 학생 비활성화
```bash
curl -X DELETE http://localhost:8080/api/students/1 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## 3. 수업 관리 API (예정)

### 3.1 수업 등록
```bash
curl -X POST http://localhost:8080/api/courses \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "courseName": "초급 영어 회화",
    "description": "기초 영어 회화 수업",
    "teacherId": 2,
    "maxStudents": 10,
    "durationMinutes": 60,
    "level": "Beginner",
    "color": "#03C75A"
  }'
```

### 3.2 수업 스케줄 등록
```bash
curl -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "courseId": 1,
    "scheduleDate": "2025-12-20",
    "startTime": "14:00:00",
    "endTime": "15:00:00",
    "dayOfWeek": "금요일"
  }'
```

## 4. 출석 관리 API (예정)

### 4.1 출석 체크인
```bash
curl -X POST http://localhost:8080/api/attendance/checkin \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "studentId": 1,
    "scheduleId": 1,
    "checkInTime": "2025-12-20T14:05:00",
    "expectedLeaveTime": "15:00:00"
  }'
```

### 4.2 출석 체크아웃
```bash
curl -X POST http://localhost:8080/api/attendance/checkout \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "attendanceId": 1,
    "checkOutTime": "2025-12-20T15:00:00"
  }'
```

### 4.3 출석 현황 조회
```bash
# 특정 날짜 출석 현황
curl -X GET "http://localhost:8080/api/attendance/date?date=2025-12-20" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# 학생별 출석 기록
curl -X GET "http://localhost:8080/api/attendance/student/1" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## 5. 예약 관리 API (예정)

### 5.1 수업 예약
```bash
curl -X POST http://localhost:8080/api/reservations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "studentId": 1,
    "scheduleId": 1,
    "enrollmentId": 1
  }'
```

### 5.2 예약 취소
```bash
curl -X POST http://localhost:8080/api/reservations/1/cancel \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "reason": "개인 사정"
  }'
```

**참고**: 예약은 **전날 오후 6시까지만 취소 가능**합니다.

### 5.3 예약 조회
```bash
# 학생별 예약 조회
curl -X GET "http://localhost:8080/api/reservations/student/1" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# 특정 날짜 예약 조회
curl -X GET "http://localhost:8080/api/reservations/date?date=2025-12-20" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## 6. 수강권 관리 API (예정)

### 6.1 수강권 등록

#### 기간권
```bash
curl -X POST http://localhost:8080/api/enrollments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "studentId": 1,
    "courseId": 1,
    "enrollmentType": "PERIOD",
    "startDate": "2025-12-01",
    "endDate": "2025-12-31"
  }'
```

#### 횟수권
```bash
curl -X POST http://localhost:8080/api/enrollments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "studentId": 1,
    "courseId": 1,
    "enrollmentType": "COUNT",
    "totalCount": 10
  }'
```

### 6.2 수강권 조회
```bash
# 학생별 수강권 조회
curl -X GET "http://localhost:8080/api/enrollments/student/1" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# 만료 임박 수강권 조회 (7일 이내)
curl -X GET "http://localhost:8080/api/enrollments/expiring?days=7" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## 7. 레벨테스트 관리 API (예정)

### 7.1 레벨테스트 등록
```bash
curl -X POST http://localhost:8080/api/leveltests \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "studentId": 1,
    "teacherId": 2,
    "testDate": "2025-12-25",
    "testTime": "15:00:00"
  }'
```

### 7.2 레벨테스트 결과 입력
```bash
curl -X PUT http://localhost:8080/api/leveltests/1/complete \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "testResult": "Intermediate",
    "feedback": "전반적으로 우수합니다. 문법은 잘 이해하고 있으나, 발음 연습이 필요합니다.",
    "strengths": "문법, 독해",
    "improvements": "발음, 리스닝",
    "recommendedLevel": "Intermediate High"
  }'
```

### 7.3 레벨테스트 일정 조회
```bash
# 월별 조회
curl -X GET "http://localhost:8080/api/leveltests/month?year=2025&month=12" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# 학생별 조회
curl -X GET "http://localhost:8080/api/leveltests/student/1" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## 8. 상담 관리 API (예정)

### 8.1 상담 기록 등록
```bash
curl -X POST http://localhost:8080/api/consultations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "studentId": 1,
    "consultationDate": "2025-12-16",
    "title": "학습 상담",
    "content": "학생의 영어 실력이 향상되고 있으나, 말하기에 자신감이 부족함. 더 많은 회화 연습이 필요함.",
    "consultationType": "학습상담",
    "actionItems": "1. 매일 영어 일기 작성\n2. 주 2회 원어민 회화 수업 추가",
    "nextConsultationDate": "2026-01-16"
  }'
```

### 8.2 상담 기록 조회
```bash
# 학생별 상담 이력 조회
curl -X GET "http://localhost:8080/api/consultations/student/1" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# 최근 상담 기록 조회
curl -X GET "http://localhost:8080/api/consultations/recent?limit=10" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## 9. 문자 발송 API (예정)

### 9.1 문자 발송
```bash
curl -X POST http://localhost:8080/api/messages/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "studentId": 1,
    "messageType": "GENERAL",
    "content": "[학원명] 다음 주 월요일(12/23)은 공휴일로 휴원합니다."
  }'
```

### 9.2 문자 발송 템플릿 사용
```bash
# 지각 안내
curl -X POST http://localhost:8080/api/messages/send-late-notification \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "studentId": 1,
    "scheduleId": 1
  }'

# 수강권 만료 안내
curl -X POST http://localhost:8080/api/messages/send-expiry-notification \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "enrollmentId": 1
  }'
```

### 9.3 문자 발송 이력 조회
```bash
# 학생별 발송 이력
curl -X GET "http://localhost:8080/api/messages/student/1" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# 전체 발송 이력
curl -X GET "http://localhost:8080/api/messages" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## 10. 통계 API (예정)

### 10.1 대시보드 통계
```bash
curl -X GET http://localhost:8080/api/stats/dashboard \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**응답 예시:**
```json
{
  "totalStudents": 150,
  "activeStudents": 145,
  "totalCourses": 20,
  "todayAttendance": 85,
  "todayReservations": 92,
  "expiringEnrollments": 8,
  "pendingLevelTests": 3
}
```

### 10.2 출석률 통계
```bash
# 월별 출석률
curl -X GET "http://localhost:8080/api/stats/attendance?year=2025&month=12" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# 학생별 출석률
curl -X GET "http://localhost:8080/api/stats/attendance/student/1?startDate=2025-12-01&endDate=2025-12-31" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## Postman Collection

Postman을 사용하는 경우, 다음과 같이 환경 변수를 설정하세요:

```json
{
  "baseUrl": "http://localhost:8080",
  "accessToken": "{{loginResponse.accessToken}}",
  "refreshToken": "{{loginResponse.refreshToken}}"
}
```

## 오류 응답 형식

모든 API는 오류 발생 시 다음 형식으로 응답합니다:

```json
{
  "status": 400,
  "message": "오류 메시지",
  "timestamp": "2025-12-16T10:30:00"
}
```

### 주요 HTTP 상태 코드
- `200 OK`: 요청 성공
- `201 Created`: 리소스 생성 성공
- `204 No Content`: 요청 성공 (응답 본문 없음)
- `400 Bad Request`: 잘못된 요청
- `401 Unauthorized`: 인증 실패
- `403 Forbidden`: 권한 없음
- `404 Not Found`: 리소스를 찾을 수 없음
- `500 Internal Server Error`: 서버 오류

## 개발 팁

### 1. 토큰 자동 저장 (Bash)
```bash
# 로그인 후 토큰 저장
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.accessToken')

# 저장된 토큰으로 API 호출
curl -X GET http://localhost:8080/api/students \
  -H "Authorization: Bearer $TOKEN"
```

### 2. Postman Pre-request Script
```javascript
// 토큰이 없거나 만료된 경우 자동 로그인
if (!pm.environment.get("accessToken") || pm.environment.get("tokenExpired")) {
    pm.sendRequest({
        url: pm.environment.get("baseUrl") + "/api/auth/login",
        method: 'POST',
        header: {
            'Content-Type': 'application/json',
        },
        body: {
            mode: 'raw',
            raw: JSON.stringify({
                username: "admin",
                password: "admin123"
            })
        }
    }, function (err, res) {
        const jsonData = res.json();
        pm.environment.set("accessToken", jsonData.accessToken);
        pm.environment.set("refreshToken", jsonData.refreshToken);
    });
}
```

## 참고 사항

1. 모든 날짜는 `YYYY-MM-DD` 형식
2. 모든 시간은 `HH:mm:ss` 형식
3. 모든 날짜시간은 `YYYY-MM-DDT HH:mm:ss` 형식
4. 권한이 필요한 API는 반드시 `Authorization` 헤더에 유효한 토큰 포함
5. 개발 환경에서는 H2 Console(`http://localhost:8080/h2-console`)로 데이터 확인 가능
