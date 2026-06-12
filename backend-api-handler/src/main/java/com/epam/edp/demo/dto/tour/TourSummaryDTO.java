package com.epam.edp.demo.dto.tour;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Minimal tour representation used on listing/search pages.
 * Built from one Tour + one TourInstance (the selected or earliest departure).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TourSummaryDTO {

    private String id;

    private String name;

    /** Formatted as "City, Country". */
    private String destination;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /** e.g. ["7 days", "10 days", "12 days"] */
    private List<String> durations;

    /** Enum names as-is: "BB", "HB", "FB", "AI". */
    private List<String> mealPlans;

    /** e.g. "from $1400 for 1 person" */
    private String price;

    private Double rating;

    private int reviews;

    /** Last date on which free cancellation is permitted. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate freeCancellation;
}
