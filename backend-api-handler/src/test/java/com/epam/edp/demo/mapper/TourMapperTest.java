package com.epam.edp.demo.mapper;

import com.epam.edp.demo.dto.tour.GuestQuantityDTO;
import com.epam.edp.demo.dto.tour.TourDetailsDTO;
import com.epam.edp.demo.dto.tour.TourListResponseDTO;
import com.epam.edp.demo.dto.tour.TourSummaryDTO;
import com.epam.edp.demo.entity.Destination;
import com.epam.edp.demo.entity.GuestQuantityInfo;
import com.epam.edp.demo.entity.Hotel;
import com.epam.edp.demo.entity.PricingOption;
import com.epam.edp.demo.entity.Tour;
import com.epam.edp.demo.entity.TourInstance;
import com.epam.edp.demo.enums.MealPlan;
import com.epam.edp.demo.util.MealPlanFormatter;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TourMapperTest {

    private TourMapper mapper;

    @Before
    public void setUp() {
        mapper = new TourMapper(new MealPlanFormatter());
    }

    // ΓöÇΓöÇ toTourSummaryDTO ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ

    @Test
    public void toTourSummaryDTO_mapsAllFieldsCorrectly() {
        Tour tour = buildTour();
        TourInstance instance = buildInstance("i-1", "t-1",
                LocalDate.of(2025, 1, 4), 10,
                Arrays.asList(7, 10),
                Arrays.asList(MealPlan.BB, MealPlan.AI),
                Arrays.asList(
                        pricing(7,  new BigDecimal("1400"), null),
                        pricing(10, new BigDecimal("1900"), null)));

        TourSummaryDTO dto = mapper.toTourSummaryDTO(tour, instance);

        assertNotNull(dto);
        assertEquals("t-1",                              dto.getId());
        assertEquals("Garden Resort",                    dto.getName());
        assertEquals("Punta Cana, Dominican Republic",   dto.getDestination());
        assertEquals(LocalDate.of(2025, 1, 4),           dto.getStartDate());
        assertEquals(Arrays.asList("7 days", "10 days"), dto.getDurations());
        assertEquals(2,                                  dto.getMealPlans().size());
        assertEquals("Breakfast (BB)",                   dto.getMealPlans().get(0));
        assertEquals("All inclusive (AI)",               dto.getMealPlans().get(1));
        assertEquals("from $1400 for 1 person",          dto.getPrice());
        assertEquals(4.8,                                dto.getRating(), 0.001);
        assertEquals(12,                                 dto.getReviews());
        // freeCancelation = startDate(2025-01-04) - freeCancDays(10) = 2024-12-25
        assertEquals(LocalDate.of(2024, 12, 25),         dto.getFreeCancellation());
    }

    @Test
    public void toTourSummaryDTO_instanceFreeCancOverridesTourDefault() {
        Tour tour = Tour.builder()
                .id("t-1").name("Resort")
                .destination(new Destination("City", "Country"))
                .rating(4.0).reviewCount(5)
                .freeCancellationDays(7)        // tour-level default
                .build();
        TourInstance instance = buildInstance("i-1", "t-1",
                LocalDate.of(2025, 3, 1), 14,   // instance-level: 14 days
                Arrays.asList(7), Arrays.asList(MealPlan.BB),
                Arrays.asList(pricing(7, new BigDecimal("1000"), null)));

        TourSummaryDTO dto = mapper.toTourSummaryDTO(tour, instance);

        // instance 14 days wins: 2025-03-01 minus 14 = 2025-02-15
        assertEquals(LocalDate.of(2025, 2, 15), dto.getFreeCancellation());
    }

    @Test
    public void toTourSummaryDTO_instanceFreeCancZero_fallsBackToTourDefault() {
        Tour tour = Tour.builder()
                .id("t-1").name("Resort")
                .destination(new Destination("City", "Country"))
                .rating(4.0).reviewCount(5)
                .freeCancellationDays(7)        // tour-level default
                .build();
        TourInstance instance = buildInstance("i-1", "t-1",
                LocalDate.of(2025, 3, 1), 0,    // 0 ΓåÆ fall back to tour default
                Arrays.asList(7), Arrays.asList(MealPlan.BB),
                Arrays.asList(pricing(7, new BigDecimal("1000"), null)));

        TourSummaryDTO dto = mapper.toTourSummaryDTO(tour, instance);

        // tour default 7 days: 2025-03-01 minus 7 = 2025-02-22
        assertEquals(LocalDate.of(2025, 2, 22), dto.getFreeCancellation());
    }

    @Test
    public void toTourSummaryDTO_minPriceSelectedFromMultipleOptions() {
        Tour tour = Tour.builder()
                .id("t-1")
                .destination(new Destination("City", "Country"))
                .rating(4.0).reviewCount(0)
                .freeCancellationDays(5)
                .build();
        TourInstance instance = buildInstance("i-1", "t-1",
                LocalDate.of(2025, 6, 1), 5,
                Arrays.asList(7, 10, 12),
                Arrays.asList(MealPlan.BB),
                Arrays.asList(
                        pricing(12, new BigDecimal("2200"), null),
                        pricing(7,  new BigDecimal("1400"), null),  // this is the minimum
                        pricing(10, new BigDecimal("1900"), null)));

        TourSummaryDTO dto = mapper.toTourSummaryDTO(tour, instance);

        assertEquals("from $1400 for 1 person", dto.getPrice());
    }

    @Test
    public void toTourSummaryDTO_nullTour_returnsNull() {
        TourInstance instance = buildInstance("i-1", "t-1",
                LocalDate.of(2025, 1, 1), 7,
                Arrays.asList(7), Arrays.asList(MealPlan.BB),
                Arrays.asList(pricing(7, new BigDecimal("1000"), null)));

        assertNull(mapper.toTourSummaryDTO(null, instance));
    }

    @Test
    public void toTourSummaryDTO_nullInstance_returnsNull() {
        assertNull(mapper.toTourSummaryDTO(buildTour(), null));
    }

    // ΓöÇΓöÇ toTourDetailsDTO ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ

    @Test
    public void toTourDetailsDTO_mapsAllFieldsCorrectly() {
        Tour tour = buildTour();
        List<TourInstance> instances = Arrays.asList(
                buildInstance("i-1", "t-1",
                        LocalDate.of(2025, 1, 4), 10,
                        Arrays.asList(7, 10),
                        Arrays.asList(MealPlan.BB, MealPlan.HB),
                        Arrays.asList(
                                pricing(7,  new BigDecimal("1400"),
                                        Map.of(MealPlan.HB, new BigDecimal("30"))),
                                pricing(10, new BigDecimal("1900"),
                                        Map.of(MealPlan.HB, new BigDecimal("30"))))),
                buildInstance("i-2", "t-1",
                        LocalDate.of(2025, 1, 11), 0,
                        Arrays.asList(7, 12),
                        Arrays.asList(MealPlan.BB, MealPlan.AI),
                        Arrays.asList(
                                pricing(7,  new BigDecimal("1400"),
                                        Map.of(MealPlan.AI, new BigDecimal("80"))),
                                pricing(12, new BigDecimal("2200"),
                                        Map.of(MealPlan.AI, new BigDecimal("80")))))
        );

        TourDetailsDTO dto = mapper.toTourDetailsDTO(tour, instances);

        // Core tour fields
        assertEquals("t-1",                             dto.getId());
        assertEquals("Garden Resort",                   dto.getName());
        assertEquals("Punta Cana, Dominican Republic",  dto.getDestination());
        assertEquals(4.8,                               dto.getRating(), 0.001);
        assertEquals("Great resort",                    dto.getSummary());
        assertEquals(1,                                 dto.getImageUrls().size());

        // Hotel ΓÇö both accomodiation and hotelDescription map to hotel.getDescription()
        assertEquals("Garden Hotel",                    dto.getHotelName());
        assertEquals("Spacious rooms with sea view",    dto.getAccomodiation());
        assertEquals("Spacious rooms with sea view",    dto.getHotelDescription());

        // Guest quantity from Tour
        GuestQuantityDTO gq = dto.getGuestQuantity();
        assertEquals(2, gq.getAdultsMaxValue());
        assertEquals(1, gq.getChildrenMaxValue());
        assertEquals(3, gq.getTotalMaxVelue());

        // Free cancellation: first instance with days > 0 wins (instance 1 = 10)
        assertEquals(10, dto.getFreeCancelationDaysBefore());

        // Durations: deduplicated and sorted across both instances [7, 10, 12]
        assertEquals(Arrays.asList("7 days", "10 days", "12 days"), dto.getDurations());

        // Meal plans: distinct across both instances ΓÇö BB, HB (from i-1), AI (from i-2)
        assertEquals(3, dto.getMealPlans().size());
        assertTrue(dto.getMealPlans().contains("Breakfast (BB)"));
        assertTrue(dto.getMealPlans().contains("Half-board (HB)"));
        assertTrue(dto.getMealPlans().contains("All inclusive (AI)"));

        // Start dates: sorted ascending
        assertEquals(Arrays.asList(LocalDate.of(2025, 1, 4), LocalDate.of(2025, 1, 11)),
                dto.getStartDates());

        // Price map: first-wins for duplicate durations
        assertEquals("$1400", dto.getPrice().get("7 days"));
        assertEquals("$1900", dto.getPrice().get("10 days"));
        assertEquals("$2200", dto.getPrice().get("12 days"));

        // Meal supplements: collected from instance 1 only (stops after first with data)
        assertEquals(1,     dto.getMealSupplementsPerDay().size());
        assertEquals("$30", dto.getMealSupplementsPerDay().get("HB"));
        assertNull(dto.getMealSupplementsPerDay().get("AI")); // instance 2 not reached

        // customDetails is always empty per mapper contract
        assertTrue(dto.getCustomDetails().isEmpty());
    }

    @Test
    public void toTourDetailsDTO_emptyInstanceList_returnsEmptyCollectionsAndTourDefaultFreeCance() {
        Tour tour = buildTour(); // freeCancellationDays = 10

        TourDetailsDTO dto = mapper.toTourDetailsDTO(tour, Collections.emptyList());

        assertTrue(dto.getDurations().isEmpty());
        assertTrue(dto.getMealPlans().isEmpty());
        assertTrue(dto.getStartDates().isEmpty());
        assertTrue(dto.getPrice().isEmpty());
        assertTrue(dto.getMealSupplementsPerDay().isEmpty());
        // No instances ΓåÆ falls back to tour-level freeCancellationDays
        assertEquals(10, dto.getFreeCancelationDaysBefore());
    }

    @Test
    public void toTourDetailsDTO_nullHotel_returnsNullHotelFields() {
        Tour tour = Tour.builder()
                .id("t-2").name("No Hotel Tour")
                .destination(new Destination("Maldives", "Maldives"))
                .rating(4.5).reviewCount(3)
                .hotel(null)
                .build();

        TourDetailsDTO dto = mapper.toTourDetailsDTO(tour, Collections.emptyList());

        assertNull(dto.getAccomodiation());
        assertNull(dto.getHotelName());
        assertNull(dto.getHotelDescription());
    }

    @Test
    public void toTourDetailsDTO_nullDestination_returnsNullDestination() {
        Tour tour = Tour.builder()
                .id("t-3").name("Destination-less Tour")
                .destination(null)
                .build();

        TourDetailsDTO dto = mapper.toTourDetailsDTO(tour, Collections.emptyList());

        assertNull(dto.getDestination());
    }

    @Test
    public void toTourDetailsDTO_nullGuestQuantity_returnsEmptyGuestQuantityDTO() {
        Tour tour = Tour.builder()
                .id("t-4")
                .destination(new Destination("Rome", "Italy"))
                .guestQuantity(null)
                .build();

        TourDetailsDTO dto = mapper.toTourDetailsDTO(tour, Collections.emptyList());

        assertNotNull(dto.getGuestQuantity());
        assertEquals(0, dto.getGuestQuantity().getAdultsMaxValue());
        assertEquals(0, dto.getGuestQuantity().getChildrenMaxValue());
        assertEquals(0, dto.getGuestQuantity().getTotalMaxVelue());
    }

    @Test
    public void toTourDetailsDTO_duplicateDurationsAcrossInstances_deduplicatesAndSorts() {
        Tour tour = Tour.builder().id("t-1").build();
        List<TourInstance> instances = Arrays.asList(
                buildInstance("i-1", "t-1", LocalDate.of(2025, 2, 1), 0,
                        Arrays.asList(10, 7),   // unsorted on purpose
                        Arrays.asList(MealPlan.BB), Collections.emptyList()),
                buildInstance("i-2", "t-1", LocalDate.of(2025, 3, 1), 0,
                        Arrays.asList(7, 12),   // 7 duplicated across instances
                        Arrays.asList(MealPlan.BB), Collections.emptyList())
        );

        TourDetailsDTO dto = mapper.toTourDetailsDTO(tour, instances);

        // [10,7] + [7,12] ΓåÆ distinct ΓåÆ deduplicated+sorted ΓåÆ ["7 days","10 days","12 days"]
        assertEquals(Arrays.asList("7 days", "10 days", "12 days"), dto.getDurations());
    }

    @Test
    public void toTourDetailsDTO_priceMapPutIfAbsent_firstInstanceWinsOnSameDuration() {
        Tour tour = Tour.builder().id("t-1").build();
        List<TourInstance> instances = Arrays.asList(
                buildInstance("i-1", "t-1", LocalDate.of(2025, 1, 1), 0,
                        Arrays.asList(7), Arrays.asList(MealPlan.BB),
                        Arrays.asList(pricing(7, new BigDecimal("1400"), null))),
                buildInstance("i-2", "t-1", LocalDate.of(2025, 2, 1), 0,
                        Arrays.asList(7), Arrays.asList(MealPlan.BB),
                        Arrays.asList(pricing(7, new BigDecimal("1600"), null))) // higher price
        );

        TourDetailsDTO dto = mapper.toTourDetailsDTO(tour, instances);

        // First instance value is kept via putIfAbsent
        assertEquals("$1400", dto.getPrice().get("7 days"));
        assertEquals(1, dto.getPrice().size());
    }

    @Test
    public void toTourDetailsDTO_freeCancFirstInstanceWithNonZeroWins() {
        Tour tour = Tour.builder()
                .id("t-1")
                .freeCancellationDays(5)   // tour default
                .build();
        List<TourInstance> instances = Arrays.asList(
                buildInstance("i-1", "t-1", LocalDate.of(2025, 1, 1), 0,
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList()),
                buildInstance("i-2", "t-1", LocalDate.of(2025, 2, 1), 14,  // first non-zero
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList()),
                buildInstance("i-3", "t-1", LocalDate.of(2025, 3, 1), 21,
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList())
        );

        TourDetailsDTO dto = mapper.toTourDetailsDTO(tour, instances);

        // Instance i-2 is the first with freeCancellationDays > 0 ΓåÆ 14 wins
        assertEquals(14, dto.getFreeCancelationDaysBefore());
    }

    @Test
    public void toTourDetailsDTO_mealSupplementsOnlyFromFirstInstanceWithData() {
        Tour tour = Tour.builder().id("t-1").build();
        List<TourInstance> instances = Arrays.asList(
                buildInstance("i-1", "t-1", LocalDate.of(2025, 1, 1), 0,
                        Arrays.asList(7), Arrays.asList(MealPlan.BB, MealPlan.HB),
                        Arrays.asList(pricing(7, new BigDecimal("1400"),
                                Map.of(MealPlan.HB, new BigDecimal("25"))))),
                buildInstance("i-2", "t-1", LocalDate.of(2025, 2, 1), 0,
                        Arrays.asList(7), Arrays.asList(MealPlan.BB, MealPlan.FB),
                        Arrays.asList(pricing(7, new BigDecimal("1400"),
                                Map.of(MealPlan.FB, new BigDecimal("45")))))
        );

        TourDetailsDTO dto = mapper.toTourDetailsDTO(tour, instances);

        // Supplements collected from i-1 only; loop breaks after first non-empty instance
        assertEquals("$25", dto.getMealSupplementsPerDay().get("HB"));
        assertNull(dto.getMealSupplementsPerDay().get("FB"));
    }

    // ΓöÇΓöÇ toTourListResponseDTO ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ

    @Test
    public void toTourListResponseDTO_wrapsListWithPaginationMetadata() {
        TourSummaryDTO s1 = TourSummaryDTO.builder().id("t-1").name("Resort A").build();
        TourSummaryDTO s2 = TourSummaryDTO.builder().id("t-2").name("Hike B").build();

        TourListResponseDTO dto = mapper.toTourListResponseDTO(
                Arrays.asList(s1, s2), 2, 6, 5, 28);

        assertEquals(2,    dto.getTours().size());
        assertEquals(2,    dto.getPage());
        assertEquals(6,    dto.getPageSize());
        assertEquals(5,    dto.getTotalPages());
        assertEquals(28,   dto.getTotalItems());
        assertEquals("t-1", dto.getTours().get(0).getId());
        assertEquals("t-2", dto.getTours().get(1).getId());
    }

    @Test
    public void toTourListResponseDTO_emptyList_returnsEnvelopeWithZeroItems() {
        TourListResponseDTO dto = mapper.toTourListResponseDTO(
                Collections.emptyList(), 1, 6, 0, 0);

        assertTrue(dto.getTours().isEmpty());
        assertEquals(0, dto.getTotalItems());
        assertEquals(0, dto.getTotalPages());
    }

    // ΓöÇΓöÇ helpers ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ

    private Tour buildTour() {
        return Tour.builder()
                .id("t-1")
                .name("Garden Resort")
                .destination(new Destination("Punta Cana", "Dominican Republic"))
                .hotel(new Hotel("Garden Hotel", "Spacious rooms with sea view", 5))
                .rating(4.8)
                .reviewCount(12)
                .freeCancellationDays(10)
                .imageUrls(Collections.singletonList("https://example.com/img.jpg"))
                .summary("Great resort")
                .guestQuantity(new GuestQuantityInfo(2, 1, 3))
                .build();
    }

    private TourInstance buildInstance(String id, String tourId,
                                        LocalDate startDate, int freeCancDays,
                                        List<Integer> durations,
                                        List<MealPlan> mealPlans,
                                        List<PricingOption> pricing) {
        return TourInstance.builder()
                .id(id)
                .tourId(tourId)
                .startDate(startDate)
                .freeCancellationDays(freeCancDays)
                .durations(durations)
                .mealPlans(mealPlans)
                .pricing(pricing)
                .build();
    }

    private PricingOption pricing(int duration, BigDecimal price,
                                   Map<MealPlan, BigDecimal> supplements) {
        return PricingOption.builder()
                .duration(duration)
                .pricePerPerson(price)
                .mealSupplementsPerDay(supplements)
                .build();
    }
}
