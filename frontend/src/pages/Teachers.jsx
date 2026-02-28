import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { teacherAttendanceAPI } from '../services/api';
import LoadingSpinner from '../components/LoadingSpinner';
import '../styles/Teachers.css';

function Teachers() {
  const queryClient = useQueryClient();
  const [searchKeyword, setSearchKeyword] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedTeacher, setSelectedTeacher] = useState(null);
  const [form, setForm] = useState({ name: '', phoneNumber: '', username: '', password: '', passwordConfirm: '' });

  const { data: teachers = [], isLoading } = useQuery({
    queryKey: ['teachers'],
    queryFn: async () => (await teacherAttendanceAPI.getTeachers()).data,
  });

  const registerMutation = useMutation({
    mutationFn: (data) => teacherAttendanceAPI.registerTeacher(data),
    onSuccess: (res) => {
      queryClient.invalidateQueries(['teachers']);
      closeModal();
      alert(res.data?.message || '선생님이 등록되었습니다.');
    },
    onError: (err) => alert(err.response?.data?.message || '등록 실패'),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => teacherAttendanceAPI.updateTeacher(id, data),
    onSuccess: (res) => {
      queryClient.invalidateQueries(['teachers']);
      closeModal();
      alert(res.data?.message || '수정되었습니다.');
    },
    onError: (err) => alert(err.response?.data?.message || '수정 실패'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id) => teacherAttendanceAPI.deleteTeacher(id),
    onSuccess: (res) => {
      queryClient.invalidateQueries(['teachers']);
      closeModal();
      alert(res.data?.message || '삭제되었습니다.');
    },
    onError: (err) => alert(err.response?.data?.message || '삭제 실패'),
  });

  const closeModal = () => {
    setShowCreateModal(false);
    setShowEditModal(false);
    setSelectedTeacher(null);
    setForm({ name: '', phoneNumber: '', username: '', password: '', passwordConfirm: '' });
  };

  const formatPhone = (val) => {
    const v = val.replace(/[^0-9]/g, '');
    if (v.length > 7) return v.slice(0,3)+'-'+v.slice(3,7)+'-'+v.slice(7,11);
    if (v.length > 3) return v.slice(0,3)+'-'+v.slice(3);
    return v;
  };

  const openEdit = (t) => {
    setSelectedTeacher(t);
    setForm({ name: t.name, phoneNumber: t.phoneNumber || '', username: t.username || '', password: '', passwordConfirm: '' });
    setShowEditModal(true);
  };

  const handleSubmit = () => {
    if (!form.name) return alert('이름은 필수입니다.');
    if (form.password && form.password !== form.passwordConfirm) return alert('비밀번호가 일치하지 않습니다.');

    if (showCreateModal) {
      if (!form.username || !form.password) return alert('아이디, 비밀번호는 필수입니다.');
      registerMutation.mutate(form);
    } else {
      const data = { name: form.name, phoneNumber: form.phoneNumber };
      if (form.password) data.password = form.password;
      updateMutation.mutate({ id: selectedTeacher.id, data });
    }
  };

  const filtered = teachers.filter(
    (t) => t.name?.includes(searchKeyword) || t.phoneNumber?.includes(searchKeyword)
  );

  if (isLoading) return <LoadingSpinner />;

  return (
    <div className="tch-page">
      <div className="tch-header">
        <div className="tch-header-content">
          <div>
            <h1 className="tch-title"><i className="fas fa-user-tie"></i>선생님 관리</h1>
            <p className="tch-subtitle">총 {teachers.length}명의 선생님</p>
          </div>
          <button className="tch-add-btn" onClick={() => setShowCreateModal(true)}>
            <i className="fas fa-plus"></i> 선생님 등록
          </button>
        </div>
      </div>

      <div className="tch-content">
        <div className="tch-search-section">
          <div className="tch-search-box">
            <i className="fas fa-search"></i>
            <input type="text" placeholder="이름 또는 전화번호로 검색..." value={searchKeyword} onChange={(e) => setSearchKeyword(e.target.value)} />
          </div>
        </div>

        <div className="tch-table-wrap">
          <table className="tch-table">
            <thead>
              <tr>
                <th>이름</th>
                <th>아이디</th>
                <th>전화번호</th>
                <th style={{ width: 100 }}>관리</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr><td colSpan="4" className="tch-empty">등록된 선생님이 없습니다.</td></tr>
              ) : (
                filtered.map((t) => (
                  <tr key={t.id}>
                    <td>{t.name}</td>
                    <td>{t.username || '-'}</td>
                    <td>{t.phoneNumber || '-'}</td>
                    <td>
                      <div className="tch-action-btns">
                        <button className="tch-edit-btn" onClick={() => openEdit(t)} title="수정">
                          <i className="fas fa-pen"></i>
                        </button>
                        <button className="tch-delete-btn" onClick={() => {
                          if (window.confirm(`${t.name} 선생님을 삭제하시겠습니까?`)) deleteMutation.mutate(t.id);
                        }} title="삭제">
                          <i className="fas fa-trash"></i>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {(showCreateModal || showEditModal) && (
        <div className="tch-modal-overlay">
          <div className="tch-modal" onClick={(e) => e.stopPropagation()}>
            <div className="tch-modal-header">
              <h2>{showCreateModal ? '선생님 등록' : '선생님 수정'}</h2>
              <button className="tch-modal-close" onClick={closeModal}><i className="fas fa-times"></i></button>
            </div>
            <div className="tch-modal-body">
              <div className="tch-form-group">
                <label>이름 <span style={{ color: '#ef4444' }}>*</span></label>
                <input type="text" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="이름" />
              </div>
              <div className="tch-form-group">
                <label>전화번호</label>
                <input type="text" value={form.phoneNumber} onChange={(e) => setForm({ ...form, phoneNumber: formatPhone(e.target.value) })} placeholder="010-0000-0000" maxLength="13" />
              </div>
              {showCreateModal && (
                <div className="tch-form-group">
                  <label>아이디 <span style={{ color: '#ef4444' }}>*</span></label>
                  <input type="text" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} placeholder="로그인 아이디" />
                </div>
              )}
              <div className="tch-form-group">
                <label>비밀번호 {showCreateModal && <span style={{ color: '#ef4444' }}>*</span>}</label>
                <input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} placeholder={showEditModal ? '변경 시에만 입력' : '영문+숫자 8자 이상'} />
              </div>
              <div className="tch-form-group">
                <label>비밀번호 확인 {showCreateModal && <span style={{ color: '#ef4444' }}>*</span>}</label>
                <input type="password" value={form.passwordConfirm} onChange={(e) => setForm({ ...form, passwordConfirm: e.target.value })} placeholder="비밀번호를 다시 입력하세요"
                  className={form.passwordConfirm && form.password !== form.passwordConfirm ? 'tch-input-error' : ''} />
                {form.passwordConfirm && form.password !== form.passwordConfirm && (
                  <span className="tch-error-msg">비밀번호가 일치하지 않습니다</span>
                )}
              </div>
            </div>
            <div className="tch-modal-footer">
              <button className="tch-btn-cancel" onClick={closeModal}>취소</button>
              <button className="tch-btn-save" onClick={handleSubmit}
                disabled={registerMutation.isPending || updateMutation.isPending}>
                {showCreateModal ? '등록' : '저장'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Teachers;
