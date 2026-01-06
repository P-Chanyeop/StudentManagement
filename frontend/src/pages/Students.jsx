import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { studentAPI, authAPI } from '../services/api';
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
    parentName: '',
    parentPhone: '',
    parentEmail: '',
    englishLevel: '1.0',
    memo: '',
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

  // 학생 생성 mutation
  const createMutation = useMutation({
    mutationFn: (data) => studentAPI.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries(['students']);
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
      });
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
      queryClient.invalidateQueries(['students']);
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
      queryClient.invalidateQueries(['students']);
      alert('학생이 비활성화되었습니다.');
    },
    onError: (error) => {
      alert(`비활성화 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    },
  });

  const handleCreateStudent = () => {
    if (!newStudent.studentName || !newStudent.studentPhone || !newStudent.parentName || !newStudent.parentPhone) {
      alert('필수 항목을 모두 입력해주세요. (학생명, 학생 연락처, 학부모명, 학부모 연락처)');
      return;
    }

    createMutation.mutate(newStudent);
  };

  const handleUpdateStudent = () => {
    if (!selectedStudent.studentName || !selectedStudent.studentPhone || !selectedStudent.parentName || !selectedStudent.parentPhone) {
      alert('필수 항목을 모두 입력해주세요.');
      return;
    }

    updateMutation.mutate({
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
  };

  const openEditModal = (student) => {
    setSelectedStudent({ ...student });
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
    <div className="page-wrapper">
      {/* 페이지 헤더 */}
      <div className="page-header">
        <div className="page-header-content">
          <div className="page-title-section">
            <h1 className="page-title">
              <i className="fas fa-user-graduate"></i>
              {isParent ? '자녀 관리' : '학생 관리'}
            </h1>
            <p className="page-subtitle">{isParent ? '자녀 정보를 확인하고 관리합니다' : '학생 정보를 등록하고 관리합니다'}</p>
          </div>
          {(profile?.role === 'ADMIN' || profile?.role === 'TEACHER') && (
            <button className="btn-primary btn-with-icon" onClick={() => setShowCreateModal(true)}>
              <i className="fas fa-plus"></i>
              학생 등록
            </button>
          )}
        </div>
      </div>

      {/* 메인 컨텐츠 */}
      <div className="page-content">
        {/* 검색 및 필터 */}
        <div className="search-section">
          <div className="search-input-wrapper">
            <i className="fas fa-search search-icon"></i>
            <input
              type="text"
              placeholder="학생 이름 또는 학부모 이름으로 검색..."
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              className="search-input"
            />
          </div>
          <div className="result-count">
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
                    <input
                      type="date"
                      value={newStudent.birthDate}
                      onChange={(e) => setNewStudent({ ...newStudent, birthDate: e.target.value })}
                    />
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
                  />
                </div>
              </div>

              <div className="form-section">
                <h3>학부모 정보</h3>
                <div className="form-row">
                  <div className="form-group">
                    <label>학부모명 *</label>
                    <input
                      type="text"
                      value={newStudent.parentName}
                      onChange={(e) => setNewStudent({ ...newStudent, parentName: e.target.value })}
                      placeholder="홍길동"
                    />
                  </div>
                  <div className="form-group">
                    <label>학부모 연락처 *</label>
                    <input
                      type="tel"
                      value={newStudent.parentPhone}
                      onChange={(e) => {
                        const value = e.target.value.replace(/[^0-9]/g, '');
                        let formatted = value;
                        if (value.length >= 3) {
                          formatted = value.slice(0, 3) + '-' + value.slice(3);
                        }
                        if (value.length >= 7) {
                          formatted = value.slice(0, 3) + '-' + value.slice(3, 7) + '-' + value.slice(7, 11);
                        }
                        setNewStudent({ ...newStudent, parentPhone: formatted });
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
                    value={newStudent.parentEmail}
                    onChange={(e) => setNewStudent({ ...newStudent, parentEmail: e.target.value })}
                    placeholder="email@example.com"
                  />
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
                    <input
                      type="date"
                      value={selectedStudent.birthDate || ''}
                      onChange={(e) =>
                        setSelectedStudent({ ...selectedStudent, birthDate: e.target.value })
                      }
                    />
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
                  />
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

            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setShowEditModal(false)}>
                취소
              </button>
              <button className="btn-primary" onClick={handleUpdateStudent}>
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

            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setShowDetailModal(false)}>
                닫기
              </button>
              {(profile?.role === 'ADMIN' || profile?.role === 'TEACHER' || profile?.role === 'PARENT') && (
                <button
                  className="btn-primary"
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
