import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import LoadingSpinner from '../components/LoadingSpinner';
import { courseAPI, scheduleAPI, reservationAPI } from '../services/api';
import '../styles/Courses.css';

function Courses() {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedCourse, setSelectedCourse] = useState(null);
  const [newCourse, setNewCourse] = useState({
    courseName: '',
    description: '',
    level: '',
    capacity: 10,
    durationMinutes: 60,
  });

  // 코스 목록 조회
  const { data: courses = [], isLoading } = useQuery({
    queryKey: ['courses'],
    queryFn: async () => {
      const response = await courseAPI.getAll();
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
        description: '',
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
    mutationFn: (id) => courseAPI.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries(['courses']);
      alert('코스가 삭제되었습니다.');
    },
    onError: (error) => {
      alert(`삭제 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    },
  });

  const handleCreateCourse = () => {
    if (!newCourse.courseName || !newCourse.description) {
      alert('코스명과 설명을 입력해주세요.');
      return;
    }

    if (newCourse.capacity <= 0 || newCourse.durationMinutes <= 0) {
      alert('정원과 수업시간을 올바르게 입력해주세요.');
      return;
    }

    createMutation.mutate(newCourse);
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
    if (window.confirm(`"${courseName}" 수업을 삭제하시겠습니까?`)) {
      deleteMutation.mutate(id);
    }
  };

  const openEditModal = (course) => {
    setSelectedCourse({ ...course });
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
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>코스 생성</h2>
              <button className="modal-close" onClick={() => setShowCreateModal(false)}>
                ×
              </button>
            </div>

            <div className="modal-body">
              <div className="form-group">
                <label>코스명 *</label>
                <input
                  type="text"
                  value={newCourse.courseName}
                  onChange={(e) => setNewCourse({ ...newCourse, courseName: e.target.value })}
                  placeholder="예: 기초 영어 회화"
                />
              </div>

              <div className="form-group">
                <label>설명 *</label>
                <textarea
                  value={newCourse.description}
                  onChange={(e) => setNewCourse({ ...newCourse, description: e.target.value })}
                  placeholder="코스에 대한 설명을 입력하세요"
                  rows="4"
                />
              </div>

              <div className="form-group">
                <label>반 이름 *</label>
                <input
                  type="text"
                  value={newCourse.level}
                  onChange={(e) => setNewCourse({ ...newCourse, level: e.target.value })}
                  placeholder="예: A반, 초급반, 월수금반 등"
                  required
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>정원 *</label>
                  <input
                    type="number"
                    value={newCourse.capacity}
                    onChange={(e) =>
                      setNewCourse({ ...newCourse, capacity: parseInt(e.target.value) || 0 })
                    }
                    placeholder="10"
                    min="1"
                  />
                </div>

                <div className="form-group">
                  <label>수업시간 (분) *</label>
                  <input
                    type="number"
                    value={newCourse.durationMinutes}
                    onChange={(e) =>
                      setNewCourse({
                        ...newCourse,
                        durationMinutes: parseInt(e.target.value) || 0,
                      })
                    }
                    placeholder="60"
                    min="1"
                  />
                </div>
              </div>
            </div>

            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setShowCreateModal(false)}>
                취소
              </button>
              <button className="btn-primary" onClick={handleCreateCourse}>
                생성
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 코스 상세 모달 (탭 구조) */}
      {showEditModal && selectedCourse && (
        <div className="modal-overlay" onClick={() => setShowEditModal(false)}>
          <div className="modal-content course-detail-modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{selectedCourse.courseName}</h2>
              <button className="modal-close" onClick={() => setShowEditModal(false)}>
                ×
              </button>
            </div>

            {/* 탭 네비게이션 */}
            <div className="tab-navigation">
              <button 
                className={`tab-button ${activeTab === 'info' ? 'active' : ''}`}
                onClick={() => setActiveTab('info')}
              >
                <i className="fas fa-info-circle"></i> 수업 정보
              </button>
              <button 
                className={`tab-button ${activeTab === 'schedule' ? 'active' : ''}`}
                onClick={() => setActiveTab('schedule')}
              >
                <i className="fas fa-calendar-alt"></i> 스케줄 관리
              </button>
            </div>

            <div className="modal-body">
              {activeTab === 'info' ? (
                <CourseInfoTab 
                  course={selectedCourse}
                  onUpdate={(updatedCourse) => setSelectedCourse(updatedCourse)}
                  onClose={() => setShowEditModal(false)}
                  updateMutation={updateMutation}
                />
              ) : (
                <ScheduleTab 
                  course={selectedCourse}
                />
              )}
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
    updateMutation.mutate(course);
  };

  return (
    <>
      <div className="form-group">
        <label>코스명 *</label>
        <input
          type="text"
          value={course.courseName}
          onChange={(e) => onUpdate({ ...course, courseName: e.target.value })}
          placeholder="예: 기초 영어 회화"
        />
      </div>

      <div className="form-group">
        <label>설명 *</label>
        <textarea
          value={course.description || ''}
          onChange={(e) => onUpdate({ ...course, description: e.target.value })}
          placeholder="코스에 대한 설명을 입력하세요"
          rows="4"
        />
      </div>

      <div className="form-group">
        <label>반 이름 *</label>
        <input
          type="text"
          value={course.level}
          onChange={(e) => onUpdate({ ...course, level: e.target.value })}
          placeholder="예: A반, 초급반, 월수금반 등"
          required
        />
      </div>

      <div className="form-row">
        <div className="form-group">
          <label>정원 *</label>
          <input
            type="number"
            value={course.capacity || course.maxStudents}
            onChange={(e) => onUpdate({
              ...course,
              capacity: parseInt(e.target.value) || 0,
              maxStudents: parseInt(e.target.value) || 0
            })}
            placeholder="10"
            min="1"
          />
        </div>

        <div className="form-group">
          <label>수업시간 (분) *</label>
          <input
            type="number"
            value={course.durationMinutes}
            onChange={(e) => onUpdate({
              ...course,
              durationMinutes: parseInt(e.target.value) || 0
            })}
            placeholder="60"
            min="1"
          />
        </div>
      </div>

      <div className="modal-footer">
        <button className="btn-secondary" onClick={onClose}>
          취소
        </button>
        <button className="btn-primary" onClick={handleUpdateCourse}>
          저장
        </button>
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
      <div className="schedule-header">
        <button 
          className="btn-primary"
          onClick={() => setShowCreateSchedule(true)}
        >
          <i className="fas fa-plus"></i> 스케줄 추가
        </button>
      </div>

      {showCreateSchedule && (
        <div className="schedule-create-form">
          <h3>새 스케줄 생성</h3>
          <form onSubmit={handleCreateSchedule}>
            <div className="form-row">
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
      )}

      <div className="schedule-list">
        <h3>스케줄 목록</h3>
        {scheduleData.length === 0 ? (
          <div className="empty-state">
            <i className="fas fa-calendar-times"></i>
            <p>등록된 스케줄이 없습니다</p>
          </div>
        ) : (
          <div className="schedule-items">
            {scheduleData.map((schedule) => (
              <div key={schedule.id} className={`schedule-item ${schedule.isCancelled ? 'cancelled' : ''}`}>
                <div className="schedule-info">
                  <div className="schedule-date">
                    <i className="fas fa-calendar"></i>
                    {schedule.scheduleDate}
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
                  {!schedule.isCancelled ? (
                    <button 
                      className="btn-table-delete"
                      onClick={() => handleCancelSchedule(schedule.id)}
                    >
                      <i className="fas fa-times"></i> 취소
                    </button>
                  ) : (
                    <span className="cancelled-badge">취소됨</span>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </>
  );
}

export default Courses;

// 수업 관리 탭 컴포넌트
function CoursesTab({ courses, searchQuery, setSearchQuery, setShowCreateModal, openEditModal, handleDeleteCourse, isLoading, filteredCourses }) {
  const getClassBadge = (className) => {
    return <span className="level-badge default">{className || '미지정'}</span>;
  };

  return (
    <div className="tab-content-wrapper">
      <div className="tab-header">
        <button className="btn-primary" onClick={() => setShowCreateModal(true)}>
          <i className="fas fa-plus"></i> 반 생성
        </button>
      </div>

      <div className="search-section">
        <div className="search-input-wrapper">
          <i className="fas fa-search search-icon"></i>
          <input
            type="text"
            placeholder="수업명, 설명, 레벨로 검색..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="search-input"
          />
        </div>
        <div className="result-count">
          <i className="fas fa-chalkboard-teacher"></i>
          총 <strong>{filteredCourses.length}</strong>개
        </div>
      </div>

      <div className="content-section">
        {filteredCourses.length === 0 ? (
          <div className="empty-state">
            {searchQuery ? '검색 결과가 없습니다.' : '등록된 수업이 없습니다.'}
          </div>
        ) : (
          <div className="courses-grid">
            {filteredCourses.map((course) => (
              <div key={course.id} className="course-card">
                <div className="course-header">
                  <h3>{course.courseName}</h3>
                  {getClassBadge(course.level)}
                </div>

                <p className="course-description">{course.description}</p>

                <div className="course-details">
                  <div className="detail-item">
                    <span className="icon"><i className="fas fa-users"></i></span>
                    <span className="label">현재 수강생:</span>
                    <span className="value">{course.currentEnrollments || 0}명</span>
                  </div>
                  <div className="detail-item">
                    <span className="icon"><i className="fas fa-clock"></i></span>
                    <span className="label">수업시간:</span>
                    <span className="value">{course.durationMinutes}분</span>
                  </div>
                </div>

                <div className="course-actions">
                  <button className="btn-table-edit" onClick={() => openEditModal(course)}>
                    <i className="fas fa-edit"></i> 수정
                  </button>
                  <button
                    className="btn-table-delete"
                    onClick={() => handleDeleteCourse(course.id, course.courseName)}
                  >
                    <i className="fas fa-trash"></i> 삭제
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

// 스케줄 관리 탭 컴포넌트
function SchedulesTab() {
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
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
