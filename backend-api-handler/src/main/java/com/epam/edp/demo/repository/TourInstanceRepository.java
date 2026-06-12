package com.epam.edp.demo.repository;

import com.epam.edp.demo.entity.TourInstance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourInstanceRepository extends MongoRepository<TourInstance, String> {

    /**
     * Returns all departure instances for a given tour, ordered naturally by
     * MongoDB's default (insertion order).  Used on the detail page and when
     * computing free-cancellation dates per-booking.
     */
    List<TourInstance> findByTourId(String tourId);
}
