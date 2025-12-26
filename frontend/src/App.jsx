import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import UserDashboard from './pages/UserDashboard';
import Students from './pages/Students';
import Courses from './pages/Courses';
import Attendance from './pages/Attendance';
import Reservations from './pages/Reservations';
import ParentReservation from './pages/ParentReservation';
import Enrollments from './pages/Enrollments';
import EnrollmentAdjustment from './pages/EnrollmentAdjustment';
import LevelTests from './pages/LevelTests';
import Consultations from './pages/Consultations';
import Messages from './pages/Messages';
import MakeupClasses from './pages/MakeupClasses';
import Notices from './pages/Notices';
import MyPage from './pages/MyPage';
import { useQuery } from '@tanstack/react-query';
import { authAPI } from './services/api';

// 보호된 라우트 컴포넌트
function ProtectedRoute({ children }) {
  const token = localStorage.getItem('accessToken');

  if (!token) {
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

function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
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
        path="/enrollments"
        element={
          <ProtectedRoute>
            <Enrollments />
          </ProtectedRoute>
        }
      />
      <Route
        path="/enrollment-adjustment"
        element={
          <ProtectedRoute>
            <EnrollmentAdjustment />
          </ProtectedRoute>
        }
      />
      <Route
        path="/leveltests"
        element={
          <ProtectedRoute>
            <LevelTests />
          </ProtectedRoute>
        }
      />
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
        path="/mypage"
        element={
          <ProtectedRoute>
            <MyPage />
          </ProtectedRoute>
        }
      />
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}

export default App;
