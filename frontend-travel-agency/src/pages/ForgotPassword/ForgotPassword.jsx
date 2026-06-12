import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import beachImage from '../../assets/images/beach-registration.png';
import travelbagIcon from '../../assets/icons/travelbag.svg';
import { forgotPassword } from '../../services/authService';
import { useAuth } from '../../context/AuthContext';
import { ROUTES } from '../../config/routes';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const ForgotPassword = () => {
  const navigate = useNavigate();
  const { user } = useAuth();

  // Redirect to /tours if already authenticated
  useEffect(() => {
    if (user) {
      navigate(ROUTES.TOURS, { replace: true });
    }
  }, [user, navigate]);

  const [email, setEmail]         = useState('');
  const [emailError, setEmailError] = useState('');
  const [loading, setLoading]     = useState(false);
  const [view, setView]           = useState('form'); // 'form' | 'sent'

  const inputCls = (hasError) =>
    `h-14 w-full rounded-[8px] border bg-white px-4 text-[14px] leading-6 text-[#0B3857] outline-none placeholder:text-[#A2AEB9] 2xl:h-16 2xl:text-[16px] transition-colors ${
      hasError
        ? 'border-[#B70B0B] focus:border-[#B70B0B]'
        : 'border-[#D3E1ED] focus:border-[#027EAC]'
    }`;

  const canSubmit = email.trim() && !loading;

  const handleEmailChange = (e) => {
    setEmail(e.target.value);
    setEmailError('');
  };

  const handleEmailBlur = () => {
    if (email.trim() && !EMAIL_RE.test(email.trim())) {
      setEmailError('Please enter a valid email address.');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!EMAIL_RE.test(email.trim())) {
      setEmailError('Please enter a valid email address.');
      return;
    }
    setEmailError('');
    setLoading(true);
    try {
      await forgotPassword(email.trim());
      setView('sent');
    } catch (err) {
      const status = err.response?.status;
      if (status === 404) {
        setEmailError('No account found with this email address.');
      } else {
        setEmailError('Failed to send reset link. Please try again later.');
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

          {view === 'form' ? (
            /* ── Step 1: Enter email form ── */
            <form
              onSubmit={handleSubmit}
              noValidate
              className="mx-auto flex w-full max-w-[496px] flex-col gap-10 px-5 py-12 sm:px-8 sm:py-14 lg:px-10 lg:py-16 xl:max-w-[540px] 2xl:max-w-[580px] 2xl:px-12 2xl:py-20 min-[1920px]:max-w-[720px]"
            >
              {/* Title */}
              <h1 className="m-0 text-[24px] font-bold leading-10 text-[#0B3857] 2xl:text-[30px] 2xl:leading-[44px]">
                Reset password
              </h1>

              {/* Email field */}
              <div className="flex w-full flex-col gap-4">
                <div className="flex w-full flex-col gap-1">
                  <label htmlFor="forgot-email" className="w-fit text-[14px] font-extrabold leading-6 text-[#0B3857] 2xl:text-[15px]">
                    Email
                  </label>
                  <input
                    id="forgot-email"
                    type="email"
                    placeholder="Enter your email"
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
              </div>

              {/* Actions */}
              <div className="flex w-full flex-col gap-4">
                <button
                  type="submit"
                  disabled={!canSubmit}
                  className={`flex h-10 w-full items-center justify-center rounded-[8px] bg-[#027EAC] px-4 py-2 2xl:h-12 ${canSubmit ? 'cursor-pointer opacity-100 transition-opacity hover:opacity-90' : 'cursor-not-allowed opacity-50'}`}
                >
                  <span className="whitespace-nowrap text-[14px] font-bold leading-6 text-white 2xl:text-[15px]">
                    {loading ? 'Sending…' : 'Request a reset link'}
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
            </form>
          ) : (
            /* ── Step 2: Email sent confirmation ── */
            <div className="mx-auto flex w-full max-w-[496px] flex-col gap-10 px-5 py-12 sm:px-8 sm:py-14 lg:px-10 lg:py-16 xl:max-w-[540px] 2xl:max-w-[580px] 2xl:px-12 2xl:py-20 min-[1920px]:max-w-[720px]">
              <div className="flex flex-col gap-3">
                <h1 className="m-0 text-[24px] font-bold leading-10 text-[#0B3857] 2xl:text-[30px] 2xl:leading-[44px]">
                  Email sent
                </h1>
                <p className="m-0 text-[14px] font-normal leading-6 text-[#0B3857] 2xl:text-[15px]">
                  We sent an email to{' '}
                  <span className="font-bold">{email}</span>{' '}
                  with a link to reset your password. Please follow the instructions in the email.
                </p>
              </div>
            </div>
          )}
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

export default ForgotPassword;

