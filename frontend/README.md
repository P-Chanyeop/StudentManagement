# 학원 관리 시스템 - React 프론트엔드

네이버 스타일(화이트 & 그린) 디자인의 학원 관리 시스템 프론트엔드입니다.

## 🎨 디자인 컨셉

- **색상**: 네이버 그린 (#03C75A) & 화이트
- **폰트**: Noto Sans KR
- **스타일**: 깔끔하고 모던한 UI
- **반응형**: 모바일/태블릿/PC 모두 지원

## 🚀 시작하기

### 1. 의존성 설치

```bash
cd frontend
npm install
```

### 2. 개발 서버 실행

```bash
npm run dev
```

브라우저에서 http://localhost:3000 접속

### 3. 백엔드 연동

백엔드 서버가 http://localhost:8080 에서 실행 중이어야 합니다.

```bash
# 백엔드 실행 (프로젝트 루트에서)
cd ..
./gradlew bootRun
```

## 📁 프로젝트 구조

```
frontend/
├── src/
│   ├── components/      # 재사용 가능한 컴포넌트
│   │   └── Layout.jsx   # 레이아웃 컴포넌트
│   ├── pages/           # 페이지 컴포넌트
│   │   ├── Login.jsx    # 로그인 페이지
│   │   ├── Dashboard.jsx # 대시보드
│   │   └── Students.jsx # 학생 관리
│   ├── services/        # API 서비스
│   │   └── api.js       # Axios 설정 및 API 함수
│   ├── styles/          # CSS 스타일
│   │   ├── global.css   # 전역 스타일
│   │   ├── Login.css
│   │   ├── Dashboard.css
│   │   ├── Students.css
│   │   └── Layout.css
│   ├── utils/           # 유틸리티 함수
│   ├── App.jsx          # 메인 앱 컴포넌트
│   └── main.jsx         # 엔트리 포인트
├── index.html           # HTML 템플릿
├── package.json         # 의존성 관리
└── vite.config.js       # Vite 설정
```

## 🔐 인증 시스템

- **JWT 토큰 기반** 인증
- **자동 토큰 갱신** (Refresh Token)
- **401 에러 시** 자동 로그인 페이지 이동
- **로컬 스토리지**에 토큰 저장

### 로그인 플로우

1. 사용자가 아이디/비밀번호 입력
2. `/api/auth/login` API 호출
3. Access Token & Refresh Token 받음
4. 로컬 스토리지에 저장
5. 대시보드로 이동

## 🎯 구현된 페이지

### 1. 로그인 (/login)
- 깔끔한 로그인 폼
- 네이버 그린 색상 적용
- 에러 메시지 표시
- 데모 계정 안내

### 2. 대시보드 (/dashboard)
- 통계 카드 (학생/출석/예약/수강권)
- 최근 공지사항
- 오늘의 수업 일정

### 3. 학생 관리 (/students)
- 학생 목록 테이블
- 검색 기능
- 학생 등록 버튼
- 수정/상세 보기

## 📦 주요 라이브러리

| 라이브러리 | 용도 | 버전 |
|-----------|------|------|
| React | UI 프레임워크 | 18.2.0 |
| React Router | 라우팅 | 6.20.0 |
| Axios | HTTP 클라이언트 | 1.6.2 |
| TanStack Query | 서버 상태 관리 | 5.12.2 |
| React Icons | 아이콘 | 4.12.0 |
| Vite | 빌드 도구 | 5.0.8 |

## 🔧 API 사용 예시

```javascript
import { studentAPI } from './services/api';

// 학생 목록 조회
const students = await studentAPI.getAll();

// 학생 등록
const newStudent = await studentAPI.create({
  studentName: '홍길동',
  grade: '5학년',
  parentName: '홍부모',
  parentPhone: '010-1234-5678'
});
```

## 🎨 스타일링 가이드

### 네이버 컬러 팔레트

```css
--naver-green: #03C75A         /* Primary Green */
--naver-green-dark: #02B350    /* Hover State */
--naver-green-light: #E8F8F0   /* Background */
```

### 버튼 스타일

```jsx
<button className="btn-primary">Primary Button</button>
<button className="btn-outline">Outline Button</button>
<button className="btn-sm">Small Button</button>
```

## 📱 반응형 디자인

- **Desktop**: 1200px 이상
- **Tablet**: 768px ~ 1199px
- **Mobile**: 768px 이하

## 🔜 다음 구현 예정

- [ ] 출석 체크 화면 (태블릿 최적화)
- [ ] 예약 관리 화면
- [ ] 수강권 관리 화면
- [ ] 레벨테스트 캘린더
- [ ] 문자 발송 화면
- [ ] 상담 관리 화면
- [ ] 통계 대시보드

## 🐛 트러블슈팅

### CORS 에러 발생 시

백엔드의 `SecurityConfig.java`에서 CORS 설정 확인:

```java
configuration.setAllowedOrigins(List.of("http://localhost:3000"));
```

### API 호출 실패 시

1. 백엔드 서버가 실행 중인지 확인
2. 브라우저 개발자 도구 Network 탭 확인
3. 토큰이 올바르게 저장되었는지 확인

## 📞 문의

- 홈페이지: https://softcat.co.kr
- 이메일: oracle7579@gmail.com
