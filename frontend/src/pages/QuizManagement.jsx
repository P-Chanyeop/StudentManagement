import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import '../styles/QuizManagement.css';

function QuizManagement() {
  const [excelFile, setExcelFile] = useState(null);
  const queryClient = useQueryClient();

  // 엑셀 파일 업로드
  const uploadMutation = useMutation({
    mutationFn: async (file) => {
      const formData = new FormData();
      formData.append('file', file);
      // TODO: API 연동
      // const response = await fetch('/api/quiz/upload-renaissance-ids', {
      //   method: 'POST',
      //   body: formData
      // });
      // return response.json();
      return { success: true, matched: 120, unmatched: 23 };
    },
    onSuccess: (data) => {
      alert(`업로드 완료!\n매칭: ${data.matched}명\n미매칭: ${data.unmatched}명`);
      setExcelFile(null);
    },
    onError: () => {
      alert('업로드 실패');
    }
  });

  // 퀴즈 데이터 동기화
  const syncMutation = useMutation({
    mutationFn: async () => {
      // TODO: API 연동
      // const response = await fetch('/api/quiz/sync', { method: 'POST' });
      // return response.json();
      return { success: true, newQuizzes: 15, updatedQuizzes: 3 };
    },
    onSuccess: (data) => {
      alert(`동기화 완료!\n새 퀴즈: ${data.newQuizzes}개\n업데이트: ${data.updatedQuizzes}개`);
    },
    onError: () => {
      alert('동기화 실패');
    }
  });

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file && file.name.endsWith('.xlsx')) {
      setExcelFile(file);
    } else {
      alert('엑셀 파일(.xlsx)만 업로드 가능합니다.');
    }
  };

  const handleUpload = () => {
    if (excelFile) {
      uploadMutation.mutate(excelFile);
    }
  };

  const handleSync = () => {
    if (window.confirm('퀴즈 데이터를 동기화하시겠습니까?')) {
      syncMutation.mutate();
    }
  };

  return (
    <div className="quiz-management-container">
      <header className="page-header">
        <h1>📊 퀴즈 관리</h1>
        <p>르네상스 퀴즈 데이터를 관리합니다</p>
      </header>

      <div className="management-section">
        <div className="section-card">
          <h2>📁 르네상스 아이디 업로드</h2>
          <p className="section-desc">학생 이름과 르네상스 아이디가 포함된 엑셀 파일을 업로드하세요.</p>
          
          <div className="upload-area">
            <input
              type="file"
              id="excelFile"
              accept=".xlsx"
              onChange={handleFileChange}
              style={{ display: 'none' }}
            />
            <label htmlFor="excelFile" className="upload-label">
              <i className="fas fa-cloud-upload-alt"></i>
              <span>엑셀 파일 선택</span>
            </label>
            
            {excelFile && (
              <div className="file-info">
                <i className="fas fa-file-excel"></i>
                <span>{excelFile.name}</span>
                <button onClick={() => setExcelFile(null)} className="remove-btn">
                  <i className="fas fa-times"></i>
                </button>
              </div>
            )}
          </div>

          <button
            className="btn-primary"
            onClick={handleUpload}
            disabled={!excelFile || uploadMutation.isLoading}
          >
            {uploadMutation.isLoading ? '업로드 중...' : '업로드 및 매칭'}
          </button>
        </div>

        <div className="section-card">
          <h2>🔄 퀴즈 데이터 동기화</h2>
          <p className="section-desc">르네상스 API에서 최신 퀴즈 데이터를 가져옵니다.</p>
          
          <div className="sync-info">
            <div className="info-item">
              <i className="fas fa-info-circle"></i>
              <span>매일 새벽 2시 자동 동기화됩니다</span>
            </div>
            <div className="info-item">
              <i className="fas fa-clock"></i>
              <span>수동 동기화는 언제든 가능합니다</span>
            </div>
          </div>

          <button
            className="btn-sync"
            onClick={handleSync}
            disabled={syncMutation.isLoading}
          >
            {syncMutation.isLoading ? (
              <>
                <i className="fas fa-spinner fa-spin"></i>
                동기화 중...
              </>
            ) : (
              <>
                <i className="fas fa-sync-alt"></i>
                지금 동기화
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}

export default QuizManagement;
