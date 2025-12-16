# 학원 출석·수업관리·문자 발송 통합 시스템

## 프로젝트 개요
학원의 출석 관리, 수업 스케줄 관리, 예약 관리, 문자 발송을 통합한 웹 기반 관리 시스템입니다.

## 기술 스택

### Backend
- **Framework**: Spring Boot 4.0.0
- **Language**: Java 17
- **Build Tool**: Gradle
- **Database**:
  - 개발: H2 (In-Memory)
  - 운영: MySQL 8.0+
- **Security**: Spring Security + JWT
- **ORM**: Spring Data JPA + QueryDSL
- **Password Encryption**: BCrypt
- **Web Crawler**: Jsoup (네이버 예약 크롤링)
- **Scheduler**: Spring Quartz

### Frontend (예정)
- **Framework**: React 18+
- **UI Library**: Material-UI or Ant Design
- **HTTP Client**: Axios
- **State Management**: Redux or Context API
- **Design Theme**: 네이버 스타일 (화이트 & 그린)

## 주요 기능

### 1. 사용자 관리 및 권한
- **역할**: 관리자(ADMIN), 선생님(TEACHER), 학부모(PARENT), 학생(STUDENT)
- **인증**: JWT 기반 토큰 인증
- **보안**:
  - 비밀번호 BCrypt 암호화
  - CSRF 토큰 방어
  - XSS/SQL Injection 방어

### 2. 학생 관리
- 학생 정보 등록/수정/조회
- 학부모 정보 연동
- 영어 레벨 관리
- 상담 이력 관리

### 3. 수업 관리
- 수업(Course) 등록 및 관리
- 수업 스케줄 생성
- 선생님 배정
- 최대 수강 인원 제한

### 4. 수강권 관리
- **기간권**: 시작일~종료일 기반
- **횟수권**: 총 횟수 및 잔여 횟수 관리
- 수강권 만료 임박 알림

### 5. 출석 관리
- 출석 체크인/체크아웃
- 출석 상태: 출석, 지각, 결석, 사유결석, 조퇴
- 예상 하원 시간 자동 계산
- 지각 자동 판단 (수업 시작 10분 후)

### 6. 예약 관리
- 수업 예약 등록
- **예약 취소 제한**: 전날 오후 6시까지만 취소 가능
- 관리자 강제 취소 기능
- 예약 상태: 대기, 확정, 취소, 완료, 노쇼

### 7. 네이버 예약 연동
- 네이버 예약 페이지 크롤링
- 매일 새벽 2시 자동 동기화
- 예약 정보 자동 반영

### 8. 문자 발송 시스템
- **발송 유형**:
  - 지각 안내
  - 수강권 만료 임박 안내
  - 레벨테스트 일정 안내
  - 예약 확인/취소 안내
- SMS API 연동 준비

### 9. 레벨테스트 관리
- 레벨테스트 일정 등록
- 캘린더 뷰
- 테스트 결과 및 피드백 기록
- 권장 레벨 제안
- 자동 문자 알림

### 10. 상담 관리
- 상담 이력 저장
- 녹음 파일 링크 관리
- 후속 조치 사항 추적
- 다음 상담 일정 관리

## 프로젝트 구조

```
src/
├── main/
│   ├── java/web/kplay/studentmanagement/
│   │   ├── config/              # 설정 클래스
│   │   │   ├── JpaAuditingConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── DataInitializer.java
│   │   ├── controller/          # REST API 컨트롤러
│   │   │   ├── auth/
│   │   │   ├── student/
│   │   │   ├── course/
│   │   │   ├── attendance/
│   │   │   ├── reservation/
│   │   │   ├── leveltest/
│   │   │   ├── consultation/
│   │   │   └── message/
│   │   ├── domain/              # 엔티티
│   │   │   ├── BaseEntity.java
│   │   │   ├── user/
│   │   │   ├── student/
│   │   │   ├── course/
│   │   │   ├── attendance/
│   │   │   ├── reservation/
│   │   │   ├── leveltest/
│   │   │   ├── consultation/
│   │   │   └── message/
│   │   ├── dto/                 # 데이터 전송 객체
│   │   ├── exception/           # 예외 처리
│   │   ├── repository/          # JPA Repository
│   │   ├── security/            # 보안 관련
│   │   │   ├── JwtTokenProvider.java
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   ├── UserDetailsImpl.java
│   │   │   └── UserDetailsServiceImpl.java
│   │   ├── service/             # 비즈니스 로직
│   │   ├── scheduler/           # 스케줄러 (예정)
│   │   └── util/                # 유틸리티
│   └── resources/
│       ├── application.yml      # 설정 파일
│       └── data.sql             # 초기 데이터 (선택)
└── test/
```

## 데이터베이스 설계

### 주요 테이블
1. **users**: 사용자 정보
2. **students**: 학생 정보
3. **courses**: 수업 정보
4. **course_schedules**: 수업 일정
5. **enrollments**: 수강권
6. **attendances**: 출석 기록
7. **reservations**: 예약 정보
8. **level_tests**: 레벨테스트
9. **consultations**: 상담 이력
10. **messages**: 문자 발송 이력

## 설치 및 실행

### 사전 요구사항
- JDK 17 이상
- Gradle 8.0 이상 (또는 Gradle Wrapper 사용)
- MySQL 8.0 이상 (운영 환경)

### 개발 환경 실행
```bash
# 프로젝트 클론
git clone <repository-url>
cd StudentManagement

# 빌드
./gradlew clean build

# 실행 (H2 데이터베이스 사용)
./gradlew bootRun

# 또는
java -jar build/libs/StudentManagement-0.0.1-SNAPSHOT.jar
```

### H2 Console 접속
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (빈 문자열)

### 운영 환경 실행
```bash
# MySQL 설정
export DB_HOST=your-db-host
export DB_PORT=3306
export DB_NAME=student_management
export DB_USERNAME=your-username
export DB_PASSWORD=your-password
export JWT_SECRET=your-secret-key-minimum-256-bits

# 프로필 설정하여 실행
java -jar -Dspring.profiles.active=prod build/libs/StudentManagement-0.0.1-SNAPSHOT.jar
```

## 기본 계정

### 관리자
- ID: `admin`
- PW: `admin123`

### 선생님
- ID: `teacher`
- PW: `teacher123`

> ⚠️ **보안 경고**: 운영 환경에서는 반드시 기본 비밀번호를 변경하세요!

## API 문서

### 인증 API

#### 로그인
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}

Response:
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "id": 1,
  "username": "admin",
  "name": "관리자",
  "role": "ROLE_ADMIN"
}
```

#### 회원가입
```http
POST /api/auth/signup
Content-Type: application/json

{
  "username": "newuser",
  "password": "password123",
  "name": "홍길동",
  "phoneNumber": "010-1234-5678",
  "email": "user@example.com",
  "role": "STUDENT"
}
```

#### 토큰 갱신
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGc..."
}
```

#### 로그아웃
```http
POST /api/auth/logout
Authorization: Bearer {accessToken}
```

#### 현재 사용자 정보
```http
GET /api/auth/me
Authorization: Bearer {accessToken}
```

## 환경 설정

### application.yml 주요 설정

```yaml
# JWT 설정
jwt:
  secret: ${JWT_SECRET:your-secret-key}
  expiration: 86400000  # 24시간
  refresh-expiration: 604800000  # 7일

# SMS API 설정
sms:
  api:
    key: ${SMS_API_KEY:your-api-key}
    sender: ${SMS_SENDER:01012345678}

# 네이버 예약 크롤링
naver:
  booking:
    url: ${NAVER_BOOKING_URL:https://booking.naver.com/...}
    cron: "0 0 2 * * ?"  # 매일 새벽 2시
```

## 보안 기능

### 1. 비밀번호 암호화
- BCrypt 알고리즘 사용
- 솔트(Salt) 자동 생성
- 레인보우 테이블 공격 방어

### 2. JWT 토큰 인증
- Access Token: 24시간 유효
- Refresh Token: 7일 유효
- 서명 검증을 통한 위변조 방지

### 3. CSRF 방어
- Spring Security CSRF 토큰
- REST API는 Stateless 세션

### 4. SQL Injection 방어
- JPA Prepared Statement 자동 적용
- QueryDSL 타입 안전성

### 5. XSS 방어
- Spring Security 기본 헤더
- Content Security Policy

## 예약 취소 로직

```java
// 예약 취소 가능 여부 체크
public boolean canCancel() {
    LocalDate scheduleDate = schedule.getScheduleDate();
    LocalDateTime cancelDeadline = scheduleDate.minusDays(1).atTime(18, 0);
    return LocalDateTime.now().isBefore(cancelDeadline);
}

// 예약 취소
public void cancel(String reason) {
    if (!canCancel()) {
        throw new IllegalStateException(
            "예약 취소 기한이 지났습니다. (전날 오후 6시까지만 취소 가능)"
        );
    }
    // 취소 처리...
}
```

## 출석 자동 지각 판단

```java
// 출석 체크인 시 자동 지각 판단
public void checkIn(LocalDateTime checkInTime, LocalTime expectedLeaveTime) {
    LocalTime scheduleStartTime = schedule.getStartTime();
    LocalTime actualCheckInTime = checkInTime.toLocalTime();

    // 수업 시작 10분 후면 지각
    if (actualCheckInTime.isAfter(scheduleStartTime.plusMinutes(10))) {
        this.status = AttendanceStatus.LATE;
    } else {
        this.status = AttendanceStatus.PRESENT;
    }
}
```

## 수강권 관리 로직

### 기간권
```java
public boolean isValid() {
    LocalDate now = LocalDate.now();
    return isActive && !now.isBefore(startDate) && !now.isAfter(endDate);
}
```

### 횟수권
```java
public void useCount() {
    if (remainingCount > 0) {
        this.usedCount++;
        this.remainingCount--;
        if (remainingCount == 0) {
            this.isActive = false;
        }
    }
}
```

## 네이버 예약 크롤링 (예정)

```java
@Scheduled(cron = "${naver.booking.cron}")
public void syncNaverBooking() {
    // Jsoup을 사용한 크롤링
    // 1. 네이버 예약 페이지 접속
    // 2. 예약 정보 파싱
    // 3. 데이터베이스 동기화
    // 4. 변경 사항 확인 및 알림
}
```

## React 프론트엔드 (예정)

### 디자인 컨셉
- **색상**: 화이트 & 그린 (네이버 스타일)
- **반응형**: 모바일, 태블릿, PC 지원
- **컴포넌트**: Material-UI 또는 Ant Design

### 주요 화면
1. 로그인/회원가입
2. 대시보드
3. 학생 관리
4. 출석 체크 (태블릿 최적화)
5. 수업 스케줄 캘린더
6. 예약 관리
7. 문자 발송
8. 레벨테스트 관리
9. 상담 이력

## 배포 (AWS)

### 권장 구성
- **Web/App Server**: AWS Lightsail (최소 사양)
  - 비용: ~$5/월
- **Database**: AWS RDS MySQL (소형)
  - 비용: ~$15/월
- **총 예상 비용**: ~$20/월

### 배포 스크립트
```bash
# JAR 파일 생성
./gradlew clean build -x test

# AWS Lightsail 인스턴스에 업로드
scp build/libs/*.jar user@your-server:/app/

# 서버에서 실행
ssh user@your-server
cd /app
java -jar -Dspring.profiles.active=prod StudentManagement-0.0.1-SNAPSHOT.jar
```

## 유지보수

### 무상 유지보수
- 기간: 시스템 오픈 후 1개월
- 범위: 버그 수정, 경미한 UI 조정

### 유상 유지보수
- 월 정액: 50,000원
- 건별: 작업 범위에 따라 별도 견적

## 라이선스
Proprietary - SOFTCAT

## 연락처
- 홈페이지: https://softcat.co.kr
- 이메일: oracle7579@gmail.com

## 개발 일정
- ✅ 기본 설정 및 DB 설계
- ✅ 인증/권한 시스템
- ⏳ 학생/수업/출석 관리
- ⏳ 예약/문자 발송
- ⏳ 레벨테스트/상담 관리
- ⏳ React 프론트엔드
- ⏳ 통합 테스트
- ⏳ AWS 배포

## 주의사항

### 보안
1. JWT Secret 키는 최소 256비트 이상
2. 운영 환경에서는 HTTPS 필수
3. 데이터베이스 비밀번호 강력하게 설정
4. 기본 관리자 계정 비밀번호 즉시 변경

### 백업
1. 데이터베이스 일일 백업 권장
2. 학생 개인정보 백업 시 암호화 필수

### 성능
1. 초기 최소 사양으로 시작
2. 학생 수 증가 시 서버 스케일업
3. 인덱스 최적화 정기 점검

## 트러블슈팅

### 빌드 오류
```bash
# Gradle 캐시 삭제
./gradlew clean --refresh-dependencies
./gradlew build
```

### JWT 토큰 만료
```bash
# Refresh Token으로 갱신
POST /api/auth/refresh
```

### H2 Console 접속 안 됨
```yaml
# application.yml 확인
spring:
  h2:
    console:
      enabled: true
```

## 향후 개선 사항
1. 모바일 앱 (React Native)
2. 실시간 알림 (WebSocket)
3. 통계 대시보드
4. 급여 관리 시스템
5. 온라인 수업 연동
