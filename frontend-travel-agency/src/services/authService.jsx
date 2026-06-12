import api from './api';

/**
 * POST /auth/sign-in
 * @param {string} email
 * @param {string} password
 * @returns {Promise<{ idToken, role, userName, email }>}
 */
export const signIn = async (email, password) => {
  const response = await api.post('/auth/sign-in', { email, password });
  return response.data;           // { idToken, role, userName, email }
};

/**
 * GET /auth/captcha/generate
 * Fetches a new CAPTCHA challenge image (Base64 PNG) and its unique ID.
 * @returns {Promise<{ captchaId: string, imageBase64: string, expiresInSeconds: number }>}
 */
export const generateCaptcha = async () => {
  const response = await api.get('/auth/captcha/generate');
  return response.data;
};

/**
 * POST /auth/sign-up
 * @param {{ firstName, lastName, email, password, captchaId, captchaAnswer }} payload
 */
export const signUp = async ({ firstName, lastName, email, password, captchaId, captchaAnswer }) => {
  const response = await api.post('/auth/sign-up', {
    firstName,
    lastName,
    email,
    password,
    captchaId,
    captchaAnswer,
  });
  return response.data;
};

/**
 * POST /auth/logout
 */
export const signOut = async () => {
  await api.post('/auth/logout');
};

/**
 * POST /auth/refresh  (cookie-based — handled by axios interceptor)
 */
export const refreshToken = async () => {
  const response = await api.post('/auth/refresh');
  return response.data;
};

/**
 * POST /auth/forgot-password
 * Sends a verification code to the user's email.
 * @param {string} email
 */
export const forgotPassword = async (email) => {
  const response = await api.post('/auth/forgot-password', { email });
  return response.data;
};

/**
 * POST /auth/reset-password
 * Resets the user's password using the verification code from the email link.
 * @param {string} email
 * @param {string} verificationCode
 * @param {string} newPassword
 */
export const resetPassword = async (email, verificationCode, newPassword) => {
  const response = await api.post('/auth/reset-password', { email, verificationCode, newPassword });
  return response.data;
};

// ---------------------------------------------------------------------------
// Email-Verified Registration (3-step flow)
// ---------------------------------------------------------------------------

/**
 * POST /auth/initiate-registration  (step 1)
 * Validates email uniqueness and sends a 6-digit OTP to the given address.
 * @param {string} firstName
 * @param {string} lastName
 * @param {string} email
 * @param {string} captchaId
 * @param {string} captchaAnswer
 */
export const initiateRegistration = async (firstName, lastName, email, captchaId, captchaAnswer) => {
  const response = await api.post('/auth/initiate-registration', { firstName, lastName, email, captchaId, captchaAnswer });
  return response.data;
};

/**
 * POST /auth/verify-registration-code  (step 2)
 * Validates the OTP without consuming it.
 * @param {string} email
 * @param {string} verificationCode
 */
export const verifyRegistrationCode = async (email, verificationCode) => {
  const response = await api.post('/auth/verify-registration-code', { email, verificationCode });
  return response.data;
};

/**
 * POST /auth/complete-registration  (step 3)
 * Finalises account creation with the validated OTP and chosen password.
 * @param {string} email
 * @param {string} verificationCode
 * @param {string} password
 */
export const completeRegistration = async (email, verificationCode, password) => {
  const response = await api.post('/auth/complete-registration', { email, verificationCode, password });
  return response.data;
};

/**
 * POST /auth/oauth2/complete-signup
 * Finalises account creation for a first-time Google OAuth user.
 * @param {string} onboardingToken  – short-lived token issued by backend on /oauth2/signup redirect
 * @returns {Promise<{ idToken, role, userName, email }>}
 */
export const completeOAuthSignup = async (onboardingToken) => {
  const response = await api.post('/auth/oauth2/complete-signup', { onboardingToken });
  return response.data;
};

