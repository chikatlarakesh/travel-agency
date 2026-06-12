import { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import beachImage from '../../assets/images/beach-registration.png';
import travelbagIcon from '../../assets/icons/travelbag.svg';
import closeIcon from '../../assets/icons/Close.svg';
import { signIn } from '../../services/authService';
import { useAuth } from '../../context/AuthContext';
import { ROUTES } from '../../config/routes';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const pwRules = [
  { label: 'At least one uppercase letter required',  test: (v) => /[A-Z]/.test(v) },
  { label: 'At least one lowercase letter required',  test: (v) => /[a-z]/.test(v) },
  { label: 'At least one number required',            test: (v) => /[0-9]/.test(v) },
  { label: 'At least one special character required', test: (v) => /[^A-Za-z0-9]/.test(v) },
  { label: 'Password must be 8-16 characters long',   test: (v) => v.length >= 8 && v.length <= 16 },
];

const MAX_ATTEMPTS  = 3;
const LOCK_DURATION = 30;

const EyeOpen = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-[#677883]" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
    <path strokeLinecap="round" strokeLinejoin="round" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.477 0 8.268 2.943 9.542 7-1.274 4.057-5.065 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
  </svg>
);

const EyeOff = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-[#677883]" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
    <path strokeLinecap="round" strokeLinejoin="round" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.477 0-8.268-2.943-9.542-7a9.97 9.97 0 012.49-4.025M6.53 6.53A9.97 9.97 0 0112 5c4.477 0 8.268 2.943 9.542 7a9.97 9.97 0 01-1.357 2.614M6.53 6.53L3 3m3.53 3.53l11.94 11.94M17.47 17.47L21 21" />
  </svg>
);

const LockIcon = () => (
  <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24" style={{ flexShrink: 0 }}>
    <rect x="3" y="11" width="18" height="11" rx="2" ry="2" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
    <path strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" d="M7 11V7a5 5 0 0110 0v4"/>
  </svg>
);

const GOOGLE_OAUTH_URL = 'http://localhost:8080/oauth2/authorization/google';

const GoogleIcon = () => (
  <svg width="20" height="20" viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg" aria-hidden="true" focusable="false">
    <path fill="#EA4335" d="M24 9.5c3.14 0 5.95 1.08 8.17 2.85l6.1-6.1C34.39 3.05 29.49 1 24 1 14.82 1 7.01 6.47 3.56 14.25l7.1 5.52C12.47 13.3 17.8 9.5 24 9.5z"/>
    <path fill="#4285F4" d="M46.56 24.5c0-1.64-.15-3.22-.42-4.75H24v9h12.73c-.55 2.98-2.2 5.5-4.68 7.2l7.1 5.52C43.17 37.32 46.56 31.4 46.56 24.5z"/>
    <path fill="#FBBC05" d="M10.66 28.23A14.5 14.5 0 0 1 9.5 24c0-1.47.25-2.9.66-4.23l-7.1-5.52A23.94 23.94 0 0 0 0 24c0 3.86.92 7.51 2.56 10.75l7.1-5.52z"/>
    <path fill="#34A853" d="M24 47c5.49 0 10.1-1.82 13.46-4.93l-7.1-5.52C28.59 38.4 26.42 39.5 24 39.5c-6.2 0-11.53-3.8-13.34-9.27l-7.1 5.52C7.01 41.53 14.82 47 24 47z"/>
    <path fill="none" d="M0 0h48v48H0z"/>
  </svg>
);

const LoginPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, user } = useAuth();

  // Redirect if already authenticated
  useEffect(() => {
    if (user) {
      navigate(user.role === 'ADMIN' ? ROUTES.ADMIN_REPORTS : ROUTES.TOURS, { replace: true });
    }
  }, [user, navigate]);

  const [email, setEmail]                   = useState('');
  const [password, setPassword]             = useState('');
  const [showPassword, setShowPassword]     = useState(false);
  const [emailError, setEmailError]         = useState('');
  const [passwordError, setPasswordError]   = useState('');
  const [loading, setLoading]               = useState(false);
  const [googleLoading, setGoogleLoading]   = useState(false);
  const [failedAttempts, setFailedAttempts] = useState(0);
  const [isLocked, setIsLocked]             = useState(false);
  const [countdown, setCountdown]           = useState(0);
  // Toast type: 'registered' | 'passwordReset' | 'email-changed' | 'email-change-failed' | null
  const incomingToastType                   = location.state?.toastType || (location.state?.registered ? 'registered' : location.state?.passwordReset ? 'passwordReset' : null);
  const incomingToastMessage                = location.state?.toastMessage || '';
  const [showToast, setShowToast]           = useState(!!incomingToastType);
  const [toastLeaving, setToastLeaving]     = useState(false);
  const countdownRef                        = useRef(null);
  const toastTimerRef                       = useRef(null);
  const toastLeaveRef                       = useRef(null);

  // Auto-dismiss toast after 5s with fade-out animation
  useEffect(() => {
    if (showToast) {
      toastTimerRef.current = setTimeout(() => dismissToast(), 5000);
    }
    return () => {
      clearTimeout(toastTimerRef.current);
      clearTimeout(toastLeaveRef.current);
    };
  }, [showToast]); // eslint-disable-line

  const dismissToast = () => {
    setToastLeaving(true);
    toastLeaveRef.current = setTimeout(() => {
      setShowToast(false);
      setToastLeaving(false);
    }, 400); // matches animation duration
  };

  useEffect(() => {
    if (isLocked) {
      setCountdown(LOCK_DURATION);
      countdownRef.current = setInterval(() => {
        setCountdown((prev) => {
          if (prev <= 1) {
            clearInterval(countdownRef.current);
            setIsLocked(false);
            setFailedAttempts(0);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    }
    return () => clearInterval(countdownRef.current);
  }, [isLocked]);

  const getPasswordError = (val) => {
    for (const rule of pwRules) {
      if (!rule.test(val)) return rule.label + '.';
    }
    return null;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!EMAIL_RE.test(email.trim())) {
      setEmailError('Please enter a valid email address.');
      setPasswordError('');
      return;
    }
    setEmailError('');
    const pwdErr = getPasswordError(password);
    if (pwdErr) { setPasswordError(pwdErr); return; }
    setPasswordError('');
    setLoading(true);
    try {
      const data = await signIn(email.trim(), password);
      login(data);
      setFailedAttempts(0);
      navigate(data.role === 'ADMIN' ? ROUTES.ADMIN_REPORTS : ROUTES.TOURS_AVAILABLE, { replace: true });
    } catch {
      const newCount = failedAttempts + 1;
      setFailedAttempts(newCount);
      if (newCount >= MAX_ATTEMPTS) {
        setIsLocked(true);
        setEmailError('');
        setPasswordError('');
      } else {
        const credMsg = 'Incorrect email or password. Try again or create an account.';
        setEmailError(credMsg);
        setPasswordError(credMsg);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleEmailChange    = (e) => { setEmail(e.target.value);    setEmailError(''); };
  const handlePasswordChange = (e) => { setPassword(e.target.value); setPasswordError(''); };

  const handleEmailBlur = () => {
    if (email.trim() && !EMAIL_RE.test(email.trim()))
      setEmailError('Please enter a valid email address.');
  };

  const handlePasswordBlur = () => {
    if (password) {
      const err = getPasswordError(password);
      if (err) setPasswordError(err);
    }
  };

  const inputCls = (hasError) =>
    `h-14 w-full rounded-[8px] border bg-white px-4 text-[14px] leading-6 text-[#0B3857] outline-none placeholder:text-[#A2AEB9] 2xl:h-16 2xl:text-[16px] transition-colors ${
      hasError
        ? 'border-[#B70B0B] focus:border-[#B70B0B]'
        : 'border-[#D3E1ED] focus:border-[#027EAC]'
    }`;

  const canSubmit = email.trim() && password.trim() && !loading && !isLocked;

  const toastTitle = incomingToastType === 'email-changed' || incomingToastType === 'passwordReset'
    ? 'Success'
    : incomingToastType === 'email-change-failed'
      ? 'Error'
      : 'Congratulations';

  const toastMessage = incomingToastType === 'email-changed'
    ? 'Your email has been changed successfully. Please sign in with your updated email.'
    : incomingToastType === 'email-change-failed'
      ? (incomingToastMessage || 'Email confirmation failed. Please request a new confirmation link.')
      : incomingToastType === 'passwordReset'
        ? 'Your password has been successfully changed.'
        : 'Your account has been created successfully. Please sign in with your details.';

  return (
    <div className="min-h-screen w-full overflow-y-auto bg-[#E7F9FF] px-4 py-6 sm:px-6 lg:px-10 lg:py-6">
      <div className="mx-auto w-full max-w-[1360px] flex flex-col gap-6 lg:grid lg:grid-cols-2 lg:gap-8 xl:max-w-[1500px] 2xl:max-w-[1640px]">

        {/* ── LEFT: White form card ── */}
        <div className="flex items-center justify-center rounded-[32px] bg-white shadow-[0px_2px_10px_6px_#027EAC33]">
          <form
            onSubmit={handleSubmit}
            noValidate
            className="mx-auto flex w-full max-w-[496px] flex-col gap-10 px-5 py-12 sm:px-8 sm:py-14 lg:px-10 lg:py-16 xl:max-w-[540px] 2xl:max-w-[580px] 2xl:px-12 2xl:py-20 min-[1920px]:max-w-[720px]"
          >

            {/* Title */}
            <div className="flex flex-col">
              <p className="m-0 text-[14px] font-light uppercase leading-6 text-[#0B3857] 2xl:text-[15px] 2xl:leading-7">
                Welcome back
              </p>
              <h1 className="m-0 text-[24px] font-bold leading-10 text-[#0B3857] 2xl:text-[30px] 2xl:leading-[44px]">
                Sign in to your account
              </h1>
            </div>

            {/* Locked banner */}
            {isLocked && (
              <div className="flex items-start gap-3 rounded-[8px] bg-[#FCE9ED] px-4 py-3 text-[13px] font-medium leading-5 text-[#C0352A]">
                <LockIcon />
                <span>
                  Your account is temporarily locked due to multiple failed login attempts.{' '}
                  <strong>Try again in {countdown}s</strong>
                </span>
              </div>
            )}

            {/* Inputs */}
            <div className="flex w-full flex-col gap-4">

              {/* Email */}
              <div className="flex w-full flex-col gap-1">
                <label htmlFor="login-email" className="w-fit text-[14px] font-extrabold leading-6 text-[#0B3857] 2xl:text-[15px]">
                  Email
                </label>
                <input
                  id="login-email"
                  type="email"
                  placeholder="Enter your email address"
                  value={email}
                  onChange={handleEmailChange}
                  onBlur={handleEmailBlur}
                  autoComplete="email"
                  className={inputCls(!!emailError)}
                />
                {emailError
                  ? <p className="m-0 text-[12px] font-normal leading-4 text-[#B70B0B] 2xl:text-[13px]">{emailError}</p>
                  : <p className="m-0 h-4 text-[12px] leading-4 text-[#677883] 2xl:text-[13px]">e.g. username@domain.com</p>
                }
              </div>

              {/* Password */}
              <div className="flex w-full flex-col gap-1">
                <label htmlFor="login-password" className="w-fit text-[14px] font-extrabold leading-6 text-[#0B3857] 2xl:text-[15px]">
                  Password
                </label>
                <div className="relative">
                  <input
                    id="login-password"
                    type={showPassword ? 'text' : 'password'}
                    placeholder="Enter your password"
                    value={password}
                    onChange={handlePasswordChange}
                    onBlur={handlePasswordBlur}
                    autoComplete="current-password"
                    className={`${inputCls(!!passwordError)} pr-12`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((v) => !v)}
                    className="absolute right-4 top-1/2 -translate-y-1/2 outline-none"
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                  >
                    {showPassword ? <EyeOpen /> : <EyeOff />}
                  </button>
                </div>
                {passwordError
                  ? <p className="m-0 text-[12px] font-normal leading-4 text-[#B70B0B] 2xl:text-[13px]">{passwordError}</p>
                  : <p className="m-0 h-4 text-[12px] leading-4 text-[#677883] 2xl:text-[13px]">&nbsp;</p>
                }
                <button
                  type="button"
                  onClick={() => navigate(ROUTES.FORGOT_PASSWORD)}
                  className="w-full text-left text-[12px] leading-4 text-[#027EAC] underline decoration-solid underline-offset-2 2xl:text-[13px]"
                >
                  Forgot password?
                </button>
              </div>

            </div>

            {/* Action */}
            <div className="flex w-full flex-col gap-4">
              <button
                type="submit"
                disabled={!canSubmit}
                className={`flex h-10 w-full items-center justify-center rounded-[8px] bg-[#027EAC] px-4 py-2 2xl:h-12 ${canSubmit ? 'cursor-pointer opacity-100 transition-opacity hover:opacity-90' : 'cursor-not-allowed opacity-50'}`}
              >
                <span className="whitespace-nowrap text-[14px] font-bold leading-6 text-white 2xl:text-[15px]">
                  {loading ? 'Signing in…' : 'Sign In'}
                </span>
              </button>
              <div className="flex w-full flex-wrap items-center gap-1 text-[12px] leading-4 2xl:text-[13px]">
                <span className="text-[#0B3857]">Don&apos;t have an account?</span>
                <button
                  type="button"
                  onClick={() => navigate(ROUTES.REGISTER)}
                  className="font-bold text-[#027EAC] underline decoration-solid underline-offset-2"
                >
                  Create an account
                </button>
              </div>
            </div>

            {/* ── Social login ── */}
            <div className="flex w-full flex-col gap-4">

              {/* OR divider */}
              <div className="flex w-full items-center gap-3">
                <div className="h-px flex-1 bg-[#D3E1ED]" />
                <span className="text-[12px] font-normal leading-4 text-[#677883] 2xl:text-[13px]">OR</span>
                <div className="h-px flex-1 bg-[#D3E1ED]" />
              </div>

              {/* Continue with Google */}
              <button
                type="button"
                disabled={googleLoading}
                aria-label="Continue with Google"
                onClick={() => { setGoogleLoading(true); window.location.href = GOOGLE_OAUTH_URL; }}
                className={`flex h-10 w-full items-center justify-center gap-2 rounded-[8px] border border-[#D3E1ED] bg-white px-4 py-2 text-[14px] font-bold leading-6 text-[#0B3857] transition-colors 2xl:h-12 2xl:text-[15px] ${
                  googleLoading
                    ? 'cursor-not-allowed opacity-60'
                    : 'cursor-pointer hover:border-[#027EAC] hover:bg-[#F5FBFE] focus:outline-none focus:ring-2 focus:ring-[#027EAC] focus:ring-offset-2'
                }`}
              >
                <GoogleIcon />
                <span>{googleLoading ? 'Redirecting…' : 'Continue with Google'}</span>
              </button>

              {/* Consent text */}
              <p className="m-0 text-center text-[11px] font-normal leading-[18px] text-[#677883] 2xl:text-[12px]">
                By continuing with Google, you consent to sharing your basic profile
                information (name and email) with the application.
              </p>

            </div>

          </form>
        </div>

        {/* ── RIGHT: Image panel with overlay ── */}
        <div className="relative min-h-[320px] rounded-[32px]">
          <img src={beachImage} alt="Travel destination" className="h-full w-full rounded-[32px] object-cover object-[center_30%]" />
          {/* Logo + tagline overlay */}
          <div className="absolute inset-0 flex flex-col justify-between overflow-hidden rounded-[32px] p-10">
            <div className="flex items-center gap-3">
              <img src={travelbagIcon} alt="Travel Agency logo" style={{ width: '48px', height: '48px', flexShrink: 0 }} />
              <span style={{ color: '#FFFFFF', fontFamily: 'Nunito, sans-serif', fontWeight: 700, fontSize: '32px', lineHeight: '100%', whiteSpace: 'nowrap' }}>
                Travel Agency
              </span>
            </div>
            <p style={{ margin: 0, color: '#0B3857', fontFamily: 'Nunito, sans-serif', fontWeight: 800, fontSize: 'clamp(36px, 5vw, 64px)', lineHeight: '100%' }}>
              Let&apos;s plan<br />your next trip!
            </p>
          </div>

          {/* Toast (registration success OR password reset success) — inside image panel, top-right */}
          {showToast && (
            <div
              className="absolute right-4 top-4 z-50 w-[min(420px,calc(100%-32px))]"
              style={{
                background: '#EDFFEE',
                border: '1px solid #118819',
                borderRadius: '4px',
                padding: '12px',
                boxShadow: '0 4px 16px rgba(0,0,0,0.10)',
                animation: toastLeaving
                  ? 'toastOut 0.4s ease forwards'
                  : 'toastIn 0.4s ease forwards',
              }}
            >
              <style>{`
                @keyframes toastIn {
                  from { opacity: 0; transform: translateY(-12px); }
                  to   { opacity: 1; transform: translateY(0); }
                }
                @keyframes toastOut {
                  from { opacity: 1; transform: translateY(0); }
                  to   { opacity: 0; transform: translateY(-12px); }
                }
              `}</style>
              <div className="flex items-start gap-2">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style={{ flexShrink: 0 }}>
                  <circle cx="12" cy="12" r="10" fill="#118819" />
                  <path d="M7.5 12.5L10.5 15.5L16.5 9" stroke="#FFFFFF" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
                <div className="flex flex-1 flex-col gap-1">
                  <p className="m-0 text-[14px] font-extrabold leading-6 text-[#0B3857]">{toastTitle}</p>
                  <p className="m-0 text-[14px] font-normal leading-6 text-[#0B3857]">
                    {toastMessage}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={dismissToast}
                  className="flex-shrink-0 outline-none"
                  aria-label="Close notification"
                >
                  <img src={closeIcon} alt="Close" width={24} height={24} />
                </button>
              </div>
            </div>
          )}
        </div>

      </div>
    </div>
  );
};

export default LoginPage;
