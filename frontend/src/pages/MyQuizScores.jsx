import React, { useState, useMemo } from 'react';
import '../styles/MyQuizScores.css';

// ruru3677 계정 = "kim, doyoon" 학생 (Grade 3)
const STUDENT_NAME = "kim, doyoon";

const allQuizData = [
  { grade: "Grade 3", name: "kim, doyoon", date: "2026-01-26", book: "Night of the Bats!", difficulty: 4.2, score: 80 },
  { grade: "Grade 1", name: "kim, doyoon", date: "2026-01-24", book: "Dirty Bertie: Fangs!", difficulty: 3.1, score: 90 }
];

const MyQuizScores = () => {
  const [sortColumn, setSortColumn] = useState('date');
  const [sortAscending, setSortAscending] = useState(false);

  const myData = useMemo(() => {
    let filtered = allQuizData.filter(row => row.name === STUDENT_NAME);
    
    filtered.sort((a, b) => {
      let aVal = a[sortColumn];
      let bVal = b[sortColumn];
      if (typeof aVal === 'string') {
        aVal = aVal.toLowerCase();
        bVal = bVal.toLowerCase();
      }
      if (aVal < bVal) return sortAscending ? -1 : 1;
      if (aVal > bVal) return sortAscending ? 1 : -1;
      return 0;
    });

    return filtered;
  }, [sortColumn, sortAscending]);

  const handleSort = (column) => {
    if (sortColumn === column) {
      setSortAscending(!sortAscending);
    } else {
      setSortColumn(column);
      setSortAscending(true);
    }
  };

  const getScoreBadgeClass = (score) => {
    if (score >= 90) return 'score-excellent';
    if (score >= 70) return 'score-good';
    if (score >= 50) return 'score-fair';
    return 'score-poor';
  };

  const stats = useMemo(() => {
    if (myData.length === 0) return { avg: 0, max: 0 };
    const avg = (myData.reduce((sum, item) => sum + item.score, 0) / myData.length).toFixed(1);
    const max = Math.max(...myData.map(d => d.score));
    return { avg, max };
  }, [myData]);

  return (
    <div className="my-quiz-container">
      <header className="quiz-header">
        <h1>📚 나의 영어 퀴즈 기록</h1>
        <div className="student-info">
          <p className="student-name">{STUDENT_NAME}</p>
          <p className="student-grade">{myData.length > 0 ? myData[0].grade : ''}</p>
        </div>
      </header>

      <div className="stats-section">
        <div className="stat-card">
          <div className="stat-label">총 활동</div>
          <div className="stat-value">{myData.length}개</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">평균 점수</div>
          <div className="stat-value">{stats.avg}%</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">최고 점수</div>
          <div className="stat-value">{stats.max}%</div>
        </div>
      </div>

      <div className="table-container">
        <table className="quiz-table">
          <thead>
            <tr>
              <th onClick={() => handleSort('date')}>날짜 ▼</th>
              <th onClick={() => handleSort('book')}>책 제목 ▼</th>
              <th onClick={() => handleSort('difficulty')}>난이도 ▼</th>
              <th onClick={() => handleSort('score')}>점수 ▼</th>
            </tr>
          </thead>
          <tbody>
            {myData.length > 0 ? (
              myData.map((row, index) => (
                <tr key={index}>
                  <td>{row.date}</td>
                  <td>{row.book}</td>
                  <td><span className="difficulty-badge">{row.difficulty}</span></td>
                  <td><span className={`score-badge ${getScoreBadgeClass(row.score)}`}>{row.score}%</span></td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="4" className="empty-state">아직 퀴즈 기록이 없습니다.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default MyQuizScores;
