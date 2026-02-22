import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import LoadingSpinner from '../components/LoadingSpinner';
import { courseAPI, scheduleAPI, reservationAPI } from '../services/api';
import { getTodayString } from '../utils/dateUtils';
import '../styles/Courses.css';

function Courses() {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedCourse, setSelectedCourse] = useState(null);
  const [activeTab, setActiveTab] = useState('info'); // activeTab state 추가
  const [newCourse, setNewCourse] = useState({
    courseName: '',
    description: '반 설명',
    level: '',
    capacity: 10,
    durationMinutes: 60,
  });

  // 코스 목록 조회 (활성화된 코스만)
  const { data: courses = [], isLoading } = useQuery({
    queryKey: ['courses'],
    queryFn: async () => {
      const response = await courseAPI.getActive(); // getAll() -> getActive()로 변경
      return response.data;
    },
  });

  // 코스 생성 mutation
  const createMutation = useMutation({
    mutationFn: (data) => courseAPI.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries(['courses']);
      setShowCreateModal(false);
      setNewCourse({
        courseName: '',
        description: '반 설명',
        level: '',
        capacity: 10,
        durationMinutes: 60,
      });
      alert('수업이 생성되었습니다.');
    },
    onError: (error) => {
      alert(`생성 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    },
  });

  // 코스 수정 mutation
  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => courseAPI.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries(['courses']);
      setShowEditModal(false);
      setSelectedCourse(null);
      alert('코스가 수정되었습니다.');
    },
    onError: (error) => {
      alert(`수정 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    },
  });

  // 코스 삭제 mutation
  const deleteMutation = useMutation({
    mutationFn: (id) => {
      console.log('Calling courseAPI.delete with id:', id); // 디버깅
      return courseAPI.delete(id);
    },
    onSuccess: (data) => {
      console.log('Delete success:', data); // 디버깅
      queryClient.invalidateQueries(['courses']);
      queryClient.refetchQueries(['courses']); // 강제 새로고침 추가
      alert('반이 성공적으로 삭제되었습니다.');
      window.location.reload(); // 임시 해결책: 페이지 새로고침
    },
    onError: (error) => {
      console.error('Delete error:', error); // 디버깅
      alert(`삭제 실패: ${error.response?.data?.message || error.message || '오류가 발생했습니다.'}`);
    },
  });

  const handleCreateCourse = () => {
    if (!newCourse.courseName || !newCourse.durationMinutes) {
      alert('반이름과 수업시간을 입력해주세요.');
      return;
    }

    if (newCourse.durationMinutes <= 0) {
      alert('수업시간을 올바르게 입력해주세요.');
      return;
    }

    const courseData = {
      ...newCourse,
      maxStudents: newCourse.capacity // capacity를 maxStudents로 매핑
    };

    createMutation.mutate(courseData);
  };

  const handleUpdateCourse = () => {
    if (!selectedCourse.courseName || !selectedCourse.description) {
      alert('코스명과 설명을 입력해주세요.');
      return;
    }

    updateMutation.mutate({
      id: selectedCourse.id,
      data: {
        courseName: selectedCourse.courseName,
        description: selectedCourse.description,
        level: selectedCourse.level,
        capacity: selectedCourse.capacity,
        durationMinutes: selectedCourse.durationMinutes,
      },
    });
  };

  const handleDeleteCourse = (id, courseName) => {
    console.log('Deleting course:', id, courseName); // 디버깅
    if (window.confirm(`"${courseName}" 반을 삭제하시겠습니까?`)) {
      console.log('Delete confirmed, calling API...'); // 디버깅
      deleteMutation.mutate(id);
    }
  };

  const openEditModal = (course) => {
    console.log('Opening edit modal for course:', course); // 디버깅
    setSelectedCourse({ ...course });
    setActiveTab('info'); // activeTab 설정 추가
    setShowEditModal(true);
  };

  // 검색 필터링
  const filteredCourses = courses.filter((course) => {
    return (
      searchQuery === '' ||
      course.courseName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      course.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
      course.level.toLowerCase().includes(searchQuery.toLowerCase())
    );
  });

  // 반 이름 배지
  const getClassBadge = (className) => {
    return (
      <span 
        className="course-level-badge" 
        style={{ 
          backgroundColor: '#03C75A',
          color: 'white',
          padding: '4px 12px',
          borderRadius: '12px',
          fontSize: '13px',
          fontWeight: '600',
          whiteSpace: 'nowrap'
        }}
      >
        {className || '미지정'}
      </span>
    );
  };

  if (isLoading) {
    return (
      <div className="page-wrapper">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="page-wrapper">
      <div className="page-header">
        <div className="page-header-content">
          <div className="page-title-section">
            <h1 className="page-title">
              <i className="fas fa-chalkboard-teacher"></i>
              반 관리
            </h1>
            <p className="page-subtitle">학원의 수업 과정을 관리합니다</p>
          </div>
        </div>
      </div>

      {/* 메인 컨텐츠 */}
      <div className="page-content">
        <CoursesTab 
          courses={courses}
          searchQuery={searchQuery}
          setSearchQuery={setSearchQuery}
          setShowCreateModal={setShowCreateModal}
          openEditModal={openEditModal}
          handleDeleteCourse={handleDeleteCourse}
          isLoading={isLoading}
          filteredCourses={filteredCourses}
        />
      </div>

      {/* 코스 생성 모달 */}
      {showCreateModal && (
        <div className="crs-modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="crs-modal" onClick={(e) => e.stopPropagation()}>
            <div className="crs-modal-header">
              <h2>반 생성</h2>
              <button className="crs-modal-close" onClick={() => setShowCreateModal(false)}>×</button>
            </div>
            <div className="crs-modal-body">
              <div className="crs-field">
                <label>반이름 *</label>
                <input type="text" value={newCourse.courseName} onChange={(e) => setNewCourse({ ...newCourse, courseName: e.target.value })} placeholder="예: A반, 초급반, 월수금반 등" />
              </div>
              <div className="crs-field">
                <label>설명</label>
                <textarea value={newCourse.description || ''} onChange={(e) => setNewCourse({ ...newCourse, description: e.target.value })} placeholder="반에 대한 설명을 입력하세요" rows="3" />
              </div>
              <div className="crs-field">
                <label>수업시간 (분) *</label>
                <input type="number" value={newCourse.durationMinutes} onChange={(e) => setNewCourse({ ...newCourse, durationMinutes: parseInt(e.target.value) || 0 })} placeholder="60" min="1" />
              </div>
            </div>
            <div className="crs-modal-actions">
              <button className="crs-btn-cancel" onClick={() => setShowCreateModal(false)}>취소</button>
              <button className="crs-btn-submit" onClick={handleCreateCourse}>생성</button>
            </div>
          </div>
        </div>
      )}

      {/* 코스 상세 모달 */}
      {showEditModal && selectedCourse && (
        <div className="crs-modal-overlay" onClick={() => setShowEditModal(false)}>
          <div className="crs-modal" style={{ maxWidth: 700 }} onClick={(e) => e.stopPropagation()}>
            <div className="crs-modal-header">
              <h2>{selectedCourse.courseName}</h2>
              <button className="crs-modal-close" onClick={() => setShowEditModal(false)}>×</button>
            </div>
            <div className="crs-modal-body">
              <CourseInfoTab course={selectedCourse} onUpdate={(u) => setSelectedCourse(u)} onClose={() => setShowEditModal(false)} updateMutation={updateMutation} />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// 수업 정보 탭 컴포넌트
function CourseInfoTab({ course, onUpdate, onClose, updateMutation }) {
  const handleUpdateCourse = () => {
    const courseData = {
      ...course,
      maxStudents: course.capacity || course.maxStudents || 10
    };
    updateMutation.mutate({ id: course.id, data: courseData });
  };

  return (
    <>
      <div className="crs-field">
        <label>코스명 *</label>
        <input type="text" value={course.courseName} onChange={(e) => onUpdate({ ...course, courseName: e.target.value })} placeholder="예: 기초 영어 회화" />
      </div>
      <div className="crs-field">
        <label>설명</label>
        <textarea value={course.description || ''} onChange={(e) => onUpdate({ ...course, description: e.target.value })} placeholder="코스에 대한 설명을 입력하세요" rows="4" />
      </div>
      <div className="crs-field">
        <label>반 이름</label>
        <input type="text" value={course.level} onChange={(e) => onUpdate({ ...course, level: e.target.value })} placeholder="예: A반, 초급반" />
      </div>
      <div className="crs-field">
        <label>수업시간 (분) *</label>
        <input type="number" value={course.durationMinutes} onChange={(e) => onUpdate({ ...course, durationMinutes: parseInt(e.target.value) || 0 })} placeholder="60" min="1" />
      </div>
      <div className="crs-modal-actions" style={{ padding: '16px 0 0', border: 'none' }}>
        <button className="crs-btn-cancel" onClick={onClose}>취소</button>
        <button className="crs-btn-submit" onClick={handleUpdateCourse}>저장</button>
      </div>
    </>
  );
}

// 스케줄 관리 탭 컴포넌트  
function ScheduleTab({ course }) {
  const [showCreateSchedule, setShowCreateSchedule] = useState(false);
  const [newSchedule, setNewSchedule] = useState({
    scheduleDate: '',
    startTime: '',
    endTime: '',
    memo: ''
  });

  // 스케줄 목록 조회
  const { data: scheduleData = [], refetch } = useQuery({
    queryKey: ['schedules', course.id],
    queryFn: async () => {
      const response = await scheduleAPI.getByCourse(course.id);
      return response.data;
    }
  });

  // 스케줄 생성
  const createScheduleMutation = useMutation({
    mutationFn: (data) => scheduleAPI.create({
      ...data,
      courseId: course.id
    }),
    onSuccess: () => {
      refetch();
      setShowCreateSchedule(false);
      setNewSchedule({
        scheduleDate: '',
        startTime: '',
        endTime: '',
        memo: ''
      });
      alert('스케줄이 생성되었습니다.');
    },
    onError: (error) => {
      alert(`생성 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    }
  });

  // 스케줄 취소
  const cancelScheduleMutation = useMutation({
    mutationFn: ({ id, reason }) => scheduleAPI.cancel(id, reason),
    onSuccess: () => {
      refetch();
      alert('스케줄이 취소되었습니다.');
    }
  });

  const handleCreateSchedule = (e) => {
    e.preventDefault();
    if (!newSchedule.scheduleDate || !newSchedule.startTime || !newSchedule.endTime) {
      alert('필수 항목을 모두 입력해주세요.');
      return;
    }
    createScheduleMutation.mutate(newSchedule);
  };

  const handleCancelSchedule = (scheduleId) => {
    const reason = prompt('취소 사유를 입력해주세요:');
    if (reason) {
      cancelScheduleMutation.mutate({ id: scheduleId, reason });
    }
  };

  return (
    <>
      <div style={{ marginBottom: 16 }}>
        <button className="crs-btn-submit" onClick={() => setShowCreateSchedule(true)}>
          <i className="fas fa-plus"></i> 스케줄 추가
        </button>
      </div>

      {showCreateSchedule && (
        <div style={{ background: '#f8f9fa', padding: 20, borderRadius: 8, marginBottom: 16 }}>
          <h3 style={{ margin: '0 0 12px' }}>새 스케줄 생성</h3>
          <form onSubmit={handleCreateSchedule}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 12, marginBottom: 12 }}>
              <div className="crs-field"><label>날짜 *</label><input type="date" value={newSchedule.scheduleDate} onChange={(e) => setNewSchedule({...newSchedule, scheduleDate: e.target.value})} required /></div>
              <div className="crs-field"><label>시작 *</label><input type="time" value={newSchedule.startTime} onChange={(e) => setNewSchedule({...newSchedule, startTime: e.target.value})} required /></div>
              <div className="crs-field"><label>종료 *</label><input type="time" value={newSchedule.endTime} onChange={(e) => setNewSchedule({...newSchedule, endTime: e.target.value})} required /></div>
            </div>
            <div className="crs-field"><label>메모</label><textarea value={newSchedule.memo} onChange={(e) => setNewSchedule({...newSchedule, memo: e.target.value})} placeholder="수업 관련 메모" /></div>
            <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 12 }}>
              <button type="submit" className="crs-btn-submit">생성</button>
              <button type="button" className="crs-btn-cancel" onClick={() => setShowCreateSchedule(false)}>취소</button>
            </div>
          </form>
        </div>
      )}

      <h3 style={{ margin: '0 0 12px', fontSize: 15 }}>스케줄 목록</h3>
      {scheduleData.length === 0 ? (
        <div className="crs-empty"><i className="fas fa-calendar-times"></i>등록된 스케줄이 없습니다</div>
      ) : (
        <div className="crs-schedule-items">
          {scheduleData.map((schedule) => (
            <div key={schedule.id} className={`crs-schedule-item ${schedule.isCancelled ? 'cancelled' : ''}`}>
              <div className="crs-schedule-info">
                <span><i className="fas fa-calendar"></i> {schedule.scheduleDate}</span>
                <span><i className="fas fa-clock"></i> {schedule.startTime} - {schedule.endTime}</span>
                <span><i className="fas fa-users"></i> {schedule.currentStudents}명</span>
                {schedule.memo && <span><i className="fas fa-sticky-note"></i> {schedule.memo}</span>}
              </div>
              <div>
                {!schedule.isCancelled ? (
                  <button className="crs-btn-delete" onClick={() => handleCancelSchedule(schedule.id)}>
                    <i className="fas fa-times"></i> 취소
                  </button>
                ) : (
                  <span style={{ fontSize: 12, background: '#dc3545', color: '#fff', padding: '4px 8px', borderRadius: 4 }}>취소됨</span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </>
  );
}

export default Courses;

// 수업 관리 탭 컴포넌트
function CoursesTab({ courses, searchQuery, setSearchQuery, setShowCreateModal, openEditModal, handleDeleteCourse, isLoading, filteredCourses }) {
  return (
    <div>
      <div className="crs-toolbar">
        <div className="crs-search-wrap">
          <i className="fas fa-search"></i>
          <input
            type="text"
            placeholder="수업명, 설명, 레벨로 검색..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="crs-search-input"
          />
        </div>
        <span className="crs-count">
          총 <strong>{filteredCourses.length}</strong>개
        </span>
        <button className="crs-create-btn" onClick={() => setShowCreateModal(true)}>
          <i className="fas fa-plus"></i> 반 생성
        </button>
      </div>

      {filteredCourses.length === 0 ? (
        <div className="crs-empty">
          <i className="fas fa-chalkboard"></i>
          {searchQuery ? '검색 결과가 없습니다.' : '등록된 수업이 없습니다.'}
        </div>
      ) : (
        <div className="crs-grid">
          {filteredCourses.map((course) => (
            <div key={course.id} className="crs-card">
              <div className="crs-card-header">
                <h3>{course.courseName}</h3>
                {course.level && <span className="crs-level-badge">{course.level}</span>}
              </div>
              <p className="crs-desc">{course.description}</p>
              <div className="crs-details">
                <div className="crs-detail-item">
                  <i className="fas fa-users"></i>
                  <span>수강생: {course.currentEnrollments || 0}명</span>
                </div>
                <div className="crs-detail-item">
                  <i className="fas fa-clock"></i>
                  <span>수업: {course.durationMinutes}분</span>
                </div>
              </div>
              <div className="crs-actions">
                <button className="crs-btn-edit" onClick={() => openEditModal(course)}>
                  <i className="fas fa-edit"></i> 수정
                </button>
                <button className="crs-btn-delete" onClick={() => handleDeleteCourse(course.id, course.courseName)}>
                  <i className="fas fa-trash"></i> 삭제
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

// 스케줄 관리 탭 컴포넌트
function SchedulesTab() {
  const [selectedDate, setSelectedDate] = useState(getTodayString());
  const [selectedCourse, setSelectedCourse] = useState('');
  const [showCreateSchedule, setShowCreateSchedule] = useState(false);
  const [newSchedule, setNewSchedule] = useState({
    courseId: '',
    scheduleDate: '',
    startTime: '',
    endTime: '',
    memo: ''
  });

  // 수업 목록 조회
  const { data: courses = [] } = useQuery({
    queryKey: ['courses'],
    queryFn: async () => {
      const response = await courseAPI.getAll();
      return response.data;
    }
  });

  // 스케줄 목록 조회
  const { data: schedules = [], refetch } = useQuery({
    queryKey: ['schedules', selectedDate, selectedCourse],
    queryFn: async () => {
      if (selectedCourse) {
        const response = await scheduleAPI.getByCourse(selectedCourse);
        return response.data.filter(s => s.scheduleDate === selectedDate);
      } else {
        const response = await scheduleAPI.getByDate(selectedDate);
        return response.data;
      }
    }
  });

  // 재원생상담 예약 조회
  const { data: classReservations = [] } = useQuery({
    queryKey: ['classReservations', selectedDate],
    queryFn: async () => {
      const response = await reservationAPI.getByDate(selectedDate);
      // 재원생상담 유형만 필터링
      return response.data.filter(reservation => reservation.consultationType === '재원생상담');
    },
  });

  // 스케줄과 예약 데이터 합치기
  const combinedSchedules = [
    ...schedules,
    ...classReservations.map(reservation => ({
      id: `reservation-${reservation.id}`,
      courseName: '영어 수업 (예약)',
      scheduleDate: reservation.scheduleDate,
      startTime: reservation.startTime,
      endTime: reservation.endTime,
      currentStudents: 1,
      maxStudents: 1,
      memo: reservation.memo || '상담 예약에서 등록',
      isCancelled: false,
      isReservation: true,
      reservationId: reservation.id,
      studentName: reservation.studentName
    }))
  ];

  // 스케줄 생성
  const createScheduleMutation = useMutation({
    mutationFn: (data) => scheduleAPI.create(data),
    onSuccess: () => {
      refetch();
      setShowCreateSchedule(false);
      setNewSchedule({
        courseId: '',
        scheduleDate: '',
        startTime: '',
        endTime: '',
        memo: ''
      });
      alert('스케줄이 생성되었습니다.');
    }
  });

  const handleCreateSchedule = (e) => {
    e.preventDefault();
    if (!newSchedule.courseId || !newSchedule.scheduleDate || !newSchedule.startTime || !newSchedule.endTime) {
      alert('필수 항목을 모두 입력해주세요.');
      return;
    }
    createScheduleMutation.mutate(newSchedule);
  };

  return (
    <div className="tab-content-wrapper">
      <div className="tab-header">
        <button className="btn-primary" onClick={() => setShowCreateSchedule(true)}>
          <i className="fas fa-plus"></i> 스케줄 생성
        </button>
      </div>

      <div className="filter-section">
        <div className="filter-card">
          <div className="filter-group">
            <label><i className="fas fa-calendar"></i> 날짜</label>
            <input
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="filter-input"
            />
          </div>
          <div className="filter-group">
            <label><i className="fas fa-book"></i> 수업</label>
            <select
              value={selectedCourse}
              onChange={(e) => setSelectedCourse(e.target.value)}
              className="filter-select"
            >
              <option value="">전체 수업</option>
              {courses.map(course => (
                <option key={course.id} value={course.id}>{course.courseName}</option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {showCreateSchedule && (
        <div className="content-section">
          <div className="schedule-create-form">
            <h3>새 스케줄 생성</h3>
            <form onSubmit={handleCreateSchedule}>
              <div className="form-row">
                <div className="form-group">
                  <label>수업 *</label>
                  <select
                    value={newSchedule.courseId}
                    onChange={(e) => setNewSchedule({...newSchedule, courseId: e.target.value})}
                    required
                  >
                    <option value="">수업을 선택하세요</option>
                    {courses.map(course => (
                      <option key={course.id} value={course.id}>{course.courseName}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label>날짜 *</label>
                  <input
                    type="date"
                    value={newSchedule.scheduleDate}
                    onChange={(e) => setNewSchedule({...newSchedule, scheduleDate: e.target.value})}
                    required
                  />
                </div>
                <div className="form-group">
                  <label>시작 시간 *</label>
                  <input
                    type="time"
                    value={newSchedule.startTime}
                    onChange={(e) => setNewSchedule({...newSchedule, startTime: e.target.value})}
                    required
                  />
                </div>
                <div className="form-group">
                  <label>종료 시간 *</label>
                  <input
                    type="time"
                    value={newSchedule.endTime}
                    onChange={(e) => setNewSchedule({...newSchedule, endTime: e.target.value})}
                    required
                  />
                </div>
              </div>
              <div className="form-group">
                <label>메모</label>
                <textarea
                  value={newSchedule.memo}
                  onChange={(e) => setNewSchedule({...newSchedule, memo: e.target.value})}
                  placeholder="수업 관련 메모를 입력하세요"
                />
              </div>
              <div className="form-actions">
                <button type="submit" className="btn-primary">생성</button>
                <button type="button" className="btn-secondary" onClick={() => setShowCreateSchedule(false)}>취소</button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="content-section">
        <div className="schedule-list">
          <h3>{selectedDate} 스케줄 목록</h3>
          {combinedSchedules.length === 0 ? (
            <div className="empty-state">
              <i className="fas fa-calendar-times"></i>
              <p>해당 날짜에 등록된 스케줄이 없습니다</p>
            </div>
          ) : (
            <div className="schedule-items">
              {combinedSchedules.map((schedule) => (
                <div key={schedule.id} className={`schedule-item ${schedule.isCancelled ? 'cancelled' : ''}`}>
                  <div className="schedule-info">
                    <div className="schedule-course">
                      <i className="fas fa-book"></i>
                      {schedule.courseName}
                    </div>
                    <div className="schedule-time">
                      <i className="fas fa-clock"></i>
                      {schedule.startTime} - {schedule.endTime}
                    </div>
                    <div className="schedule-students">
                      <i className="fas fa-users"></i>
                      {schedule.currentStudents}명 등록
                    </div>
                    {schedule.memo && (
                      <div className="schedule-memo">
                        <i className="fas fa-sticky-note"></i>
                        {schedule.memo}
                      </div>
                    )}
                  </div>
                  <div className="schedule-actions">
                    {!schedule.isCancelled && (
                      <button className="btn-table-delete">
                        <i className="fas fa-times"></i> 취소
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
