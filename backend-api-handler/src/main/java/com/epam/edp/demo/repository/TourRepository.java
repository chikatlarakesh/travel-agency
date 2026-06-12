package com.epam.edp.demo.repository;

import com.epam.edp.demo.entity.Tour;
import com.epam.edp.demo.enums.TourType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TourRepository extends MongoRepository<Tour, String> {

    @Query("{ $or: [ " +
           "{ 'destination.city':    { $regex: ?0, $options: 'i' } }, " +
           "{ 'destination.country': { $regex: ?0, $options: 'i' } } " +
           "] }")
    List<Tour> searchDestinations(String query);

    Page<Tour> findByTourType(TourType tourType, Pageable pageable);

    @Query("{ $or: [ " +
           "{ 'destination.city':    { $regex: ?0, $options: 'i' } }, " +
           "{ 'destination.country': { $regex: ?0, $options: 'i' } } " +
           "] }")
    Page<Tour> searchDestinationsPaged(String query, Pageable pageable);

    @Query("{ $and: [ " +
           "{ $or: [ " +
           "  { 'destination.city':    { $regex: ?0, $options: 'i' } }, " +
           "  { 'destination.country': { $regex: ?0, $options: 'i' } } " +
           "] }, " +
           "{ 'tourType': ?1 } " +
           "] }")
    Page<Tour> searchDestinationsAndTourType(String query, TourType tourType, Pageable pageable);
}
