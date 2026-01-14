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

  if (!userData) {
    return <div>사용자 정보를 불러올 수 없습니다.</div>;
  }

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto', padding: '2rem' }}>
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '2rem', color: '#1f2937', margin: '0 0 0.5rem 0' }}>마이페이지</h1>
        <p style={{ color: '#6b7280', fontSize: '1rem', margin: 0 }}>개인정보를 관리할 수 있습니다</p>
      </div>

      <div style={{ background: 'white', borderRadius: '12px', boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)', padding: '2rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem', paddingBottom: '1rem', borderBottom: '2px solid #e5e7eb' }}>
          <h2 style={{ fontSize: '1.5rem', color: '#1f2937', margin: 0 }}>개인정보</h2>
          {!isEditing && (
            <button 
              style={{ padding: '0.5rem 1rem', background: '#03C75A', color: 'white', border: 'none', borderRadius: '6px', fontSize: '0.9rem', fontWeight: 500, cursor: 'pointer' }}
              onClick={() => setIsEditing(true)}
            >
              <i className="fas fa-edit"></i> 수정
            </button>
          )}
        </div>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            <label style={{ fontWeight: 600, color: '#374151', fontSize: '0.95rem' }}>이름</label>
            {isEditing ? (
              <input
                type="text"
                name="name"
                value={formData.name}
                onChange={handleChange}
                placeholder="이름을 입력하세요"
                required
                style={{ width: '100%', padding: '0.75rem', border: '1px solid #d1d5db', borderRadius: '6px', fontSize: '1rem', boxSizing: 'border-box' }}
              />
            ) : (
              <div style={{ padding: '0.75rem', background: '#f9fafb', borderRadius: '6px', color: '#1f2937', fontSize: '1rem' }}>{userData?.name || '-'}</div>
            )}
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            <label style={{ fontWeight: 600, color: '#374151', fontSize: '0.95rem' }}>핸드폰 번호</label>
            {isEditing ? (
              <input
                type="tel"
                name="phoneNumber"
                value={formData.phoneNumber}
                onChange={handleChange}
                placeholder="010-0000-0000"
                required
                style={{ width: '100%', padding: '0.75rem', border: '1px solid #d1d5db', borderRadius: '6px', fontSize: '1rem', boxSizing: 'border-box' }}
              />
            ) : (
              <div style={{ padding: '0.75rem', background: '#f9fafb', borderRadius: '6px', color: '#1f2937', fontSize: '1rem' }}>{userData?.phoneNumber || '-'}</div>
            )}
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            <label style={{ fontWeight: 600, color: '#374151', fontSize: '0.95rem' }}>주소</label>
            {isEditing ? (
              <input
                type="text"
                name="address"
                value={formData.address}
                onChange={handleChange}
                placeholder="주소를 입력하세요"
                style={{ width: '100%', padding: '0.75rem', border: '1px solid #d1d5db', borderRadius: '6px', fontSize: '1rem', boxSizing: 'border-box' }}
              />
            ) : (
              <div style={{ padding: '0.75rem', background: '#f9fafb', borderRadius: '6px', color: '#1f2937', fontSize: '1rem' }}>{userData?.address || '-'}</div>
            )}
          </div>

          {isEditing && (
            <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end', marginTop: '1rem', paddingTop: '1rem', borderTop: '1px solid #e5e7eb' }}>
              <button 
                type="button" 
                style={{ padding: '0.75rem 1.5rem', background: 'white', color: '#6b7280', border: '1px solid #d1d5db', borderRadius: '6px', fontSize: '1rem', fontWeight: 500, cursor: 'pointer' }}
                onClick={handleCancel}
              >
                취소
              </button>
              <button 
                type="submit" 
                style={{ padding: '0.75rem 1.5rem', background: '#03C75A', color: 'white', border: 'none', borderRadius: '6px', fontSize: '1rem', fontWeight: 500, cursor: 'pointer', opacity: updateMutation.isPending ? 0.6 : 1 }}
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
