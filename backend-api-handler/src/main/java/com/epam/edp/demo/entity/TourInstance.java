package com.epam.edp.demo.entity;

import com.epam.edp.demo.enums.MealPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;

/**
 * A single departure of a {@link Tour} — captures everything that varies
 * per departure date: availability, capacity, pricing, and date range.
 *
 * <h3>Relationship to Tour</h3>
 * <p>Many TourInstances reference one Tour via {@link #tourId}.
 * The search flow is:</p>
 * <ol>
 *   <li>Filter TourInstances by {@code startDate}, {@code availableSlots},
 *       {@code durations}, {@code mealPlans}.</li>
 *   <li>Join (lookup) matching {@code tourId} values into the Tours collection
 *       to apply {@code destination}, {@code tourType} filters.</li>
 * </ol>
 *
 * <h3>Index strategy</h3>
 * <ul>
 *   <li>{@code startDate}      — date-range filter (User Story 1296)</li>
 *   <li>{@code availableSlots} — overbooking guard + filter (US 1296, 1298)</li>
 *   <li>Compound {@code {tourId, startDate}} — efficient lookup of all
 *       departures for a given tour, ordered by date (US 1297 detail page)</li>
 *   <li>Compound {@code {startDate, availableSlots}} — primary search path:
 *       date range + availability in a single index scan (US 1296)</li>
 * </ul>
 *
 * <p>{@code durations} and {@code mealPlans} are arrays; MongoDB creates
 * multikey indexes automatically — query them with {@code $in}.</p>
 *
 * <p>Requires {@code spring.data.mongodb.auto-index-creation=true} in
 * {@code application.properties}.</p>
 */
@Document(collection = "tour_instances")
@CompoundIndex(
    name   = "idx_tourId_startDate",
    def    = "{'tourId': 1, 'startDate': 1}"
)
@CompoundIndex(
    name   = "idx_startDate_availableSlots",
    def    = "{'startDate': 1, 'availableSlots': 1}"
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourInstance {

    @Id
    private String id;

    /**
     * Foreign key to {@link Tour#getId()}.
     * Indexed individually to support fast lookups by tour without a date range.
     */
    @Indexed
    private String tourId;

    /**
     * Departure date of this instance.
     * Indexed — primary axis for date-range search queries.
     */
    @Indexed
    private LocalDate startDate;

    /**
     * Calculated as {@code startDate + (selected duration in days)}.
     * Stored for display purposes; not indexed (derived field).
     */
    private LocalDate endDate;

    /**
     * Maximum number of guests this departure can accommodate.
     * Set once at creation; never decremented.
     */
    private int totalCapacity;

    /**
     * Remaining seats.  Decremented atomically on booking (using
     * MongoDB {@code $inc} with a {@code $gte: requestedGuests} condition
     * to prevent overbooking).
     * Indexed — filtered in every availability search query.
     */
    @Indexed
    private int availableSlots;

    /**
     * Available durations in days for this departure, e.g. [7, 10, 12].
     * Stored as integers so range/equality queries are type-safe.
     * MongoDB creates a multikey index automatically; use {@code $in} to filter.
     */
    private List<Integer> durations;

    /**
     * Meal plans offered on this departure.
     * MongoDB creates a multikey index automatically; use {@code $in} to filter.
     */
    private List<MealPlan> mealPlans;

    /**
     * One pricing entry per available duration.
     *
     * <p>Each {@link PricingOption} holds:</p>
     * <ul>
     *   <li>{@code duration}              — number of days</li>
     *   <li>{@code pricePerPerson}        — base price (BB included)</li>
     *   <li>{@code mealSupplementsPerDay} — extra cost per day per meal plan</li>
     * </ul>
     *
     * Example:
     * <pre>
     * [
     *   { "duration": 7,  "pricePerPerson": 1400.00, "mealSupplementsPerDay": {"HB": 30, "FB": 50, "AI": 80} },
     *   { "duration": 10, "pricePerPerson": 1900.00, "mealSupplementsPerDay": {"HB": 30, "FB": 50, "AI": 80} },
     *   { "duration": 12, "pricePerPerson": 2200.00, "mealSupplementsPerDay": {"HB": 30, "FB": 50, "AI": 80} }
     * ]
     * </pre>
     */
    private List<PricingOption> pricing;

    /**
     * Days before {@link #startDate} within which cancellation is free.
     * Overrides the tour-level default when set; allows per-departure policies.
     * A value of {@code 0} means no free cancellation.
     */
    private int freeCancellationDays;
}
