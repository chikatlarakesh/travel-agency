package com.epam.edp.demo.repository;

import com.epam.edp.demo.entity.BookingDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for booking document operations.
 */
@Repository
public interface BookingDocumentRepository extends MongoRepository<BookingDocument, String> {

    /**
     * Find all documents for a specific booking.
     */
    List<BookingDocument> findByBookingId(String bookingId);

    /**
     * Find all documents of a specific type for a booking.
     */
    List<BookingDocument> findByBookingIdAndDocumentType(String bookingId, BookingDocument.BookingDocumentCategory documentType);

    /**
     * Delete all documents for a booking.
     */
    void deleteByBookingId(String bookingId);

    /**
     * Count documents for a booking.
     */
    long countByBookingId(String bookingId);
}

