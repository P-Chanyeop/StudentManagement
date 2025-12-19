# 포트원(PortOne) 결제 연동 가이드

## 📋 개요

K-PLAY 학원 관리 시스템의 온라인 결제 기능입니다. 포트원(구 아임포트)을 통해 다양한 PG사 결제를 지원합니다.

## 🚀 빠른 시작

### 1. 포트원 회원가입 및 설정

1. **포트원 가입**: https://portone.io 접속하여 회원가입
2. **가맹점 생성**: 콘솔에서 새 가맹점 생성
3. **PG사 연동**: 나이스페이먼츠, KCP, 이니시스 등 원하는 PG사 계약
4. **API 키 발급**:
   - REST API 키
   - REST API Secret
   - 가맹점 식별코드 (imp 코드)

### 2. 환경변수 설정

`.env` 파일 생성 또는 시스템 환경변수 설정:

```bash
# 포트원 설정
export PORTONE_API_KEY="발급받은_API_KEY"
export PORTONE_API_SECRET="발급받은_API_SECRET"
export PORTONE_IMP_CODE="imp12345678"
export PORTONE_PG_PROVIDER="nice"  # nice, kcp, inicis, kakaopay 등
export PORTONE_TEST_MODE="false"   # 운영: false, 테스트: true
```

### 3. HTML 파일 수정

`src/main/resources/static/payment.html` 파일에서 포트원 가맹점 식별코드 수정:

```javascript
// 44번 줄 근처
let impCode = 'imp00000000'; // 여기를 발급받은 imp 코드로 변경
```

또는 백엔드에서 동적으로 주입하도록 수정 가능합니다.

### 4. 서버 실행

```bash
./gradlew bootRun
```

### 5. 웹 브라우저 접속

- **메인 페이지**: http://localhost:8080
- **결제 페이지**: http://localhost:8080/payment.html
- **관리자 페이지**: http://localhost:8080/admin-invoices.html
- **결제 내역**: http://localhost:8080/payment-history.html

## 📱 프론트엔드 페이지 설명

### 1. index.html - 메인 대시보드
- 시스템 개요 및 각 페이지로 이동

### 2. payment.html - 청구서 결제 (학부모용)
- 로그인
- 청구서 목록 조회
- 결제 진행
- 결제 완료 확인

**사용 흐름:**
1. 로그인 (username/password)
2. 미납 청구서 조회
3. "결제하기" 버튼 클릭
4. 구매자 정보 입력
5. 결제 수단 선택
6. 포트원 결제창에서 결제 진행
7. 결제 완료 메시지 확인

### 3. admin-invoices.html - 청구서 관리 (관리자용)
- 청구서 생성
- 청구서 목록 조회
- 청구서 삭제
- 통계 대시보드

**사용 흐름:**
1. 관리자 로그인
2. "청구서 생성" 버튼 클릭
3. 학생 선택, 금액, 기한 입력
4. 청구서 생성 완료

### 4. payment-history.html - 결제 내역 (관리자용)
- 전체 결제 내역 조회
- 결제 상세 정보 확인
- 결제 취소/환불 처리

## 💳 지원 결제 수단

### 카드 결제 (card)
- 신용카드
- 체크카드
- 해외카드

### 계좌이체 (trans)
- 실시간 계좌이체

### 가상계좌 (vbank)
- 가상계좌 발급
- 입금 확인

### 간편결제
- 카카오페이 (kakaopay)
- 네이버페이 (naverpay)

### 휴대폰 결제 (phone)
- 휴대폰 소액결제

## 🏦 지원 PG사

- **나이스페이먼츠** (nice) - 기본값
- **KCP** (kcp)
- **이니시스** (inicis)
- **카카오페이** (kakaopay)
- 기타 포트원이 지원하는 모든 PG사

## 🔒 보안 기능

### 1. 중복 결제 방지
- impUid 중복 체크
- 데이터베이스 레벨에서 UNIQUE 제약조건

### 2. 결제 금액 검증
- 클라이언트 요청 금액과 청구서 금액 비교
- 포트원 API로 실제 결제 금액 재확인
- 금액 불일치 시 결제 거부

### 3. 서버 간 통신
- 결제 완료 후 서버에서 포트원 API 직접 호출
- 클라이언트 데이터 신뢰하지 않음

### 4. 청구서 상태 검증
- 이미 납부된 청구서 결제 방지
- 취소된 청구서 결제 방지

## 🧪 테스트 모드

개발 환경에서 API 키 없이 테스트 가능:

```yaml
portone:
  test-mode: true
```

테스트 모드에서는:
- 실제 포트원 API 호출하지 않음
- 모든 결제 시뮬레이션
- 결제창은 뜨지만 실제 결제 안 됨 (포트원 테스트 환경 필요)

## 🔄 결제 플로우

```
┌─────────────┐
│ 클라이언트  │
└──────┬──────┘
       │
       │ 1. POST /api/payments/prepare
       │    (invoiceId, amount, buyerInfo)
       ▼
┌─────────────┐
│   서버      │  - 청구서 검증
│             │  - 주문번호 생성
└──────┬──────┘
       │
       │ 2. Response (merchantUid, amount 등)
       ▼
┌─────────────┐
│ 클라이언트  │  - 포트원 결제창 띄우기
│             │  - IMP.request_pay()
└──────┬──────┘
       │
       │ 3. 사용자 결제 진행
       │    (카드 정보 입력 등)
       ▼
┌─────────────┐
│  포트원     │  - 결제 처리
│             │  - impUid 발급
└──────┬──────┘
       │
       │ 4. 결제 완료 콜백
       ▼
┌─────────────┐
│ 클라이언트  │
└──────┬──────┘
       │
       │ 5. POST /api/payments/complete
       │    (impUid, merchantUid)
       ▼
┌─────────────┐
│   서버      │  - 포트원 API로 결제 정보 조회
│             │  - 금액 검증
│             │  - Payment 저장
│             │  - Invoice 납부 처리
└──────┬──────┘
       │
       │ 6. Response (결제 완료)
       ▼
┌─────────────┐
│ 클라이언트  │  - 결과 표시
└─────────────┘
```

## 📊 데이터베이스 스키마

### payments 테이블
```sql
CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    invoice_id BIGINT NOT NULL,
    imp_uid VARCHAR(100) UNIQUE NOT NULL,
    merchant_uid VARCHAR(100) UNIQUE NOT NULL,
    amount INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(50),
    pg_provider VARCHAR(50),
    paid_at DATETIME,
    receipt_url VARCHAR(500),
    card_name VARCHAR(100),
    card_number VARCHAR(50),
    buyer_name VARCHAR(100),
    buyer_tel VARCHAR(20),
    buyer_email VARCHAR(100),
    created_at DATETIME,
    updated_at DATETIME,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id)
);
```

## 🛠️ API 엔드포인트

### 결제 준비
```http
POST /api/payments/prepare
Authorization: Bearer {token}
Content-Type: application/json

{
  "invoiceId": 1,
  "amount": 100000,
  "buyerName": "홍길동",
  "buyerTel": "010-1234-5678",
  "buyerEmail": "hong@example.com",
  "paymentMethod": "card"
}
```

### 결제 완료 검증
```http
POST /api/payments/complete
Authorization: Bearer {token}
Content-Type: application/json

{
  "impUid": "imp_123456789",
  "merchantUid": "ORDER_INV-20251218-00001_abc123"
}
```

### 결제 취소
```http
POST /api/payments/cancel
Authorization: Bearer {token}
Content-Type: application/json

{
  "impUid": "imp_123456789",
  "reason": "고객 요청으로 취소",
  "cancelAmount": null,
  "refundHolder": "홍길동",
  "refundBank": "국민은행",
  "refundAccount": "123-456-789012"
}
```

### 결제 내역 조회
```http
GET /api/payments
GET /api/payments/{id}
GET /api/payments/imp/{impUid}
GET /api/payments/invoice/{invoiceId}
GET /api/payments/student/{studentId}
Authorization: Bearer {token}
```

## ⚠️ 주의사항

### 운영 환경 배포 전 체크리스트

- [ ] 포트원 가맹점 실제 계약 완료
- [ ] PG사 계약 완료 (나이스페이먼츠 등)
- [ ] API 키 환경변수 설정 완료
- [ ] TEST_MODE를 false로 변경
- [ ] payment.html의 impCode 변경
- [ ] HTTPS 적용 (결제는 반드시 HTTPS 필요)
- [ ] 도메인 등록 및 DNS 설정
- [ ] 포트원 콘솔에서 도메인 화이트리스트 등록

### 보안 주의사항

- API 키는 절대 코드에 하드코딩하지 말 것
- 환경변수 또는 AWS Secrets Manager 사용
- 프론트엔드에서 API Secret 노출 금지
- HTTPS 필수 적용
- CORS 설정 확인

## 🐛 문제 해결

### 결제창이 뜨지 않는 경우
1. impCode가 올바른지 확인
2. 포트원 JavaScript SDK 로드 확인
3. 브라우저 콘솔에서 에러 메시지 확인

### 결제는 성공했는데 서버 검증 실패
1. 서버 로그 확인
2. 포트원 API 키가 올바른지 확인
3. 네트워크 연결 확인

### 금액 검증 실패
1. 청구서 금액과 결제 요청 금액이 일치하는지 확인
2. 포트원 콘솔에서 실제 결제 금액 확인

## 📞 지원

- **포트원 문서**: https://portone.gitbook.io/docs/
- **포트원 고객센터**: https://portone.io/support
- **테스트 카드번호**: 포트원 문서 참조

## 📝 라이선스

이 프로젝트는 K-PLAY 학원의 내부 시스템입니다.
