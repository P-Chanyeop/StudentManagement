import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
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
      setNewMessage({
        studentId: '',
        recipientPhone: '',
        recipientName: '',
        messageType: 'GENERAL',
        content: '',
      });
      alert('문자가 발송되었습니다.');
    },
    onError: (error) => {
      const errorMsg = error.response && error.response.data && error.response.data.message
        ? error.response.data.message
        : '오류가 발생했습니다.';
      alert('발송 실패: ' + errorMsg);
    },
  });

  const handleSendMessage = () => {
    if (!newMessage.recipientPhone || !newMessage.content) {
      alert('수신자 전화번호와 내용을 입력해주세요.');
      return;
    }

    sendMutation.mutate(newMessage);
  };

  const handleStudentSelect = (e) => {
    const studentId = e.target.value;
    const student = students.find((s) => s.id === parseInt(studentId));

    if (student) {
      setNewMessage({
        ...newMessage,
        studentId,
        recipientPhone: student.parentPhone || student.studentPhone,
        recipientName: student.parentName || student.studentName,
      });
    } else {
      setNewMessage({
        ...newMessage,
        studentId: '',
        recipientPhone: '',
        recipientName: '',
      });
    }
  };

  // 상태별 배지
  const getStatusBadge = (status) => {
    const statusMap = {
      PENDING: { text: '대기', color: '#FF9800' },
      SENT: { text: '발송 완료', color: '#03C75A' },
      FAILED: { text: '발송 실패', color: '#FF3B30' },
    };
    const statusInfo = statusMap[status] || { text: status, color: '#999' };
    return <span className="status-badge" style={{ backgroundColor: statusInfo.color }}>{statusInfo.text}</span>;
  };

  // 메시지 타입별 아이콘
  const getTypeIcon = (type) => {
    const typeMap = {
      GENERAL: '📧',
      ATTENDANCE: '✅',
      PAYMENT: '💰',
      RESERVATION: '📅',
      EMERGENCY: '🚨',
    };
    return typeMap[type] || '📧';
  };

  if (isLoading) {
    return <div className="messages-container">로딩 중...</div>;
  }

  return (
    <div className="messages-container">
      <div className="messages-header">
        <h1>문자 발송 관리</h1>
        <button className="btn-send-message" onClick={() => setShowSendModal(true)}>
          + 문자 발송
        </button>
      </div>

      <div className="messages-stats">
        <div className="stat-card">
          <span className="stat-label">전체 발송</span>
          <span className="stat-value">{messages.length}건</span>
        </div>
        <div className="stat-card success">
          <span className="stat-label">발송 완료</span>
          <span className="stat-value">
            {messages.filter((m) => m.sendStatus === 'SENT').length}건
          </span>
        </div>
        <div className="stat-card pending">
          <span className="stat-label">대기</span>
          <span className="stat-value">
            {messages.filter((m) => m.sendStatus === 'PENDING').length}건
          </span>
        </div>
        <div className="stat-card failed">
          <span className="stat-label">실패</span>
          <span className="stat-value">
            {messages.filter((m) => m.sendStatus === 'FAILED').length}건
          </span>
        </div>
      </div>

      <div className="messages-list">
        {messages.length === 0 ? (
          <div className="empty-state">발송된 문자가 없습니다.</div>
        ) : (
          messages.map((message) => (
            <div key={message.id} className="message-card">
              <div className="message-header">
                <div className="message-info">
                  <span className="message-icon">{getTypeIcon(message.messageType)}</span>
                  <div className="recipient-info">
                    <strong>{message.recipientName || '수신자'}</strong>
                    <span className="phone">{message.recipientPhone}</span>
                  </div>
                </div>
                {getStatusBadge(message.sendStatus)}
              </div>

              <div className="message-content">
                <p>{message.content}</p>
              </div>

              <div className="message-footer">
                {message.sentAt && (
                  <span className="sent-time">
                    발송: {new Date(message.sentAt).toLocaleString('ko-KR')}
                  </span>
                )}
                {message.errorMessage && (
                  <span className="error-message">오류: {message.errorMessage}</span>
                )}
              </div>
            </div>
          ))
        )}
      </div>

      {/* 문자 발송 모달 */}
      {showSendModal && (
        <div className="modal-overlay" onClick={() => setShowSendModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>문자 발송</h2>
              <button className="modal-close" onClick={() => setShowSendModal(false)}>
                ×
              </button>
            </div>

            <div className="modal-body">
              <div className="form-group">
                <label>학생 선택 (선택사항)</label>
                <select value={newMessage.studentId} onChange={handleStudentSelect}>
                  <option value="">직접 입력</option>
                  {students.map((student) => (
                    <option key={student.id} value={student.id}>
                      {student.studentName} ({student.parentPhone || student.studentPhone})
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>수신자명</label>
                  <input
                    type="text"
                    value={newMessage.recipientName}
                    onChange={(e) =>
                      setNewMessage({ ...newMessage, recipientName: e.target.value })
                    }
                    placeholder="수신자 이름"
                  />
                </div>

                <div className="form-group">
                  <label>전화번호 *</label>
                  <input
                    type="tel"
                    value={newMessage.recipientPhone}
                    onChange={(e) =>
                      setNewMessage({ ...newMessage, recipientPhone: e.target.value })
                    }
                    placeholder="010-1234-5678"
                  />
                </div>
              </div>

              <div className="form-group">
                <label>메시지 유형</label>
                <select
                  value={newMessage.messageType}
                  onChange={(e) =>
                    setNewMessage({ ...newMessage, messageType: e.target.value })
                  }
                >
                  <option value="GENERAL">일반</option>
                  <option value="ATTENDANCE">출석 안내</option>
                  <option value="PAYMENT">결제 안내</option>
                  <option value="RESERVATION">예약 안내</option>
                  <option value="EMERGENCY">긴급</option>
                </select>
              </div>

              <div className="form-group">
                <label>메시지 내용 *</label>
                <textarea
                  value={newMessage.content}
                  onChange={(e) => setNewMessage({ ...newMessage, content: e.target.value })}
                  placeholder="메시지 내용을 입력하세요 (최대 2000자)"
                  rows="8"
                  maxLength="2000"
                />
                <span className="char-count">{newMessage.content.length} / 2000</span>
              </div>
            </div>

            <div className="modal-footer">
              <button className="btn-secondary" onClick={() => setShowSendModal(false)}>
                취소
              </button>
              <button className="btn-primary" onClick={handleSendMessage}>
                발송
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Messages;
