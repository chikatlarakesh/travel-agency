import api from './api';

/**
 * POST /api/v1/tours/{tourId}/feedbacks
 * Submits (or updates) feedback for a tour.
 */
export const submitFeedback = async (tourId, { rating, comment }) => {
  const response = await api.post(`/tours/${tourId}/feedbacks`, { rating, comment });
  return response.data;
};

/**
 * GET /api/v1/tours/{tourId}/reviews
 * Returns all reviews for a tour (used on TourDetail page).
 */
export const getTourFeedbacks = async (tourId) => {
  const response = await api.get(`/tours/${tourId}/reviews`, {
    params: { page: 1, pageSize: 100, sortBy: 'RATING_DESC' },
  });
  return response.data;
};

/**
 * GET /api/v1/tours/{tourId}/feedbacks/me
 * Returns the current user's own review for a tour.
 */
export const getMyFeedback = async (tourId) => {
  const response = await api.get(`/tours/${tourId}/feedbacks/me`);
  return response.data;
};

