package com.epam.edp.demo.entity;

import com.epam.edp.demo.enums.CancellationReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationDetails {

    /** Structured reason for cancellation. */
    private CancellationReason reason;

    /** Free-text additional detail (optional). */
    private String reasonNote;

    /** User ID of the person who triggered the cancellation. */
    private String canceledBy;

    /** Timestamp when the booking was canceled. */
    private Instant canceledAt;

    /**
     * Whether the customer is eligible for a refund.
     * true  = canceled ≥10 days before start date (free cancellation window)
     * false = canceled within 10 days of start date
     */
    @Builder.Default
    private boolean refundEligible = false;
}
