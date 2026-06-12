import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import beachImage from '../../assets/images/beach-registration.png';
import travelbagIcon from '../../assets/icons/travelbag.svg';
import { resetPassword } from '../../services/authService';
import { useAuth } from '../../context/AuthContext';
import { ROUTES } from '../../config/routes';

const pwRules = [
  { label: 'At least one uppercase letter required',  test: (v) => /[A-Z]/.test(v) },
  { label: 'At least one lowercase letter required',  test: (v) => /[a-z]/.test(v) },
  { label: 'At least one number required',            test: (v) => /[0-9]/.test(v) },
  { label: 'At least one special character required', test: (v) => /[^A-Za-z0-9]/.test(v) },
  { label: 'Password must be 8-16 characters long',   test: (v) => v.length >= 8 && v.length <= 16 },
];

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

const ResetPassword = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [searchParams] = useSearchParams();

  const email            = searchParams.get('email') || '';
  const verificationCode = searchParams.get('code')  || '';

  // Redirect to /tours if already authenticated
  useEffect(() => {
    if (user) {
      navigate(ROUTES.TOURS, { replace: true });
    }
  }, [user, navigate]);

  // Redirect to forgot-password if required params are missing
  useEffect(() => {
    if (!email || !verificationCode) {
      navigate(ROUTES.FORGOT_PASSWORD, { replace: true });
    }
  }, [email, verificationCode, navigate]);

  const [newPassword, setNewPassword]       = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showNew, setShowNew]               = useState(false);
  const [showConfirm, setShowConfirm]       = useState(false);
  const [apiError, setApiError]             = useState('');
  const [loading, setLoading]               = useState(false);

  const allPwRulesPassing = pwRules.every((r) => r.test(newPassword));
  const confirmPassing    = confirmPassword !== '' && confirmPassword === newPassword;
  const canSubmit = allPwRulesPassing && confirmPassing && !loading;

  const inputCls = (hasError) =>
    `h-14 w-full rounded-[8px] border bg-white px-4 text-[14px] leading-6 text-[#0B3857] outline-none placeholder:text-[#A2AEB9] 2xl:h-16 2xl:text-[16px] transition-colors ${
      hasError
        ? 'border-[#B70B0B] focus:border-[#B70B0B]'
        : 'border-[#D3E1ED] focus:border-[#027EAC]'
    }`;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!canSubmit) return;
    setApiError('');
    setLoading(true);
    try {
      await resetPassword(email, verificationCode, newPassword);
      navigate(ROUTES.LOGIN, { state: { passwordReset: true } });
    } catch (err) {
      const status = err.response?.status;
      if (status === 400) {
        setApiError('The reset link is invalid or has expired. Please request a new one.');
      } else if (status === 404) {
        setApiError('No account found with this email address.');
      } else {
        setApiError('Failed to reset password. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

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
            <h1 className="m-0 text-[24px] font-bold leading-10 text-[#0B3857] 2xl:text-[30px] 2xl:leading-[44px]">
              Reset password
            </h1>

            {apiError && (
              <div className="rounded-[8px] bg-[#FCE9ED] px-4 py-3 text-[13px] font-medium leading-5 text-[#C0352A]">
                {apiError}
              </div>
            )}

            <div className="flex w-full flex-col gap-4">

              {/* New password */}
              <div className="flex w-full flex-col gap-1">
                <label htmlFor="new-password" className="w-fit text-[14px] font-extrabold leading-6 text-[#0B3857] 2xl:text-[15px]">
                  New password
                </label>
                <div className="relative">
                  <input
                    id="new-password"
                    type={showNew ? 'text' : 'password'}
                    placeholder="Enter your new password"
                    value={newPassword}
                    onChange={(e) => { setNewPassword(e.target.value); setApiError(''); }}
                    autoComplete="new-password"
                    className={`${inputCls(false)} pr-12`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowNew((v) => !v)}
                    className="absolute right-4 top-1/2 -translate-y-1/2 outline-none"
                    aria-label={showNew ? 'Hide password' : 'Show password'}
                  >
                    {showNew ? <EyeOpen /> : <EyeOff />}
                  </button>
                </div>
                {/* Password rules */}
                <div className="flex w-full flex-col mt-1">
                  {pwRules.map((rule) => {
                    const passing = newPassword ? rule.test(newPassword) : null;
                    const color = passing === false ? 'font-normal text-[#B70B0B]' : passing === true ? 'font-normal text-[#15803D]' : 'text-[#677883]';
                    const dot   = passing === false ? 'bg-[#B70B0B]' : passing === true ? 'bg-[#15803D]' : 'bg-[#677883]';
                    return (
                      <div key={rule.label} className="flex h-4 items-center gap-2 2xl:h-5">
                        <span className={`h-2 w-2 rounded-full flex-shrink-0 ${dot}`} />
                        <p className={`m-0 h-4 text-[12px] leading-4 2xl:h-5 2xl:text-[13px] 2xl:leading-5 ${color}`}>{rule.label}</p>
                      </div>
                    );
                  })}

                </div>
              </div>

              {/* Confirm password */}
              <div className="flex w-full flex-col gap-1">
                <label htmlFor="confirm-password" className="w-fit text-[14px] font-extrabold leading-6 text-[#0B3857] 2xl:text-[15px]">
                  Confirm password
                </label>
                <div className="relative">
                  <input
                    id="confirm-password"
                    type={showConfirm ? 'text' : 'password'}
                    placeholder="Confirm your password"
                    value={confirmPassword}
                    onChange={(e) => { setConfirmPassword(e.target.value); setApiError(''); }}
                    autoComplete="new-password"
                    className={`${inputCls(confirmPassword && confirmPassword !== newPassword)} pr-12`}
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirm((v) => !v)}
                    className="absolute right-4 top-1/2 -translate-y-1/2 outline-none"
                    aria-label={showConfirm ? 'Hide password' : 'Show password'}
                  >
                    {showConfirm ? <EyeOpen /> : <EyeOff />}
                  </button>
                </div>
                {confirmPassword && confirmPassword !== newPassword
                  ? <p className="m-0 text-[12px] font-normal leading-4 text-[#B70B0B] 2xl:text-[13px]">Confirm password must match new password</p>
                  : confirmPassword && confirmPassword === newPassword
                  ? <p className="m-0 text-[12px] font-normal leading-4 text-[#15803D] 2xl:text-[13px]">Passwords match.</p>
                  : (
                    <div className="flex h-4 items-center gap-2 2xl:h-5">
                      <span className="h-2 w-2 rounded-full flex-shrink-0 bg-[#677883]" />
                      <p className="m-0 h-4 text-[12px] leading-4 text-[#677883] 2xl:h-5 2xl:text-[13px] 2xl:leading-5">
                        Confirm password must match new password
                      </p>
                    </div>
                  )
                }
              </div>

            </div>

            {/* Submit */}
            <div className="flex w-full flex-col gap-4">
              <button
                type="submit"
                disabled={!canSubmit}
                className={`flex h-10 w-full items-center justify-center rounded-[8px] bg-[#027EAC] px-4 py-2 2xl:h-12 ${canSubmit ? 'cursor-pointer opacity-100 transition-opacity hover:opacity-90' : 'cursor-not-allowed opacity-50'}`}
              >
                <span className="whitespace-nowrap text-[14px] font-bold leading-6 text-white 2xl:text-[15px]">
                  {loading ? 'Resetting…' : 'Reset'}
                </span>
              </button>
            </div>

          </form>
        </div>

        {/* ── RIGHT: Image panel with overlay ── */}
        <div className="relative min-h-[320px] rounded-[32px]">
          <img
            src={beachImage}
            alt="Travel destination"
            className="h-full w-full rounded-[32px] object-cover object-[center_30%]"
          />
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
        </div>

      </div>
    </div>
  );
};

export default ResetPassword;

