package com.epam.edp.demo.mapper;

import com.epam.edp.demo.dto.tour.ReviewDTO;
import com.epam.edp.demo.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewDTO toDTO(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setAuthorName(review.getAuthorName());
        dto.setAuthorImageUrl(review.getAuthorImageUrl());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setRate(review.getRate());
        dto.setReviewContent(review.getReviewContent());
        return dto;
    }
}
