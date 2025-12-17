import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import Layout from '../components/Layout';
import { studentAPI } from '../services/api';
import '../styles/Students.css';

function Students() {
  const [searchKeyword, setSearchKeyword] = useState('');

  const { data: students, isLoading } = useQuery({
    queryKey: ['students'],
    queryFn: async () => {
      const response = await studentAPI.getActive();
      return response.data;
    },
  });

  return (
    <Layout>
      <div className="students-page">
        <div className="page-header">
          <div>
            <h1 className="page-title">학생 관리</h1>
            <p className="page-subtitle">학생 정보를 관리합니다</p>
          </div>
          <button className="btn-primary">
            + 학생 등록
          </button>
        </div>

        <div className="search-bar">
          <input
            type="text"
            placeholder="학생 이름 또는 학부모 이름으로 검색..."
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            className="search-input"
          />
        </div>

        <div className="students-table-container">
          {isLoading ? (
            <div className="loading">로딩 중...</div>
          ) : students && students.length > 0 ? (
            <table className="students-table">
              <thead>
                <tr>
                  <th>번호</th>
                  <th>학생명</th>
                  <th>학년</th>
                  <th>영어 레벨</th>
                  <th>학부모명</th>
                  <th>연락처</th>
                  <th>상태</th>
                  <th>관리</th>
                </tr>
              </thead>
              <tbody>
                {students.map((student, index) => (
                  <tr key={student.id}>
                    <td>{index + 1}</td>
                    <td className="student-name">{student.studentName}</td>
                    <td>{student.grade || '-'}</td>
                    <td>
                      <span className="level-badge">
                        {student.englishLevel || '-'}
                      </span>
                    </td>
                    <td>{student.parentName || '-'}</td>
                    <td>{student.parentPhone || '-'}</td>
                    <td>
                      <span className={`status-badge ${student.isActive ? 'active' : 'inactive'}`}>
                        {student.isActive ? '활성' : '비활성'}
                      </span>
                    </td>
                    <td>
                      <div className="action-buttons">
                        <button className="btn-sm btn-outline">수정</button>
                        <button className="btn-sm btn-outline">상세</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <div className="empty-state">
              <p>등록된 학생이 없습니다</p>
              <button className="btn-primary">첫 학생 등록하기</button>
            </div>
          )}
        </div>
      </div>
    </Layout>
  );
}

export default Students;
