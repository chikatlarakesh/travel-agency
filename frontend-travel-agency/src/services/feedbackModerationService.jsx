import api from './api';

export const fetchAdminReviews = async ({ page = 1, pageSize = 20, rating, tourType, visibility, flagged } = {}) => {
  const params = { page, pageSize };
  if (rating !== undefined && rating !== null && rating !== '') params.rating = rating;
  if (tourType) params.tourType = tourType;
  if (visibility) params.visibility = visibility;
  if (flagged !== undefined && flagged !== null) params.flagged = flagged;

  const response = await api.get('/admin/reviews', { params });
  return response.data;
};

export const updateReviewVisibility = async (reviewId, visibility) => {
  const response = await api.patch(`/admin/reviews/${reviewId}/visibility`, { visibility });
  return response.data;
};
