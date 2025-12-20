# 개발자 가이드 (Developer Guide)

## 🚀 빠른 시작 (Quick Start)

### 1. 서버 실행

```bash
# Gradle 사용
./gradlew bootRun

# 또는 IntelliJ IDEA에서
# StudentManagementApplication.java 실행
```

**서버가 시작되면 자동으로 출력되는 정보:**
```
=== 초기 데이터 로딩 완료 ===

📋 로그인 정보:
  관리자: admin / admin123
  선생님1: teacher1 / teacher123
  선생님2: teacher2 / teacher123
  학부모: parent1 / parent123

🌐 Swagger UI: http://localhost:8080/swagger-ui.html
🗄️  H2 Console: http://localhost:8080/h2-console
```

---

### 2. 접속 URL

| 서비스 | URL | 설명 |
|--------|-----|------|
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` | API 문서 및 테스트 |
| **H2 Console** | `http://localhost:8080/h2-console` | 개발용 DB 콘솔 |
| **REST API** | `http://localhost:8080/api/**` | 실제 API 엔드포인트 |

#### H2 Console 접속 정보
- **JDBC URL**: `jdbc:h2:mem:testdb`
- **Username**: `sa`
- **Password**: (비어있음)

---

## 🔐 초기 계정 정보

| 역할 | 아이디 | 비밀번호 | 권한 |
|------|--------|---------|------|
| 관리자 | admin | admin123 | ROLE_ADMIN |
| 선생님1 | teacher1 | teacher123 | ROLE_TEACHER |
| 선생님2 | teacher2 | teacher123 | ROLE_TEACHER |
| 학부모 | parent1 | parent123 | ROLE_PARENT |

**초기 데이터:**
- 학생 3명 (홍길동, 김민수, 이지은)
- 각 학생의 상세 정보 포함

---

## 📋 API 테스트 체크리스트

### 인증 (Authentication)

#### 1. 로그인하여 JWT 토큰 받기

**Swagger UI 사용:**
1. `http://localhost:8080/swagger-ui.html` 접속
2. `auth-controller` 섹션 열기
3. `POST /api/auth/login` 클릭
4. Request body:
   ```json
   {
     "username": "admin",
     "password": "admin123"
   }
   ```
5. **Execute** 클릭
6. Response에서 `accessToken` 복사

#### 2. Swagger에서 인증 설정

1. 페이지 상단의 **Authorize** 버튼 클릭
2. `Bearer Authentication` 입력란에 토큰 붙여넣기 (Bearer 접두사 불필요)
3. **Authorize** 클릭
4. 이제 모든 API 호출에 자동으로 토큰이 포함됨

---

### 주요 기능 테스트

#### ✅ 학생 관리

- [ ] `GET /api/students` - 학생 목록 조회
- [ ] `POST /api/students` - 신규 학생 등록
- [ ] `GET /api/students/{id}` - 학생 상세 조회
- [ ] `PUT /api/students/{id}` - 학생 정보 수정
- [ ] `DELETE /api/students/{id}` - 학생 삭제

**테스트 포인트:**
- 필수 필드 검증 (이름, 생년월일, 연락처)
- 페이징 및 정렬 동작

---

#### ✅ 수강권 관리

- [ ] `POST /api/enrollments` - 수강권 등록
- [ ] `GET /api/enrollments/student/{studentId}` - 학생별 수강권 조회
- [ ] `PUT /api/enrollments/{id}` - 수강권 수정
- [ ] `POST /api/enrollments/{id}/add-count` - 횟수 추가
- [ ] `POST /api/enrollments/{id}/extend` - 기간 연장

**중요 테스트 시나리오:**

1. **횟수 차감 (예약 생성)**
   ```
   초기: remainingCount = 10
   → 예약 생성
   → remainingCount = 9 ✓
   ```

2. **횟수 복원 (예약 취소)** ⭐ 최근 수정
   ```
   remainingCount = 9
   → 예약 취소
   → remainingCount = 10 ✓
   ```

3. **Race Condition 방지 검증**
   - 예약 취소 시 수강권 복원이 상태 변경 **전**에 수행됨
   - 통합 테스트로 검증됨 (`ReservationServiceIntegrationTest`)

4. **유효성 검증**
   - 잔여 횟수 0일 때 예약 불가
   - 만료일 지난 수강권으로 예약 불가

---

#### ✅ 예약 관리

- [ ] `POST /api/reservations` - 예약 생성
- [ ] `DELETE /api/reservations/{id}` - 예약 삭제
- [ ] `PUT /api/reservations/{id}/cancel` - 예약 취소 (사용자)
- [ ] `PUT /api/reservations/{id}/force-cancel` - 강제 취소 (관리자)

**차이점:**
| 작업 | DB 상태 | 수강권 복원 | 스케줄 인원 |
|------|---------|------------|-----------|
| **삭제** | 완전 삭제 | ✓ 복원 | -1 |
| **취소** | CANCELLED 상태 유지 | ✓ 복원 | -1 |
| **강제취소** | CANCELLED 상태 유지 | ✓ 복원 | -1 |

---

#### ✅ 레벨테스트

- [ ] `POST /api/level-tests` - 레벨테스트 등록
- [ ] `PUT /api/level-tests/{id}` - 정보 수정
- [ ] `PUT /api/level-tests/{id}/complete` - 완료 (간단)
- [ ] `PUT /api/level-tests/{id}/result` - 결과 저장
- [ ] `PUT /api/level-tests/{id}/cancel` - 취소

**최근 개선사항:** ⭐
- `@Setter` 제거 → 비즈니스 메서드 사용
- `complete()` 메서드 - null 파라미터는 기존 값 유지
- `testScore` 필드 추가

**테스트 예시:**
```json
// 결과 저장
PUT /api/level-tests/1/result
{
  "level": "Intermediate",
  "score": 85,
  "feedback": "Great progress!"
}
```

---

#### ✅ 보강 수업

- [ ] `POST /api/makeup-classes` - 보강 신청
- [ ] `GET /api/makeup-classes` - 보강 목록
- [ ] `GET /api/makeup-classes/count?status=PENDING` - 상태별 개수

**성능 개선:** ⭐
- `.size()` → `countByStatus()` 쿼리 사용
- N+1 문제 방지

---

#### ✅ 상담 관리

- [ ] `POST /api/consultations` - 상담 등록
- [ ] `GET /api/consultations` - 상담 목록
- [ ] `GET /api/consultations/excel` - 엑셀 다운로드

**버그 수정:** ⭐
- 엑셀 "비고" 컬럼: `content` → `actionItems`
- 중복 데이터 표시 문제 해결

---

#### ✅ 결제 (PortOne)

- [ ] `POST /api/payments/prepare` - 결제 준비
- [ ] `POST /api/payments/verify` - 결제 검증
- [ ] `POST /api/payments/cancel` - 결제 취소

**환경변수 설정:**
```yaml
portone:
  test-mode: true  # 테스트 모드
  api-key: ${PORTONE_API_KEY}
  api-secret: ${PORTONE_API_SECRET}
```

---

## 🧪 테스트 실행

### 단위 테스트 실행

```bash
# 모든 테스트 실행
./gradlew test

# 특정 클래스만 실행
./gradlew test --tests LevelTestTest
./gradlew test --tests EnrollmentTest
```

### 테스트 커버리지

총 **70+ 테스트 케이스** 작성됨:

1. **LevelTestTest** (도메인 로직)
   - complete() null-safe 파라미터 처리
   - cancel(), updateDetails(), reschedule()
   - 데이터 손실 방지 검증

2. **EnrollmentTest** (도메인 로직)
   - useCount(), restoreCount()
   - isValid() 유효성 검증
   - addCount(), extendPeriod(), manualAdjustCount()
   - Edge case: 음수 값, null 입력, 오버플로우 방지

3. **ReservationServiceIntegrationTest** (통합)
   - 예약 생성/취소/삭제 시 수강권 횟수 관리
   - Race condition 방지 (복원이 상태 변경 전에 수행)
   - 스케줄 학생 수 관리
   - 다중 예약 시나리오

---

## 🗃️ 데이터베이스 확인

### H2 Console에서 직접 확인

```sql
-- 학생 목록
SELECT * FROM students;

-- 수강권 목록 (횟수 확인)
SELECT s.student_name, c.course_name,
       e.total_count, e.used_count, e.remaining_count, e.is_active
FROM enrollments e
JOIN students s ON e.student_id = s.id
JOIN courses c ON e.course_id = c.id;

-- 예약 목록
SELECT r.id, s.student_name, cs.schedule_date, r.status
FROM reservations r
JOIN students s ON r.student_id = s.id
JOIN course_schedules cs ON r.schedule_id = cs.id;

-- 레벨테스트 목록
SELECT lt.id, s.student_name, u.name as teacher_name,
       lt.test_date, lt.test_status, lt.test_result, lt.test_score
FROM level_tests lt
JOIN students s ON lt.student_id = s.id
LEFT JOIN users u ON lt.teacher_id = u.id;
```

---

## 🐛 최근 버그 수정 내역

### ✅ 완료된 수정

1. **예약 취소 시 Race Condition**
   - 수강권 복원을 상태 변경 **전**에 수행
   - Commit: `c2c6924`

2. **LevelTest 캡슐화 위반**
   - `@Setter` 제거, 비즈니스 메서드 사용
   - 중복 메서드 제거
   - Commit: `3c58df1`

3. **MakeupClass 성능 문제**
   - `.size()` → `countByStatus()` 쿼리
   - Commit: `8aee248`

4. **ConsultationExcel 로직 오류**
   - "비고" 컬럼: `content` → `actionItems`
   - Commit: `8aee248`

5. **application.yml 중복 키**
   - `spring:` 키 중복 제거
   - Commit: `605802d`

---

## 📦 추가된 기능

### ✅ Swagger UI (OpenAPI 3.0)

- **URL**: `http://localhost:8080/swagger-ui.html`
- JWT 인증 지원 (Bearer Token)
- 모든 API 실시간 테스트 가능
- Request/Response 스키마 자동 문서화

### ✅ 데이터 시드 (DataSeeder)

- `dev` 프로파일에서만 자동 실행
- 초기 관리자, 선생님, 학부모 계정
- 테스트용 학생 3명 자동 생성
- 서버 시작 시 로그인 정보 출력

### ✅ 종합 테스트 코드

- 70+ 테스트 케이스
- 도메인 로직 단위 테스트
- 서비스 계층 통합 테스트
- AssertJ 사용, Given-When-Then 패턴

---

## 🔧 환경 설정

### application.yml 주요 설정

```yaml
spring:
  profiles:
    active: dev  # dev 또는 prod

  # 파일 업로드
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

# JWT
jwt:
  secret: ${JWT_SECRET:your-secret-key...}
  expiration: 86400000  # 24시간

# SMS
sms:
  provider: ${SMS_PROVIDER:test}  # test, aligo, munjanara
```

### 환경 변수 설정 (선택)

```bash
# JWT Secret (프로덕션 필수)
export JWT_SECRET="your-production-secret-key-minimum-256-bits"

# SMS API (실제 발송 시)
export SMS_PROVIDER=aligo
export SMS_ALIGO_API_KEY=your-api-key
export SMS_ALIGO_USER_ID=your-user-id
export SMS_ALIGO_SENDER=01012345678

# 결제 (PortOne)
export PORTONE_API_KEY=your-api-key
export PORTONE_API_SECRET=your-api-secret
export PORTONE_TEST_MODE=false  # 실제 결제 시 false
```

---

## 📚 개발 시 참고사항

### 권장 개발 흐름

1. **Swagger UI에서 API 구조 확인**
   - `http://localhost:8080/swagger-ui.html`

2. **초기 데이터로 빠른 테스트**
   - 미리 생성된 admin/teacher 계정 사용

3. **H2 Console로 데이터 검증**
   - SQL 쿼리로 실제 DB 상태 확인

4. **테스트 코드 작성 후 검증**
   - 도메인 로직은 단위 테스트
   - 비즈니스 플로우는 통합 테스트

### 코드 품질 체크포인트

- [ ] 도메인 엔티티에 비즈니스 로직 캡슐화
- [ ] `@Setter` 사용 지양 (불변성 유지)
- [ ] null 체크 및 유효성 검증
- [ ] 트랜잭션 경계 명확히
- [ ] 테스트 코드 작성 (최소 주요 로직)

---

## 🎯 다음 단계 추천

1. **프론트엔드 연동**
   - Swagger에서 생성된 API 스펙 활용
   - CORS 설정 확인 (SecurityConfig)

2. **프로덕션 배포**
   - `prod` 프로파일로 전환
   - MySQL 연결 설정
   - 환경변수로 민감정보 관리

3. **추가 기능 개발**
   - 출석 체크 시스템
   - 성적 관리
   - 학부모 포털

---

## ❓ 문제 해결

### 서버가 시작되지 않아요

```bash
# 포트 충돌 확인
netstat -ano | findstr :8080

# H2 데이터베이스 초기화
rm -rf ~/h2db/  # 또는 data 디렉토리 삭제
```

### Swagger UI가 안 보여요

1. URL 확인: `http://localhost:8080/swagger-ui.html` (마지막 .html 필수)
2. SecurityConfig에서 경로 허용 확인
3. 브라우저 캐시 삭제

### JWT 토큰이 만료되었어요

1. `/api/auth/login`으로 재로그인
2. 새 토큰 복사
3. Swagger의 **Authorize** 버튼으로 재설정

---

## 📞 문의

프로젝트 관련 문의:
- GitHub Issues: [StudentManagement/issues]
- Email: support@kplay.web

---

**Happy Coding! 🎉**
