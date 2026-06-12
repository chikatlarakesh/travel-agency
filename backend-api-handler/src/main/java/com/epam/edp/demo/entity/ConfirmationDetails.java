package com.epam.edp.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmationDetails {

    /** User ID of the travel agent who confirmed the booking. */
    private String confirmedBy;

    /** Timestamp when the booking was confirmed. */
    private Instant confirmedAt;

    /** Optional notes from the travel agent at confirmation. */
    private String notes;
}
