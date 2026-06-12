import { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import beachImage from '../../assets/images/beach-registration.png';
import travelbagIcon from '../../assets/icons/travelbag.svg';
import { useAuth } from '../../context/AuthContext';
import { ROUTES } from '../../config/routes';
import { completeOAuthSignup } from '../../services/authService';

// ---------------------------------------------------------------------------
// Google brand icon (same as Login page)
// ---------------------------------------------------------------------------
const GoogleIcon = () => (
  <svg width="20" height="20" viewBox="0 0 48 48" xmlns="http://www.w3.org/2000/svg" aria-hidden="true" focusable="false">
    <path fill="#EA4335" d="M24 9.5c3.14 0 5.95 1.08 8.17 2.85l6.1-6.1C34.39 3.05 29.49 1 24 1 14.82 1 7.01 6.47 3.56 14.25l7.1 5.52C12.47 13.3 17.8 9.5 24 9.5z"/>
    <path fill="#4285F4" d="M46.56 24.5c0-1.64-.15-3.22-.42-4.75H24v9h12.73c-.55 2.98-2.2 5.5-4.68 7.2l7.1 5.52C43.17 37.32 46.56 31.4 46.56 24.5z"/>
    <path fill="#FBBC05" d="M10.66 28.23A14.5 14.5 0 0 1 9.5 24c0-1.47.25-2.9.66-4.23l-7.1-5.52A23.94 23.94 0 0 0 0 24c0 3.86.92 7.51 2.56 10.75l7.1-5.52z"/>
    <path fill="#34A853" d="M24 47c5.49 0 10.1-1.82 13.46-4.93l-7.1-5.52C28.59 38.4 26.42 39.5 24 39.5c-6.2 0-11.53-3.8-13.34-9.27l-7.1 5.52C7.01 41.53 14.82 47 24 47z"/>
    <path fill="none" d="M0 0h48v48H0z"/>
  </svg>
);

// ---------------------------------------------------------------------------
// Read-only field — visually matches the login/registration input style
// ---------------------------------------------------------------------------
const ReadOnlyField = ({ label, value, id }) => (
  <div className="flex w-full flex-col gap-1">
    <label htmlFor={id} className="w-fit text-[14px] font-extrabold leading-6 text-[#0B3857] 2xl:text-[15px]">
      {label}
    </label>
    <div
      id={id}
      aria-readonly="true"
      className="flex h-14 w-full items-center rounded-[8px] border border-[#D3E1ED] bg-[#F5FBFE] px-4 text-[14px] leading-6 text-[#0B3857] 2xl:h-16 2xl:text-[16px]"
    >
      {value}
    </div>
  </div>
);

// ---------------------------------------------------------------------------
// Page component
// ---------------------------------------------------------------------------
export default function OAuthSignup() {
  const [searchParams] = useSearchParams();
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const handled = useRef(false);

  const onboardingToken = searchParams.get('onboarding_token');
  const email           = searchParams.get('email')     || '';
  const firstName       = searchParams.get('firstName') || '';
  const lastName        = searchParams.get('lastName')  || '';
  const fullName        = [firstName, lastName].filter(Boolean).join(' ');

  const [loading, setLoading]   = useState(false);
  const [apiError, setApiError] = useState('');

  // Redirect already-authenticated users away immediately
  useEffect(() => {
    if (user) {
      navigate(user.role === 'ADMIN' ? ROUTES.ADMIN_REPORTS : ROUTES.TOURS, { replace: true });
    }
  }, [user, navigate]);

  // Guard: if there is no onboarding_token, this page makes no sense — send to login
  useEffect(() => {
    if (!onboardingToken) {
      navigate(ROUTES.LOGIN, { replace: true });
    }
  }, [onboardingToken, navigate]);

  // Don't render anything until guards have run
  if (!onboardingToken || user) return null;

  // ---------------------------------------------------------------------------
  // Handlers
  // ---------------------------------------------------------------------------
  const handleContinue = async () => {
    if (loading || handled.current) return;
    setLoading(true);
    setApiError('');
    try {
      const data = await completeOAuthSignup(onboardingToken);
      handled.current = true;
      login(data); // reuse existing AuthContext.login — identical to email/password flow
      navigate(data.role === 'ADMIN' ? ROUTES.ADMIN_REPORTS : ROUTES.TOURS, { replace: true });
    } catch (err) {
      const status = err.response?.status;
      if (status === 400) {
        setApiError('Your signup session expired. Please sign in with Google again.');
      } else if (status === 409) {
        setApiError('An account with this email already exists. Please sign in instead.');
      } else {
        setApiError('Something went wrong. Please try again.');
      }
      setLoading(false);
    }
  };

  const handleCancel = () => {
    navigate(ROUTES.LOGIN, { replace: true });
  };

  const canContinue = !loading;

  // ---------------------------------------------------------------------------
  // Render — layout identical to Login / Registration pages
  // ---------------------------------------------------------------------------
  return (
    <div className="min-h-screen w-full overflow-y-auto bg-[#E7F9FF] px-4 py-6 sm:px-6 lg:px-10 lg:py-6">
      <div className="mx-auto w-full max-w-[1360px] flex flex-col gap-6 lg:grid lg:grid-cols-2 lg:gap-8 xl:max-w-[1500px] 2xl:max-w-[1640px]">

        {/* ── LEFT: White form card ── */}
        <div className="flex items-center justify-center rounded-[32px] bg-white shadow-[0px_2px_10px_6px_#027EAC33]">
          <div className="mx-auto flex w-full max-w-[496px] flex-col gap-8 px-5 py-12 sm:px-8 sm:py-14 lg:px-10 lg:py-16 xl:max-w-[540px] 2xl:max-w-[580px] 2xl:px-12 2xl:py-20 min-[1920px]:max-w-[720px]">

            {/* Header */}
            <div className="flex flex-col gap-0">
              <p className="m-0 text-[14px] font-light uppercase leading-6 text-[#0B3857] 2xl:text-[15px] 2xl:leading-7">
                One more step
              </p>
              <h1 className="m-0 text-[24px] font-bold leading-10 text-[#0B3857] 2xl:text-[30px] 2xl:leading-[44px]">
                Create your account
              </h1>
            </div>

            {/* Google success indicator */}
            <div className="flex w-full items-start gap-3 rounded-[8px] border border-[#D3E1ED] bg-[#F5FBFE] px-4 py-3">
              <div className="mt-0.5 flex-shrink-0">
                <GoogleIcon />
              </div>
              <div className="flex flex-col gap-0.5">
                <p className="m-0 text-[14px] font-bold leading-6 text-[#0B3857] 2xl:text-[15px]">
                  Signed in with Google
                </p>
                <p className="m-0 text-[12px] font-normal leading-4 text-[#677883] 2xl:text-[13px]">
                  No existing account found for this email
                </p>
              </div>
            </div>

            {/* API error banner */}
            {apiError && (
              <div
                role="alert"
                className="rounded-[8px] bg-[#FCE9ED] px-4 py-3 text-[13px] font-medium leading-5 text-[#C0352A]"
              >
                {apiError}
              </div>
            )}

            {/* Read-only fields */}
            <div className="flex w-full flex-col gap-4">
              <ReadOnlyField id="oauth-name"  label="Name"  value={fullName || '—'} />
              <ReadOnlyField id="oauth-email" label="Email" value={email    || '—'} />
            </div>

            {/* Explanation */}
            <p className="m-0 text-[13px] font-normal leading-[22px] text-[#677883] 2xl:text-[14px] 2xl:leading-6">
              Continuing will create a new Travel Agency account linked to your
              Google profile. You can sign in with Google on future visits.
            </p>

            {/* Actions */}
            <div className="flex w-full flex-col gap-3">
              {/* Continue — primary */}
              <button
                type="button"
                onClick={handleContinue}
                disabled={!canContinue}
                aria-label="Continue and create account"
                className={`flex h-10 w-full items-center justify-center rounded-[8px] bg-[#027EAC] px-4 py-2 2xl:h-12 ${
                  canContinue
                    ? 'cursor-pointer opacity-100 transition-opacity hover:opacity-90'
                    : 'cursor-not-allowed opacity-50'
                }`}
              >
                <span className="whitespace-nowrap text-[14px] font-bold leading-6 text-white 2xl:text-[15px]">
                  {loading ? 'Creating account…' : 'Continue'}
                </span>
              </button>

              {/* Cancel — subtle text button */}
              <button
                type="button"
                onClick={handleCancel}
                disabled={loading}
                aria-label="Cancel and go back to login"
                className="flex h-10 w-full items-center justify-center rounded-[8px] border border-[#D3E1ED] bg-white px-4 py-2 text-[14px] font-bold leading-6 text-[#677883] transition-colors hover:border-[#027EAC] hover:text-[#027EAC] disabled:cursor-not-allowed disabled:opacity-50 2xl:h-12 2xl:text-[15px]"
              >
                Cancel
              </button>
            </div>

          </div>
        </div>

        {/* ── RIGHT: Image panel — identical to Login / Registration ── */}
        <div className="relative min-h-[320px] rounded-[32px]">
          <img
            src={beachImage}
            alt="Travel destination"
            className="h-full w-full rounded-[32px] object-cover object-[center_30%]"
          />
          <div className="absolute inset-0 flex flex-col justify-between overflow-hidden rounded-[32px] p-10">
            <div className="flex items-center gap-3">
              <img
                src={travelbagIcon}
                alt="Travel Agency logo"
                style={{ width: '48px', height: '48px', flexShrink: 0 }}
              />
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
}

