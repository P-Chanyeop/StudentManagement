import { useState, useEffect, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authAPI } from '../services/api';
import '../styles/Login.css';

function ForgotPassword() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1); // 1: 발송, 2: 인증, 3: 비밀번호 변경
  const [formData, setFormData] = useState({
    username: '',
    phoneNumber: '',
    code: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [resetToken, setResetToken] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [timer, setTimer] = useState(0);
  const [cooldown, setCooldown] = useState(0);

  // 인증번호 타이머
  useEffect(() => {
    if (timer <= 0) return;
    const interval = setInterval(() => setTimer(t => t - 1), 1000);
    return () => clearInterval(interval);
  }, [timer]);

  // 재발송 쿨다운 타이머
  useEffect(() => {
    if (cooldown <= 0) return;
    const interval = setInterval(() => setCooldown(c => c - 1), 1000);
    return () => clearInterval(interval);
  }, [cooldown]);

  const formatTime = (seconds) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setError('');
  };

  // 1단계: 인증번호 발송
  const handleSendCode = useCallback(async () => {
    if (!formData.username || !formData.phoneNumber) {
      setError('아이디와 휴대폰번호를 입력해주세요');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const res = await authAPI.passwordResetSendCode({
        username: formData.username,
        phoneNumber: formData.phoneNumber,
      });
      setTimer(res.data.expiresIn);
      setCooldown(60);
      setStep(2);
      setSuccess('인증번호가 발송되었습니다');
    } catch (err) {
      setError(err.response?.data?.message || '인증번호 발송에 실패했습니다');
    } finally {
      setLoading(false);
    }
  }, [formData.username, formData.phoneNumber]);

  // 2단계: 인증번호 검증
  const handleVerifyCode = useCallback(async () => {
    if (!formData.code) {
      setError('인증번호를 입력해주세요');
      return;
    }
    setLoading(true);
    setError('');
    setSuccess('');
    try {
      const res = await authAPI.passwordResetVerifyCode({
        username: formData.username,
        phoneNumber: formData.phoneNumber,
        code: formData.code,
      });
      setResetToken(res.data.resetToken);
      setStep(3);
      setTimer(res.data.expiresIn);
      setSuccess('인증이 완료되었습니다. 새 비밀번호를 입력해주세요');
    } catch (err) {
      setError(err.response?.data?.message || '인증에 실패했습니다');
    } finally {
      setLoading(false);
    }
  }, [formData]);

  // 3단계: 비밀번호 변경
  const handleResetPassword = useCallback(async () => {
    if (formData.newPassword !== formData.confirmPassword) {
      setError('비밀번호가 일치하지 않습니다');
      return;
    }
    setLoading(true);
    setError('');
    setSuccess('');
    try {
      await authAPI.passwordReset({
        resetToken,
        newPassword: formData.newPassword,
      });
      alert('비밀번호가 변경되었습니다. 로그인해주세요.');
      navigate('/login');
    } catch (err) {
      setError(err.response?.data?.message || '비밀번호 변경에 실패했습니다');
    } finally {
      setLoading(false);
    }
  }, [formData, resetToken, navigate]);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (step === 1) handleSendCode();
    else if (step === 2) handleVerifyCode();
    else if (step === 3) handleResetPassword();
  };

  return (
    <div className="login-container">
      <div className="login-box">
        <div className="login-header">
          <div className="logo">
            <div className="logo-icon"><i className="fas fa-key"></i></div>
            <h1>비밀번호 찾기</h1>
          </div>
          <p className="subtitle">
            {step === 1 && '아이디와 등록된 휴대폰번호를 입력해주세요'}
            {step === 2 && '발송된 인증번호를 입력해주세요'}
            {step === 3 && '새 비밀번호를 입력해주세요'}
          </p>
        </div>

        <form onSubmit={handleSubmit} className="login-form">
          {/* 1단계: 아이디 + 휴대폰번호 */}
          {step >= 1 && (
            <>
              <div className="form-group">
                <label htmlFor="username">아이디</label>
                <input
                  type="text"
                  id="username"
                  name="username"
                  value={formData.username}
                  onChange={handleChange}
                  placeholder="아이디를 입력하세요"
                  disabled={step > 1}
                  required
                  autoFocus
                />
              </div>
              <div className="form-group">
                <label htmlFor="phoneNumber">휴대폰번호</label>
                <input
                  type="tel"
                  id="phoneNumber"
                  name="phoneNumber"
                  value={formData.phoneNumber}
                  onChange={handleChange}
                  placeholder="'-' 없이 번호만 입력 (01012345678)"
                  disabled={step > 1}
                  required
                />
              </div>
            </>
          )}

          {/* 2단계: 인증번호 입력 */}
          {step === 2 && (
            <div className="form-group">
              <label htmlFor="code">
                인증번호
                {timer > 0 && (
                  <span style={{ color: timer <= 60 ? '#e53e3e' : '#03C75A', marginLeft: 8, fontWeight: 600 }}>
                    {formatTime(timer)}
                  </span>
                )}
                {timer <= 0 && (
                  <span style={{ color: '#e53e3e', marginLeft: 8 }}>만료됨</span>
                )}
              </label>
              <input
                type="text"
                id="code"
                name="code"
                value={formData.code}
                onChange={handleChange}
                placeholder="6자리 인증번호"
                maxLength={6}
                required
                autoFocus
              />
              <button
                type="button"
                className="resend-button"
                onClick={handleSendCode}
                disabled={cooldown > 0 || loading}
              >
                {cooldown > 0 ? `재발송 (${cooldown}초)` : '인증번호 재발송'}
              </button>
            </div>
          )}

          {/* 3단계: 새 비밀번호 */}
          {step === 3 && (
            <>
              {timer > 0 && (
                <div style={{ textAlign: 'center', color: timer <= 60 ? '#e53e3e' : '#03C75A', fontWeight: 600, marginBottom: 8 }}>
                  남은 시간: {formatTime(timer)}
                </div>
              )}
              <div className="form-group">
                <label htmlFor="newPassword">새 비밀번호</label>
                <input
                  type="password"
                  id="newPassword"
                  name="newPassword"
                  value={formData.newPassword}
                  onChange={handleChange}
                  placeholder="영어 + 숫자 포함 8자 이상"
                  required
                  autoFocus
                />
              </div>
              <div className="form-group">
                <label htmlFor="confirmPassword">비밀번호 확인</label>
                <input
                  type="password"
                  id="confirmPassword"
                  name="confirmPassword"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  placeholder="비밀번호를 다시 입력하세요"
                  required
                />
              </div>
            </>
          )}

          {error && <div className="error-message">{error}</div>}
          {success && (
            <div style={{
              background: '#F0FFF4', border: '1px solid #C6F6D5', color: '#276749',
              padding: '12px 16px', borderRadius: 8, fontSize: 14
            }}>
              {success}
            </div>
          )}

          <button type="submit" className="login-button" disabled={loading || (step === 2 && timer <= 0)}>
            {loading ? '처리 중...' :
              step === 1 ? '인증번호 발송' :
              step === 2 ? '인증 확인' : '비밀번호 변경'}
          </button>
        </form>

        <div className="login-links">
          <Link to="/login" className="register-link">
            로그인으로 돌아가기
          </Link>
        </div>
      </div>
    </div>
  );
}

export default ForgotPassword;
