import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { useEffect } from 'react';
import Layout from './components/Layout';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import UserDashboard from './pages/UserDashboard';
import Students from './pages/Students';
import Courses from './pages/Courses';
import Attendance from './pages/Attendance';
import CheckIn from './pages/CheckIn';
import Reservations from './pages/Reservations';
import ParentReservation from './pages/ParentReservation';
import ConsultationReservation from './pages/ConsultationReservation';
import ClassInfo from './pages/ClassInfo';
import Enrollments from './pages/Enrollments';
// import EnrollmentAdjustment from './pages/EnrollmentAdjustment';
import Consultations from './pages/Consultations';
import Messages from './pages/Messages';
import MakeupClasses from './pages/MakeupClasses';
import Notices from './pages/Notices';
import MyPage from './pages/MyPage';
import ParentMyPage from './pages/ParentMyPage';
import AdditionalClass from './pages/AdditionalClass';
import MyQuizScores from './pages/MyQuizScores';
import QuizManagement from './pages/QuizManagement';
import { useQuery } from '@tanstack/react-query';
import { authAPI } from './services/api';

// 페이지 변경 시 스크롤을 맨 위로 이동시키는 컴포넌트
function ScrollToTop() {
  const { pathname } = useLocation();

  useEffect(() => {
    window.scrollTo(0, 0);
  }, [pathname]);

  return null;
}

// 보호된 라우트 컴포넌트
function ProtectedRoute({ children }) {
  const token = localStorage.getItem('accessToken');

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  // 토큰 만료 체크 (JWT payload의 exp)
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    if (payload.exp * 1000 < Date.now()) {
      // 만료됨 - refresh 시도
      const refreshToken = localStorage.getItem('refreshToken');
      if (!refreshToken) {
        localStorage.clear();
        return <Navigate to="/login" replace />;
      }
    }
  } catch {
    localStorage.clear();
    return <Navigate to="/login" replace />;
  }

  return <Layout>{children}</Layout>;
}

// 역할별 대시보드 라우팅
function RoleDashboard() {
  const { data: profile, isLoading } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
  });

  if (isLoading) {
    return <div>로딩 중...</div>;
  }

  // 부모님은 UserDashboard, 관리자/선생님은 Dashboard
  if (profile?.role === 'PARENT') {
    return <UserDashboard />;
  } else {
    return <Dashboard />;
  }
}

// 역할별 마이페이지 라우팅
function RoleMyPage() {
  const { data: profile, isLoading } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
  });

  if (isLoading) {
    return <div>로딩 중...</div>;
  }

  // 부모님은 ParentMyPage, 관리자/선생님은 MyPage
  if (profile?.role === 'PARENT') {
    return <ParentMyPage />;
  } else {
    return <MyPage />;
  }
}

function App() {
  return (
    <>
      <ScrollToTop />
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <RoleDashboard />
            </ProtectedRoute>
          }
        />
      <Route
        path="/students"
        element={
          <ProtectedRoute>
            <Students />
          </ProtectedRoute>
        }
      />
      <Route
        path="/courses"
        element={
          <ProtectedRoute>
            <Courses />
          </ProtectedRoute>
        }
      />
      <Route
        path="/attendance"
        element={
          <ProtectedRoute>
            <Attendance />
          </ProtectedRoute>
        }
      />
      <Route
        path="/check-in"
        element={
          <ProtectedRoute>
            <CheckIn />
          </ProtectedRoute>
        }
      />
      <Route
        path="/reservations"
        element={
          <ProtectedRoute>
            <Reservations />
          </ProtectedRoute>
        }
      />
      <Route
        path="/parent-reservation"
        element={
          <ProtectedRoute>
            <ParentReservation />
          </ProtectedRoute>
        }
      />
      <Route
        path="/consultation-reservation"
        element={
          <ProtectedRoute>
            <ConsultationReservation />
          </ProtectedRoute>
        }
      />
      <Route
        path="/class-info"
        element={
          <ProtectedRoute>
            <ClassInfo />
          </ProtectedRoute>
        }
      />
      <Route
        path="/enrollments"
        element={
          <ProtectedRoute>
            <Enrollments />
          </ProtectedRoute>
        }
      />
      {/* 횟수 조정 페이지 주석 처리
      <Route
        path="/enrollment-adjustment"
        element={
          <ProtectedRoute>
            <EnrollmentAdjustment />
          </ProtectedRoute>
        }
      />
      */}
      <Route
        path="/consultations"
        element={
          <ProtectedRoute>
            <Consultations />
          </ProtectedRoute>
        }
      />
      <Route
        path="/messages"
        element={
          <ProtectedRoute>
            <Messages />
          </ProtectedRoute>
        }
      />
      <Route
        path="/makeup-classes"
        element={
          <ProtectedRoute>
            <MakeupClasses />
          </ProtectedRoute>
        }
      />
      <Route
        path="/notices"
        element={
          <ProtectedRoute>
            <Notices />
          </ProtectedRoute>
        }
      />
      <Route
        path="/additional-class"
        element={
          <ProtectedRoute>
            <AdditionalClass />
          </ProtectedRoute>
        }
      />
      <Route
        path="/my-quiz"
        element={
          <ProtectedRoute>
            <MyQuizScores />
          </ProtectedRoute>
        }
      />
      <Route
        path="/quiz-management"
        element={
          <ProtectedRoute>
            <QuizManagement />
          </ProtectedRoute>
        }
      />
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
    </Routes>
    </>
  );
}

export default App;
