package com.epam.edp.demo.dto.tour;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Full tour representation used on the tour detail page.
 * Built from one Tour + all of its TourInstances.
 *
 * Field names intentionally match the existing API contract verbatim
 * (including legacy spellings: "accomodiation", "freeCancelationDaysBefore").
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TourDetailsDTO {

    private String id;

    private String name;

    /** Formatted as "City, Country". */
    private String destination;

    private Double rating;

    private List<String> imageUrls;

    private String summary;

    /** Days before departure within which cancellation is free. */
    private int freeCancelationDaysBefore;

    /** Deduplicated, sorted: ["7 days", "10 days", "12 days"]. */
    private List<String> durations;

    /** Accommodation description sourced from hotel.description. */
    private String accomodiation;

    private String hotelName;

    private String hotelDescription;

    /** Deduplicated meal plan names: "BB", "HB", "FB", "AI". */
    private List<String> mealPlans;

    /**
     * Optional extra attributes (e.g. pool info, tour guide language).
     * Always present; empty map when none apply.
     */
    private Map<String, String> customDetails;

    /** Departure dates across all available instances, sorted ascending. */
    private List<LocalDate> startDates;

    private GuestQuantityDTO guestQuantity;

    /**
     * Base price per person keyed by duration string.
     * e.g. {"7 days": "$1400", "10 days": "$1900"}
     */
    private Map<String, String> price;

    /**
     * Daily meal supplement per person keyed by meal plan name.
     * e.g. {"BB": "$0", "HB": "$25", "FB": "$40", "AI": "$80"}
     */
    private Map<String, String> mealSupplementsPerDay;
}
