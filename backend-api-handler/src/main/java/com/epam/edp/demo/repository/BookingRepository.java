package com.epam.edp.demo.repository;

import com.epam.edp.demo.entity.Booking;
import com.epam.edp.demo.enums.BookingStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {

    List<Booking> findByUserId(String userId);

    /** Used by the scheduler to find bookings needing state transitions. */
    List<Booking> findByState(BookingStatus state);

    /** Used to detect duplicate bookings for the same user + tour instance. */
    List<Booking> findByUserIdAndTourInstanceId(String userId, String tourInstanceId);

    /** Used to find bookings for a specific user and tour (for feedback eligibility check). */
    List<Booking> findByUserIdAndTourId(String userId, String tourId);

    // ── Sprint 2 additions ────────────────────────────────────────────────────

    /** Returns all bookings assigned to the given travel agent. */
    List<Booking> findByTravelAgentId(String travelAgentId);

    /** Returns all bookings in any of the provided states. */
    List<Booking> findByStateIn(List<BookingStatus> states);

    /** Returns bookings assigned to an agent filtered by state. */
    List<Booking> findByTravelAgentIdAndState(String travelAgentId, BookingStatus state);

    /** Returns bookings assigned to an agent in any of the provided states. */
    List<Booking> findByTravelAgentIdAndStateIn(String travelAgentId, List<BookingStatus> states);

    /** Single booking looked up by id and agent (ownership check). */
    Optional<Booking> findByIdAndTravelAgentId(String id, String travelAgentId);
}
