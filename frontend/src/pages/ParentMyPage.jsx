import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { authAPI } from '../services/api';
import LoadingSpinner from '../components/LoadingSpinner';
import '../styles/ParentMyPage.css';

function ParentMyPage() {
  const queryClient = useQueryClient();
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    phoneNumber: '',
    address: ''
  });

  // 현재 사용자 정보 조회
  const { data: userData, isLoading } = useQuery({
    queryKey: ['currentUser'],
    queryFn: async () => {
      const response = await authAPI.getCurrentUser();
      setFormData({
        name: response.data.name || '',
        phoneNumber: response.data.phoneNumber || '',
        address: response.data.address || ''
      });
      return response.data;
    },
  });

  // 정보 수정 mutation
  const updateMutation = useMutation({
    mutationFn: async (data) => {
      const response = await authAPI.updateProfile(data);
      return response.data;
    },
    onSuccess: () => {
      alert('정보가 수정되었습니다.');
      setIsEditing(false);
      queryClient.invalidateQueries(['currentUser']);
    },
    onError: (error) => {
      alert(error.response?.data?.message || '정보 수정에 실패했습니다.');
    }
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!formData.name.trim()) {
      alert('이름을 입력해주세요.');
      return;
    }
    if (!formData.phoneNumber.trim()) {
      alert('핸드폰 번호를 입력해주세요.');
      return;
    }
    updateMutation.mutate(formData);
  };

  const handleCancel = () => {
    setFormData({
      name: userData.name || '',
      phoneNumber: userData.phoneNumber || '',
      address: userData.address || ''
    });
    setIsEditing(false);
  };

  if (isLoading) {
    return <LoadingSpinner />;
  }

  return (
    <div className="parent-mypage">
      <div className="page-header">
        <h1>마이페이지</h1>
        <p>개인정보를 관리할 수 있습니다</p>
      </div>

      <div className="profile-card">
        <div className="card-header">
          <h2>개인정보</h2>
          {!isEditing && (
            <button 
              className="edit-btn"
              onClick={() => setIsEditing(true)}
            >
              <i className="fas fa-edit"></i> 수정
            </button>
          )}
        </div>

        <form onSubmit={handleSubmit} className="profile-form">
          <div className="form-group">
            <label>이름</label>
            {isEditing ? (
              <input
                type="text"
                name="name"
                value={formData.name}
                onChange={handleChange}
                placeholder="이름을 입력하세요"
                required
              />
            ) : (
              <div className="form-value">{userData.name}</div>
            )}
          </div>

          <div className="form-group">
            <label>핸드폰 번호</label>
            {isEditing ? (
              <input
                type="tel"
                name="phoneNumber"
                value={formData.phoneNumber}
                onChange={handleChange}
                placeholder="010-0000-0000"
                required
              />
            ) : (
              <div className="form-value">{userData.phoneNumber}</div>
            )}
          </div>

          <div className="form-group">
            <label>주소</label>
            {isEditing ? (
              <input
                type="text"
                name="address"
                value={formData.address}
                onChange={handleChange}
                placeholder="주소를 입력하세요"
              />
            ) : (
              <div className="form-value">{userData.address || '-'}</div>
            )}
          </div>

          {isEditing && (
            <div className="form-actions">
              <button 
                type="button" 
                className="cancel-btn"
                onClick={handleCancel}
              >
                취소
              </button>
              <button 
                type="submit" 
                className="save-btn"
                disabled={updateMutation.isPending}
              >
                {updateMutation.isPending ? '저장 중...' : '저장'}
              </button>
            </div>
          )}
        </form>
      </div>
    </div>
  );
}

export default ParentMyPage;
