import { useState, useEffect, useRef } from 'react';
import editIcon from '../../assets/icons/edit.svg';
import cameraIcon from '../../assets/icons/camera.svg';
import frameUserIcon from '../../assets/icons/Frameuser.svg';
import closeIcon from '../../assets/icons/Close.svg';
import { useAuth } from '../../context/AuthContext';
import { getUserProfile, updateUserName, updateUserPassword, changeUserEmail, updateUserImage } from '../../services/userService';

const navItems = [
  { id: 'general', label: 'General information' },
  { id: 'password', label: 'Change password' },
  { id: 'email', label: 'Change email' },
];

const NAME_RE = /^[A-Za-z\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u00FF'\\-]{1,50}$/;

const Profile = () => {
  const { user, updateUser } = useAuth();

  // Avatar state
  const avatarInputRef = useRef(null);
  const [avatarUrl, setAvatarUrl]           = useState('');
  const [isUploadingAvatar, setIsUploadingAvatar] = useState(false);

  const [activeSection, setActiveSection] = useState('general');
  const [isEditing, setIsEditing] = useState(false);
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [draftFirst, setDraftFirst] = useState('');
  const [draftLast, setDraftLast] = useState('');

  // Change password state
  const [oldPassword, setOldPassword]       = useState('');
  const [newPassword, setNewPassword]       = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showOld, setShowOld]               = useState(false);
  const [showNew, setShowNew]               = useState(false);
  const [showConfirm, setShowConfirm]       = useState(false);

  const pwRules = [
    { label: 'At least one uppercase letter required',  test: (v) => /[A-Z]/.test(v) },
    { label: 'At least one lowercase letter required',  test: (v) => /[a-z]/.test(v) },
    { label: 'At least one number required',            test: (v) => /[0-9]/.test(v) },
    { label: 'At least one special character required', test: (v) => /[^A-Za-z0-9]/.test(v) },
    { label: 'Password must be 8-16 characters long',   test: (v) => v.length >= 8 && v.length <= 16 },
    { label: 'Confirm password must match new password', test: () => confirmPassword === newPassword && confirmPassword !== '' },
  ];
  const allPwRulesMet = pwRules.every((r) => r.test(newPassword)) && confirmPassword === newPassword && confirmPassword !== '';
  const canSavePw = oldPassword.trim() !== '' && allPwRulesMet;

  // Change email state
  const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const displayEmail = user?.email || '';
  const [newEmail, setNewEmail]         = useState('');
  const [confirmEmail, setConfirmEmail] = useState('');
  const [emailPassword, setEmailPassword] = useState('');
  const [showEmailPw, setShowEmailPw]   = useState(false);
  const newEmailError     = newEmail     && !EMAIL_RE.test(newEmail.trim())     ? 'Please enter a valid email address.' : '';
  const confirmEmailError = confirmEmail && confirmEmail.trim() !== newEmail.trim() ? "Emails don't match." : '';
  const confirmEmailMatch = confirmEmail && confirmEmail.trim() === newEmail.trim() && EMAIL_RE.test(newEmail.trim());
  const canSaveEmail = !newEmailError && !confirmEmailError && newEmail.trim() !== '' && confirmEmailMatch && emailPassword.trim() !== '';
  const [emailSubmitted, setEmailSubmitted] = useState(false);

  // API loading / error state
  const [isSavingName, setIsSavingName]   = useState(false);
  const [nameApiError, setNameApiError]   = useState('');
  const [isSavingPw, setIsSavingPw]       = useState(false);
  const [isSavingEmail, setIsSavingEmail] = useState(false);
  const [emailApiError, setEmailApiError] = useState('');

  const [showToast, setShowToast] = useState(false);
  const [toastLeaving, setToastLeaving] = useState(false);
  const toastTimerRef = useRef(null);
  const toastLeaveRef = useRef(null);

  const [showErrorToast, setShowErrorToast] = useState(false);
  const [errorToastLeaving, setErrorToastLeaving] = useState(false);
  const [toastErrorMessage, setToastErrorMessage] = useState('');
  const errorToastTimerRef = useRef(null);
  const errorToastLeaveRef = useRef(null);

  useEffect(() => {
    if (showToast) {
      toastTimerRef.current = setTimeout(() => dismissToast(), 5000);
    }
    return () => {
      clearTimeout(toastTimerRef.current);
      clearTimeout(toastLeaveRef.current);
    };
  }, [showToast]); // eslint-disable-line

  useEffect(() => {
    if (showErrorToast) {
      errorToastTimerRef.current = setTimeout(() => dismissErrorToast(), 5000);
    }
    return () => {
      clearTimeout(errorToastTimerRef.current);
      clearTimeout(errorToastLeaveRef.current);
    };
  }, [showErrorToast]); // eslint-disable-line

  const dismissToast = () => {
    setToastLeaving(true);
    toastLeaveRef.current = setTimeout(() => {
      setShowToast(false);
      setToastLeaving(false);
    }, 400);
  };

  const dismissErrorToast = () => {
    setErrorToastLeaving(true);
    errorToastLeaveRef.current = setTimeout(() => {
      setShowErrorToast(false);
      setErrorToastLeaving(false);
    }, 400);
  };

  // Load profile from API on mount
  useEffect(() => {
    if (!user?.userId) return;
    getUserProfile(user.userId)
      .then((res) => {
        const fn = res.data.firstName || '';
        const ln = res.data.lastName  || '';
        setFirstName(fn); setDraftFirst(fn);
        setLastName(ln);  setDraftLast(ln);
        setAvatarUrl(res.data.imageUrl || '');
        // Keep context (and localStorage) in sync with the latest data from DB
        updateUser({
          imageUrl: res.data.imageUrl || '',
          userName: `${fn} ${ln}`.trim(),
        });
      })
      .catch(() => {
        // Fallback: split userName stored at login
        const parts = (user.userName || '').split(' ');
        const fn = parts[0] || '';
        const ln = parts.slice(1).join(' ') || '';
        setFirstName(fn); setDraftFirst(fn);
        setLastName(ln);  setDraftLast(ln);
      });
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleAvatarChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = async () => {
      const base64 = reader.result;
      setIsUploadingAvatar(true);
      try {
        await updateUserImage(user.userId, base64);
        setAvatarUrl(base64);
        updateUser({ imageUrl: base64 });
      } catch {
        // silently ignore — avatar stays as-is
      } finally {
        setIsUploadingAvatar(false);
        // reset so same file can be re-selected
        e.target.value = '';
      }
    };
    reader.readAsDataURL(file);
  };

  const firstNameError = !NAME_RE.test(draftFirst)
    ? 'First name must be up to 50 characters. Only Latin letters, hyphens, and apostrophes are allowed.'
    : '';
  const lastNameError = !NAME_RE.test(draftLast)
    ? 'Last name must be up to 50 characters. Only Latin letters, hyphens, and apostrophes are allowed.'
    : '';
  const canSave = !firstNameError && !lastNameError;

  const handleEdit = () => {
    setDraftFirst(firstName);
    setDraftLast(lastName);
    setIsEditing(true);
  };

  const handleCancel = () => {
    setIsEditing(false);
  };

  const handleSave = async () => {
    if (!canSave || isSavingName) return;
    setIsSavingName(true);
    setNameApiError('');
    try {
      await updateUserName(user.userId, draftFirst, draftLast);
      setFirstName(draftFirst);
      setLastName(draftLast);
      updateUser({ userName: `${draftFirst} ${draftLast}` });
      setIsEditing(false);
      setShowToast(true);
    } catch (err) {
      setNameApiError(err.response?.data?.message || 'Failed to save changes. Please try again.');
    } finally {
      setIsSavingName(false);
    }
  };

  const handleSavePw = async () => {
    if (!canSavePw || isSavingPw) return;
    setIsSavingPw(true);
    try {
      await updateUserPassword(user.userId, oldPassword, newPassword);
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setActiveSection('general');
      setShowToast(true);
    } catch (err) {
      // Extract error message from backend response safely
      let errorMessage = 'Failed to update password. Please try again.';
      
      if (err.response?.data?.message && typeof err.response.data.message === 'string') {
        errorMessage = err.response.data.message;
      } else if (err.response?.data?.error && typeof err.response.data.error === 'string') {
        errorMessage = err.response.data.error;
      } else if (typeof err.response?.data === 'string') {
        errorMessage = err.response.data;
      } else if (typeof err.message === 'string') {
        errorMessage = err.message;
      }
      
      setToastErrorMessage(errorMessage);
      setShowErrorToast(true);
    } finally {
      setIsSavingPw(false);
    }
  };

  const handleEmailConfirm = async () => {
    if (!canSaveEmail || isSavingEmail) return;
    setIsSavingEmail(true);
    setEmailApiError('');
    try {
      await changeUserEmail(user.userId, newEmail, emailPassword);
      setEmailSubmitted(true);
    } catch (err) {
      setEmailApiError(err.response?.data?.message || 'Failed to send confirmation email. Please try again.');
    } finally {
      setIsSavingEmail(false);
    }
  };

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

  const cardContent = (
    <>
      {activeSection === 'general' && (
        <div
          className="flex w-full flex-col gap-3 rounded-[12px] bg-white p-6 shadow-[0px_2px_10px_6px_#027EAC33] md:w-[544px]"
        >

          {/* Top: same in both modes */}
          <div className="flex min-h-[48px] w-full items-center justify-between">
            <span className="font-nunito text-[20px] font-bold leading-[40px] text-[#0B3857] md:text-[24px]">
              General information
            </span>
            {!isEditing && (
              <button
                type="button"
                onClick={handleEdit}
                className="ml-2 shrink-0"
                aria-label="Edit general information"
              >
                <img src={editIcon} alt="Edit" width="24" height="24" />
              </button>
            )}
          </div>

          <input
            ref={avatarInputRef}
            type="file"
            accept="image/*"
            className="hidden"
            onChange={handleAvatarChange}
          />

          {/* View mode: avatar + name rows */}
          {!isEditing && (
            <div className="flex w-full flex-wrap gap-6 md:flex-nowrap">
              {/* Photo — view mode */}
              <div className="relative h-[120px] w-[120px] shrink-0">
                <div
                  className="flex h-[120px] w-[120px] items-center justify-center overflow-hidden"
                  style={{
                    borderRadius: '1552.68px',
                    background: '#A2AEB9',
                    ...(avatarUrl ? {} : { paddingTop: '20.7px', paddingBottom: '20.7px', paddingLeft: '41.4px', paddingRight: '41.4px' }),
                  }}
                >
                  {avatarUrl
                    ? <img src={avatarUrl} alt="User avatar" className="h-full w-full object-cover" style={{ borderRadius: '1552.68px' }} />
                    : <img src={frameUserIcon} alt="User avatar" style={{ width: '57.73px', height: '57.73px' }} />
                  }
                </div>
                <button
                  type="button"
                  aria-label="Upload photo"
                  disabled={isUploadingAvatar}
                  onClick={() => avatarInputRef.current?.click()}
                  style={{
                    width: '40px', height: '40px', borderRadius: '1000000px',
                    border: '2px solid #027EAC', background: '#FFFFFF',
                    position: 'absolute', bottom: 0, right: 0, padding: 0,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    opacity: isUploadingAvatar ? 0.5 : 1,
                  }}
                >
                  <img src={cameraIcon} alt="Upload" style={{ width: '24px', height: '24px' }} />
                </button>
              </div>
              {/* Name rows */}
              <div className="flex flex-1 flex-col gap-2 self-start">
                <div className="flex h-[24px] w-full items-center gap-4 md:gap-[33px]">
                  <span className="w-[72px] shrink-0 font-nunito text-[14px] font-extrabold leading-[24px] text-[#0B3857]">First name</span>
                  <span className="font-nunito text-[14px] font-normal leading-[24px] text-[#0B3857]">{firstName}</span>
                </div>
                <div className="flex h-[24px] w-full items-center gap-4 md:gap-[33px]">
                  <span className="w-[72px] shrink-0 font-nunito text-[14px] font-extrabold leading-[24px] text-[#0B3857]">Last name</span>
                  <span className="font-nunito text-[14px] font-normal leading-[24px] text-[#0B3857]">{lastName}</span>
                </div>
              </div>
            </div>
          )}

          {/* Edit mode: inner container 496×288, gap 24 */}
          {isEditing && (
            <div className="flex w-full flex-col gap-6">

              {/* Main info: photo + frame side by side */}
              <div className="flex w-full gap-6">

                {/* Photo + upload button — edit mode */}
                <div className="relative h-[120px] w-[120px] shrink-0">
                  <div
                    className="flex h-[120px] w-[120px] items-center justify-center overflow-hidden"
                    style={{
                      borderRadius: '1552.68px',
                      background: '#A2AEB9',
                      ...(avatarUrl ? {} : { paddingTop: '20.7px', paddingBottom: '20.7px', paddingLeft: '41.4px', paddingRight: '41.4px' }),
                    }}
                  >
                    {avatarUrl
                      ? <img src={avatarUrl} alt="User avatar" className="h-full w-full object-cover" style={{ borderRadius: '1552.68px' }} />
                      : <img src={frameUserIcon} alt="User avatar" style={{ width: '57.73px', height: '57.73px' }} />
                    }
                  </div>
                  <button
                    type="button"
                    aria-label="Upload photo"
                    disabled={isUploadingAvatar}
                    onClick={() => avatarInputRef.current?.click()}
                    style={{
                      width: '40px', height: '40px', borderRadius: '1000000px',
                      border: '2px solid #027EAC', background: '#FFFFFF',
                      position: 'absolute', bottom: 0, right: 0, padding: 0,
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      opacity: isUploadingAvatar ? 0.5 : 1,
                    }}
                  >
                    <img src={cameraIcon} alt="Upload" style={{ width: '24px', height: '24px' }} />
                  </button>
                </div>

                {/* Frame: 352×auto, gap 16 */}
                <div className="flex w-[352px] flex-col gap-4">

                  {/* Input 1 */}
                  <div className="flex w-[352px] flex-col gap-1">
                    <label className="font-nunito text-[14px] font-extrabold leading-[24px] text-[#0B3857]">First name</label>
                    <input
                      type="text"
                      value={draftFirst}
                      onChange={(e) => setDraftFirst(e.target.value)}
                      placeholder="e.g. Johnson"
                      className={`h-[56px] w-[352px] rounded-[8px] border px-4 font-nunito text-[14px] leading-6 text-[#0B3857] outline-none transition-colors ${
                        firstNameError
                          ? 'border-[#B70B0B] focus:border-[#B70B0B]'
                          : 'border-[#D3E1ED] focus:border-[#027EAC]'
                      }`}
                    />
                    {firstNameError
                      ? <span className="w-[352px] font-nunito text-[12px] font-normal leading-[16px] text-[#B70B0B]">{firstNameError}</span>
                      : <span className="h-[16px] w-[352px] font-nunito text-[12px] font-normal leading-[16px] text-[#677883]">e.g. Johnson</span>
                    }
                  </div>

                  {/* Input 2 */}
                  <div className="flex w-[352px] flex-col gap-1">
                    <label className="font-nunito text-[14px] font-extrabold leading-[24px] text-[#0B3857]">Last name</label>
                    <input
                      type="text"
                      value={draftLast}
                      onChange={(e) => setDraftLast(e.target.value)}
                      placeholder="e.g. Doe"
                      className={`h-[56px] w-[352px] rounded-[8px] border px-4 font-nunito text-[14px] leading-6 text-[#0B3857] outline-none transition-colors ${
                        lastNameError
                          ? 'border-[#B70B0B] focus:border-[#B70B0B]'
                          : 'border-[#D3E1ED] focus:border-[#027EAC]'
                      }`}
                    />
                    {lastNameError
                      ? <span className="w-[352px] font-nunito text-[12px] font-normal leading-[16px] text-[#B70B0B]">{lastNameError}</span>
                      : <span className="h-[16px] w-[352px] font-nunito text-[12px] font-normal leading-[16px] text-[#677883]">e.g. Doe</span>
                    }
                  </div>

                </div>

              </div>

              {/* Actions: 360×40, gap 8 */}
              <div className="flex flex-col gap-1 self-end">
                {nameApiError && (
                  <span className="font-nunito text-[12px] font-normal text-[#B70B0B]">{nameApiError}</span>
                )}
                <div className="flex h-[40px] w-[360px] items-center justify-end gap-2">
                  <button
                    type="button"
                    onClick={handleCancel}
                    className="h-[40px] rounded-[8px] border-2 border-[#027EAC] bg-white px-4 py-2 font-nunito text-[14px] font-bold leading-6 text-[#027EAC] transition hover:bg-[#E7F9FF]"
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    onClick={handleSave}
                    disabled={!canSave || isSavingName}
                    className="h-[40px] rounded-[8px] bg-[#027EAC] px-4 py-2 font-nunito text-[14px] font-bold leading-6 text-white transition hover:bg-[#025f84] disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {isSavingName ? 'Saving…' : 'Save changes'}
                  </button>
                </div>
              </div>

            </div>
          )}

        </div>
      )}
      {activeSection === 'password' && (
        <div className="flex w-full flex-col rounded-[12px] bg-white shadow-[0px_2px_10px_6px_#027EAC33] md:w-[544px]">

          {/* Tab: 216×48, border-bottom, py-1 px-0 pr-4 */}
          <div
            className="mx-6 mt-6 flex items-center"
            style={{ paddingTop: '4px', paddingBottom: '4px', paddingRight: '16px', gap: '8px', width: 'fit-content', minWidth: '216px' }}
          >
            <span className="font-nunito text-[20px] font-bold leading-[40px] text-[#0B3857] md:text-[24px]">
              Change password
            </span>
          </div>

          {/* Inner container: 496×484, gap 40px */}
          <div className="flex w-full flex-col gap-10 p-6">

            {/* Main info: 496×404, gap 16px */}
            <div className="flex w-full flex-col gap-4">

              {/* Old password — 496×84 */}
              <div className="flex h-[84px] w-full flex-col gap-1">
                <label className="h-[24px] font-nunito text-[14px] font-extrabold leading-[24px] text-[#0B3857]">Old password</label>
                <div className="relative">
                  <input
                    type={showOld ? 'text' : 'password'}
                    value={oldPassword}
                    onChange={(e) => setOldPassword(e.target.value)}
                    placeholder="Enter your password"
                    className="h-[56px] w-full rounded-[8px] border border-[#D3E1ED] bg-white px-4 pr-12 font-nunito text-[14px] leading-6 text-[#0B3857] outline-none transition-colors focus:border-[#027EAC]"
                  />
                  <button type="button" onClick={() => setShowOld((v) => !v)} className="absolute right-4 top-1/2 -translate-y-1/2 outline-none" aria-label={showOld ? 'Hide password' : 'Show password'}>
                    {showOld ? <EyeOpen /> : <EyeOff />}
                  </button>
                </div>
              </div>

              {/* New password — 496×168 (5 rules only) */}
              <div className="flex h-[168px] w-full flex-col gap-1">
                <label className="h-[24px] font-nunito text-[14px] font-extrabold leading-[24px] text-[#0B3857]">New password</label>
                <div className="relative">
                  <input
                    type={showNew ? 'text' : 'password'}
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="Enter your new password"
                    className="h-[56px] w-full rounded-[8px] border border-[#D3E1ED] bg-white px-4 pr-12 font-nunito text-[14px] leading-6 text-[#0B3857] outline-none transition-colors focus:border-[#027EAC]"
                  />
                  <button type="button" onClick={() => setShowNew((v) => !v)} className="absolute right-4 top-1/2 -translate-y-1/2 outline-none" aria-label={showNew ? 'Hide password' : 'Show password'}>
                    {showNew ? <EyeOpen /> : <EyeOff />}
                  </button>
                </div>
                <div className="flex w-full flex-col">
                  {pwRules.slice(0, 5).map((rule) => {
                    const passing = newPassword ? rule.test(newPassword) : null;
                    const color = passing === false ? 'text-[#B70B0B]' : passing === true ? 'text-[#15803D]' : 'text-[#677883]';
                    const dot   = passing === false ? 'bg-[#B70B0B]'   : passing === true ? 'bg-[#15803D]'   : 'bg-[#677883]';
                    return (
                      <div key={rule.label} className="flex h-4 items-center gap-2">
                        <span className={`h-2 w-2 shrink-0 rounded-full ${dot}`} />
                        <p className={`m-0 text-[12px] font-normal leading-4 ${color}`}>{rule.label}</p>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Confirm password — 496×120 (input + match rule) */}
              <div className="flex h-[120px] w-full flex-col gap-1">
                <label className="h-[24px] font-nunito text-[14px] font-extrabold leading-[24px] text-[#0B3857]">Confirm password</label>
                <div className="relative">
                  <input
                    type={showConfirm ? 'text' : 'password'}
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="Confirm your new password"
                    className="h-[56px] w-full rounded-[8px] border border-[#D3E1ED] bg-white px-4 pr-12 font-nunito text-[14px] leading-6 text-[#0B3857] outline-none transition-colors focus:border-[#027EAC]"
                  />
                  <button type="button" onClick={() => setShowConfirm((v) => !v)} className="absolute right-4 top-1/2 -translate-y-1/2 outline-none" aria-label={showConfirm ? 'Hide password' : 'Show password'}>
                    {showConfirm ? <EyeOpen /> : <EyeOff />}
                  </button>
                </div>
                {(() => {
                  const passing = confirmPassword ? confirmPassword === newPassword : null;
                  const color = passing === false ? 'text-[#B70B0B]' : passing === true ? 'text-[#15803D]' : 'text-[#677883]';
                  const dot   = passing === false ? 'bg-[#B70B0B]'   : passing === true ? 'bg-[#15803D]'   : 'bg-[#677883]';
                  const label = passing === false ? "Passwords don't match" : passing === true ? 'Passwords match' : 'Confirm password must match new password';
                  return (
                    <div className="flex h-4 items-center gap-2">
                      <span className={`h-2 w-2 shrink-0 rounded-full ${dot}`} />
                      <p className={`m-0 text-[12px] font-normal leading-4 ${color}`}>{label}</p>
                    </div>
                  );
                })()}
              </div>

            </div>

            {/* Actions: 496×40, gap 8px */}
            <div className="flex w-full flex-col items-end gap-1">
              <div className="flex h-[40px] w-full items-center justify-end gap-2">
                <button
                  type="button"
                  onClick={handleSavePw}
                  disabled={!canSavePw || isSavingPw}
                  className="h-[40px] rounded-[8px] bg-[#027EAC] px-4 py-2 font-nunito text-[14px] font-bold leading-6 text-white transition hover:bg-[#025f84] disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {isSavingPw ? 'Saving…' : 'Save changes'}
                </button>
              </div>
            </div>

          </div>
        </div>
      )}
      {activeSection === 'email' && (
        <>
        {emailSubmitted ? (
          /* Success card: 544×156, border-radius 12px, padding 24px */
          <div className="flex w-full flex-col gap-3 rounded-[12px] bg-white p-6 shadow-[0px_2px_10px_6px_#027EAC33] md:w-[544px]">

            {/* Tab: 169×48, pt:4 pb:4 pr:16, gap 8 */}
            <div
              className="flex items-center"
              style={{ width: '169px', height: '48px', paddingTop: '4px', paddingBottom: '4px', paddingRight: '16px', gap: '8px' }}
            >
              <span className="font-nunito text-[24px] font-bold leading-[40px] text-[#0B3857]">
                Change email
              </span>
            </div>

            {/* Text: 496×48, Nunito 400 14px lh:24px #0B3857 — 2 lines */}
            <p
              className="m-0 font-nunito text-[14px] font-normal text-[#0B3857]"
              style={{ width: '496px', maxWidth: '100%', lineHeight: '24px' }}
            >
              We sent an email to {`{${newEmail}}`} with a confirmation link.{' '}
              Please follow the instructions in the email.
            </p>

          </div>
        ) : (
        <div className="flex w-full flex-col rounded-[12px] bg-white shadow-[0px_2px_10px_6px_#027EAC33] md:w-[544px]">

          {/* Tab: 169×48 */}
          <div
            className="mx-6 mt-6 flex items-center"
            style={{ paddingTop: '4px', paddingBottom: '4px', paddingRight: '16px', gap: '8px', width: 'fit-content', minWidth: '169px' }}
          >
            <span className="font-nunito text-[20px] font-bold leading-[40px] text-[#0B3857] md:text-[24px]">
              Change email
            </span>
          </div>

          {/* Inner container: 496×444, gap 40px */}
          <div className="flex w-full flex-col gap-10 p-6">

            <>
            {/* Main info: 496×364, gap 16px */}
            <div className="flex w-full flex-col gap-4">

              {/* Frame: 287×24, gap 8px — "Current email:" text + inner frame with email value */}
              <div className="flex h-[24px] items-center gap-2" style={{ width: '287px' }}>
                {/* Text: 95×24, Nunito 800 14px #0B3857 */}
                <span className="shrink-0 font-nunito text-[14px] font-extrabold leading-[24px] text-[#0B3857]" style={{ width: '95px' }}>Current email:</span>
                {/* Inner frame: 158×24, gap 8px */}
                <div className="flex items-center gap-2" style={{ width: '158px' }}>
                  {/* Inner frame text: 158×24, Nunito 400 14px #0B3857 */}
                  <span className="truncate font-nunito text-[14px] font-normal leading-[24px] text-[#0B3857]" style={{ width: '158px' }}>{displayEmail}</span>
                </div>
              </div>

              {/* New email — 496×104, gap 4px */}
              <div className="flex h-[104px] w-full flex-col gap-1">
                {/* Label + input: 496×84, gap 4px */}
                <div className="flex h-[84px] w-full flex-col gap-1">
                  <label className="h-[24px] font-nunito text-[14px] font-extrabold leading-[24px] text-[#0B3857]">New email</label>
                  <input
                    type="email"
                    value={newEmail}
                    onChange={(e) => setNewEmail(e.target.value)}
                    placeholder="Enter your new email"
                    className={`h-[56px] w-full rounded-[8px] border bg-white px-4 font-nunito text-[14px] leading-6 text-[#0B3857] outline-none transition-colors ${
                      newEmailError ? 'border-[#B70B0B] focus:border-[#B70B0B]' : 'border-[#D3E1ED] focus:border-[#027EAC]'
                    }`}
                  />
                </div>
                {/* $input hint: 496×16, Nunito 400 12px #677883 */}
                {newEmailError
                  ? <span className="h-[16px] w-full font-nunito text-[12px] font-normal leading-[16px] text-[#B70B0B]">{newEmailError}</span>
                  : <span className="h-[16px] w-full font-nunito text-[12px] font-normal leading-[16px] text-[#677883]">e.g. username@domain.com</span>
                }
              </div>

              {/* Confirm new email — 496×104, gap 4px */}
              <div className="flex h-[104px] w-full flex-col gap-1">
                {/* Label + input: 496×84, gap 4px */}
                <div className="flex h-[84px] w-full flex-col gap-1">
                  <label className="h-[24px] font-nunito text-[14px] font-extrabold leading-[24px] text-[#0B3857]">Confirm new email</label>
                  <input
                    type="email"
                    value={confirmEmail}
                    onChange={(e) => setConfirmEmail(e.target.value)}
                    placeholder="Confirm your new email"
                    className={`h-[56px] w-full rounded-[8px] border bg-white px-4 font-nunito text-[14px] leading-6 text-[#0B3857] outline-none transition-colors ${
                      confirmEmailError ? 'border-[#B70B0B] focus:border-[#B70B0B]' : 'border-[#D3E1ED] focus:border-[#027EAC]'
                    }`}
                  />
                </div>
                {/* $input hint: 496×16, Nunito 400 12px */}
                {confirmEmailError
                  ? <span className="h-[16px] w-full font-nunito text-[12px] font-normal leading-[16px] text-[#B70B0B]">{confirmEmailError}</span>
                  : confirmEmailMatch
                  ? <span className="h-[16px] w-full font-nunito text-[12px] font-normal leading-[16px] text-[#15803D]">Emails match.</span>
                  : <span className="h-[16px] w-full font-nunito text-[12px] font-normal leading-[16px] text-[#677883]">e.g. username@domain.com</span>
                }
              </div>

              {/* Current password — 496×84 */}
              <div className="flex h-[84px] w-full flex-col gap-1">
                <label className="h-[24px] font-nunito text-[14px] font-extrabold leading-[24px] text-[#0B3857]">Current password</label>
                <div className="relative">
                  <input
                    type={showEmailPw ? 'text' : 'password'}
                    value={emailPassword}
                    onChange={(e) => setEmailPassword(e.target.value)}
                    placeholder="Enter your password"
                    className="h-[56px] w-full rounded-[8px] border border-[#D3E1ED] bg-white px-4 pr-12 font-nunito text-[14px] leading-6 text-[#0B3857] outline-none transition-colors focus:border-[#027EAC]"
                  />
                  <button type="button" onClick={() => setShowEmailPw((v) => !v)} className="absolute right-4 top-1/2 -translate-y-1/2 outline-none" aria-label={showEmailPw ? 'Hide password' : 'Show password'}>
                    {showEmailPw ? <EyeOpen /> : <EyeOff />}
                  </button>
                </div>
              </div>

            </div>

            {/* Actions: 496×40, gap 8px */}
            <div className="flex w-full flex-col items-end gap-1">
              {emailApiError && (
                <span className="font-nunito text-[12px] font-normal text-[#B70B0B]">{emailApiError}</span>
              )}
              <div className="flex h-[40px] w-full items-center justify-end gap-2">
                <button
                  type="button"
                  disabled={!canSaveEmail || isSavingEmail}
                  onClick={handleEmailConfirm}
                  className="h-[40px] rounded-[8px] bg-[#027EAC] px-4 py-2 font-nunito text-[14px] font-bold leading-6 text-white transition hover:bg-[#025f84] disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {isSavingEmail ? 'Sending…' : 'Confirm'}
                </button>
              </div>
            </div>

            </>

          </div>
        </div>
        )}
        </>
      )}
    </>
  );

  // Sidebar: w-168, h-200, gap-24px between heading and tabs group
  const Sidebar = ({ mobile }) => (
    <div
      className={
        mobile
          ? 'flex w-full flex-col gap-6 mb-8'
          : 'flex w-[168px] flex-col gap-6'
      }
    >
      {/* "My profile" heading — Nunito 600 32px, lh 40px, #0B3857 */}
      <h1 className="font-nunito text-[32px] font-semibold leading-[40px] text-[#0B3857]">
        My profile
      </h1>

      {/* Tabs group — w-168, h-136, gap-8px between tabs */}
      <div className="flex flex-col gap-2">
        {navItems.map((item) => {
          const isActive = activeSection === item.id;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => setActiveSection(item.id)}
              className="flex h-[40px] w-full flex-col items-start pt-1 pb-1 md:w-[168px]"
            >
              {/* Text + bar wrapped together so bar matches text width */}
              <span className="relative inline-block">
                <span
                  className={`block h-8 text-left font-nunito text-base leading-8 transition-colors ${
                    isActive ? 'font-semibold text-[#0B3857]' : 'font-medium text-[#677883] hover:text-[#0B3857]'
                  }`}
                >
                  {item.label}
                </span>
                {/* Active bar: width matches text, h-5, border-radius-6, #027EAC */}
                {isActive && (
                  <span className="absolute bottom-0 left-0 h-[5px] w-full rounded-[6px] bg-[#027EAC]" />
                )}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );

  return (
    <div className="mx-auto w-full max-w-[1440px]">

      {/* Mobile (< md): sidebar on top, card below, padded */}
      <div className="flex min-h-[calc(100vh-72px)] flex-col gap-6 overflow-y-auto px-4 py-8 md:hidden">
        <Sidebar mobile />
        <div className="flex flex-1 items-start justify-center pb-8">
          {cardContent}
        </div>
      </div>

      {/* Tablet (md–lg): sidebar left, card top-aligned */}
      <div className="hidden overflow-y-auto md:flex lg:hidden">
        <div className="w-[168px] shrink-0 px-6 pt-10">
          <Sidebar />
        </div>
        <div className="flex flex-1 items-start justify-center py-10 pb-10">
          {cardContent}
        </div>
      </div>

      {/* Desktop (lg–xl): sidebar absolute left, card top-aligned with padding */}
      <div className="relative hidden lg:flex lg:items-start lg:justify-center lg:py-10 xl:hidden">
        <div className="absolute left-[40px] top-[40px]">
          <Sidebar />
        </div>
        {cardContent}
      </div>

      {/* Large screens (xl+): sidebar absolute left, card top-aligned with padding */}
      <div className="relative hidden xl:flex xl:items-start xl:justify-center xl:py-10">
        <div className="absolute left-[40px] top-[40px]">
          <Sidebar />
        </div>
        {cardContent}
      </div>

      {/* Success toast — 406×76, top:88px, padding:12px */}
      {showToast && (
        <div
          className="fixed right-10 z-50 w-[min(406px,calc(100%-32px))]"
          style={{
            top: '88px',
            height: '76px',
            background: '#EDFFEE',
            border: '1px solid #118819',
            borderRadius: '4px',
            padding: '12px',
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
          {/* Outer row: frame (350×52) + close icon (24×24), gap 8px */}
          <div className="flex items-start gap-2">
            {/* Frame: green tick (24×24) + text block (318×52), gap 8px */}
            <div className="flex items-start gap-2" style={{ width: '350px' }}>
              {/* Green tick icon */}
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style={{ flexShrink: 0 }}>
                <circle cx="12" cy="12" r="10" fill="#118819" />
                <path d="M7.5 12.5L10.5 15.5L16.5 9" stroke="#FFFFFF" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
              {/* Text: 318×52, gap 4px */}
              <div className="flex flex-col gap-1" style={{ width: '318px' }}>
                <p className="m-0 font-nunito text-[14px] font-extrabold leading-6 text-[#0B3857]">Success</p>
                <p className="m-0 font-nunito text-[14px] font-normal leading-6 text-[#0B3857]">
                  Your account has been updated successfully.
                </p>
              </div>
            </div>
            {/* Close icon 24×24 */}
            <button
              type="button"
              onClick={dismissToast}
              className="shrink-0 outline-none"
              aria-label="Close notification"
            >
              <img src={closeIcon} alt="Close" width={24} height={24} />
            </button>
          </div>
        </div>
      )}

      {/* Error toast — 406×76, top:88px, padding:12px */}
      {showErrorToast && (
        <div
          className="fixed right-10 z-50 w-[min(406px,calc(100%-32px))]"
          style={{
            top: '88px',
            height: '76px',
            background: '#FCE9ED',
            border: '1px solid #C0352A',
            borderRadius: '4px',
            padding: '12px',
            animation: errorToastLeaving
              ? 'errorToastOut 0.4s ease forwards'
              : 'errorToastIn 0.4s ease forwards',
          }}
        >
          <style>{`
            @keyframes errorToastIn {
              from { opacity: 0; transform: translateY(-12px); }
              to   { opacity: 1; transform: translateY(0); }
            }
            @keyframes errorToastOut {
              from { opacity: 1; transform: translateY(0); }
              to   { opacity: 0; transform: translateY(-12px); }
            }
          `}</style>
          {/* Outer row: frame (350×52) + close icon (24×24), gap 8px */}
          <div className="flex items-start gap-2">
            {/* Frame: error icon (24×24) + text block (318×52), gap 8px */}
            <div className="flex items-start gap-2" style={{ width: '350px' }}>
              {/* Error icon */}
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" style={{ flexShrink: 0 }}>
                <circle cx="12" cy="12" r="10" fill="#C0352A" />
                <path d="M12 7V13" stroke="#FFFFFF" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round"/>
                <circle cx="12" cy="17" r="0.5" fill="#FFFFFF" stroke="#FFFFFF" strokeWidth="1.75"/>
              </svg>
              {/* Text: 318×52, gap 4px */}
              <div className="flex flex-col gap-1" style={{ width: '318px' }}>
                <p className="m-0 font-nunito text-[14px] font-extrabold leading-6 text-[#0B3857]">Error</p>
                <p className="m-0 font-nunito text-[14px] font-normal leading-6 text-[#0B3857]">
                  {toastErrorMessage}
                </p>
              </div>
            </div>
            {/* Close icon 24×24 */}
            <button
              type="button"
              onClick={dismissErrorToast}
              className="shrink-0 outline-none"
              aria-label="Close notification"
            >
              <img src={closeIcon} alt="Close" width={24} height={24} />
            </button>
          </div>
        </div>
      )}

    </div>
  );
};

export default Profile;


