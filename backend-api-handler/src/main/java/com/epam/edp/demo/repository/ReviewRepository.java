package com.epam.edp.demo.repository;

import com.epam.edp.demo.entity.Review;

import java.util.List;

public interface ReviewRepository {

    List<Review> findByTourId(String tourId);

    boolean existsByUserIdAndTourId(String userId, String tourId);
}
