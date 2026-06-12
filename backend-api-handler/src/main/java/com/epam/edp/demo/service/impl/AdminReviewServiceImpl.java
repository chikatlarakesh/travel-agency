package com.epam.edp.demo.service.impl;

import com.epam.edp.demo.dto.admin.AdminReviewDTO;
import com.epam.edp.demo.dto.admin.AdminReviewListResponseDTO;
import com.epam.edp.demo.entity.Review;
import com.epam.edp.demo.exception.TourNotFoundException;
import com.epam.edp.demo.repository.MongoReviewRepository;
import com.epam.edp.demo.service.AdminReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReviewServiceImpl implements AdminReviewService {

    private final MongoReviewRepository reviewRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public AdminReviewListResponseDTO getReviews(int page, int pageSize,
                                                  Integer rating, String tourType,
                                                  String visibility, Boolean flagged) {
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();

        if (rating != null)     criteria.add(Criteria.where("rate").is(rating));
        if (tourType != null && !tourType.isBlank()) criteria.add(Criteria.where("tourType").is(tourType.toUpperCase()));
        if (visibility != null && !visibility.isBlank()) criteria.add(Criteria.where("visibility").is(visibility.toUpperCase()));
        if (flagged != null)    criteria.add(Criteria.where("flagged").is(flagged));

        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }

        long totalItems = mongoTemplate.count(query, Review.class);
        int totalPages = totalItems == 0 ? 1 : (int) Math.ceil((double) totalItems / pageSize);
        int safePage   = Math.min(page, totalPages);

        query.skip((long) (safePage - 1) * pageSize).limit(pageSize);
        List<Review> reviews = mongoTemplate.find(query, Review.class);

        List<AdminReviewDTO> dtos = reviews.stream().map(this::toDTO).toList();
        return new AdminReviewListResponseDTO(dtos, safePage, pageSize, totalPages, (int) totalItems);
    }

    @Override
    @CacheEvict(value = "reviews", allEntries = true)
    public AdminReviewDTO updateVisibility(String reviewId, String visibility, String adminId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new TourNotFoundException("Review not found: " + reviewId));

        String previous = review.getVisibility();
        review.setVisibility(visibility);
        reviewRepository.save(review);

        log.info("admin.review.moderation adminId={} reviewId={} tourId={} action=visibility_change from={} to={}",
                adminId, reviewId, review.getTourId(), previous, visibility);

        return toDTO(review);
    }

    private AdminReviewDTO toDTO(Review r) {
        return AdminReviewDTO.builder()
                .id(r.getId())
                .tourId(r.getTourId())
                .tourName(r.getTourName())
                .tourType(r.getTourType())
                .userId(r.getUserId())
                .authorName(r.getAuthorName())
                .authorImageUrl(r.getAuthorImageUrl())
                .createdAt(r.getCreatedAt())
                .rate(r.getRate())
                .reviewContent(r.getReviewContent())
                .visibility(r.getVisibility())
                .flagged(r.isFlagged())
                .flagReason(r.getFlagReason())
                .build();
    }
}
