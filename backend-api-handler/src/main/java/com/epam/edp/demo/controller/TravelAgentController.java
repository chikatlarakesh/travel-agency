package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.booking.AgentBookingListResponseDTO;
import com.epam.edp.demo.dto.booking.TravelAgentBookingDetailDTO;
import com.epam.edp.demo.dto.booking.document.RetrieveDocumentsResponseDTO;
import com.epam.edp.demo.service.BookingDocumentService;
import com.epam.edp.demo.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/travel-agent")
@RequiredArgsConstructor
@Tag(name = "Travel Agent", description = "Endpoints for travel agents to manage their assigned bookings")
@SecurityRequirement(name = "Bearer")
public class TravelAgentController {

    private final BookingService bookingService;
    private final BookingDocumentService bookingDocumentService;

    @Operation(
        summary = "List bookings assigned to the calling travel agent",
        description = "Returns all bookings where the JWT subject matches the booking's travelAgentId. "
                    + "Optionally filter by booking status (BOOKED, CONFIRMED, STARTED, FINISHED, CANCELED)."
    )
    @ApiResponse(responseCode = "200", description = "Bookings retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid status filter value")
    @ApiResponse(responseCode = "401", description = "Unauthorized — valid JWT required")
    @GetMapping("/bookings")
    public ResponseEntity<AgentBookingListResponseDTO> getAgentBookings(
            @Parameter(description = "Optional booking status filter", example = "BOOKED")
            @RequestParam(required = false) String status,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Travel agent listing bookings statusFilter={}", status);
        return ResponseEntity.ok(bookingService.getAgentBookings(status, authorization));
    }

    @Operation(
        summary = "Get a single booking assigned to the calling travel agent",
        description = "Returns full booking detail including customer info, documents, and lifecycle metadata. "
                    + "Returns 401 if the calling agent is not the one assigned to this booking."
    )
    @ApiResponse(responseCode = "200", description = "Booking retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized — not the assigned agent")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @GetMapping("/bookings/{bookingId}")
    public ResponseEntity<TravelAgentBookingDetailDTO> getAgentBookingById(
            @PathVariable String bookingId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Travel agent fetching bookingId={}", bookingId);
        return ResponseEntity.ok(bookingService.getAgentBookingById(bookingId, authorization));
    }

    @Operation(summary = "Get documents for a booking assigned to the calling travel agent")
    @ApiResponse(responseCode = "200", description = "Documents retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized — not the assigned agent")
    @ApiResponse(responseCode = "404", description = "Booking not found")
    @GetMapping("/bookings/{bookingId}/documents")
    public ResponseEntity<RetrieveDocumentsResponseDTO> getAgentBookingDocuments(
            @PathVariable String bookingId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.debug("Travel agent fetching documents for bookingId={}", bookingId);
        return ResponseEntity.ok(bookingDocumentService.getDocumentsForAgent(bookingId, authorization));
    }
}
