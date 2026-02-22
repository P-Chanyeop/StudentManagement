import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { studentAPI, authAPI, courseAPI, enrollmentAPI } from '../services/api';
import { getTodayString, getDateAfterDays } from '../utils/dateUtils';
import LoadingSpinner from '../components/LoadingSpinner';
import '../styles/Students.css';

function Students() {
  const queryClient = useQueryClient();
  const [searchKeyword, setSearchKeyword] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [selectedStudent, setSelectedStudent] = useState(null);
  const [newStudent, setNewStudent] = useState({
    studentName: '',
    studentPhone: '',
    birthDate: '',
    gender: 'MALE',
    address: '',
    school: '',
    grade: '1',
    parentId: null,
    parentName: '',
    parentPhone: '',
    parentEmail: '',
    englishLevel: '1.0',
    memo: '',
    selectedCourseId: null,
  });

  // 생년월일 선택을 위한 분리된 상태
  const [birthDateComponents, setBirthDateComponents] = useState({
    year: '',
    month: '',
    day: ''
  });
  const [editBirthDateComponents, setEditBirthDateComponents] = useState({
    year: '',
    month: '',
    day: ''
  });

  // 사용자 프로필 조회
  const { data: profile } = useQuery({
    queryKey: ['userProfile'],
    queryFn: async () => {
      const response = await authAPI.getProfile();
      return response.data;
    },
  });

  const isParent = profile?.role === 'PARENT';

  // 학생 목록 조회
  const { data: students = [], isLoading } = useQuery({
    queryKey: ['students', profile?.role],
    queryFn: async () => {
      if (profile?.role === 'ADMIN' || profile?.role === 'TEACHER') {
        const response = await studentAPI.getActive();
        return response.data;
      } else if (profile?.role === 'PARENT') {
        const response = await studentAPI.getMyStudents();
        return response.data;
      }
      return [];
    },
    enabled: !!profile,
  });

  // 반 목록 조회
  const { data: courses = [] } = useQuery({
    queryKey: ['courses'],
    queryFn: async () => {
      const response = await courseAPI.getActive();
      return response.data;
    },
  });

  // 학부모 목록 조회
  const { data: parentList = [] } = useQuery({
    queryKey: ['parents'],
    queryFn: async () => {
      const response = await authAPI.getParents();
      return response.data;
    },
    enabled: !isParent,
  });
  const [parentSearch, setParentSearch] = useState('');

  // 생년월일 컴포넌트 변경 핸들러 (신규 학생)
  const handleBirthDateComponentChange = (component, value) => {
    const newComponents = {
      ...birthDateComponents,
      [component]: value
    };
    setBirthDateComponents(newComponents);
    
    if (newComponents.year && newComponents.month && newComponents.day) {
      const formattedDate = `${newComponents.year}-${newComponents.month.padStart(2, '0')}-${newComponents.day.padStart(2, '0')}`;
      setNewStudent(prev => ({
        ...prev,
        birthDate: formattedDate
      }));
    }
  };

  // 생년월일 컴포넌트 변경 핸들러 (수정)
  const handleEditBirthDateComponentChange = (component, value) => {
    const newComponents = {
      ...editBirthDateComponents,
      [component]: value
    };
    setEditBirthDateComponents(newComponents);
    
    if (newComponents.year && newComponents.month && newComponents.day) {
      const formattedDate = `${newComponents.year}-${newComponents.month.padStart(2, '0')}-${newComponents.day.padStart(2, '0')}`;
      setSelectedStudent(prev => ({
        ...prev,
        birthDate: formattedDate
      }));
    }
  };

  // 학생 생성 mutation
  const createMutation = useMutation({
    mutationFn: (data) => studentAPI.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries(['students', profile?.role]);
      setShowCreateModal(false);
      setNewStudent({
        studentName: '',
        studentPhone: '',
        birthDate: '',
        gender: 'MALE',
        address: '',
        school: '',
        grade: '1',
        parentId: null,
        parentName: '',
        parentPhone: '',
        parentEmail: '',
        englishLevel: '1.0',
        memo: '',
        selectedCourseId: null,
      });
      setParentSearch('');
      alert('학생이 등록되었습니다.');
    },
    onError: (error) => {
      alert(`등록 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    },
  });

  // 학생 수정 mutation
  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => studentAPI.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries(['students', profile?.role]);
      setShowEditModal(false);
      setSelectedStudent(null);
      alert('학생 정보가 수정되었습니다.');
    },
    onError: (error) => {
      alert(`수정 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    },
  });

  // 학생 비활성화 mutation
  const deactivateMutation = useMutation({
    mutationFn: (id) => studentAPI.deactivate(id),
    onSuccess: () => {
      queryClient.invalidateQueries(['students', profile?.role]);
      alert('학생이 비활성화되었습니다.');
    },
    onError: (error) => {
      alert(`비활성화 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    },
  });

  const handleCreateStudent = async () => {
    if (!newStudent.studentName || !newStudent.studentPhone || !newStudent.parentName || !newStudent.parentPhone) {
      alert('필수 항목을 모두 입력해주세요. (학생명, 학생 연락처, 학부모 선택)');
      return;
    }

    try {
      // 학생 생성
      const response = await studentAPI.create(newStudent);
      const createdStudent = response.data;

      // 반이 선택되었으면 반 등록
      if (newStudent.selectedCourseId) {
        await enrollmentAPI.create({
          studentId: createdStudent.id,
          courseId: newStudent.selectedCourseId,
          startDate: getTodayString(),
          endDate: getDateAfterDays(90),
          totalCount: 24,
          usedCount: 0,
          remainingCount: 24,
          isActive: true
        });
      }

      queryClient.invalidateQueries(['students', profile?.role]);
      setShowCreateModal(false);
      setNewStudent({
        studentName: '',
        studentPhone: '',
        birthDate: '',
        gender: 'MALE',
        address: '',
        school: '',
        grade: '1',
        parentName: '',
        parentPhone: '',
        parentEmail: '',
        englishLevel: '1.0',
        memo: '',
        selectedCourseId: null,
      });
      setBirthDateComponents({ year: '', month: '', day: '' });
      alert('학생이 등록되었습니다.');
    } catch (error) {
      alert(`등록 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    }
  };

  const handleUpdateStudent = async () => {
    if (!selectedStudent.studentName || !selectedStudent.studentPhone || !selectedStudent.parentName || !selectedStudent.parentPhone) {
      alert('필수 항목을 모두 입력해주세요.');
      return;
    }

    try {
      // 학생 정보 업데이트
      await updateMutation.mutateAsync({
        id: selectedStudent.id,
        data: {
          studentName: selectedStudent.studentName,
          studentPhone: selectedStudent.studentPhone,
          birthDate: selectedStudent.birthDate,
          gender: selectedStudent.gender,
          address: selectedStudent.address,
          school: selectedStudent.school,
          grade: selectedStudent.grade,
          parentName: selectedStudent.parentName,
          parentPhone: selectedStudent.parentPhone,
          parentEmail: selectedStudent.parentEmail,
          englishLevel: selectedStudent.englishLevel,
          memo: selectedStudent.memo,
        },
      });

      // 반 정보가 변경되었으면 수강권도 업데이트
      if (selectedStudent.selectedCourseId) {
        const enrollmentsResponse = await enrollmentAPI.getByStudent(selectedStudent.id);
        const activeEnrollments = enrollmentsResponse.data.filter(e => e.isActive);
        
        // 활성 수강권이 있으면 반 정보 업데이트
        if (activeEnrollments.length > 0) {
          for (const enrollment of activeEnrollments) {
            await enrollmentAPI.update(enrollment.id, {
              studentId: selectedStudent.id,
              courseId: selectedStudent.selectedCourseId,
              startDate: enrollment.startDate,
              endDate: enrollment.endDate,
              totalCount: enrollment.totalCount,
              remainingCount: enrollment.remainingCount
            });
          }
        }
      }
    } catch (error) {
      console.error('학생 수정 실패:', error);
    }
  };

  const openEditModal = (student) => {
    setSelectedStudent({ ...student });
    
    // 생년월일 컴포넌트 초기화
    if (student.birthDate) {
      const [year, month, day] = student.birthDate.split('-');
      setEditBirthDateComponents({
        year: year || '',
        month: month ? parseInt(month).toString() : '',
        day: day ? parseInt(day).toString() : ''
      });
    } else {
      setEditBirthDateComponents({ year: '', month: '', day: '' });
    }
    
    setShowEditModal(true);
  };

  const openDetailModal = (student) => {
    setSelectedStudent(student);
    setShowDetailModal(true);
  };

  const handleDeactivate = (id, name) => {
    if (window.confirm(`${name} 학생을 비활성화하시겠습니까?`)) {
      deactivateMutation.mutate(id);
    }
  };

  // 레벨별 배지
  const getLevelBadge = (level) => {
    // 숫자 레벨을 그대로 표시 (색상 없음)
    return <span className="level-text">{level || '0.0'}</span>;
  };

  // 검색 필터
  const filteredStudents = students.filter((student) =>
    student.studentName.toLowerCase().includes(searchKeyword.toLowerCase()) ||
    student.parentName?.toLowerCase().includes(searchKeyword.toLowerCase())
  );

  if (isLoading) {
    return (
      <div className="students-container">
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="students-page">
      {/* 페이지 헤더 */}
      <div className="students-header">
        <div className="students-header-content">
          <div className="students-title-section">
            <h1 className="students-title">
              <i className="fas fa-user-graduate"></i>
              {isParent ? '자녀 관리' : '학생 관리'}
            </h1>
            <p className="students-subtitle">{isParent ? '자녀 정보를 확인하고 관리합니다' : '학생 정보를 등록하고 관리합니다'}</p>
          </div>
          {(profile?.role === 'ADMIN' || profile?.role === 'TEACHER') && (
            <button className="students-add-btn" onClick={() => setShowCreateModal(true)}>
              <i className="fas fa-plus"></i>
              학생 등록
            </button>
          )}
        </div>
      </div>

      {/* 메인 컨텐츠 */}
      <div className="students-content">
        {/* 검색 및 필터 */}
        <div className="students-search-section">
          <div className="students-search-wrapper">
            <i className="fas fa-search students-search-icon"></i>
            <input
              type="text"
              placeholder="학생 이름 또는 학부모 이름으로 검색..."
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              className="students-search-input"
            />
          </div>
          <div className="students-result-count">
            <i className="fas fa-users"></i>
            총 <strong>{filteredStudents.length}</strong>명
          </div>
        </div>

        {/* 테이블 */}
        <div className="table-wrapper">
          {filteredStudents.length === 0 ? (
            <div className="empty-state">
              <i className="fas fa-user-slash"></i>
              <p>{searchKeyword ? '검색 결과가 없습니다.' : '등록된 학생이 없습니다.'}</p>
            </div>
          ) : (
            <table className="data-table">
              <thead>
                <tr>
                  <th>번호</th>
                  <th>학생명</th>
                  <th>학년</th>
                  <th>영어 레벨</th>
                  <th>학부모명</th>
                  <th>연락처</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                {filteredStudents.map((student, index) => (
                  <tr key={student.id}>
                    <td>{index + 1}</td>
                    <td>
                      <div className="student-info">
                        <strong>{student.studentName}</strong>
                      </div>
                    </td>
                    <td>{student.grade ? `${student.grade}학년` : '-'}</td>
                    <td>{getLevelBadge(student.englishLevel)}</td>
                    <td>{student.parentName || '-'}</td>
                    <td>{student.parentPhone || '-'}</td>
                    <td>
                      <div className="action-buttons">
                        <button className="btn-table-detail" onClick={() => openDetailModal(student)}>
                          <i className="fas fa-eye"></i>
                          상세
                        </button>
                        {(profile?.role === 'ADMIN' || profile?.role === 'TEACHER' || profile?.role === 'PARENT') && (
                          <button className="btn-table-edit" onClick={() => openEditModal(student)}>
                            <i className="fas fa-edit"></i>
                            수정
                          </button>
                        )}
                        {(profile?.role === 'ADMIN' || profile?.role === 'TEACHER') && (
                          <button
                            className="btn-table-delete"
                            onClick={() => handleDeactivate(student.id, student.studentName)}
                          >
                            <i className="fas fa-trash"></i>
                            삭제
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* 학생 등록 모달 */}
      {showCreateModal && (
        <div className="modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>학생 등록</h2>
              <button className="modal-close" onClick={() => setShowCreateModal(false)}>
                ×
              </button>
            </div>

            <div className="modal-body">
              <div className="form-section">
                <h3>학생 정보</h3>
                <div className="form-row">
                  <div className="form-group">
                    <label>학생명 *</label>
                    <input
                      type="text"
                      value={newStudent.studentName}
                      onChange={(e) => setNewStudent({ ...newStudent, studentName: e.target.value })}
                      placeholder="홍길동"
                    />
                  </div>
                  <div className="form-group">
                    <label>학생 연락처 *</label>
                    <input
                      type="tel"
                      value={newStudent.studentPhone}
                      onChange={(e) => setNewStudent({ ...newStudent, studentPhone: e.target.value })}
                      placeholder="010-1234-5678"
                    />
                  </div>
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>생년월일</label>
                    <div className="birth-date-inputs">
                      <select
                        value={birthDateComponents.year}
                        onChange={(e) => handleBirthDateComponentChange('year', e.target.value)}
                      >
                        <option value="">년도</option>
                        {Array.from({length: 20}, (_, i) => {
                          const year = new Date().getFullYear() - 5 - i;
                          return <option key={year} value={year}>{year}년</option>;
                        })}
                      </select>
                      
                      <select
                        value={birthDateComponents.month}
                        onChange={(e) => handleBirthDateComponentChange('month', e.target.value)}
                      >
                        <option value="">월</option>
                        {Array.from({length: 12}, (_, i) => {
                          const month = i + 1;
                          return <option key={month} value={month}>{month}월</option>;
                        })}
                      </select>
                      
                      <select
                        value={birthDateComponents.day}
                        onChange={(e) => handleBirthDateComponentChange('day', e.target.value)}
                      >
                        <option value="">일</option>
                        {Array.from({length: 31}, (_, i) => {
                          const day = i + 1;
                          return <option key={day} value={day}>{day}일</option>;
                        })}
                      </select>
                    </div>
                  </div>
                  <div className="form-group">
                    <label>성별</label>
                    <select
                      value={newStudent.gender}
                      onChange={(e) => setNewStudent({ ...newStudent, gender: e.target.value })}
                    >
                      <option value="MALE">남</option>
                      <option value="FEMALE">여</option>
                    </select>
                  </div>
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>학교</label>
                    <input
                      type="text"
                      value={newStudent.school}
                      onChange={(e) => setNewStudent({ ...newStudent, school: e.target.value })}
                      placeholder="학교명"
                    />
                  </div>
                  <div className="form-group">
                    <label>학년</label>
                    <select
                      value={newStudent.grade}
                      onChange={(e) => setNewStudent({ ...newStudent, grade: e.target.value })}
                    >
                      {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map((grade) => (
                        <option key={grade} value={grade.toString()}>
                          {grade}학년
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="form-group">
                  <label>주소</label>
                  <input
                    type="text"
                    value={newStudent.address}
                    onChange={(e) => setNewStudent({ ...newStudent, address: e.target.value })}
                    placeholder="주소"
                  />
                </div>

                <div className="form-group">
                  <label>영어 레벨</label>
                  <input
                    type="number"
                    step="0.1"
                    min="0"
                    max="10"
                    value={newStudent.englishLevel}
                    onChange={(e) => setNewStudent({ ...newStudent, englishLevel: e.target.value })}
                    placeholder="1.0"
                    disabled={profile?.role === 'PARENT'}
                    readOnly={profile?.role === 'PARENT'}
                  />
                </div>

                <div className="form-group">
                  <label>반 선택</label>
                  <div className="class-list">
                    {profile?.role === 'PARENT' ? (
                      <p className="info-text">반 배정은 관리자가 진행합니다.</p>
                    ) : (
                    courses.map((course) => (
                      <div key={course.id} className="class-item">
                        <input
                          type="checkbox"
                          id={`new-course-${course.id}`}
                          checked={newStudent.selectedCourseId === course.id}
                          onChange={(e) => {
                            if (e.target.checked) {
                              setNewStudent({ ...newStudent, selectedCourseId: course.id });
                            } else {
                              setNewStudent({ ...newStudent, selectedCourseId: null });
                            }
                          }}
                        />
                        <label htmlFor={`new-course-${course.id}`}>
                          {course.courseName}
                        </label>
                      </div>
                    ))
                    )}
                  </div>
                </div>
              </div>

              <div className="form-section">
                <h3>학부모 선택</h3>
                <div className="form-group">
                  <label>학부모 검색 *</label>
                  <input
                    type="text"
                    value={parentSearch}
                    onChange={(e) => setParentSearch(e.target.value)}
                    placeholder="학부모 이름 또는 전화번호로 검색..."
                  />
                  {parentSearch && (
                    <div className="std-parent-list">
                      {parentList.filter(p =>
                        p.name?.includes(parentSearch) || p.phoneNumber?.includes(parentSearch)
                      ).map(p => (
                        <div key={p.id} className="std-parent-item" onClick={() => {
                          setNewStudent({ ...newStudent, parentId: p.id, parentName: p.name, parentPhone: p.phoneNumber });
                          setParentSearch('');
                        }}>
                          <span className="std-parent-name">{p.name}</span>
                          <span className="std-parent-phone">{p.phoneNumber}</span>
                        </div>
                      ))}
                      {parentList.filter(p =>
                        p.name?.includes(parentSearch) || p.phoneNumber?.includes(parentSearch)
                      ).length === 0 && (
                        <div className="std-parent-empty">검색 결과가 없습니다</div>
                      )}
                    </div>
                  )}
                  {newStudent.parentId && (
                    <div className="std-parent-selected">
                      <span>✅ {newStudent.parentName} ({newStudent.parentPhone})</span>
                      <button type="button" onClick={() => setNewStudent({ ...newStudent, parentId: null, parentName: '', parentPhone: '' })}>✕</button>
                    </div>
                  )}
                </div>
              </div>

              <div className="form-group">
                <label>메모</label>
                <textarea
                  value={newStudent.memo}
                  onChange={(e) => setNewStudent({ ...newStudent, memo: e.target.value })}
                  placeholder="추가 메모 사항"
                  rows="3"
                />
              </div>
            </div>

            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setShowCreateModal(false)}>
                취소
              </button>
              <button className="btn-primary" onClick={handleCreateStudent}>
                등록
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 학생 수정 모달 */}
      {showEditModal && selectedStudent && (
        <div className="modal-overlay" onClick={() => setShowEditModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>학생 정보 수정</h2>
              <button className="modal-close" onClick={() => setShowEditModal(false)}>
                ×
              </button>
            </div>

            <div className="modal-body">
              <div className="form-section">
                <h3>학생 정보</h3>
                <div className="form-row">
                  <div className="form-group">
                    <label>학생명 *</label>
                    <input
                      type="text"
                      value={selectedStudent.studentName}
                      onChange={(e) =>
                        setSelectedStudent({ ...selectedStudent, studentName: e.target.value })
                      }
                    />
                  </div>
                  <div className="form-group">
                    <label>학생 연락처 *</label>
                    <input
                      type="tel"
                      value={selectedStudent.studentPhone}
                      onChange={(e) =>
                        setSelectedStudent({ ...selectedStudent, studentPhone: e.target.value })
                      }
                    />
                  </div>
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>생년월일</label>
                    <div className="birth-date-inputs">
                      <select
                        value={editBirthDateComponents.year}
                        onChange={(e) => handleEditBirthDateComponentChange('year', e.target.value)}
                      >
                        <option value="">년도</option>
                        {Array.from({length: 20}, (_, i) => {
                          const year = new Date().getFullYear() - 5 - i;
                          return <option key={year} value={year}>{year}년</option>;
                        })}
                      </select>
                      
                      <select
                        value={editBirthDateComponents.month}
                        onChange={(e) => handleEditBirthDateComponentChange('month', e.target.value)}
                      >
                        <option value="">월</option>
                        {Array.from({length: 12}, (_, i) => {
                          const month = i + 1;
                          return <option key={month} value={month}>{month}월</option>;
                        })}
                      </select>
                      
                      <select
                        value={editBirthDateComponents.day}
                        onChange={(e) => handleEditBirthDateComponentChange('day', e.target.value)}
                      >
                        <option value="">일</option>
                        {Array.from({length: 31}, (_, i) => {
                          const day = i + 1;
                          return <option key={day} value={day}>{day}일</option>;
                        })}
                      </select>
                    </div>
                  </div>
                  <div className="form-group">
                    <label>성별</label>
                    <select
                      value={selectedStudent.gender}
                      onChange={(e) => setSelectedStudent({ ...selectedStudent, gender: e.target.value })}
                    >
                      <option value="MALE">남</option>
                      <option value="FEMALE">여</option>
                    </select>
                  </div>
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>학교</label>
                    <input
                      type="text"
                      value={selectedStudent.school || ''}
                      onChange={(e) => setSelectedStudent({ ...selectedStudent, school: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label>학년</label>
                    <select
                      value={selectedStudent.grade || '1'}
                      onChange={(e) =>
                        setSelectedStudent({ ...selectedStudent, grade: e.target.value })
                      }
                    >
                      {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map((grade) => (
                        <option key={grade} value={grade.toString()}>
                          {grade}학년
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="form-group">
                  <label>주소</label>
                  <input
                    type="text"
                    value={selectedStudent.address || ''}
                    onChange={(e) => setSelectedStudent({ ...selectedStudent, address: e.target.value })}
                  />
                </div>

                <div className="form-group">
                  <label>영어 레벨</label>
                  <input
                    type="number"
                    step="0.1"
                    min="0"
                    max="10"
                    value={selectedStudent.englishLevel}
                    onChange={(e) =>
                      setSelectedStudent({ ...selectedStudent, englishLevel: e.target.value })
                    }
                    placeholder="1.0"
                    disabled={profile?.role === 'PARENT'}
                    readOnly={profile?.role === 'PARENT'}
                  />
                </div>

                <div className="form-group">
                  <label>수강 반</label>
                  <div className="class-selection">
                    <div className="current-classes">
                      <strong>현재 수강 중인 반:</strong>
                      {selectedStudent.enrollments && selectedStudent.enrollments.length > 0 ? (
                        <div className="enrolled-classes">
                          {selectedStudent.enrollments
                            .filter(enrollment => enrollment.isActive)
                            .map((enrollment, index) => (
                              <span key={index} className="class-badge">
                                {enrollment.courseName || '미지정'}
                              </span>
                            ))
                          }
                        </div>
                      ) : (
                        <span className="no-classes">수강 중인 반이 없습니다</span>
                      )}
                    </div>
                    
                    <div className="available-classes">
                      <strong>등록 가능한 반:</strong>
                      <div className="class-list">
                        {courses.map((course) => (
                          <div key={course.id} className="class-item">
                            <input
                              type="checkbox"
                              id={`course-${course.id}`}
                              checked={selectedStudent.enrollments?.some(
                                enrollment => enrollment.courseId === course.id && enrollment.isActive
                              ) || false}
                              onChange={async (e) => {
                                const isChecked = e.target.checked;
                                try {
                                  if (isChecked) {
                                    // 다른 모든 반 해제
                                    const otherEnrollments = selectedStudent.enrollments?.filter(
                                      e => e.courseId !== course.id && e.isActive
                                    );
                                    for (const enrollment of otherEnrollments || []) {
                                      await enrollmentAPI.cancel(enrollment.id);
                                    }
                                    
                                    // 새 반 등록
                                    await enrollmentAPI.create({
                                      studentId: selectedStudent.id,
                                      courseId: course.id,
                                      startDate: getTodayString(),
                                      endDate: getDateAfterDays(90),
                                      totalCount: 24,
                                      usedCount: 0,
                                      remainingCount: 24,
                                      isActive: true
                                    });
                                  } else {
                                    // 반 해제
                                    const enrollment = selectedStudent.enrollments?.find(
                                      e => e.courseId === course.id && e.isActive
                                    );
                                    if (enrollment) {
                                      await enrollmentAPI.cancel(enrollment.id);
                                    }
                                  }
                                  queryClient.invalidateQueries(['students', profile?.role]);
                                  
                                  // selectedStudent 상태 즉시 업데이트
                                  const updatedResponse = await studentAPI.getById(selectedStudent.id);
                                  setSelectedStudent(updatedResponse.data);
                                } catch (error) {
                                  alert('처리 실패: ' + (error.response?.data?.message || '오류가 발생했습니다.'));
                                }
                              }}
                            />
                            <label htmlFor={`course-${course.id}`}>
                              {course.courseName}
                            </label>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div className="form-section">
                <h3>학부모 정보</h3>
                <div className="form-row">
                  <div className="form-group">
                    <label>학부모명 *</label>
                    <input
                      type="text"
                      value={selectedStudent.parentName}
                      onChange={(e) =>
                        setSelectedStudent({ ...selectedStudent, parentName: e.target.value })
                      }
                    />
                  </div>
                  <div className="form-group">
                    <label>학부모 연락처 *</label>
                    <input
                      type="tel"
                      value={selectedStudent.parentPhone}
                      onChange={(e) => {
                        const value = e.target.value.replace(/[^0-9]/g, '');
                        let formatted = value;
                        if (value.length >= 3) {
                          formatted = value.slice(0, 3) + '-' + value.slice(3);
                        }
                        if (value.length >= 7) {
                          formatted = value.slice(0, 3) + '-' + value.slice(3, 7) + '-' + value.slice(7, 11);
                        }
                        setSelectedStudent({ ...selectedStudent, parentPhone: formatted });
                      }}
                      placeholder="010-1234-5678"
                      maxLength="13"
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label>학부모 이메일</label>
                  <input
                    type="email"
                    value={selectedStudent.parentEmail || ''}
                    onChange={(e) =>
                      setSelectedStudent({ ...selectedStudent, parentEmail: e.target.value })
                    }
                  />
                </div>
              </div>

              <div className="form-group">
                <label>메모</label>
                <textarea
                  value={selectedStudent.memo || ''}
                  onChange={(e) => setSelectedStudent({ ...selectedStudent, memo: e.target.value })}
                  rows="3"
                />
              </div>
            </div>

            <div className="std-edit-footer">
              <button className="std-edit-cancel" onClick={() => setShowEditModal(false)}>
                취소
              </button>
              <button className="std-edit-submit" onClick={handleUpdateStudent}>
                수정
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 학생 상세 모달 */}
      {showDetailModal && selectedStudent && (
        <div className="modal-overlay" onClick={() => setShowDetailModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>학생 상세 정보</h2>
              <button className="modal-close" onClick={() => setShowDetailModal(false)}>
                ×
              </button>
            </div>

            <div className="modal-body">
              <div className="detail-section">
                <h3>학생 정보</h3>
                <div className="detail-grid">
                  <div className="detail-item">
                    <span className="detail-label">학생명</span>
                    <span className="detail-value">{selectedStudent.studentName}</span>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">학생 연락처</span>
                    <span className="detail-value">{selectedStudent.studentPhone}</span>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">생년월일</span>
                    <span className="detail-value">{selectedStudent.birthDate || '-'}</span>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">성별</span>
                    <span className="detail-value">
                      {selectedStudent.gender === 'MALE' ? '남' : '여'}
                    </span>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">학교</span>
                    <span className="detail-value">{selectedStudent.school || '-'}</span>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">학년</span>
                    <span className="detail-value">
                      {selectedStudent.grade ? `${selectedStudent.grade}학년` : '-'}
                    </span>
                  </div>
                  <div className="detail-item full-width">
                    <span className="detail-label">주소</span>
                    <span className="detail-value">{selectedStudent.address || '-'}</span>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">영어 레벨</span>
                    <span className="detail-value">{getLevelBadge(selectedStudent.englishLevel)}</span>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">수강 반</span>
                    <span className="detail-value">
                      {selectedStudent.enrollments && selectedStudent.enrollments.length > 0 
                        ? selectedStudent.enrollments
                            .filter(enrollment => enrollment.isActive)
                            .map(enrollment => enrollment.courseName)
                            .join(', ') || '-'
                        : '-'
                      }
                    </span>
                  </div>
                </div>
              </div>

              <div className="detail-section">
                <h3>학부모 정보</h3>
                <div className="detail-grid">
                  <div className="detail-item">
                    <span className="detail-label">학부모명</span>
                    <span className="detail-value">{selectedStudent.parentName}</span>
                  </div>
                  <div className="detail-item">
                    <span className="detail-label">학부모 연락처</span>
                    <span className="detail-value">{selectedStudent.parentPhone}</span>
                  </div>
                  <div className="detail-item full-width">
                    <span className="detail-label">학부모 이메일</span>
                    <span className="detail-value">{selectedStudent.parentEmail || '-'}</span>
                  </div>
                </div>
              </div>

              {selectedStudent.memo && (
                <div className="detail-section">
                  <h3>메모</h3>
                  <p className="memo-content">{selectedStudent.memo}</p>
                </div>
              )}
            </div>

            <div className="std-edit-footer">
              <button className="std-edit-cancel" onClick={() => setShowDetailModal(false)}>
                닫기
              </button>
              {(profile?.role === 'ADMIN' || profile?.role === 'TEACHER' || profile?.role === 'PARENT') && (
                <button
                  className="std-edit-submit"
                  onClick={() => {
                    setShowDetailModal(false);
                    openEditModal(selectedStudent);
                  }}
                >
                  수정하기
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Students;
