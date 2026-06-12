import api from './api';

/**
 * GET /users/{id}
 * @returns {Promise<{ firstName, lastName, imageUrl, role }>}
 */
export const getUserProfile = (userId) => api.get(`/users/${userId}`);

/**
 * PUT /users/{id}/name
 * @param {string} userId
 * @param {string} firstName
 * @param {string} lastName
 */
export const updateUserName = (userId, firstName, lastName) =>
  api.put(`/users/${userId}/name`, { firstName, lastName });

/**
 * PUT /users/{id}/password
 * @param {string} userId
 * @param {string} currentPassword
 * @param {string} newPassword
 */
export const updateUserPassword = (userId, currentPassword, newPassword) =>
  api.put(`/users/${userId}/password`, { currentPassword, newPassword });

/**
 * PUT /users/{id}/email
 * Sends a confirmation link to the new email address.
 * @param {string} userId
 * @param {string} newEmail
 * @param {string} password
 */
export const changeUserEmail = (userId, newEmail, password) =>
  api.put(`/users/${userId}/email`, { newEmail, password });

/**
 * POST /users/{id}/email/confirm
 * @param {string} userId
 * @param {string} confirmationToken
 */
export const confirmUserEmailChange = (userId, confirmationToken) =>
  api.post(`/users/${userId}/email/confirm`, { confirmationToken });

/**
 * PUT /users/{id}/image
 * @param {string} userId
 * @param {string} imageBase64  full data URI, e.g. "data:image/jpeg;base64,..."
 */
export const updateUserImage = (userId, imageBase64) =>
  api.put(`/users/${userId}/image`, { imageBase64 });
