package com.epam.edp.demo.repository;

import com.epam.edp.demo.entity.Review;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Primary
@Repository
public interface MongoReviewRepository extends MongoRepository<Review, String>, ReviewRepository {

    boolean existsByUserIdAndTourId(String userId, String tourId);

    java.util.Optional<Review> findByUserIdAndTourId(String userId, String tourId);

    // Public tour page — only show published reviews
    java.util.List<Review> findByTourIdAndVisibility(String tourId, String visibility);

    // Admin panel queries
    java.util.List<Review> findByVisibility(String visibility);
    java.util.List<Review> findByFlagged(boolean flagged);
    java.util.List<Review> findByRate(int rate);
    java.util.List<Review> findByTourType(String tourType);
    java.util.List<Review> findByRateAndVisibility(int rate, String visibility);
}
