import { useState, useEffect, useCallback } from 'react';
import { generateCaptcha } from '../../services/authService';

const RefreshIcon = () => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    className="h-5 w-5"
    fill="none"
    viewBox="0 0 24 24"
    stroke="currentColor"
    strokeWidth={2}
  >
    <path
      strokeLinecap="round"
      strokeLinejoin="round"
      d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
    />
  </svg>
);

/**
 * CaptchaWidget
 *
 * Reusable component that:
 * - Fetches a CAPTCHA image from the backend on mount and on every
 *   `refreshTrigger` increment.
 * - Exposes the generated captchaId to the parent via `onCaptchaLoaded`.
 * - Shows a controlled text input for the user's answer.
 * - Has an inline refresh button the user can click to get a new image.
 *
 * Props:
 *  refreshTrigger  {number}   – Increment from parent to force a new CAPTCHA
 *                               (e.g. after a failed submission).
 *  captchaAnswer   {string}   – Controlled value for the answer input.
 *  onAnswerChange  {Function} – Called with the new answer string on every keystroke.
 *  onCaptchaLoaded {Function} – Called with the new captchaId after each load.
 *  captchaError    {string}   – Server-side CAPTCHA error message from the parent.
 */
const CaptchaWidget = ({
  refreshTrigger,
  captchaAnswer,
  onAnswerChange,
  onCaptchaLoaded,
  captchaError,
}) => {
  const [imageBase64, setImageBase64] = useState('');
  const [captchaLoading, setCaptchaLoading] = useState(false);
  const [loadError, setLoadError] = useState('');

  const loadCaptcha = useCallback(async () => {
    setCaptchaLoading(true);
    setLoadError('');
    try {
      const data = await generateCaptcha();
      setImageBase64(data.imageBase64);
      onCaptchaLoaded(data.captchaId);
    } catch {
      setLoadError('Unable to load CAPTCHA. Click refresh to try again.');
      onCaptchaLoaded('');
    } finally {
      setCaptchaLoading(false);
    }
  }, [onCaptchaLoaded]);

  // Load on mount and whenever refreshTrigger changes (parent-triggered refresh)
  useEffect(() => {
    loadCaptcha();
  }, [refreshTrigger, loadCaptcha]);

  const handleRefreshClick = () => {
    onAnswerChange('');
    loadCaptcha();
  };

  const showError = captchaError || loadError;
  const inputDisabled = captchaLoading || !!loadError;

  const inputCls = `h-14 w-full rounded-[8px] border bg-white px-4 text-[14px] leading-6 text-[#0B3857] outline-none placeholder:text-[#A2AEB9] 2xl:h-16 2xl:text-[16px] ${
    showError
      ? 'border-[#B70B0B] focus:border-[#B70B0B]'
      : 'border-[#D3E1ED] focus:border-[#027EAC]'
  } ${inputDisabled ? 'cursor-not-allowed bg-[#F5F7F9]' : ''}`;

  return (
    <div className="flex w-full flex-col gap-1">
      <label className="w-fit whitespace-nowrap text-[14px] font-extrabold leading-6 text-[#0B3857] 2xl:text-[15px]">
        Security check
      </label>

      {/* CAPTCHA image row */}
      <div className="flex items-center gap-3">
        <div
          className={`flex h-[60px] w-[200px] flex-shrink-0 items-center justify-center overflow-hidden rounded-[8px] border ${
            showError ? 'border-[#B70B0B]' : 'border-[#D3E1ED]'
          } bg-white`}
        >
          {captchaLoading ? (
            <div className="h-full w-full animate-pulse rounded-[7px] bg-[#E7F9FF]" />
          ) : loadError ? (
            <p className="px-3 text-center text-[11px] leading-[14px] text-[#B70B0B]">
              {loadError}
            </p>
          ) : imageBase64 ? (
            <img
              src={imageBase64}
              alt="CAPTCHA challenge – type the characters shown"
              className="h-full w-auto select-none rounded-[7px]"
              draggable={false}
            />
          ) : null}
        </div>

        <button
          type="button"
          onClick={handleRefreshClick}
          disabled={captchaLoading}
          title="Get a new CAPTCHA"
          aria-label="Refresh CAPTCHA"
          className={`flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full border border-[#D3E1ED] bg-white text-[#027EAC] transition-colors ${
            captchaLoading
              ? 'cursor-not-allowed opacity-40'
              : 'cursor-pointer hover:border-[#027EAC] hover:bg-[#E7F9FF]'
          }`}
        >
          <RefreshIcon />
        </button>

        <p className="m-0 text-[12px] leading-4 text-[#677883] 2xl:text-[13px]">
          Can&apos;t read? Click refresh for a new image.
        </p>
      </div>

      {/* Answer input */}
      <input
        id="captchaAnswer"
        type="text"
        placeholder="Type the characters shown above"
        value={captchaAnswer}
        onChange={(e) => onAnswerChange(e.target.value)}
        disabled={inputDisabled}
        className={inputCls}
        autoComplete="off"
        autoCorrect="off"
        autoCapitalize="off"
        spellCheck={false}
        maxLength={20}
      />

      {showError ? (
        <p className="m-0 text-[12px] font-normal leading-4 text-[#B70B0B] 2xl:text-[13px]">
          {captchaError || loadError}
        </p>
      ) : (
        <p className="m-0 h-4 text-[12px] leading-4 text-[#677883] 2xl:text-[13px]">
          Enter the code exactly as shown (case-sensitive).
        </p>
      )}
    </div>
  );
};

export default CaptchaWidget;
