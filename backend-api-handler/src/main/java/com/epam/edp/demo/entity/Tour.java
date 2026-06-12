package com.epam.edp.demo.entity;

import com.epam.edp.demo.enums.TourType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Static tour metadata — things that do NOT change per departure date.
 *
 * <p>Date-specific availability and pricing are stored in {@link TourInstance}
 * (collection: "tour_instances").  The separation keeps this document stable
 * and allows instances to be updated independently without touching
 * marketing/hotel copy.</p>
 *
 * <h3>Index strategy</h3>
 * <ul>
 *   <li>{@code destination.city}    — single-field, drives city filter</li>
 *   <li>{@code destination.country} — single-field, drives country filter</li>
 *   <li>{@code tourType}            — single-field, drives type filter</li>
 *   <li>Compound {@code {destination.city, destination.country}} — covers
 *       combined city+country search in one index scan</li>
 * </ul>
 *
 * <p>Requires {@code spring.data.mongodb.auto-index-creation=true} in
 * {@code application.properties} for Spring Data to create indexes on startup.</p>
 */
@Document(collection = "tours")
@CompoundIndex(
    name = "idx_destination_city_country",
    def  = "{'destination.city': 1, 'destination.country': 1}"
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tour {

    @Id
    private String id;

    /** Display name shown on listing and detail pages. */
    private String name;

    /**
     * Structured destination. {@link Destination#getCity()} and
     * {@link Destination#getCountry()} are individually {@code @Indexed}
     * — Spring Data resolves the dotted path automatically.
     */
    private Destination destination;

    /**
     * Hotel / accommodation details embedded here because they are static
     * and fetched on every detail-page load together with the tour.
     */
    private Hotel hotel;

    /**
     * Tour style classification. Indexed to support the tourType filter
     * in the search query without a collection scan.
     */
    @Indexed
    private TourType tourType;

    /**
     * Aggregate guest rating (1.0–5.0).
     * Wrapper type used so that unrated tours can be represented as {@code null}
     * rather than defaulting to 0.0.
     */
    private Double rating;

    /** Total number of submitted reviews; displayed alongside the rating. */
    private int reviewCount;

    /**
     * Ordered image URLs; first element is used as the thumbnail on
     * listing cards and the hero image on the detail page.
     */
    private List<String> imageUrls;

    /** Short marketing summary (1–3 sentences). */
    private String summary;

    /**
     * Default number of days before {@code startDate} within which
     * cancellation is free-of-charge.  Can be overridden per
     * {@link TourInstance} if needed.
     */
    private int freeCancellationDays;

    /**
     * Maximum guest constraints that apply to every instance of this tour.
     * Controls what the guest-picker UI permits.
     */
    private GuestQuantityInfo guestQuantity;

    /**
     * Reference to the travel agent responsible for this tour.
     * Stored as an opaque string ID — no embedded agent document —
     * to keep this collection decoupled from the agent service.
     */
    private String travelAgentId;
}
