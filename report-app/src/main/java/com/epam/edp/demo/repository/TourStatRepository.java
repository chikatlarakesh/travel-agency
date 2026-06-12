package com.epam.edp.demo.repository;

import com.epam.edp.demo.entity.TourStatDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TourStatRepository extends MongoRepository<TourStatDocument, String> {

    Optional<TourStatDocument> findByBookingId(String bookingId);

    List<TourStatDocument> findByEventTimestampBetween(Instant from, Instant to);

    List<TourStatDocument> findByTravelAgentIdAndEventTimestampBetween(String travelAgentId, Instant from, Instant to);

    List<TourStatDocument> findByTourIdAndEventTimestampBetween(String tourId, Instant from, Instant to);

    List<TourStatDocument> findByCountryAndEventTimestampBetween(String country, Instant from, Instant to);

    List<TourStatDocument> findByCityAndEventTimestampBetween(String city, Instant from, Instant to);
}
