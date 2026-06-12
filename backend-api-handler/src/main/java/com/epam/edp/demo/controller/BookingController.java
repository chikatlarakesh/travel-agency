package com.epam.edp.demo.controller;

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
import com.epam.edp.demo.dto.booking.document.RetrieveDocumentsResponseDTO;
import com.epam.edp.demo.dto.booking.document.UpdateDocumentRequestDTO;
import com.epam.edp.demo.dto.booking.document.UploadDocumentsRequestDTO;
import com.epam.edp.demo.dto.response.MessageResponse;
import com.epam.edp.demo.service.BookingDocumentService;
import com.epam.edp.demo.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Tour booking lifecycle management")
@SecurityRequirement(name = "Bearer")
public class BookingController {

    private final BookingService bookingService;
    private final BookingDocumentService documentService;

    // =========================================================================
    // Sprint 1 endpoints
    // =========================================================================

    @Operation(summary = "Create a new booking")
    @ApiResponse(responseCode = "201", description = "Booking created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Tour not found")
    @PostMapping
    public ResponseEntity<CreateBookingResponseDTO> createBooking(
            @Valid @RequestBody CreateBookingRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Creating booking for userId={} tourId={}", request.getUserId(), request.getTourId());
        CreateBookingResponseDTO response = bookingService.createBooking(request, authorization);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get all bookings for a user")
    @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @GetMapping
    public ResponseEntity<BookedTourListResponseDTO> getBookings(
            @Parameter(description = "ID of the user whose bookings to retrieve")
            @RequestParam String userId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Fetching bookings for userId={}", userId);
        return ResponseEntity.ok(bookingService.getBookings(userId, authorization));
    }

    @Operation(summary = "Get a single booking by ID")
    @ApiResponse(responseCode = "200", description = "Booking retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookedTourDTO> getBooking(
            @PathVariable String bookingId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Fetching booking bookingId={}", bookingId);
        return ResponseEntity.ok(bookingService.getBooking(bookingId, authorization));
    }

    @Operation(summary = "Update a booking (Sprint 1)",
            description = "Updates booking details such as meal plan, traveler info and guest count")
    @ApiResponse(responseCode = "204", description = "Booking updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data or booking state")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @PutMapping("/{bookingId}")
    public ResponseEntity<Void> updateBooking(
            @PathVariable String bookingId,
            @Valid @RequestBody UpdateBookingRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Updating booking bookingId={}", bookingId);
        bookingService.updateBooking(bookingId, request, authorization);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cancel a booking (customer, Sprint 1)",
            description = "Allowed only if current date is at least 10 days before tour start date")
    @ApiResponse(responseCode = "204", description = "Booking cancelled successfully")
    @ApiResponse(responseCode = "400", description = "Cancellation not allowed")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable String bookingId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Cancelling booking bookingId={}", bookingId);
        bookingService.cancelBooking(bookingId, authorization);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Confirm a booking (Sprint 1 - simple)",
            description = "Transitions a booking from BOOKED to CONFIRMED state (no document check)")
    @ApiResponse(responseCode = "204", description = "Booking confirmed successfully")
    @ApiResponse(responseCode = "400", description = "Booking is not in BOOKED state")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @PatchMapping("/{bookingId}/confirm")
    public ResponseEntity<Void> confirmBooking(
            @PathVariable String bookingId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Confirming booking bookingId={}", bookingId);
        bookingService.confirmBooking(bookingId, authorization);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Document Management Endpoints (from feature/upload-document-be)
    // -------------------------------------------------------------------------

    @Operation(summary = "Upload documents for a booking",
            description = "Allows customers to upload payment receipts and guest documents (passports, IDs) for a specific booking. Fails with 409 if documents already exist - use PATCH to update individual documents.")
    @ApiResponse(responseCode = "201", description = "Documents uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @ApiResponse(responseCode = "409", description = "Documents already exist for this booking")
    @PostMapping("/{bookingId}/documents")
    public ResponseEntity<MessageResponse> uploadDocuments(
            @PathVariable String bookingId,
            @Valid @RequestBody UploadDocumentsRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Uploading documents for bookingId={}", bookingId);
        documentService.uploadDocuments(bookingId, request, authorization);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MessageResponse.of("Documents uploaded successfully."));
    }

    @Operation(summary = "Update a single document",
            description = "Updates a specific document (payment receipt or guest document) in a booking")
    @ApiResponse(responseCode = "200", description = "Document updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Booking or document not found")
    @PatchMapping("/{bookingId}/documents/{documentId}")
    public ResponseEntity<MessageResponse> updateDocument(
            @PathVariable String bookingId,
            @PathVariable String documentId,
            @Valid @RequestBody UpdateDocumentRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Updating document documentId={} for bookingId={}", documentId, bookingId);
        documentService.updateDocument(bookingId, documentId, request, authorization);
        return ResponseEntity.ok(MessageResponse.of("Document updated successfully."));
    }

    @Operation(summary = "Retrieve documents for a booking",
            description = "Retrieves all payment receipts and guest documents for a specific booking")
    @ApiResponse(responseCode = "200", description = "Documents retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @GetMapping("/{bookingId}/documents")
    public ResponseEntity<RetrieveDocumentsResponseDTO> getDocuments(
            @PathVariable String bookingId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Retrieving documents for bookingId={}", bookingId);
        return ResponseEntity.ok(documentService.getDocuments(bookingId, authorization));
    }

    @Operation(summary = "Delete a document from a booking",
            description = "Deletes a specific document (payment receipt or guest document) from a booking")
    @ApiResponse(responseCode = "200", description = "Document deleted successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Booking or document not found")
    @DeleteMapping("/{bookingId}/documents/{documentId}")
    public ResponseEntity<MessageResponse> deleteDocument(
            @PathVariable String bookingId,
            @PathVariable String documentId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Deleting document documentId={} from bookingId={}", documentId, bookingId);
        documentService.deleteDocument(bookingId, documentId, authorization);
        return ResponseEntity.ok(MessageResponse.of("Document deleted successfully."));
    }

    // =========================================================================
    // Sprint 2 endpoints
    // =========================================================================

    @Operation(summary = "Verify a document (travel agent action)",
            description = "Travel agent approves or rejects a specific document uploaded by the customer")
    @ApiResponse(responseCode = "200", description = "Document verification recorded")
    @ApiResponse(responseCode = "400", description = "Invalid action or document not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @PatchMapping("/{bookingId}/documents/verify")
    public ResponseEntity<TravelAgentBookingDetailDTO> verifyDocument(
            @PathVariable String bookingId,
            @Valid @RequestBody DocumentVerificationRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Verifying document for bookingId={} docId={}", bookingId, request.getDocumentId());
        return ResponseEntity.ok(bookingService.verifyDocument(bookingId, request, authorization));
    }

    @Operation(summary = "Confirm booking with document check (travel agent, Sprint 2)",
            description = "Transitions BOOKED → CONFIRMED after verifying all uploaded documents")
    @ApiResponse(responseCode = "200", description = "Booking confirmed")
    @ApiResponse(responseCode = "400", description = "Documents missing or unverified")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<TravelAgentBookingDetailDTO> confirmBookingV2(
            @PathVariable String bookingId,
            @RequestBody(required = false) ConfirmBookingRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Confirming booking v2 bookingId={}", bookingId);
        ConfirmBookingRequestDTO body = request != null ? request : new ConfirmBookingRequestDTO();
        return ResponseEntity.ok(bookingService.confirmBookingV2(bookingId, body, authorization));
    }

    @Operation(summary = "Cancel booking with reason (travel agent, Sprint 2)",
            description = "Cancels a BOOKED or CONFIRMED booking with a structured reason and refund eligibility")
    @ApiResponse(responseCode = "200", description = "Booking cancelled")
    @ApiResponse(responseCode = "400", description = "Invalid reason or booking cannot be cancelled")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @DeleteMapping("/{bookingId}/cancel")
    public ResponseEntity<TravelAgentBookingDetailDTO> cancelBookingV2(
            @PathVariable String bookingId,
            @Valid @RequestBody CancelBookingRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Cancelling booking v2 bookingId={}", bookingId);
        return ResponseEntity.ok(bookingService.cancelBookingV2(bookingId, request, authorization));
    }

    @Operation(summary = "Edit booking details (travel agent action)",
            description = "Edits guests, meal plan, or duration — only allowed before confirmation and after customer approval")
    @ApiResponse(responseCode = "200", description = "Booking updated")
    @ApiResponse(responseCode = "400", description = "Edit not allowed (post-confirmation or no approval)")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @PatchMapping("/{bookingId}/edit")
    public ResponseEntity<TravelAgentBookingDetailDTO> editBooking(
            @PathVariable String bookingId,
            @Valid @RequestBody EditBookingRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Editing booking bookingId={}", bookingId);
        return ResponseEntity.ok(bookingService.editBooking(bookingId, request, authorization));
    }

    @Operation(summary = "Record customer approval for agent edit",
            description = "Records whether the customer approved or declined the travel agent's proposed changes (OFFLINE mode)")
    @ApiResponse(responseCode = "200", description = "Approval recorded")
    @ApiResponse(responseCode = "400", description = "Invalid state or request")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @PatchMapping("/{bookingId}/customer-approval")
    public ResponseEntity<TravelAgentBookingDetailDTO> recordCustomerApproval(
            @PathVariable String bookingId,
            @Valid @RequestBody CustomerApprovalRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Recording customer approval for bookingId={}", bookingId);
        return ResponseEntity.ok(bookingService.recordCustomerApproval(bookingId, request, authorization));
    }

    @Operation(summary = "Approve agent-proposed booking changes (customer action)",
            description = "Customer approves the changes an agent made to their booking")
    @ApiResponse(responseCode = "204", description = "Changes approved")
    @ApiResponse(responseCode = "400", description = "No pending changes")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @PatchMapping("/{bookingId}/approve")
    public ResponseEntity<Void> approveBookingChange(
            @PathVariable String bookingId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Customer approving changes for bookingId={}", bookingId);
        bookingService.approveBookingChange(bookingId, authorization);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Decline agent-proposed booking changes (customer action)",
            description = "Customer declines the changes an agent made to their booking")
    @ApiResponse(responseCode = "204", description = "Changes declined")
    @ApiResponse(responseCode = "400", description = "No pending changes")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @PatchMapping("/{bookingId}/decline")
    public ResponseEntity<Void> declineBookingChange(
            @PathVariable String bookingId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Customer declining changes for bookingId={}", bookingId);
        bookingService.declineBookingChange(bookingId, authorization);
        return ResponseEntity.noContent().build();
    }
}
