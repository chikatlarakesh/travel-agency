package com.epam.edp.demo.dto.booking;

import com.epam.edp.demo.enums.CancellationReason;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Schema(description = "Booked tour summary for customer and agent views")
public class BookedTourDTO {
    private String id;
    private String tourId;
    private String state;
    private String tourImageUrl;
    private String name;
    private String destination;
    private TourDetailsBookingDTO tourDetails;
    private TravelAgentDTO travelAgent;

    /** @deprecated kept for backward compatibility; use cancellation.canceledBy */
    private String canceledBy;
    /** @deprecated kept for backward compatibility; use cancellation.reason */
    private String cancelReason;

    private List<PersonDetailDTO> personalDetails;
    private int travelerCount;
    private int childrenCount;
    private String freeCancellationDate;

    // ── Sprint 2 additions ────────────────────────────────────────────────────

    @Schema(description = "Customer contact details (visible to travel agent)")
    private CustomerDetailsDTO customerDetails;

    @Schema(description = "Documents uploaded by the customer")
    private List<BookingDocumentDTO> documents;

    @Schema(description = "Confirmation metadata")
    private ConfirmationDTO confirmation;

    @Schema(description = "Cancellation metadata")
    private CancellationDTO cancellationDetails;

    @Schema(description = "Customer approval for agent edits")
    private CustomerApprovalDTO customerApproval;

    @Schema(description = "Whether the customer has already submitted a review for this tour")
    private boolean hasReview;
}
