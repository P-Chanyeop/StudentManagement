import axios from 'axios';

// Axios 인스턴스 생성
const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청 인터셉터: 토큰 자동 추가
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 응답 인터셉터: 401 에러 시 로그아웃
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      // 토큰 만료 시 리프레시 토큰으로 재발급 시도
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        try {
          const response = await axios.post('/api/auth/refresh', { refreshToken });
          const { accessToken } = response.data;
          localStorage.setItem('accessToken', accessToken);

          // 원래 요청 재시도
          error.config.headers.Authorization = `Bearer ${accessToken}`;
          return axios(error.config);
        } catch (refreshError) {
          // 리프레시 토큰도 만료됨
          localStorage.clear();
          window.location.href = '/login';
        }
      } else {
        localStorage.clear();
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// 인증 API
export const authAPI = {
  login: (credentials) => api.post('/auth/login', credentials),
  signup: (userData) => api.post('/auth/signup', userData),
  logout: () => api.post('/auth/logout'),
  me: () => api.get('/auth/me'),
};

// 학생 API
export const studentAPI = {
  getAll: () => api.get('/students'),
  getActive: () => api.get('/students/active'),
  getById: (id) => api.get(`/students/${id}`),
  search: (keyword) => api.get(`/students/search?keyword=${keyword}`),
  create: (data) => api.post('/students', data),
  update: (id, data) => api.put(`/students/${id}`, data),
  deactivate: (id) => api.delete(`/students/${id}`),
};

// 수업 API
export const courseAPI = {
  getAll: () => api.get('/courses'),
  getActive: () => api.get('/courses/active'),
  getById: (id) => api.get(`/courses/${id}`),
  getByTeacher: (teacherId) => api.get(`/courses/teacher/${teacherId}`),
  create: (data) => api.post('/courses', data),
  update: (id, data) => api.put(`/courses/${id}`, data),
  deactivate: (id) => api.delete(`/courses/${id}`),
};

// 수강권 API
export const enrollmentAPI = {
  getAll: () => api.get('/enrollments'),
  getByStudent: (studentId) => api.get(`/enrollments/student/${studentId}`),
  getActiveByStudent: (studentId) => api.get(`/enrollments/student/${studentId}/active`),
  getExpiring: (days = 7) => api.get(`/enrollments/expiring?days=${days}`),
  getLowCount: (threshold = 3) => api.get(`/enrollments/low-count?threshold=${threshold}`),
  create: (data) => api.post('/enrollments', data),
  extendPeriod: (id, newEndDate) => api.patch(`/enrollments/${id}/extend?newEndDate=${newEndDate}`),
  addCount: (id, additionalCount) => api.patch(`/enrollments/${id}/add-count?additionalCount=${additionalCount}`),
  cancel: (id) => api.delete(`/enrollments/${id}`),
  deactivate: (id) => api.delete(`/enrollments/${id}`),
};

// 출석 API
export const attendanceAPI = {
  checkIn: (data) => api.post('/attendance/checkin', data),
  checkOut: (id) => api.post(`/attendance/${id}/checkout`),
  updateStatus: (id, status, reason) => api.patch(`/attendance/${id}/status?status=${status}${reason ? `&reason=${reason}` : ''}`),
  getByDate: (date) => api.get(`/attendance/date/${date}`),
  getByStudent: (studentId) => api.get(`/attendance/student/${studentId}`),
  getBySchedule: (scheduleId) => api.get(`/attendance/schedule/${scheduleId}`),
  getByStudentAndRange: (studentId, startDate, endDate) =>
    api.get(`/attendance/student/${studentId}/range?startDate=${startDate}&endDate=${endDate}`),
};

// 예약 API
export const reservationAPI = {
  create: (data) => api.post('/reservations', data),
  getById: (id) => api.get(`/reservations/${id}`),
  getByStudent: (studentId) => api.get(`/reservations/student/${studentId}`),
  getByDate: (date) => api.get(`/reservations/date/${date}`),
  getBySchedule: (scheduleId) => api.get(`/reservations/schedule/${scheduleId}`),
  confirm: (id) => api.post(`/reservations/${id}/confirm`),
  cancel: (id, reason) => api.post(`/reservations/${id}/cancel`, { reason }),
  forceCancel: (id, reason) => api.post(`/reservations/${id}/force-cancel`, { reason }),
};

// 스케줄 API
export const scheduleAPI = {
  create: (data) => api.post('/schedules', data),
  getById: (id) => api.get(`/schedules/${id}`),
  getByDate: (date) => api.get(`/schedules/date/${date}`),
  getByRange: (startDate, endDate) => api.get(`/schedules/range?startDate=${startDate}&endDate=${endDate}`),
  getByCourse: (courseId) => api.get(`/schedules/course/${courseId}`),
  update: (id, data) => api.put(`/schedules/${id}`, data),
  cancel: (id, reason) => api.post(`/schedules/${id}/cancel`, { reason }),
  restore: (id) => api.post(`/schedules/${id}/restore`),
};

// 레벨테스트 API
export const levelTestAPI = {
  create: (data) => api.post('/leveltests', data),
  complete: (id, data) => api.post(`/leveltests/${id}/complete`, data),
  getByRange: (startDate, endDate) => api.get(`/leveltests/range?startDate=${startDate}&endDate=${endDate}`),
  getByStudent: (studentId) => api.get(`/leveltests/student/${studentId}`),
};

// 상담 API
export const consultationAPI = {
  create: (data) => api.post('/consultations', data),
  getByStudent: (studentId) => api.get(`/consultations/student/${studentId}`),
};

// 문자 API
export const messageAPI = {
  send: (data) => api.post('/messages/send', data),
  getByStudent: (studentId) => api.get(`/messages/student/${studentId}`),
  getPending: () => api.get('/messages/pending'),
  getAll: () => api.get('/messages'),
};

// 마이페이지 API
export const mypageAPI = {
  getMyPage: () => api.get('/mypage/me'),
  getStudentMyPage: (studentId) => api.get(`/mypage/student/${studentId}`),
};

export default api;
