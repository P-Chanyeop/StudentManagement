import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import '../styles/QuizManagement.css';

function QuizManagement() {
  const [excelFile, setExcelFile] = useState(null);
  const queryClient = useQueryClient();

  // 엑셀 파일 업로드
  const uploadMutation = useMutation({
    mutationFn: async (file) => {
      const formData = new FormData();
      formData.append('file', file);
      
      const token = localStorage.getItem('accessToken');
      const response = await fetch('/api/quiz/upload-renaissance-ids', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        },
        body: formData
      });
      
      if (!response.ok) {
        throw new Error('업로드 실패');
      }
      
      return response.json();
    },
    onSuccess: (data) => {
      alert(`업로드 완료!\n${data.updatedCount}명의 학생 정보가 업데이트되었습니다.`);
      setExcelFile(null);
    },
    onError: (error) => {
      alert('업로드 실패: ' + error.message);
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
      </div>
    </div>
  );
}

export default QuizManagement;
