import { useState, useEffect, useRef, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import beachImage from '../../assets/images/beach-registration.png';
import travelbagIcon from '../../assets/icons/travelbag.svg';
import { ROUTES } from '../../config/routes';
import {
  initiateRegistration,
  verifyRegistrationCode,
  completeRegistration,
} from '../../services/authService';
import { useAuth } from '../../context/AuthContext';
import CaptchaWidget from '../../components/CaptchaWidget/CaptchaWidget';

// ---------------------------------------------------------------------------
// Validation helpers
// ---------------------------------------------------------------------------

const NAME_RE = /^[A-Za-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u00FF'\\-]{1,50}$/;
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const pwRules = [
  { label: 'At least one uppercase letter required',  test: (v) => /[A-Z]/.test(v) },
  { label: 'At least one lowercase letter required',  test: (v) => /[a-z]/.test(v) },
  { label: 'At least one number required',            test: (v) => /[0-9]/.test(v) },
  { label: 'At least one special character required', test: (v) => /[^A-Za-z0-9]/.test(v) },
  { label: 'Password must be 8-16 characters long',   test: (v) => v.length >= 8 && v.length <= 16 },
];

function validateStep1(fields) {
  const errors = {};
  if (fields.firstName && !NAME_RE.test(fields.firstName))
    errors.firstName = 'First name must be up to 50 characters. Only Latin letters, hyphens, and apostrophes are allowed.';
  if (fields.lastName && !NAME_RE.test(fields.lastName))
    errors.lastName = 'Last name must be up to 50 characters. Only Latin letters, hyphens, and apostrophes are allowed.';
  if (fields.email && !EMAIL_RE.test(fields.email))
    errors.email = 'Invalid email address. Please ensure it follows the format: username@domain.com';
  return errors;
}

function validateStep3(fields) {
  const errors = {};
  if (fields.confirmPassword && fields.password && fields.confirmPassword !== fields.password)
    errors.confirmPassword = "Passwords don't match.";
  return errors;
}

// OTP expiry duration in seconds (matches back-end 15 min)
const OTP_EXPIRY_SECONDS = 15 * 60;

// ---------------------------------------------------------------------------
// Main component
// ---------------------------------------------------------------------------

const Registration = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  useEffect(() => {
    if (user) navigate(ROUTES.TOURS, { replace: true });
  }, [user, navigate]);

  // step: 1 = personal info, 2 = email verification, 3 = credentials
  const [step, setStep] = useState(1);

  const [fields1, setFields1] = useState({ firstName: '', lastName: '', email: '' });
  const [otp, setOtp] = useState('');
  const [countdown, setCountdown] = useState(OTP_EXPIRY_SECONDS);
  const countdownRef = useRef(null);
  const [fields3, setFields3] = useState({ password: '', confirmPassword: '' });

  // Captcha state (used in Step 1)
  const [captchaId, setCaptchaId] = useState('');
  const [captchaAnswer, setCaptchaAnswer] = useState('');
  const [captchaRefreshTrigger, setCaptchaRefreshTrigger] = useState(0);
  const [captchaError, setCaptchaError] = useState('');
  const [showPw, setShowPw] = useState(false);
  const [showCpw, setShowCpw] = useState(false);
  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState('');

  // ---------------------------------------------------------------------------
  // Countdown timer (step 2)
  // ---------------------------------------------------------------------------

  const handleCaptchaLoaded = useCallback((newId) => {
    setCaptchaId(newId);
  }, []);

  const handleCaptchaAnswerChange = useCallback((answer) => {
    setCaptchaAnswer(answer);
    if (answer) setCaptchaError('');
  }, []);

  const startCountdown = useCallback(() => {
    setCountdown(OTP_EXPIRY_SECONDS);
    clearInterval(countdownRef.current);
    countdownRef.current = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) { clearInterval(countdownRef.current); return 0; }
        return prev - 1;
      });
    }, 1000);
  }, []);

  useEffect(() => () => clearInterval(countdownRef.current), []);

  const formatCountdown = (secs) => {
    const m = String(Math.floor(secs / 60)).padStart(2, '0');
    const s = String(secs % 60).padStart(2, '0');
    return `${m}:${s}`;
  };

  // ---------------------------------------------------------------------------
  // Style helpers
  // ---------------------------------------------------------------------------

  const inputCls = (hasError) =>
    `h-14 w-full rounded-[8px] border bg-white px-4 text-[14px] leading-6 text-[#0B3857] outline-none placeholder:text-[#A2AEB9] 2xl:h-16 2xl:text-[16px] ${
      hasError ? 'border-[#B70B0B] focus:border-[#B70B0B]' : 'border-[#D3E1ED] focus:border-[#027EAC]'
    }`;

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

  // ---------------------------------------------------------------------------
  // Step 1 handlers
  // ---------------------------------------------------------------------------

  const errors1 = validateStep1(fields1);
  const step1Filled = Object.values(fields1).every((v) => v.trim() !== '');
  const step1Valid = step1Filled && Object.keys(errors1).length === 0 && captchaId !== '' && captchaAnswer.trim() !== '';

  const handleChange1 = (e) => {
    const { id, value } = e.target;
    setFields1((prev) => ({ ...prev, [id]: value }));
    setApiError('');
  };

  const handleStep1Submit = async () => {
    if (!step1Valid || loading) return;
    setLoading(true);
    setApiError('');
    setCaptchaError('');
    try {
      await initiateRegistration(fields1.firstName, fields1.lastName, fields1.email, captchaId, captchaAnswer);
      setStep(2);
      startCountdown();
    } catch (err) {
      const status = err.response?.status;
      const errorMsg = err.response?.data?.error || err.response?.data?.message || '';
      const isCaptchaError = errorMsg.toLowerCase().includes('captcha');
      if (isCaptchaError) {
        setCaptchaError(errorMsg || 'CAPTCHA validation failed. Please try again.');
        setCaptchaId('');
        setCaptchaAnswer('');
        setCaptchaRefreshTrigger((t) => t + 1);
      } else if (status === 409) {
        setApiError('An account with this email already exists. Please sign in instead.');
      } else if (status === 503) {
        setApiError('Unable to send verification email. Please check your email address and try again later.');
      } else {
        setApiError(err.response?.data?.message || 'Failed to send verification code. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  // ---------------------------------------------------------------------------
  // Step 2 handlers
  // ---------------------------------------------------------------------------

  const otpTrimmed = otp.trim();
  const step2Valid = otpTrimmed.length === 6 && /^\d{6}$/.test(otpTrimmed) && countdown > 0;

  const handleStep2Submit = async () => {
    if (!step2Valid || loading) return;
    setLoading(true);
    setApiError('');
    try {
      await verifyRegistrationCode(fields1.email, otpTrimmed);
      clearInterval(countdownRef.current);
      setStep(3);
    } catch (err) {
      setApiError(err.response?.data?.message || 'Invalid or expired verification code.');
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    if (loading) return;
    setLoading(true);
    setApiError('');
    setOtp('');
    try {
      await initiateRegistration(fields1.firstName, fields1.lastName, fields1.email);
      startCountdown();
    } catch (err) {
      const status = err.response?.status;
      if (status === 409) {
        setApiError('An account with this email already exists. Please sign in instead.');
      } else if (status === 503) {
        setApiError('Unable to send verification email. Please try again later.');
      } else {
        setApiError(err.response?.data?.message || 'Failed to resend the code. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  // ---------------------------------------------------------------------------
  // Step 3 handlers
  // ---------------------------------------------------------------------------

  const errors3 = validateStep3(fields3);
  const step3Filled = fields3.password.trim() !== '' && fields3.confirmPassword.trim() !== '';
  const allPwRulesPass = pwRules.every((r) => r.test(fields3.password));
  const step3Valid = step3Filled && Object.keys(errors3).length === 0 && allPwRulesPass;

  const handleChange3 = (e) => {
    const { id, value } = e.target;
    setFields3((prev) => ({ ...prev, [id]: value }));
    setApiError('');
  };

  const handleStep3Submit = async () => {
    if (!step3Valid || loading) return;
    setLoading(true);
    setApiError('');
    try {
      await completeRegistration(fields1.email, otpTrimmed, fields3.password);
      navigate(ROUTES.LOGIN, { state: { registered: true } });
    } catch (err) {
      const status = err.response?.status;
      if (status === 409) {
        setApiError('An account with this email already exists. Please sign in instead.');
      } else if (status === 400) {
        setApiError(err.response?.data?.message || 'Verification code expired. Please request a new one.');
        setStep(2);
        setOtp('');
        startCountdown();
      } else {
        setApiError(err.response?.data?.message || 'Registration failed. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  // ---------------------------------------------------------------------------
  // Step indicator
  // ---------------------------------------------------------------------------

  const StepDot = ({ n }) => (
    <div className="flex items-center gap-2">
      <div
        className={`flex h-7 w-7 items-center justify-center rounded-full text-[12px] font-bold 2xl:h-8 2xl:w-8 2xl:text-[13px] ${
          step === n ? 'bg-[#027EAC] text-white' : step > n ? 'bg-[#15803D] text-white' : 'bg-[#D3E1ED] text-[#677883]'
        }`}
      >
        {step > n ? '✓' : n}
      </div>
      <span className={`text-[12px] 2xl:text-[13px] ${step === n ? 'font-bold text-[#0B3857]' : step > n ? 'font-medium text-[#15803D]' : 'text-[#677883]'}`}>
        {n === 1 ? 'Your info' : n === 2 ? 'Verify email' : 'Set password'}
      </span>
    </div>
  );

  // ---------------------------------------------------------------------------
  // Render
  // ---------------------------------------------------------------------------

  return (
    <div className="min-h-screen w-full overflow-y-auto bg-[#E7F9FF] px-4 py-6 sm:px-6 lg:px-10 lg:py-6">
      <div className="mx-auto w-full max-w-[1360px] flex flex-col gap-6 lg:grid lg:grid-cols-2 lg:gap-8 xl:max-w-[1500px] 2xl:max-w-[1640px]">

        {/* Form panel */}
        <div className="flex items-center justify-center rounded-[32px] bg-white shadow-[0px_2px_10px_6px_#027EAC33]">
          <div className="mx-auto flex w-full max-w-[496px] flex-col gap-8 px-5 py-8 sm:px-8 sm:py-10 lg:px-10 lg:pb-8 lg:pt-[54px] xl:max-w-[540px] 2xl:max-w-[580px] 2xl:px-12 2xl:pb-10 2xl:pt-16 min-[1920px]:max-w-[720px]">

            {/* Header */}
            <div className="flex flex-col gap-1">
              <p className="m-0 text-[14px] font-light uppercase leading-6 text-[#0B3857] 2xl:text-[15px] 2xl:leading-7">
                LET&apos;S GET YOU STARTED
              </p>
              <h1 className="m-0 text-[24px] font-bold leading-10 text-[#0B3857] 2xl:text-[30px] 2xl:leading-[44px]">
                Create an account
              </h1>
            </div>

            {/* Step indicators */}
            <div className="flex items-center gap-4">
              <StepDot n={1} />
              <div className="h-px flex-1 bg-[#D3E1ED]" />
              <StepDot n={2} />
              <div className="h-px flex-1 bg-[#D3E1ED]" />
              <StepDot n={3} />
            </div>

            {/* API error banner */}
            {apiError && (
              <div className="rounded-[8px] bg-[#FCE9ED] px-4 py-3 text-[13px] font-medium leading-5 text-[#C0352A]">
                {apiError}
              </div>
            )}

            {/* ── STEP 1 ── */}
            {step === 1 && (
              <div className="flex w-full flex-col gap-4">
                <div className="grid w-full grid-cols-1 gap-4 sm:grid-cols-2">
                  <div className="flex min-w-0 flex-col gap-1">
                    <label htmlFor="firstName" className="w-fit whitespace-nowrap text-[14px] font-extrabold leading-6 text-[#0B3857] 2xl:text-[15px]">First name</label>
                    <input id="firstName" type="text" placeholder="Enter your first name" value={fields1.firstName} onChange={handleChange1} className={inputCls(!!errors1.firstName)} />
                    {errors1.firstName
                      ? <p className="m-0 text-[12px] font-normal leading-4 text-[#B70B0B] 2xl:text-[13px]">{errors1.firstName}</p>
                      : <p className="m-0 h-4 text-[12px] leading-4 text-[#677883] 2xl:text-[13px]">e.g. Johnson</p>}
                  </div>
                  <div className="flex min-w-0 flex-col gap-1">
                    <label htmlFor="lastName" className="w-fit whitespace-nowrap text-[14px] font-extrabold leading-6 text-[#0B3857] 2xl:text-[15px]">Last name</label>
                    <input id="lastName" type="text" placeholder="Enter your last name" value={fields1.lastName} onChange={handleChange1} className={inputCls(!!errors1.lastName)} />
                    {errors1.lastName
                      ? <p className="m-0 text-[12px] font-normal leading-4 text-[#B70B0B] 2xl:text-[13px]">{errors1.lastName}</p>
                      : <p className="m-0 h-4 text-[12px] leading-4 text-[#677883] 2xl:text-[13px]">e.g. Doe</p>}
                  </div>
                </div>

                <div className="flex w-full flex-col gap-1">
                  <label htmlFor="email" className="w-fit whitespace-nowrap text-[14px] font-extrabold leading-6 text-[#0B3857] 2xl:text-[15px]">Email</label>
                  <input id="email" type="email" placeholder="Enter your email" value={fields1.email} onChange={handleChange1} className={inputCls(!!errors1.email)} />
                  {errors1.email
                    ? <p className="m-0 text-[12px] font-normal leading-4 text-[#B70B0B] 2xl:text-[13px]">{errors1.email}</p>
                    : <p className="m-0 h-4 text-[12px] leading-4 text-[#677883] 2xl:text-[13px]">e.g. username@domain.com</p>}
                </div>

                <div className="flex w-full flex-col gap-4 pt-2">
                <CaptchaWidget
                    refreshTrigger={captchaRefreshTrigger}
                    captchaAnswer={captchaAnswer}
                    onAnswerChange={handleCaptchaAnswerChange}
                    onCaptchaLoaded={handleCaptchaLoaded}
                    captchaError={captchaError}
                  />

                  <button type="button" disabled={!step1Valid || loading} onClick={handleStep1Submit}
                    className={`flex h-10 w-full items-center justify-center rounded-[8px] bg-[#027EAC] px-4 py-2 2xl:h-12 ${step1Valid && !loading ? 'cursor-pointer opacity-100 transition-opacity hover:opacity-90' : 'cursor-not-allowed opacity-50'}`}>
                    <span className="whitespace-nowrap text-[14px] font-bold leading-6 text-white 2xl:text-[15px]">
                      {loading ? 'Sending code…' : 'Continue'}
                    </span>
                  </button>
                  <div className="flex w-full flex-wrap items-center gap-1 text-[12px] leading-4 2xl:text-[13px]">
                    <span className="text-[#0B3857]">Already have an account?</span>
                    <Link to={ROUTES.LOGIN} className="font-bold text-[#027EAC] underline decoration-solid underline-offset-[9.5%]">Sign in</Link>
                  </div>
                </div>
              </div>
            )}

            {/* ── STEP 2 ── */}
            {step === 2 && (
              <div className="flex w-full flex-col gap-6">
                <div className="rounded-[8px] bg-[#E7F9FF] px-4 py-3 text-[13px] leading-5 text-[#0B3857]">
                  We sent a 6-digit verification code to <span className="font-bold">{fields1.email}</span>. Please check your inbox.
                </div>

                <div className="flex w-full flex-col gap-1">
                  <label htmlFor="otp" className="w-fit whitespace-nowrap text-[14px] font-extrabold leading-6 text-[#0B3857] 2xl:text-[15px]">Verification code</label>
                  <input
                    id="otp" type="text" inputMode="numeric" maxLength={6}
                    placeholder="Enter 6-digit code" value={otp}
                    onChange={(e) => { setOtp(e.target.value.replace(/\D/g, '').slice(0, 6)); setApiError(''); }}
                    className={inputCls(false)}
                  />
                  <p className="m-0 text-[12px] leading-4 text-[#677883] 2xl:text-[13px]">
                    {countdown > 0
                      ? <>Expires in <span className="font-bold text-[#0B3857]">{formatCountdown(countdown)}</span></>
                      : <span className="font-medium text-[#B70B0B]">Code expired. Please request a new one.</span>}
                  </p>
                </div>

                <div className="flex w-full flex-col gap-3">
                  <button type="button" disabled={!step2Valid || loading} onClick={handleStep2Submit}
                    className={`flex h-10 w-full items-center justify-center rounded-[8px] bg-[#027EAC] px-4 py-2 2xl:h-12 ${step2Valid && !loading ? 'cursor-pointer opacity-100 transition-opacity hover:opacity-90' : 'cursor-not-allowed opacity-50'}`}>
                    <span className="whitespace-nowrap text-[14px] font-bold leading-6 text-white 2xl:text-[15px]">
                      {loading ? 'Verifying…' : 'Verify Email'}
                    </span>
                  </button>
                  <div className="flex w-full flex-wrap items-center gap-1 text-[12px] leading-4 2xl:text-[13px]">
                    <span className="text-[#0B3857]">Didn&apos;t receive a code?</span>
                    <button type="button" onClick={handleResend} disabled={loading}
                      className="font-bold text-[#027EAC] underline decoration-solid underline-offset-[9.5%] disabled:opacity-50">
                      Resend
                    </button>
                  </div>
                  <button type="button"
                    onClick={() => { setStep(1); setApiError(''); setOtp(''); clearInterval(countdownRef.current); }}
                    className="text-left text-[12px] font-medium text-[#677883] underline decoration-solid 2xl:text-[13px]">
                    ← Change email address
                  </button>
                </div>
              </div>
            )}

            {/* ── STEP 3 ── */}
            {step === 3 && (
              <div className="flex w-full flex-col gap-4">
                <div className="rounded-[8px] bg-[#E7F9FF] px-4 py-3 text-[13px] leading-5 text-[#0B3857]">
                  Email verified! Now set a password for <span className="font-bold">{fields1.email}</span>.
                </div>

                <div className="flex w-full flex-col gap-1">
                  <label htmlFor="password" className="w-fit whitespace-nowrap text-[14px] font-extrabold leading-6 text-[#0B3857] 2xl:text-[15px]">Password</label>
                  <div className="relative">
                    <input id="password" type={showPw ? 'text' : 'password'} placeholder="Enter your password"
                      value={fields3.password} onChange={handleChange3} className={`${inputCls(false)} pr-12`} />
                    <button type="button" onClick={() => setShowPw((p) => !p)} className="absolute right-4 top-1/2 -translate-y-1/2 outline-none">
                      {showPw ? <EyeOpen /> : <EyeOff />}
                    </button>
                  </div>
                  <div className="flex w-full flex-col">
                    {pwRules.map((rule) => {
                      const passing = fields3.password ? rule.test(fields3.password) : null;
                      const color = passing === false ? 'font-normal text-[#B70B0B]' : passing === true ? 'font-normal text-[#15803D]' : 'text-[#677883]';
                      const dot   = passing === false ? 'bg-[#B70B0B]' : passing === true ? 'bg-[#15803D]' : 'bg-[#677883]';
                      return (
                        <div key={rule.label} className="flex h-4 items-center gap-2 2xl:h-5">
                          <span className={`h-2 w-2 rounded-full ${dot}`} />
                          <p className={`m-0 h-4 text-[12px] leading-4 2xl:h-5 2xl:text-[13px] 2xl:leading-5 ${color}`}>{rule.label}</p>
                        </div>
                      );
                    })}
                  </div>
                </div>

                <div className="flex w-full flex-col gap-1">
                  <label htmlFor="confirmPassword" className="w-fit whitespace-nowrap text-[14px] font-extrabold leading-6 text-[#0B3857] 2xl:text-[15px]">Confirm password</label>
                  <div className="relative">
                    <input id="confirmPassword" type={showCpw ? 'text' : 'password'} placeholder="Confirm your password"
                      value={fields3.confirmPassword} onChange={handleChange3} className={`${inputCls(!!errors3.confirmPassword)} pr-12`} />
                    <button type="button" onClick={() => setShowCpw((p) => !p)} className="absolute right-4 top-1/2 -translate-y-1/2 outline-none">
                      {showCpw ? <EyeOpen /> : <EyeOff />}
                    </button>
                  </div>
                  {errors3.confirmPassword
                    ? <p className="m-0 text-[12px] font-normal leading-4 text-[#B70B0B] 2xl:text-[13px]">{errors3.confirmPassword}</p>
                    : fields3.confirmPassword && fields3.confirmPassword === fields3.password
                    ? <p className="m-0 text-[12px] font-normal leading-4 text-[#15803D] 2xl:text-[13px]">Passwords match.</p>
                    : (
                      <div className="flex h-4 items-center gap-2 2xl:h-5">
                        <span className="h-2 w-2 rounded-full bg-[#677883]" />
                        <p className="m-0 h-4 text-[12px] leading-4 text-[#677883] 2xl:h-5 2xl:text-[13px] 2xl:leading-5">Confirm password must match your password</p>
                      </div>
                    )}
                </div>

                <div className="flex w-full flex-col gap-4 pt-2">
                  <button type="button" disabled={!step3Valid || loading} onClick={handleStep3Submit}
                    className={`flex h-10 w-full items-center justify-center rounded-[8px] bg-[#027EAC] px-4 py-2 2xl:h-12 ${step3Valid && !loading ? 'cursor-pointer opacity-100 transition-opacity hover:opacity-90' : 'cursor-not-allowed opacity-50'}`}>
                    <span className="whitespace-nowrap text-[14px] font-bold leading-6 text-white 2xl:text-[15px]">
                      {loading ? 'Creating account…' : 'Create an account'}
                    </span>
                  </button>
                  <div className="flex w-full flex-wrap items-center gap-1 text-[12px] leading-4 2xl:text-[13px]">
                    <span className="text-[#0B3857]">Already have an account?</span>
                    <Link to={ROUTES.LOGIN} className="font-bold text-[#027EAC] underline decoration-solid underline-offset-[9.5%]">Sign in</Link>
                  </div>
                </div>
              </div>
            )}

          </div>
        </div>

        {/* Image panel */}
        <div className="relative min-h-[320px] overflow-hidden rounded-[32px]">
          <img src={beachImage} alt="Beach destination" className="h-full w-full object-cover object-[center_30%]" />
          <div className="absolute inset-0 flex flex-col justify-between p-10">
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
        </div>

      </div>
    </div>
  );
};

export default Registration;
