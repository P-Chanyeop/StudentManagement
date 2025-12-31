import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import LoadingSpinner from '../components/LoadingSpinner';
import { messageAPI, studentAPI } from '../services/api';
import '../styles/Messages.css';

function Messages() {
  const queryClient = useQueryClient();
  const [showSendModal, setShowSendModal] = useState(false);
  const [newMessage, setNewMessage] = useState({
    studentId: '',
    recipientPhone: '',
    recipientName: '',
    messageType: 'GENERAL',
    content: '',
  });

  // 전체 문자 내역 조회
  const { data: messages = [], isLoading } = useQuery({
    queryKey: ['messages'],
    queryFn: async () => {
      const response = await messageAPI.getAll();
      return response.data;
    },
  });

  // 학생 목록 조회
  const { data: students = [] } = useQuery({
    queryKey: ['students'],
    queryFn: async () => {
      const response = await studentAPI.getAll();
      return response.data;
    },
  });

  // 문자 발송 mutation
  const sendMutation = useMutation({
    mutationFn: (data) => messageAPI.send(data),
    onSuccess: () => {
      queryClient.invalidateQueries(['messages']);
      setShowSendModal(false);
      resetForm();
      alert('문자가 발송되었습니다.');
    },
    onError: (error) => {
      alert(`발송 실패: ${error.response?.data?.message || error.message}`);
    },
  });

  const resetForm = () => {
    setNewMessage({
      studentId: '',
      recipientPhone: '',
      recipientName: '',
      messageType: 'GENERAL',
      content: '',
    });
  };

  const handleStudentSelect = (studentId) => {
    const student = students.find((s) => s.id === parseInt(studentId));
    if (student) {
      setNewMessage({
        ...newMessage,
        studentId,
        recipientPhone: student.phoneNumber || '',
        recipientName: student.studentName,
      });
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    sendMutation.mutate(newMessage);
  };

  const getStatusBadge = (status) => {
    const badges = {
      SENT: { text: '발송완료', className: 'messages-status-success' },
      PENDING: { text: '발송중', className: 'messages-status-pending' },
      FAILED: { text: '발송실패', className: 'messages-status-error' },
    };
    return badges[status] || { text: status, className: 'messages-status-default' };
  };

  const getTypeBadge = (type) => {
    const badges = {
      GENERAL: { text: '일반', className: 'messages-type-general' },
      ATTENDANCE: { text: '출석', className: 'messages-type-attendance' },
      PAYMENT: { text: '결제', className: 'messages-type-payment' },
      RESERVATION: { text: '예약', className: 'messages-type-reservation' },
      EMERGENCY: { text: '긴급', className: 'messages-type-emergency' },
    };
    return badges[type] || { text: type, className: 'messages-type-default' };
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
              <i className="fas fa-envelope"></i>
              문자 발송 관리
            </h1>
            <p className="page-subtitle">학생 및 학부모에게 문자를 발송합니다</p>
          </div>
          <button className="btn-primary" onClick={() => setShowSendModal(true)}>
            <i className="fas fa-paper-plane"></i> 문자 발송
          </button>
        </div>
      </div>

      <div className="page-content">
        {/* 통계 카드 */}
        <div className="messages-stats-grid">
          <div className="messages-stat-card">
            <div className="messages-stat-icon">
              <i className="fas fa-envelope"></i>
            </div>
            <div className="messages-stat-content">
              <div className="messages-stat-value">{messages.length}</div>
              <div className="messages-stat-label">전체 발송</div>
            </div>
          </div>
          <div className="messages-stat-card success">
            <div className="messages-stat-icon">
              <i className="fas fa-check-circle"></i>
            </div>
            <div className="messages-stat-content">
              <div className="messages-stat-value">
                {messages.filter((m) => m.sendStatus === 'SENT').length}
              </div>
              <div className="messages-stat-label">발송 완료</div>
            </div>
          </div>
          <div className="messages-stat-card warning">
            <div className="messages-stat-icon">
              <i className="fas fa-clock"></i>
            </div>
            <div className="messages-stat-content">
              <div className="messages-stat-value">
                {messages.filter((m) => m.sendStatus === 'PENDING').length}
              </div>
              <div className="messages-stat-label">발송 중</div>
            </div>
          </div>
          <div className="messages-stat-card error">
            <div className="messages-stat-icon">
              <i className="fas fa-exclamation-triangle"></i>
            </div>
            <div className="messages-stat-content">
              <div className="messages-stat-value">
                {messages.filter((m) => m.sendStatus === 'FAILED').length}
              </div>
              <div className="messages-stat-label">발송 실패</div>
            </div>
          </div>
        </div>

        {/* 문자 발송 내역 */}
        <div className="messages-list-container">
          <h3 className="messages-list-title">발송 내역</h3>
          {messages.length > 0 ? (
            <div className="table-wrapper">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>발송일시</th>
                    <th>수신자</th>
                    <th>전화번호</th>
                    <th>유형</th>
                    <th>내용</th>
                    <th>상태</th>
                  </tr>
                </thead>
                <tbody>
                  {messages.map((message) => (
                    <tr key={message.id}>
                      <td>{new Date(message.sentAt).toLocaleString('ko-KR')}</td>
                      <td>{message.recipientName}</td>
                      <td>{message.recipientPhone}</td>
                      <td>
                        <span className={`messages-type-badge ${getTypeBadge(message.messageType).className}`}>
                          {getTypeBadge(message.messageType).text}
                        </span>
                      </td>
                      <td className="messages-content-cell">
                        {message.content.length > 30 
                          ? `${message.content.substring(0, 30)}...` 
                          : message.content}
                      </td>
                      <td>
                        <span className={`messages-status-badge ${getStatusBadge(message.sendStatus).className}`}>
                          {getStatusBadge(message.sendStatus).text}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="empty-state">
              <i className="fas fa-envelope-open"></i>
              <p>발송된 문자가 없습니다.</p>
            </div>
          )}
        </div>
      </div>

      {/* 문자 발송 모달 */}
      {showSendModal && (
        <div className="modal-overlay" onClick={() => {
          setShowSendModal(false);
          resetForm();
        }}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>문자 발송</h2>
              <button className="modal-close" onClick={() => {
                setShowSendModal(false);
                resetForm();
              }}>
                ×
              </button>
            </div>

            <div className="modal-body">
              <form onSubmit={handleSubmit}>
                <div className="form-group">
                  <label>학생 선택</label>
                  <select
                    value={newMessage.studentId}
                    onChange={(e) => handleStudentSelect(e.target.value)}
                  >
                    <option value="">학생을 선택하세요</option>
                    {students.map((student) => (
                      <option key={student.id} value={student.id}>
                        {student.studentName}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="messages-form-row">
                  <div className="form-group">
                    <label>수신자명 *</label>
                    <input
                      type="text"
                      value={newMessage.recipientName}
                      onChange={(e) => setNewMessage({ ...newMessage, recipientName: e.target.value })}
                      placeholder="수신자 이름"
                      required
                    />
                  </div>
                  <div className="form-group">
                    <label>전화번호 *</label>
                    <input
                      type="tel"
                      value={newMessage.recipientPhone}
                      onChange={(e) => setNewMessage({ ...newMessage, recipientPhone: e.target.value })}
                      placeholder="010-1234-5678"
                      required
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label>메시지 유형</label>
                  <select
                    value={newMessage.messageType}
                    onChange={(e) => setNewMessage({ ...newMessage, messageType: e.target.value })}
                  >
                    <option value="GENERAL">일반</option>
                    <option value="ATTENDANCE">출석 관련</option>
                    <option value="PAYMENT">결제 관련</option>
                    <option value="RESERVATION">예약 관련</option>
                    <option value="EMERGENCY">긴급</option>
                  </select>
                </div>

                <div className="form-group">
                  <label>메시지 내용 *</label>
                  <textarea
                    value={newMessage.content}
                    onChange={(e) => setNewMessage({ ...newMessage, content: e.target.value })}
                    placeholder="메시지 내용을 입력하세요"
                    rows="4"
                    maxLength="90"
                    required
                  />
                  <div className="messages-char-count">
                    {newMessage.content.length}/90자
                  </div>
                </div>
              </form>
            </div>

            <div className="modal-footer">
              <button 
                type="submit" 
                className="btn-primary"
                onClick={handleSubmit}
                disabled={sendMutation.isLoading}
              >
                {sendMutation.isLoading ? '발송 중...' : '발송'}
              </button>
              <button
                type="button"
                className="btn-secondary"
                onClick={() => {
                  setShowSendModal(false);
                  resetForm();
                }}
              >
                취소
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Messages;
