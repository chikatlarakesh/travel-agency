package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.booking.AgentBookingListResponseDTO;
import com.epam.edp.demo.dto.booking.BookedTourDTO;
import com.epam.edp.demo.dto.booking.BookedTourListResponseDTO;
import com.epam.edp.demo.dto.booking.CancelBookingRequestDTO;
import com.epam.edp.demo.dto.booking.ConfirmBookingRequestDTO;
import com.epam.edp.demo.dto.booking.CreateBookingRequestDTO;
import com.epam.edp.demo.dto.booking.CreateBookingResponseDTO;
import com.epam.edp.demo.dto.booking.CustomerApprovalRequestDTO;
import com.epam.edp.demo.dto.booking.DocumentVerificationRequestDTO;
import com.epam.edp.demo.dto.booking.EditBookingRequestDTO;
import com.epam.edp.demo.dto.booking.TravelAgentBookingDetailDTO;
import com.epam.edp.demo.dto.booking.UploadDocumentRequestDTO;
import com.epam.edp.demo.dto.booking.UpdateBookingRequestDTO;

public interface BookingService {

    // ── Sprint 1 ──────────────────────────────────────────────────────────────

    CreateBookingResponseDTO createBooking(CreateBookingRequestDTO request, String authorizationHeader);

    BookedTourListResponseDTO getBookings(String userId, String authorizationHeader);

    BookedTourDTO getBooking(String bookingId, String authorizationHeader);

    void updateBooking(String bookingId, UpdateBookingRequestDTO request, String authorizationHeader);

    void cancelBooking(String bookingId, String authorizationHeader);

    void confirmBooking(String bookingId, String authorizationHeader);

    // ── Sprint 2 ──────────────────────────────────────────────────────────────

    /**
     * Confirms a booking with structured request (travel agent action).
     * Transitions BOOKED → CONFIRMED after verifying all documents.
     */
    TravelAgentBookingDetailDTO confirmBookingV2(String bookingId,
                                                  ConfirmBookingRequestDTO request,
                                                  String authorizationHeader);

    /**
     * Cancels a booking with structured reason (travel agent action).
     * Sets refundEligible based on free-cancellation window.
     */
    TravelAgentBookingDetailDTO cancelBookingV2(String bookingId,
                                                 CancelBookingRequestDTO request,
                                                 String authorizationHeader);

    /**
     * Edits booking details (travel agent action, before confirmation,
     * requires prior customer approval).
     */
    TravelAgentBookingDetailDTO editBooking(String bookingId,
                                             EditBookingRequestDTO request,
                                             String authorizationHeader);

    /**
     * Records a customer approval decision for a travel-agent proposed edit.
     */
    TravelAgentBookingDetailDTO recordCustomerApproval(String bookingId,
                                                        CustomerApprovalRequestDTO request,
                                                        String authorizationHeader);

    /**
     * Customer approves the travel agent's proposed edits online.
     */
    void approveBookingChange(String bookingId, String authorizationHeader);

    /**
     * Customer declines the travel agent's proposed edits online.
     */
    void declineBookingChange(String bookingId, String authorizationHeader);

    /**
     * Uploads a document to a booking (customer action).
     */
    TravelAgentBookingDetailDTO uploadDocument(String bookingId,
                                                UploadDocumentRequestDTO request,
                                                String authorizationHeader);

    /**
     * Verifies (or rejects) a single document within a booking (travel agent action).
     */
    TravelAgentBookingDetailDTO verifyDocument(String bookingId,
                                                DocumentVerificationRequestDTO request,
                                                String authorizationHeader);

    /**
     * Returns all bookings assigned to the calling travel agent.
     */
    AgentBookingListResponseDTO getAgentBookings(String stateFilter, String authorizationHeader);

    /**
     * Returns a single booking by id, accessible by the assigned travel agent.
     */
    TravelAgentBookingDetailDTO getAgentBookingById(String bookingId, String authorizationHeader);
}
