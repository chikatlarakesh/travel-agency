package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.admin.AdminReviewDTO;
import com.epam.edp.demo.dto.admin.AdminReviewListResponseDTO;

public interface AdminReviewService {

    AdminReviewListResponseDTO getReviews(int page, int pageSize,
                                          Integer rating, String tourType,
                                          String visibility, Boolean flagged);

    AdminReviewDTO updateVisibility(String reviewId, String visibility, String adminId);
}
