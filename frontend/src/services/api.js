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
    const originalRequest = error.config;
    
    // refresh 요청 자체가 실패하면 바로 로그인으로
    if (originalRequest.url?.includes('/auth/refresh')) {
      localStorage.clear();
      window.location.href = '/login';
      return Promise.reject(error);
    }
    
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        try {
          const response = await api.post('/auth/refresh', { refreshToken });
          const { accessToken } = response.data;
          localStorage.setItem('accessToken', accessToken);

          // 원래 요청 재시도
          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          return api(originalRequest);
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
  register: (userData) => api.post('/auth/register', userData),
  logout: () => api.post('/auth/logout'),
  me: () => api.get('/auth/me'),
  getProfile: () => api.get('/auth/profile'),
  getCurrentUser: () => api.get('/auth/me'),
  updateProfile: (data) => api.put('/auth/profile', data),
  checkUsername: (username) => api.get(`/auth/check-username?username=${username}`),
};

// 학생 API
export const studentAPI = {
  getAll: () => api.get('/students'),
  getActive: () => api.get('/students/active'),
  getMyStudents: () => api.get('/students/my-students'),
  getById: (id) => api.get(`/students/${id}`),
  search: (keyword) => api.get(`/students/search?keyword=${keyword}`),
  create: (data) => api.post('/students', data),
  update: (id, data) => api.put(`/students/${id}`, data),
  deactivate: (id) => api.delete(`/students/${id}`),
  // 추가수업 관리
  getAdditionalClass: () => api.get('/students/additional-class'),
  getAdditionalClassExcelList: () => api.get('/students/additional-class/excel-list'),
  updateAdditionalClass: (id, data) => api.put(`/students/${id}/additional-class`, data),
  uploadAdditionalClassExcel: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/students/additional-class/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },
  getExcelList: () => api.get('/student-course/list'),
};

// 수업 API
export const courseAPI = {
  getAll: () => api.get('/courses'),
  getActive: () => api.get('/courses/active'),
  getById: (id) => api.get(`/courses/${id}`),
  getByTeacher: (teacherId) => api.get(`/courses/teacher/${teacherId}`),
  create: (data) => api.post('/courses', data),
  update: (id, data) => api.put(`/courses/${id}`, data),
  delete: (id) => api.delete(`/courses/${id}`), // deactivate를 delete로 변경
  deactivate: (id) => api.delete(`/courses/${id}`),
};

// 수강권 API
export const enrollmentAPI = {
  getMyEnrollments: () => api.get('/enrollments/my'),
  getAll: () => api.get('/enrollments'),
  getEnrollments: () => api.get('/enrollments'),
  getByStudent: (studentId) => api.get(`/enrollments/student/${studentId}`),
  getActiveByStudent: (studentId) => api.get(`/enrollments/student/${studentId}/active`),
  getExpiring: (days = 7) => api.get(`/enrollments/expiring?days=${days}`),
  getLowCount: (threshold = 3) => api.get(`/enrollments/low-count?threshold=${threshold}`),
  create: (data) => api.post('/enrollments', data),
  createUnregistered: (data) => api.post('/enrollments/unregistered', data),
  extendPeriod: (id, newEndDate) => api.patch(`/enrollments/${id}/extend?newEndDate=${newEndDate}`),
  addCount: (id, additionalCount) => api.patch(`/enrollments/${id}/add-count?additionalCount=${additionalCount}`),
  manualAdjustCount: (id, data) => api.patch(`/enrollments/${id}/manual-adjust`, data),
  setCustomDuration: (id, durationMinutes) => api.patch(`/enrollments/${id}/duration?durationMinutes=${durationMinutes}`),
  cancel: (id) => api.delete(`/enrollments/${id}`),
  deactivate: (id) => api.delete(`/enrollments/${id}`),
  // 관리자 전용 기능들
  activate: (id) => api.patch(`/enrollments/${id}/activate`),
  expire: (id) => api.patch(`/enrollments/${id}/expire`),
  delete: (id) => api.delete(`/enrollments/${id}/force`),
  update: (id, data) => api.put(`/enrollments/${id}`, data),
  startHold: (id, data) => api.post(`/enrollments/${id}/hold`, data),
  endHold: (id) => api.delete(`/enrollments/${id}/hold`),
};

// 출석 API
export const attendanceAPI = {
  checkIn: (data) => api.post('/attendances/checkin', data),
  searchByPhone: (data) => api.post('/attendances/search-by-phone', data),
  checkInByPhone: (data) => api.post('/attendances/checkin-by-phone', data),
  checkOut: (id) => api.post(`/attendances/${id}/checkout`),
  updateStatus: (id, status, reason) => api.patch(`/attendances/${id}/status?status=${status}${reason ? `&reason=${reason}` : ''}`),
  completeClass: (id) => api.post(`/attendances/${id}/complete`),
  uncompleteClass: (id) => api.post(`/attendances/${id}/uncomplete`),
  updateMemo: (id, memo) => api.patch(`/attendances/${id}/memo`, { memo }),
  getByDate: (date) => api.get(`/attendances/date/${date}`),
  getByStudent: (studentId) => api.get(`/attendances/student/${studentId}`),
  getBySchedule: (scheduleId) => api.get(`/attendances/schedule/${scheduleId}`),
  getByStudentAndRange: (studentId, startDate, endDate) =>
    api.get(`/attendances/student/${studentId}/range?startDate=${startDate}&endDate=${endDate}`),
  getMyChildAttendances: (date) => api.get(`/attendances/my-child/${date}`),
  getMyChildMonthlyAttendances: (year, month) => api.get(`/attendances/my-child/monthly?year=${year}&month=${month}`),
  getMyChildSchedules: (date) => api.get(`/attendances/my-child/schedules/${date}`),
  getMyChildMonthlySchedules: (year, month) => api.get(`/attendances/my-child/schedules/monthly?year=${year}&month=${month}`),
  updateClassComplete: (attendanceId) => 
    api.patch(`/attendances/${attendanceId}/toggle-completed`),
  cancelAttendance: (attendanceId) => 
    api.delete(`/attendances/${attendanceId}`),
  updateReason: (attendanceId, reason) => 
    api.patch(`/attendances/${attendanceId}/reason`, { reason }),
  updateDcCheck: (attendanceId, dcCheck) =>
    api.put(`/attendances/${attendanceId}/dc-check`, { dcCheck }),
  updateWrCheck: (attendanceId, wrCheck) =>
    api.put(`/attendances/${attendanceId}/wr-check`, { wrCheck }),
  toggleVocabularyClass: (attendanceId) =>
    api.put(`/attendances/${attendanceId}/vocabulary`),
  toggleGrammarClass: (attendanceId) =>
    api.put(`/attendances/${attendanceId}/grammar`),
  togglePhonicsClass: (attendanceId) =>
    api.put(`/attendances/${attendanceId}/phonics`),
  toggleSpeakingClass: (attendanceId) =>
    api.put(`/attendances/${attendanceId}/speaking`),
  addManual: (data) => api.post('/attendances/manual', data),
};

// 선생님 출퇴근 API
export const teacherAttendanceAPI = {
  getToday: () => api.get('/teacher-attendance/today'),
  getByDate: (date) => api.get(`/teacher-attendance/date/${date}`),
  getByRange: (startDate, endDate) => api.get(`/teacher-attendance/range?startDate=${startDate}&endDate=${endDate}`),
  checkIn: () => api.post('/teacher-attendance/check-in'),
  checkOut: () => api.post('/teacher-attendance/check-out'),
  getMy: () => api.get('/teacher-attendance/my'),
  getTeachers: () => api.get('/teacher-attendance/teachers'),
  registerTeacher: (data) => api.post('/teacher-attendance/register', data),
};

// 예약 API
export const reservationAPI = {
  create: (data) => api.post('/reservations', data),
  getById: (id) => api.get(`/reservations/${id}`),
  getByStudent: (studentId) => api.get(`/reservations/student/${studentId}`),
  getByDate: (date) => api.get(`/reservations/date/${date}`),
  getMyReservations: () => api.get('/reservations/my-reservations'),
  getNewReservations: (since) => api.get(`/reservations/new?since=${since}`),
  getReservedTimes: (date, consultationType) => {
    const params = consultationType ? { consultationType } : {};
    return api.get(`/reservations/reserved-times/${date}`, { params });
  },
  getTimeSlotStatus: (date, consultationType) => {
    const params = consultationType ? { consultationType } : {};
    return api.get(`/reservations/time-slot-status/${date}`, { params });
  },
  getBySchedule: (scheduleId) => api.get(`/reservations/schedule/${scheduleId}`),
  confirm: (id) => api.post(`/reservations/${id}/confirm`),
  cancel: (id, reason) => api.post(`/reservations/${id}/cancel`, { reason }),
  forceCancel: (id, reason) => api.post(`/reservations/${id}/force-cancel`, { reason }),
  checkAvailability: () => api.get('/reservations/availability'),
  getAvailableDates: () => api.get('/reservations/available-dates'),
};

export const blockedTimeSlotAPI = {
  getAll: () => api.get('/blocked-time-slots'),
  create: (data) => api.post('/blocked-time-slots', data),
  delete: (id) => api.delete(`/blocked-time-slots/${id}`),
};

// 네이버 예약 API
export const naverBookingAPI = {
  sync: (date) => api.post(`/naver-booking/sync${date ? `?date=${date}` : ''}`),
  getToday: () => api.get('/naver-booking/today'),
  getByDate: (date) => api.get(`/naver-booking/date/${date}`),
};

// 스케줄 API
export const scheduleAPI = {
  create: (data) => api.post('/schedules', data),
  getById: (id) => api.get(`/schedules/${id}`),
  getByDate: (date) => api.get(`/schedules/date/${date}`),
  getAvailableByDate: (date) => api.get(`/schedules/available/${date}`),
  getByRange: (startDate, endDate) => api.get(`/schedules/range?startDate=${startDate}&endDate=${endDate}`),
  getByCourse: (courseId) => api.get(`/schedules/course/${courseId}`),
  update: (id, data) => api.put(`/schedules/${id}`, data),
  cancel: (id, reason) => api.post(`/schedules/${id}/cancel`, { reason }),
  restore: (id) => api.post(`/schedules/${id}/restore`),
  // 역할별 스케줄 조회
  getMySchedules: (date) => api.get(`/schedules/my/${date}`),
  getAllSchedules: (date) => api.get(`/schedules/all/${date}`),
  getMyMonthlySchedules: (year, month) => api.get(`/schedules/my/monthly?year=${year}&month=${month}`),
  getAllMonthlySchedules: (year, month) => api.get(`/schedules/all/monthly?year=${year}&month=${month}`),
};

// 레벨테스트 API
export const levelTestAPI = {
  create: (data) => api.post('/leveltests', data),
  complete: (id, data) => api.post(`/leveltests/${id}/complete`, data),
  getByRange: (startDate, endDate) => api.get(`/leveltests/range?startDate=${startDate}&endDate=${endDate}`),
  getByStudent: (studentId) => api.get(`/leveltests/student/${studentId}`),
};

// 파일 업로드 API
export const fileAPI = {
  uploadAudio: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/files/upload/audio', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },
  uploadMultipleAudio: (files) => {
    const formData = new FormData();
    files.forEach(file => formData.append('files', file));
    return api.post('/files/upload/audio/multiple', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },
  uploadDocument: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/files/upload/document', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },
  uploadMultipleDocument: (files) => {
    const formData = new FormData();
    files.forEach(file => formData.append('files', file));
    return api.post('/files/upload/document/multiple', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },
  download: (filePath) => api.get(`/files/download?filePath=${encodeURIComponent(filePath)}`, { responseType: 'blob' }),
  delete: (filePath) => api.delete(`/files/delete?filePath=${encodeURIComponent(filePath)}`),
};

// 상담 API
export const consultationAPI = {
  create: (data) => api.post('/consultations', data),
  getByStudent: (studentId) => api.get(`/consultations/student/${studentId}`),
  getMyChildren: () => api.get('/consultations/my-children'),
  update: (id, data) => api.put(`/consultations/${id}`, data),
  delete: (id) => api.delete(`/consultations/${id}`),
  exportByStudent: (studentId) => api.get(`/consultations/export/student/${studentId}`, { responseType: 'blob' }),
  exportAll: () => api.get('/consultations/export/all', { responseType: 'blob' }),
  exportByDateRange: (startDate, endDate) => api.get(`/consultations/export/date-range?startDate=${startDate}&endDate=${endDate}`, { responseType: 'blob' })
};

// 문자 API
export const messageAPI = {
  send: (data) => api.post('/messages/send', data),
  getByStudent: (studentId) => api.get(`/messages/student/${studentId}`),
  getPending: () => api.get('/messages/pending'),
  getAll: () => api.get('/messages'),
};

// SMS API
export const smsAPI = {
  send: (data) => api.post('/sms/send', data),
  getRemain: () => api.get('/sms/remain'),
};

// SMS 템플릿 API
export const smsTemplateAPI = {
  getAll: () => api.get('/sms-templates'),
  create: (data) => api.post('/sms-templates', data),
  update: (id, data) => api.put(`/sms-templates/${id}`, data),
  delete: (id) => api.delete(`/sms-templates/${id}`),
};

// 마이페이지 API
export const mypageAPI = {
  getMyPage: () => api.get('/mypage/me'),
  getStudentMyPage: (studentId) => api.get(`/mypage/student/${studentId}`),
};

// 대시보드 API
export const dashboardAPI = {
  getStats: () => api.get('/dashboard/stats'),
};

// 공지사항 API
export const noticeAPI = {
  getAll: (page = 0, size = 10) => api.get(`/notices?page=${page}&size=${size}`),
  getById: (id) => api.get(`/notices/${id}`),
  getPinned: () => api.get('/notices/pinned'),
  search: (keyword, page = 0, size = 10) => api.get(`/notices/search?keyword=${keyword}&page=${page}&size=${size}`),
  create: (data) => api.post('/notices', data),
  update: (id, data) => api.put(`/notices/${id}`, data),
  delete: (id) => api.delete(`/notices/${id}`),
  pin: (id) => api.patch(`/notices/${id}/pin`),
  unpin: (id) => api.patch(`/notices/${id}/unpin`),
  getViewers: (id) => api.get(`/notices/${id}/viewers`),
};

// 사용자 메뉴 설정 API
export const userMenuAPI = {
  saveMenuOrder: (menuPaths) => api.post('/user/menu-settings', { menuPaths }),
  getMenuOrder: () => api.get('/user/menu-settings'),
};

// 보강 수업 API
export const makeupClassAPI = {
  getAll: () => api.get('/makeup-classes'),
  getById: (id) => api.get(`/makeup-classes/${id}`),
  getByStudent: (studentId) => api.get(`/makeup-classes/student/${studentId}`),
  getByCourse: (courseId) => api.get(`/makeup-classes/course/${courseId}`),
  getByDateRange: (startDate, endDate) => api.get(`/makeup-classes/date-range?startDate=${startDate}&endDate=${endDate}`),
  getByDate: (date) => api.get(`/makeup-classes/date/${date}`),
  getByStatus: (status) => api.get(`/makeup-classes/status/${status}`),
  getUpcomingByStudent: (studentId) => api.get(`/makeup-classes/upcoming/student/${studentId}`),
  getAllUpcoming: () => api.get('/makeup-classes/upcoming'),
  getMyChildMakeupClasses: () => api.get('/makeup-classes/my-child'),
  create: (data) => api.post('/makeup-classes', data),
  update: (id, data) => api.put(`/makeup-classes/${id}`, data),
  delete: (id) => api.delete(`/makeup-classes/${id}`),
  complete: (id) => api.patch(`/makeup-classes/${id}/complete`),
  cancel: (id) => api.patch(`/makeup-classes/${id}/cancel`),
};

export default api;
