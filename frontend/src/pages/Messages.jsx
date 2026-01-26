import { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import LoadingSpinner from '../components/LoadingSpinner';
import { messageAPI, studentAPI, smsTemplateAPI } from '../services/api';
import '../styles/Messages.css';

function Messages() {
  const queryClient = useQueryClient();
  const [showSendModal, setShowSendModal] = useState(false);
  const [showTemplateModal, setShowTemplateModal] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState(null);
  // const [testPhone, setTestPhone] = useState('');
  // const [testMessage, setTestMessage] = useState('테스트 메시지입니다.');
  const [newMessage, setNewMessage] = useState({
    studentId: '',
    recipientPhone: '',
    recipientName: '',
    messageType: 'GENERAL',
    content: '',
  });
  
  // 학생 선택 드롭다운 상태
  const [showStudentDropdown, setShowStudentDropdown] = useState(false);
  const [studentSearchQuery, setStudentSearchQuery] = useState('');
  const [selectedTemplateId, setSelectedTemplateId] = useState('');

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

  // 템플릿 목록 조회
  const { data: templates = [] } = useQuery({
    queryKey: ['smsTemplates'],
    queryFn: async () => {
      const response = await smsTemplateAPI.getAll();
      return response.data;
    },
  });

  // 템플릿 선택 핸들러
  const handleTemplateSelect = (templateId) => {
    setSelectedTemplateId(templateId);
    if (templateId) {
      const template = templates.find(t => t.id.toString() === templateId);
      if (template) {
        let content = template.content;
        // 학생 이름 치환
        if (selectedStudent) {
          content = content.replace(/{studentName}/g, selectedStudent.studentName);
        }
        setNewMessage(prev => ({ ...prev, content }));
      }
    }
  };

  // 학생 선택 핸들러
  const handleStudentSelect = (student) => {
    setNewMessage(prev => {
      let content = prev.content;
      // 현재 내용에서 학생 이름 치환
      if (content) {
        content = content.replace(/{studentName}/g, student.studentName);
      }
      return {
        ...prev,
        studentId: student.id.toString(),
        recipientPhone: student.parentPhone || student.studentPhone,
        recipientName: student.parentName || student.studentName,
        content
      };
    });
    setShowStudentDropdown(false);
    setStudentSearchQuery('');
  };

  // 선택된 학생 정보 가져오기 (메모이제이션)
  const selectedStudent = useMemo(() => {
    return students.find(student => student.id.toString() === newMessage.studentId);
  }, [students, newMessage.studentId]);

  // 필터링된 학생 목록 (메모이제이션)
  const filteredStudents = useMemo(() => {
    if (!studentSearchQuery) return students;
    return students.filter(student => 
      student.studentName.toLowerCase().includes(studentSearchQuery.toLowerCase()) ||
      student.parentName?.toLowerCase().includes(studentSearchQuery.toLowerCase())
    );
  }, [students, studentSearchQuery]);

  // 통계 메모이제이션
  const messageStats = useMemo(() => ({
    total: messages.length,
    sent: messages.filter(m => m.sendStatus === 'SENT').length,
    pending: messages.filter(m => m.sendStatus === 'PENDING').length,
    failed: messages.filter(m => m.sendStatus === 'FAILED').length
  }), [messages]);

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

  // 템플릿 수정 mutation
  const updateTemplateMutation = useMutation({
    mutationFn: ({ id, data }) => smsTemplateAPI.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries(['smsTemplates']);
      setEditingTemplate(null);
      alert('템플릿이 수정되었습니다.');
    },
    onError: (error) => {
      alert(`수정 실패: ${error.response?.data?.message || error.message}`);
    },
  });

  // 템플릿 삭제 mutation
  const deleteTemplateMutation = useMutation({
    mutationFn: (id) => smsTemplateAPI.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries(['smsTemplates']);
      alert('템플릿이 삭제되었습니다.');
    },
    onError: (error) => {
      alert(`삭제 실패: ${error.response?.data?.message || error.message}`);
    },
  });

  // 템플릿 생성 mutation
  const createTemplateMutation = useMutation({
    mutationFn: (data) => smsTemplateAPI.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries(['smsTemplates']);
      setEditingTemplate(null);
      alert('템플릿이 생성되었습니다.');
    },
    onError: (error) => {
      alert(`생성 실패: ${error.response?.data?.message || error.message}`);
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
    setSelectedTemplateId('');
  };

  /* SMS 테스트 발송 - 주석 처리
  const testSmsMutation = useMutation({
    mutationFn: (data) => smsAPI.send(data),
    onSuccess: (response) => {
      console.log('SMS 응답:', response);
      const data = response.data;
      if (data.resultCode < 0) {
        alert(`발송 실패\n에러코드: ${data.resultCode}\n메시지: ${data.message}`);
      } else {
        alert(`테스트 문자 발송 성공!\n성공: ${data.successCnt}건\n실패: ${data.errorCnt}건\nMSG ID: ${data.msgId}`);
      }
      setTestPhone('');
      setTestMessage('테스트 메시지입니다.');
    },
    onError: (error) => {
      console.error('SMS 에러:', error);
      alert(`발송 실패: ${error.response?.data?.message || error.message}`);
    },
  });

  const handleTestSms = () => {
    if (!testPhone) {
      alert('전화번호를 입력하세요');
      return;
    }
    if (!testMessage) {
      alert('메시지를 입력하세요');
      return;
    }
    testSmsMutation.mutate({
      receiver: testPhone,
      message: testMessage,
    });
  };
  */

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
      ENROLLMENT_REGISTER: { text: '수강등록', className: 'messages-type-enrollment' },
      ENROLLMENT_EXPIRY: { text: '수강만료', className: 'messages-type-enrollment' },
      ENROLLMENT_COUNT_EXPIRED: { text: '횟수소진', className: 'messages-type-enrollment' },
      ENROLLMENT_PERIOD_EXPIRED: { text: '기간만료', className: 'messages-type-enrollment' },
      TEXTBOOK: { text: '교재안내', className: 'messages-type-general' },
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
          <div className="header-buttons">
            <button className="btn-secondary" onClick={() => setShowTemplateModal(true)}>
              <i className="fas fa-file-alt"></i> 템플릿 관리
            </button>
            <button className="btn-primary" onClick={() => setShowSendModal(true)}>
              <i className="fas fa-paper-plane"></i> 문자 발송
            </button>
          </div>
        </div>
      </div>

      <div className="page-content">
        {/* SMS 테스트 발송 - 주석 처리
        <div className="sms-test-section">
          <h2>SMS 테스트 발송</h2>
          <div className="sms-test-form">
            <input
              type="tel"
              placeholder="전화번호 (01012345678)"
              value={testPhone}
              onChange={(e) => setTestPhone(e.target.value)}
              className="sms-test-input"
            />
            <input
              type="text"
              placeholder="메시지 내용"
              value={testMessage}
              onChange={(e) => setTestMessage(e.target.value)}
              className="sms-test-input"
            />
            <button 
              onClick={handleTestSms} 
              disabled={testSmsMutation.isPending}
              className="btn-primary"
            >
              {testSmsMutation.isPending ? '발송중...' : '테스트 발송'}
            </button>
          </div>
        </div>
        */}

        {/* 통계 카드 */}
        <div className="messages-stats-grid">
          <div className="messages-stat-card">
            <div className="messages-stat-icon">
              <i className="fas fa-envelope"></i>
            </div>
            <div className="messages-stat-content">
              <div className="messages-stat-value">{messageStats.total}</div>
              <div className="messages-stat-label">전체 발송</div>
            </div>
          </div>
          <div className="messages-stat-card success">
            <div className="messages-stat-icon">
              <i className="fas fa-check-circle"></i>
            </div>
            <div className="messages-stat-content">
              <div className="messages-stat-value">{messageStats.sent}</div>
              <div className="messages-stat-label">발송 완료</div>
            </div>
          </div>
          <div className="messages-stat-card warning">
            <div className="messages-stat-icon">
              <i className="fas fa-clock"></i>
            </div>
            <div className="messages-stat-content">
              <div className="messages-stat-value">{messageStats.pending}</div>
              <div className="messages-stat-label">발송 중</div>
            </div>
          </div>
          <div className="messages-stat-card error">
            <div className="messages-stat-icon">
              <i className="fas fa-exclamation-triangle"></i>
            </div>
            <div className="messages-stat-content">
              <div className="messages-stat-value">{messageStats.failed}</div>
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
                  <label>학생을 선택해 주세요</label>
                  <div className="student-select-wrapper">
                    <div 
                      className="student-select-input"
                      onClick={() => setShowStudentDropdown(!showStudentDropdown)}
                    >
                      {selectedStudent ? (
                        <div className="selected-student-info">
                          <span className="student-name">{selectedStudent.studentName}</span>
                          <span className="parent-info">
                            {selectedStudent.parentName} · {selectedStudent.parentPhone || selectedStudent.studentPhone}
                          </span>
                        </div>
                      ) : (
                        <span className="placeholder">학생을 선택해 주세요</span>
                      )}
                      <i className={`fas fa-chevron-${showStudentDropdown ? 'up' : 'down'}`}></i>
                    </div>
                    
                    {showStudentDropdown && (
                      <div className="student-dropdown">
                        <div className="student-search">
                          <input
                            type="text"
                            placeholder="학생 이름으로 검색..."
                            value={studentSearchQuery}
                            onChange={(e) => setStudentSearchQuery(e.target.value)}
                            onClick={(e) => e.stopPropagation()}
                          />
                        </div>
                        <div className="student-list">
                          {filteredStudents.map(student => (
                            <div
                              key={student.id}
                              className="student-option"
                              onClick={() => handleStudentSelect(student)}
                            >
                              <div className="student-info">
                                <span className="student-name">{student.studentName}</span>
                                <span className="parent-info">
                                  {student.parentName} · {student.parentPhone || student.studentPhone}
                                </span>
                              </div>
                            </div>
                          ))}
                          {filteredStudents.length === 0 && (
                            <div className="no-students">검색 결과가 없습니다.</div>
                          )}
                        </div>
                      </div>
                    )}
                  </div>
                </div>

                <div className="messages-form-row">
                  <div className="form-group">
                    <label>수신자명 (학부모) *</label>
                    <input
                      type="text"
                      value={newMessage.recipientName}
                      onChange={(e) => setNewMessage({ ...newMessage, recipientName: e.target.value })}
                      placeholder="학생을 선택하면 자동으로 입력됩니다"
                      required
                      readOnly={newMessage.studentId !== ''}
                    />
                  </div>
                  <div className="form-group">
                    <label>전화번호 (학부모) *</label>
                    <input
                      type="tel"
                      value={newMessage.recipientPhone}
                      onChange={(e) => setNewMessage({ ...newMessage, recipientPhone: e.target.value })}
                      placeholder="학생을 선택하면 자동으로 입력됩니다"
                      required
                      readOnly={newMessage.studentId !== ''}
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
                  <label>템플릿 선택</label>
                  <select
                    value={selectedTemplateId}
                    onChange={(e) => handleTemplateSelect(e.target.value)}
                  >
                    <option value="">직접 입력</option>
                    {templates.map(template => (
                      <option key={template.id} value={template.id}>
                        {template.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="form-group">
                  <label>메시지 내용 *</label>
                  <textarea
                    value={newMessage.content}
                    onChange={(e) => setNewMessage({ ...newMessage, content: e.target.value })}
                    placeholder="메시지 내용을 입력하세요&#10;&#10;변수: {studentName}, {textbookName}, {content}, {date}, {reason}"
                    rows="6"
                    required
                  />
                  <div className="messages-char-count">
                    {newMessage.content.length}자 (90자 초과 시 LMS로 발송)
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

      {/* 템플릿 관리 모달 */}
      {showTemplateModal && (
        <div className="modal-overlay" onClick={() => { setShowTemplateModal(false); setEditingTemplate(null); }}>
          <div className="modal-content modal-large" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>템플릿 관리</h2>
              <button className="modal-close" onClick={() => { setShowTemplateModal(false); setEditingTemplate(null); }}>×</button>
            </div>
            <div className="modal-body">
              {editingTemplate ? (
                <div className="template-edit-form">
                  <div className="form-group">
                    <label>템플릿 이름 *</label>
                    <input
                      type="text"
                      value={editingTemplate.name}
                      onChange={(e) => setEditingTemplate({ ...editingTemplate, name: e.target.value })}
                    />
                  </div>
                  <div className="form-group">
                    <label>카테고리</label>
                    <select
                      value={editingTemplate.category || ''}
                      onChange={(e) => setEditingTemplate({ ...editingTemplate, category: e.target.value })}
                    >
                      <option value="general">일반</option>
                      <option value="textbook">교재</option>
                      <option value="enrollment">수강</option>
                      <option value="notice">공지</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label>내용 *</label>
                    <textarea
                      value={editingTemplate.content}
                      onChange={(e) => setEditingTemplate({ ...editingTemplate, content: e.target.value })}
                      rows="8"
                      placeholder="변수: {studentName} - 학생 이름"
                    />
                  </div>
                  <div className="form-group">
                    <label>설명</label>
                    <input
                      type="text"
                      value={editingTemplate.description || ''}
                      onChange={(e) => setEditingTemplate({ ...editingTemplate, description: e.target.value })}
                    />
                  </div>
                  <div className="template-edit-actions">
                    <button
                      className="btn-primary"
                      onClick={() => {
                        if (editingTemplate.id) {
                          updateTemplateMutation.mutate({ id: editingTemplate.id, data: editingTemplate });
                        } else {
                          createTemplateMutation.mutate({ ...editingTemplate, isActive: true });
                        }
                      }}
                    >
                      {editingTemplate.id ? '수정' : '생성'}
                    </button>
                    <button className="btn-secondary" onClick={() => setEditingTemplate(null)}>
                      취소
                    </button>
                  </div>
                </div>
              ) : (
                <>
                  <div className="template-list-header">
                    <button
                      className="btn-primary btn-sm"
                      onClick={() => setEditingTemplate({ name: '', content: '', category: 'general', description: '' })}
                    >
                      <i className="fas fa-plus"></i> 새 템플릿
                    </button>
                  </div>
                  <div className="template-list">
                    {templates.map((template) => (
                      <div key={template.id} className="template-item">
                        <div className="template-info">
                          <div className="template-name">{template.name}</div>
                          <div className="template-preview">{template.content.substring(0, 50)}...</div>
                        </div>
                        <div className="template-actions">
                          <button
                            className="btn-edit"
                            onClick={() => setEditingTemplate(template)}
                          >
                            <i className="fas fa-edit"></i>
                          </button>
                          <button
                            className="btn-delete"
                            onClick={() => {
                              if (window.confirm('템플릿을 삭제하시겠습니까?')) {
                                deleteTemplateMutation.mutate(template.id);
                              }
                            }}
                          >
                            <i className="fas fa-trash"></i>
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Messages;
