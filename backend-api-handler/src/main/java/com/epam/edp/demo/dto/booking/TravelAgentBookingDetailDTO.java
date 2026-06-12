package com.epam.edp.demo.dto.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Full booking detail returned to the assigned travel agent")
public class TravelAgentBookingDetailDTO {

    private String bookingId;
    private String state;
    private String tourName;
    private String destination;
    private String tourImageUrl;
    private Double tourRating;

    // Tour details
    private String startDate;
    private String duration;
    private String mealPlan;
    private Double totalPrice;

    // Customer info
    private CustomerDetailsDTO customerDetails;
    private List<PersonDetailDTO> guestDetails;

    // Documents
    private List<BookingDocumentDTO> documents;
    @Schema(description = "Number of documents uploaded")
    private int documentCount;
    @Schema(description = "Number of documents verified")
    private int verifiedDocumentCount;

    // Lifecycle metadata
    private ConfirmationDTO confirmation;
    private CancellationDTO cancellation;
    private CustomerApprovalDTO customerApproval;

    private String freeCancellationDate;
}
