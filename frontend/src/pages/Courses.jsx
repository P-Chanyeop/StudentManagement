import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { courseAPI } from '../services/api';
import '../styles/Courses.css';

function Courses() {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedCourse, setSelectedCourse] = useState(null);
  const [newCourse, setNewCourse] = useState({
    name: '',
    description: '',
    level: 'BEGINNER',
    capacity: 10,
    durationMinutes: 60,
    price: 0,
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
        name: '',
        description: '',
        level: 'BEGINNER',
        capacity: 10,
        durationMinutes: 60,
        price: 0,
      });
      alert('코스가 생성되었습니다.');
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
    if (!newCourse.name || !newCourse.description) {
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
    if (!selectedCourse.name || !selectedCourse.description) {
      alert('코스명과 설명을 입력해주세요.');
      return;
    }

    updateMutation.mutate({
      id: selectedCourse.id,
      data: {
        name: selectedCourse.name,
        description: selectedCourse.description,
        level: selectedCourse.level,
        capacity: selectedCourse.capacity,
        durationMinutes: selectedCourse.durationMinutes,
        price: selectedCourse.price,
      },
    });
  };

  const handleDeleteCourse = (id, name) => {
    if (window.confirm(`"${name}" 코스를 삭제하시겠습니까?`)) {
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
      course.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      course.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
      course.level.toLowerCase().includes(searchQuery.toLowerCase())
    );
  });

  // 레벨별 배지
  const getLevelBadge = (level) => {
    const levelMap = {
      BEGINNER: { text: '초급', color: '#03C75A' },
      INTERMEDIATE: { text: '중급', color: '#0066FF' },
      ADVANCED: { text: '고급', color: '#FF9800' },
      EXPERT: { text: '전문가', color: '#9C27B0' },
    };
    const { text, color } = levelMap[level] || { text: level, color: '#999' };
    return <span className="level-badge" style={{ backgroundColor: color }}>{text}</span>;
  };

  if (isLoading) {
    return <div className="courses-container">로딩 중...</div>;
  }

  return (
    <div className="courses-container">
      <div className="courses-header">
        <h1>코스 관리</h1>
        <button className="btn-create-course" onClick={() => setShowCreateModal(true)}>
          + 코스 생성
        </button>
      </div>

      <div className="courses-filters">
        <input
          type="text"
          placeholder="코스명, 설명, 레벨로 검색..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="search-input"
        />
        <span className="result-count">총 {filteredCourses.length}개 코스</span>
      </div>

      <div className="courses-grid">
        {filteredCourses.length === 0 ? (
          <div className="empty-state">
            {searchQuery ? '검색 결과가 없습니다.' : '등록된 코스가 없습니다.'}
          </div>
        ) : (
          filteredCourses.map((course) => (
            <div key={course.id} className="course-card">
              <div className="course-header">
                <h3>{course.name}</h3>
                {getLevelBadge(course.level)}
              </div>

              <p className="course-description">{course.description}</p>

              <div className="course-details">
                <div className="detail-item">
                  <span className="icon">👥</span>
                  <span className="label">정원:</span>
                  <span className="value">{course.capacity}명</span>
                </div>
                <div className="detail-item">
                  <span className="icon">⏱️</span>
                  <span className="label">수업시간:</span>
                  <span className="value">{course.durationMinutes}분</span>
                </div>
                <div className="detail-item">
                  <span className="icon">💰</span>
                  <span className="label">가격:</span>
                  <span className="value price">{course.price.toLocaleString()}원</span>
                </div>
              </div>

              <div className="course-actions">
                <button className="btn-edit" onClick={() => openEditModal(course)}>
                  수정
                </button>
                <button
                  className="btn-delete"
                  onClick={() => handleDeleteCourse(course.id, course.name)}
                >
                  삭제
                </button>
              </div>
            </div>
          ))
        )}
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
                  value={newCourse.name}
                  onChange={(e) => setNewCourse({ ...newCourse, name: e.target.value })}
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
                <label>레벨 *</label>
                <select
                  value={newCourse.level}
                  onChange={(e) => setNewCourse({ ...newCourse, level: e.target.value })}
                >
                  <option value="BEGINNER">초급</option>
                  <option value="INTERMEDIATE">중급</option>
                  <option value="ADVANCED">고급</option>
                  <option value="EXPERT">전문가</option>
                </select>
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

              <div className="form-group">
                <label>가격 *</label>
                <input
                  type="number"
                  value={newCourse.price}
                  onChange={(e) =>
                    setNewCourse({ ...newCourse, price: parseInt(e.target.value) || 0 })
                  }
                  placeholder="300000"
                  min="0"
                />
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

      {/* 코스 수정 모달 */}
      {showEditModal && selectedCourse && (
        <div className="modal-overlay" onClick={() => setShowEditModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>코스 수정</h2>
              <button className="modal-close" onClick={() => setShowEditModal(false)}>
                ×
              </button>
            </div>

            <div className="modal-body">
              <div className="form-group">
                <label>코스명 *</label>
                <input
                  type="text"
                  value={selectedCourse.name}
                  onChange={(e) =>
                    setSelectedCourse({ ...selectedCourse, name: e.target.value })
                  }
                  placeholder="예: 기초 영어 회화"
                />
              </div>

              <div className="form-group">
                <label>설명 *</label>
                <textarea
                  value={selectedCourse.description}
                  onChange={(e) =>
                    setSelectedCourse({ ...selectedCourse, description: e.target.value })
                  }
                  placeholder="코스에 대한 설명을 입력하세요"
                  rows="4"
                />
              </div>

              <div className="form-group">
                <label>레벨 *</label>
                <select
                  value={selectedCourse.level}
                  onChange={(e) =>
                    setSelectedCourse({ ...selectedCourse, level: e.target.value })
                  }
                >
                  <option value="BEGINNER">초급</option>
                  <option value="INTERMEDIATE">중급</option>
                  <option value="ADVANCED">고급</option>
                  <option value="EXPERT">전문가</option>
                </select>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>정원 *</label>
                  <input
                    type="number"
                    value={selectedCourse.capacity}
                    onChange={(e) =>
                      setSelectedCourse({
                        ...selectedCourse,
                        capacity: parseInt(e.target.value) || 0,
                      })
                    }
                    placeholder="10"
                    min="1"
                  />
                </div>

                <div className="form-group">
                  <label>수업시간 (분) *</label>
                  <input
                    type="number"
                    value={selectedCourse.durationMinutes}
                    onChange={(e) =>
                      setSelectedCourse({
                        ...selectedCourse,
                        durationMinutes: parseInt(e.target.value) || 0,
                      })
                    }
                    placeholder="60"
                    min="1"
                  />
                </div>
              </div>

              <div className="form-group">
                <label>가격 *</label>
                <input
                  type="number"
                  value={selectedCourse.price}
                  onChange={(e) =>
                    setSelectedCourse({ ...selectedCourse, price: parseInt(e.target.value) || 0 })
                  }
                  placeholder="300000"
                  min="0"
                />
              </div>
            </div>

            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setShowEditModal(false)}>
                취소
              </button>
              <button className="btn-primary" onClick={handleUpdateCourse}>
                저장
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Courses;
