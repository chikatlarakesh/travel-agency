package com.epam.edp.demo.mapper;

import com.epam.edp.demo.dto.tour.ReviewDTO;
import com.epam.edp.demo.entity.Review;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReviewMapperTest {

    private final ReviewMapper mapper = new ReviewMapper();

    @Test
    public void toDTO_mapsAllFieldsCorrectly() {
        Review review = new Review();
        review.setId("r-1");
        review.setTourId("t-1");
        review.setUserId("u-1");
        review.setAuthorName("David");
        review.setAuthorImageUrl("https://example.com/david.jpg");
        review.setCreatedAt("2024-06-06");
        review.setRate(5);
        review.setReviewContent("Amazing experience!");

        ReviewDTO dto = mapper.toDTO(review);

        assertEquals("David",                        dto.getAuthorName());
        assertEquals("https://example.com/david.jpg", dto.getAuthorImageUrl());
        assertEquals("2024-06-06",                   dto.getCreatedAt());
        assertEquals(5,                              dto.getRate());
        assertEquals("Amazing experience!",          dto.getReviewContent());
    }
}
