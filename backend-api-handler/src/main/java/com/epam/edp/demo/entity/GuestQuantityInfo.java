package com.epam.edp.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embedded value object that declares the maximum guest counts allowed
 * for any instance of a {@link Tour}.
 *
 * Drives the guest-picker UI constraints on the search and booking pages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuestQuantityInfo {

    /** Maximum number of adult guests per booking. */
    private int maxAdults;

    /** Maximum number of child guests per booking. */
    private int maxChildren;

    /** Hard cap on total guests (adults + children combined). */
    private int maxTotal;
}
