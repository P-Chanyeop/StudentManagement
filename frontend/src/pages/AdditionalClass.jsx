import { useState, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { studentAPI } from '../services/api';
import LoadingSpinner from '../components/LoadingSpinner';
import '../styles/Students.css';

function AdditionalClass() {
  const queryClient = useQueryClient();
  const [searchKeyword, setSearchKeyword] = useState('');
  const fileInputRef = useRef(null);

  const { data: students, isLoading } = useQuery({
    queryKey: ['students-additional-class'],
    queryFn: () => studentAPI.getAdditionalClass().then(res => res.data),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => studentAPI.updateAdditionalClass(id, data),
    onSuccess: () => queryClient.invalidateQueries(['students-additional-class']),
  });

  const uploadMutation = useMutation({
    mutationFn: (file) => studentAPI.uploadAdditionalClassExcel(file),
    onSuccess: (response) => {
      queryClient.invalidateQueries(['students-additional-class']);
      alert(`업로드 완료: ${response.data.updatedCount || 0}명 업데이트`);
    },
    onError: (error) => {
      alert(`업로드 실패: ${error.response?.data?.message || error.message}`);
    },
  });

  const handleFileUpload = (e) => {
    const file = e.target.files[0];
    if (file) {
      uploadMutation.mutate(file);
      e.target.value = '';
    }
  };

  const handleToggle = (student, field) => {
    // 하나만 선택 가능: 선택하면 나머지는 false
    const isCurrentlyActive = student[field];
    const data = {
      assignedVocabulary: false,
      assignedSightword: false,
      assignedGrammar: false,
      assignedPhonics: false,
    };
    // 현재 활성화된 걸 클릭하면 해제, 아니면 해당 항목만 활성화
    if (!isCurrentlyActive) {
      data[field] = true;
    }
    updateMutation.mutate({ id: student.id, data });
  };

  const filteredStudents = students?.filter(s =>
    s.studentName?.toLowerCase().includes(searchKeyword.toLowerCase())
  ) || [];

  if (isLoading) {
    return (
      <div className="page-wrapper">
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
              <i className="fas fa-plus-circle"></i>
              추가수업 관리
            </h1>
            <p className="page-subtitle">시스템 예약 학생의 VSGP 추가수업을 할당합니다</p>
          </div>
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
              placeholder="학생 이름으로 검색..."
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              className="search-input"
            />
          </div>
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileUpload}
            accept=".xlsx,.xls"
            style={{ display: 'none' }}
          />
          <button 
            className="btn-secondary" 
            onClick={() => fileInputRef.current?.click()}
            disabled={uploadMutation.isLoading}
          >
            <i className="fas fa-upload"></i> 
            {uploadMutation.isLoading ? '업로드 중...' : '네이버예약 학생 추가수업 리스트 업로드'}
          </button>
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
                  <th>반</th>
                  <th style={{textAlign: 'center', width: '80px'}}>V</th>
                  <th style={{textAlign: 'center', width: '80px'}}>S</th>
                  <th style={{textAlign: 'center', width: '80px'}}>G</th>
                  <th style={{textAlign: 'center', width: '80px'}}>P</th>
                  <th style={{textAlign: 'center', width: '100px'}}>할당</th>
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
                    <td>{student.className || '-'}</td>
                    <td style={{textAlign: 'center'}}>
                      <button
                        className={`vgsp-btn ${student.assignedVocabulary ? 'active' : ''}`}
                        onClick={() => handleToggle(student, 'assignedVocabulary')}
                        title="Vocabulary"
                      >
                        V
                      </button>
                    </td>
                    <td style={{textAlign: 'center'}}>
                      <button
                        className={`vgsp-btn ${student.assignedSightword ? 'active' : ''}`}
                        onClick={() => handleToggle(student, 'assignedSightword')}
                        title="Sightword"
                      >
                        S
                      </button>
                    </td>
                    <td style={{textAlign: 'center'}}>
                      <button
                        className={`vgsp-btn ${student.assignedGrammar ? 'active' : ''}`}
                        onClick={() => handleToggle(student, 'assignedGrammar')}
                        title="Grammar"
                      >
                        G
                      </button>
                    </td>
                    <td style={{textAlign: 'center'}}>
                      <button
                        className={`vgsp-btn ${student.assignedPhonics ? 'active' : ''}`}
                        onClick={() => handleToggle(student, 'assignedPhonics')}
                        title="Phonics"
                      >
                        P
                      </button>
                    </td>
                    <td style={{textAlign: 'center'}}>
                      <span className={`assigned-badge ${student.assignedClassInitials ? 'has-class' : ''}`}>
                        {student.assignedClassInitials || '-'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

export default AdditionalClass;
