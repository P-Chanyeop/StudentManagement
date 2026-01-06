import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useNavigate, Link } from 'react-router-dom';
import { authAPI } from '../services/api';
import '../styles/Register.css';

function Register() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    // 학부모 정보
    username: '',
    password: '',
    confirmPassword: '',
    parentName: '',
    parentPhone: '',
    address: '',
    
    // 학생 정보 (배열로 변경)
    students: [{
      studentName: '',
      studentPhone: '',
      birthDate: '',
      gender: 'MALE',
      school: '',
      grade: '1'
    }]
  });

  const [errors, setErrors] = useState({});
  const [isUsernameChecked, setIsUsernameChecked] = useState(false);
  const [isUsernameAvailable, setIsUsernameAvailable] = useState(false);

  // 아이디 중복 검사
  const checkUsername = useMutation({
    mutationFn: (username) => authAPI.checkUsername(username),
    onSuccess: (response) => {
      setIsUsernameAvailable(response.data.available);
      setIsUsernameChecked(true);
      if (response.data.available) {
        alert('사용 가능한 아이디입니다.');
      } else {
        alert('이미 사용 중인 아이디입니다.');
      }
    },
    onError: () => {
      alert('아이디 중복 검사 중 오류가 발생했습니다.');
    }
  });

  // 회원가입 mutation
  const registerMutation = useMutation({
    mutationFn: (data) => authAPI.register(data),
    onSuccess: () => {
      alert('회원가입이 완료되었습니다. 로그인해주세요.');
      navigate('/login');
    },
    onError: (error) => {
      alert(`회원가입 실패: ${error.response?.data?.message || '오류가 발생했습니다.'}`);
    }
  });

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    
    // 아이디가 변경되면 중복 검사 초기화
    if (name === 'username') {
      setIsUsernameChecked(false);
      setIsUsernameAvailable(false);
    }
    
    // 에러 제거
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
  };

  // 학생 정보 변경 핸들러
  const handleStudentChange = (index, field, value) => {
    setFormData(prev => ({
      ...prev,
      students: prev.students.map((student, i) => 
        i === index ? { ...student, [field]: value } : student
      )
    }));
  };

  // 학생 추가
  const addStudent = () => {
    setFormData(prev => ({
      ...prev,
      students: [...prev.students, {
        studentName: '',
        studentPhone: '',
        birthDate: '',
        gender: 'MALE',
        school: '',
        grade: '1'
      }]
    }));
  };

  // 학생 제거
  const removeStudent = (index) => {
    if (formData.students.length > 1) {
      setFormData(prev => ({
        ...prev,
        students: prev.students.filter((_, i) => i !== index)
      }));
    }
  };

  // 아이디 중복 검사
  const handleCheckUsername = () => {
    if (!formData.username.trim()) {
      alert('아이디를 입력해주세요.');
      return;
    }
    if (formData.username.length < 4) {
      alert('아이디는 4글자 이상이어야 합니다.');
      return;
    }
    checkUsername.mutate(formData.username);
  };

  const validateForm = () => {
    const newErrors = {};
    
    // 아이디 중복 검사 확인
    if (!isUsernameChecked || !isUsernameAvailable) {
      newErrors.username = '아이디 중복 검사를 완료해주세요.';
    }
    
    // 학부모 정보 검증
    if (!formData.username.trim()) {
      newErrors.username = '아이디를 입력해주세요.';
    } else if (formData.username.length < 4) {
      newErrors.username = '아이디는 4글자 이상이어야 합니다.';
    }
    
    if (!formData.password) {
      newErrors.password = '비밀번호를 입력해주세요.';
    } else if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/.test(formData.password)) {
      newErrors.password = '비밀번호는 영어와 숫자를 포함하여 8글자 이상이어야 합니다.';
    }
    
    if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = '비밀번호가 일치하지 않습니다.';
    }
    
    if (!formData.parentName.trim()) {
      newErrors.parentName = '학부모 이름을 입력해주세요.';
    }
    
    if (!formData.parentPhone.trim()) {
      newErrors.parentPhone = '연락처를 입력해주세요.';
    } else if (!/^010-\d{4}-\d{4}$/.test(formData.parentPhone)) {
      newErrors.parentPhone = '올바른 연락처 형식을 입력해주세요. (010-0000-0000)';
    }
    
    if (!formData.address.trim()) {
      newErrors.address = '주소를 입력해주세요.';
    }
    
    // 학생 정보 검증
    formData.students.forEach((student, index) => {
      if (!student.studentName.trim()) {
        newErrors[`student_${index}_name`] = '학생 이름을 입력해주세요.';
      }
      if (!student.birthDate) {
        newErrors[`student_${index}_birthDate`] = '생년월일을 선택해주세요.';
      }
      if (!student.school.trim()) {
        newErrors[`student_${index}_school`] = '학교를 입력해주세요.';
      }
    });
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    const registerData = {
      // 사용자 계정 정보
      username: formData.username,
      password: formData.password,
      name: formData.parentName,
      phoneNumber: formData.parentPhone,
      address: formData.address,
      role: 'PARENT',
      
      // 학생 정보
      student: {
        studentName: formData.studentName,
        studentPhone: formData.studentPhone,
        birthDate: formData.birthDate,
        gender: formData.gender,
        school: formData.school,
        grade: formData.grade,
        englishLevel: '1.0', // 기본값
        parentName: formData.parentName,
        parentPhone: formData.parentPhone
      }
    };

    registerMutation.mutate(registerData);
  };

  return (
    <div className="register-container">
      <div className="register-card">
        <div className="register-header">
          <h1>회원가입</h1>
          <p>학부모 계정으로 가입하여 자녀의 수업을 관리하세요</p>
        </div>

        <form onSubmit={handleSubmit} className="register-form">
          {/* 학부모 정보 */}
          <div className="form-section">
            <h2>학부모 정보</h2>
            
            <div className="form-group">
              <label htmlFor="username">아이디 *</label>
              <div className="username-check-container">
                <input
                  type="text"
                  id="username"
                  name="username"
                  value={formData.username}
                  onChange={handleInputChange}
                  placeholder="4글자 이상 입력하세요"
                  className={errors.username ? 'error' : ''}
                />
                <button
                  type="button"
                  className="btn-check"
                  onClick={handleCheckUsername}
                  disabled={checkUsername.isLoading}
                >
                  {checkUsername.isLoading ? '확인중...' : '중복확인'}
                </button>
              </div>
              {isUsernameChecked && (
                <span className={`check-message ${isUsernameAvailable ? 'success' : 'error'}`}>
                  {isUsernameAvailable ? '사용 가능한 아이디입니다.' : '이미 사용 중인 아이디입니다.'}
                </span>
              )}
              {errors.username && <span className="error-message">{errors.username}</span>}
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="password">비밀번호 *</label>
                <input
                  type="password"
                  id="password"
                  name="password"
                  value={formData.password}
                  onChange={handleInputChange}
                  placeholder="영어+숫자 8글자 이상"
                  className={errors.password ? 'error' : ''}
                />
                {errors.password && <span className="error-message">{errors.password}</span>}
              </div>

              <div className="form-group">
                <label htmlFor="confirmPassword">비밀번호 확인 *</label>
                <input
                  type="password"
                  id="confirmPassword"
                  name="confirmPassword"
                  value={formData.confirmPassword}
                  onChange={(e) => {
                    const value = e.target.value;
                    setFormData(prev => ({...prev, confirmPassword: value}));
                    
                    // 실시간 비밀번호 일치 확인
                    if (value && formData.password && value !== formData.password) {
                      setErrors(prev => ({...prev, confirmPassword: '비밀번호가 일치하지 않습니다.'}));
                    } else {
                      setErrors(prev => ({...prev, confirmPassword: ''}));
                    }
                  }}
                  placeholder="비밀번호를 다시 입력하세요"
                  className={errors.confirmPassword ? 'error' : ''}
                />
                {errors.confirmPassword && <span className="error-message">{errors.confirmPassword}</span>}
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label htmlFor="parentName">학부모 이름 *</label>
                <input
                  type="text"
                  id="parentName"
                  name="parentName"
                  value={formData.parentName}
                  onChange={handleInputChange}
                  placeholder="학부모 이름을 입력하세요"
                  className={errors.parentName ? 'error' : ''}
                />
                {errors.parentName && <span className="error-message">{errors.parentName}</span>}
              </div>

              <div className="form-group">
                <label htmlFor="parentPhone">연락처 *</label>
                <input
                  type="tel"
                  id="parentPhone"
                  name="parentPhone"
                  value={formData.parentPhone}
                  onChange={(e) => {
                    const value = e.target.value.replace(/[^0-9]/g, '');
                    let formatted = value;
                    if (value.length >= 3) {
                      formatted = value.slice(0, 3) + '-' + value.slice(3);
                    }
                    if (value.length >= 7) {
                      formatted = value.slice(0, 3) + '-' + value.slice(3, 7) + '-' + value.slice(7, 11);
                    }
                    setFormData(prev => ({...prev, parentPhone: formatted}));
                  }}
                  placeholder="010-0000-0000"
                  maxLength="13"
                  className={errors.parentPhone ? 'error' : ''}
                />
                {errors.parentPhone && <span className="error-message">{errors.parentPhone}</span>}
              </div>
            </div>

            <div className="form-group">
              <label htmlFor="address">주소 *</label>
              <input
                type="text"
                id="address"
                name="address"
                value={formData.address}
                onChange={handleInputChange}
                placeholder="주소를 입력하세요"
                className={errors.address ? 'error' : ''}
              />
              {errors.address && <span className="error-message">{errors.address}</span>}
            </div>
          </div>

          {/* 학생 정보 */}
          <div className="form-section">
            <div className="students-header">
              <h2>학생 정보</h2>
              <button type="button" className="btn-add-student" onClick={addStudent}>
                <i className="fas fa-plus"></i> 학생 추가
              </button>
            </div>
            
            {formData.students.map((student, index) => (
              <div key={index} className="student-form">
                <div className="student-header">
                  <h3>학생 {index + 1}</h3>
                  {formData.students.length > 1 && (
                    <button 
                      type="button" 
                      className="btn-remove-student"
                      onClick={() => removeStudent(index)}
                    >
                      <i className="fas fa-times"></i>
                    </button>
                  )}
                </div>
                
                <div className="form-row">
                  <div className="form-group">
                    <label>학생 이름 *</label>
                    <input
                      type="text"
                      value={student.studentName}
                      onChange={(e) => handleStudentChange(index, 'studentName', e.target.value)}
                      placeholder="학생 이름을 입력하세요"
                      className={errors[`student_${index}_name`] ? 'error' : ''}
                    />
                    {errors[`student_${index}_name`] && <span className="error-message">{errors[`student_${index}_name`]}</span>}
                  </div>

                  <div className="form-group">
                    <label>학생 연락처</label>
                    <input
                      type="tel"
                      value={student.studentPhone}
                      onChange={(e) => {
                        const value = e.target.value.replace(/[^0-9]/g, '');
                        let formatted = value;
                        if (value.length >= 3) {
                          formatted = value.slice(0, 3) + '-' + value.slice(3);
                        }
                        if (value.length >= 7) {
                          formatted = value.slice(0, 3) + '-' + value.slice(3, 7) + '-' + value.slice(7, 11);
                        }
                        handleStudentChange(index, 'studentPhone', formatted);
                      }}
                      placeholder="010-0000-0000"
                      maxLength="13"
                    />
                  </div>
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>생년월일 *</label>
                    <div className="birth-date-inputs">
                      <select
                        value={student.birthDate.split('-')[0] || ''}
                        onChange={(e) => {
                          const year = e.target.value;
                          const month = student.birthDate.split('-')[1] || '01';
                          const day = student.birthDate.split('-')[2] || '01';
                          handleStudentChange(index, 'birthDate', `${year}-${month}-${day}`);
                        }}
                        className={errors[`student_${index}_birthDate`] ? 'error' : ''}
                      >
                        <option value="">년도</option>
                        {Array.from({length: 20}, (_, i) => {
                          const year = new Date().getFullYear() - 5 - i;
                          return <option key={year} value={year}>{year}년</option>;
                        })}
                      </select>
                      
                      <select
                        value={student.birthDate.split('-')[1] || ''}
                        onChange={(e) => {
                          const year = student.birthDate.split('-')[0] || '';
                          const month = e.target.value.padStart(2, '0');
                          const day = student.birthDate.split('-')[2] || '01';
                          handleStudentChange(index, 'birthDate', `${year}-${month}-${day}`);
                        }}
                        className={errors[`student_${index}_birthDate`] ? 'error' : ''}
                      >
                        <option value="">월</option>
                        {Array.from({length: 12}, (_, i) => {
                          const month = (i + 1).toString().padStart(2, '0');
                          return <option key={month} value={month}>{i + 1}월</option>;
                        })}
                      </select>
                      
                      <select
                        value={student.birthDate.split('-')[2] || ''}
                        onChange={(e) => {
                          const year = student.birthDate.split('-')[0] || '';
                          const month = student.birthDate.split('-')[1] || '01';
                          const day = e.target.value.padStart(2, '0');
                          handleStudentChange(index, 'birthDate', `${year}-${month}-${day}`);
                        }}
                        className={errors[`student_${index}_birthDate`] ? 'error' : ''}
                      >
                        <option value="">일</option>
                        {Array.from({length: 31}, (_, i) => {
                          const day = (i + 1).toString().padStart(2, '0');
                          return <option key={day} value={day}>{i + 1}일</option>;
                        })}
                      </select>
                    </div>
                    {errors[`student_${index}_birthDate`] && <span className="error-message">{errors[`student_${index}_birthDate`]}</span>}
                  </div>

                  <div className="form-group">
                    <label>성별 *</label>
                    <select
                      value={student.gender}
                      onChange={(e) => handleStudentChange(index, 'gender', e.target.value)}
                    >
                      <option value="MALE">남성</option>
                      <option value="FEMALE">여성</option>
                    </select>
                  </div>
                </div>

                <div className="form-row">
                  <div className="form-group">
                    <label>학교 *</label>
                    <input
                      type="text"
                      value={student.school}
                      onChange={(e) => handleStudentChange(index, 'school', e.target.value)}
                      placeholder="학교명을 입력하세요"
                      className={errors[`student_${index}_school`] ? 'error' : ''}
                    />
                    {errors[`student_${index}_school`] && <span className="error-message">{errors[`student_${index}_school`]}</span>}
                  </div>

                  <div className="form-group">
                    <label>학년 *</label>
                    <select
                      value={student.grade}
                      onChange={(e) => handleStudentChange(index, 'grade', e.target.value)}
                    >
                      {[1,2,3,4,5,6].map(grade => (
                        <option key={grade} value={grade}>{grade}학년</option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* 제출 버튼 */}
          <div className="form-actions">
            <button 
              type="submit" 
              className="btn-primary"
              disabled={registerMutation.isLoading}
            >
              {registerMutation.isLoading ? '가입 중...' : '회원가입'}
            </button>
            
            <Link to="/login" className="btn-secondary">
              로그인으로 돌아가기
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
}

export default Register;
