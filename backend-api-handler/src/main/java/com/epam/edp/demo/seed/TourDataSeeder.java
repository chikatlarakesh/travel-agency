package com.epam.edp.demo.seed;

import com.epam.edp.demo.entity.Destination;
import com.epam.edp.demo.entity.GuestQuantityInfo;
import com.epam.edp.demo.entity.Hotel;
import com.epam.edp.demo.entity.PricingOption;
import com.epam.edp.demo.entity.Tour;
import com.epam.edp.demo.entity.TourInstance;
import com.epam.edp.demo.enums.MealPlan;
import com.epam.edp.demo.enums.TourType;
import com.epam.edp.demo.repository.TourInstanceRepository;
import com.epam.edp.demo.repository.TourRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Seeds the frontend dummy tours (t-1001 through t-1014) and their corresponding
 * tour instances into MongoDB on application startup.
 * Only runs if these tours are not already present.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class TourDataSeeder implements ApplicationRunner {

    private final TourRepository tourRepository;
    private final TourInstanceRepository tourInstanceRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (tourRepository.existsById("t-1001")) {
            log.info("seed.tours.skipped - dummy tours already present");
            return;
        }

        List<Tour> tours = buildTours();
        tourRepository.saveAll(tours);

        List<TourInstance> instances = buildTourInstances();
        tourInstanceRepository.saveAll(instances);

        log.info("seed.tours.loaded count={} instances={}", tours.size(), instances.size());
    }

    // Round-robin agent IDs: odd-numbered tours → Priya (a-004), even → Lakshmi (a-006)
    private static final String AGENT_ODD  = "a-004"; // Priya Sharma
    private static final String AGENT_EVEN = "a-006"; // Lakshmi Prasad

    private List<Tour> buildTours() {
        return Arrays.asList(
            tour("t-1001", "A guided hike in the Dolomites",
                dest("Dolomites", "Italy"), hotel("Mountain Hut Lodge", "Cozy mountain huts along the Brenta Dolomites with stunning panoramic views."),
                TourType.HIKE, 5.0, 19, 4,
                "Experience the excitement among the wonders of the Brenta Dolomites! Unforgettable excursions, breathtaking views, adventure in a paradise.",
                10, AGENT_ODD),
            tour("t-1002", "Tropical Caribe",
                dest("Punta Cana", "Dominican Republic"), hotel("Tropical Caribe Resort", "Beachfront resort with crystal-clear lagoon pools, direct beach access, and world-class all-inclusive dining."),
                TourType.RESORT, 5.0, 19, 2,
                "Recharge in the Caribbean sun with turquoise beaches, local culture, and relaxed island days designed for comfort and fun.",
                10, AGENT_EVEN),
            tour("t-1003", "Riverside Resort",
                dest("Ao Nang", "Thailand"), hotel("Riverside Resort Krabi", "Riverside resort in Ao Nang surrounded by limestone cliffs and jungle, with pool and river views."),
                TourType.RESORT, 5.0, 19, 3,
                "Discover jungle trails, limestone cliffs, and peaceful riverside scenery in a tropical Thailand getaway made for nature lovers.",
                10, AGENT_ODD),
            tour("t-1004", "Costa Cruise",
                dest("Mediterranean Sea", "International"), hotel("MS Costa Bella", "Luxury cruise ship sailing the Mediterranean with fine dining, entertainment, and multiple port stops."),
                TourType.CRUISE, 5.0, 19, 5,
                "Sail across the Mediterranean with curated coastal stops, cultural city walks, and sunset decks for a classic cruise experience.",
                10, AGENT_EVEN),
            tour("t-1005", "Amalfi Coast Drive",
                dest("Amalfi", "Italy"), hotel("Amalfi Cliffside Boutique", "Boutique guesthouse perched on the Amalfi cliffs with sea views and private terrace."),
                TourType.RESORT, 4.8, 291, 2,
                "Drive the Amalfi coast road with scenic viewpoints, colorful villages, and authentic southern Italian food experiences.",
                10, AGENT_ODD),
            tour("t-1006", "Machu Picchu Explorer",
                dest("Cusco", "Peru"), hotel(null, null),
                TourType.HIKE, 4.9, 387, 6,
                "Trek through Peru's Sacred Valley and reach Machu Picchu with expert support, acclimatized pacing, and mountain panoramas.",
                10, AGENT_EVEN),
            tour("t-1007", "Alpine Lakes Weekend",
                dest("Hallstatt", "Austria"), hotel("Salzkammergut Alpine Hotel", "Family-run alpine hotel with mountain lake access and stunning Hallstatt views."),
                TourType.HIKE, 4.7, 164, 4,
                "Enjoy a short alpine break among Austria's lake villages with calm trails, photography spots, and local cuisine.",
                10, AGENT_ODD),
            tour("t-1008", "Coral Bay Escape",
                dest("Boracay", "Philippines"), hotel("White Beach Resort", "Beachfront resort on Boracay's famous White Beach with turquoise waters and vibrant nightlife."),
                TourType.RESORT, 4.9, 228, 3,
                "Relax on Boracay's white sand beaches, enjoy water sports, and experience the island's vibrant beach culture.",
                10, AGENT_EVEN),
            tour("t-1009", "Rainforest River Lodge",
                dest("Ubud", "Indonesia"), hotel("Ayung River Lodge", "Jungle lodge perched above the Ayung River valley with infinity pool and rice terrace views."),
                TourType.RESORT, 4.6, 203, 4,
                "Immerse yourself in Ubud's lush jungle, sacred temples, and the peaceful sounds of the Ayung River.",
                10, AGENT_ODD),
            tour("t-1010", "Adriatic Sunset Cruise",
                dest("Dubrovnik", "Croatia"), hotel("MS Adriatic Pearl", "Premium cruise ship sailing the Adriatic coast with stops in Dubrovnik, Split, Hvar, and Montenegro."),
                TourType.CRUISE, 4.8, 312, 5,
                "Sail the stunning Adriatic Sea with stops at Croatia's most beautiful coastal towns and islands.",
                10, AGENT_EVEN),
            tour("t-1011", "Tuscan Countryside Drive",
                dest("Siena", "Italy"), hotel("Chianti Hills Agriturismo", "Charming farmhouse hotel in the Chianti hills surrounded by vineyards and olive groves."),
                TourType.RESORT, 4.7, 187, 2,
                "Explore Tuscany's rolling hills, medieval towns, and world-famous wine regions on a curated countryside drive.",
                10, AGENT_ODD),
            tour("t-1012", "Sacred Valley Trek Plus",
                dest("Ollantaytambo", "Peru"), hotel(null, null),
                TourType.HIKE, 4.9, 275, 6,
                "Extended trek through the Sacred Valley of the Incas, covering ancient ruins, mountain passes, and Machu Picchu.",
                10, AGENT_EVEN),
            tour("t-1013", "Dolomite Panorama Camp",
                dest("Cortina d Ampezzo", "Italy"), hotel("Ampezzo Valley Lodge", "Alpine lodge in the heart of the Dolomites with direct access to panoramic hiking trails."),
                TourType.HIKE, 4.8, 211, 4,
                "Camp and hike through the most dramatic landscapes of the Cortina Dolomites with expert mountain guides.",
                10, AGENT_ODD),
            tour("t-1014", "Caribbean Family Resort",
                dest("Bayahibe", "Dominican Republic"), hotel("La Romana Family Resort", "Family-friendly all-inclusive resort on the quiet shores of Bayahibe with kids clubs and water activities."),
                TourType.RESORT, 4.6, 198, 5,
                "A perfect family escape on the pristine shores of Bayahibe with all-inclusive dining and water sports.",
                10, AGENT_EVEN)
        );
    }

    private List<TourInstance> buildTourInstances() {
        List<TourInstance> instances = new ArrayList<>();

        // t-1001: Dolomites hike
        instances.addAll(instances("t-1001",
            List.of("2026-08-05", "2026-09-10", "2026-10-08"),
            List.of(7, 10, 12), List.of(MealPlan.FB), 1400.0, 20));

        // t-1002: Tropical Caribe
        instances.addAll(instances("t-1002",
            List.of("2026-08-08", "2026-09-05", "2026-10-12"),
            List.of(7, 10, 12), List.of(MealPlan.BB, MealPlan.HB, MealPlan.FB, MealPlan.AI), 1400.0, 20));

        // t-1003: Riverside Resort
        instances.addAll(instances("t-1003",
            List.of("2026-08-12", "2026-09-15", "2026-10-20"),
            List.of(7, 10, 12), List.of(MealPlan.BB, MealPlan.HB, MealPlan.FB, MealPlan.AI), 1400.0, 20));

        // t-1004: Costa Cruise
        instances.addAll(instances("t-1004",
            List.of("2026-08-20", "2026-09-20", "2026-10-25"),
            List.of(7, 10, 12), List.of(MealPlan.FB, MealPlan.AI), 1400.0, 20));

        // t-1005: Amalfi Coast Drive
        instances.addAll(instances("t-1005",
            List.of("2026-05-19", "2026-06-10", "2026-07-08"),
            List.of(3, 5), List.of(MealPlan.BB, MealPlan.HB), 980.0, 15));

        // t-1006: Machu Picchu Explorer
        instances.addAll(instances("t-1006",
            List.of("2026-05-19", "2026-06-02", "2026-06-22"),
            List.of(7, 10), List.of(MealPlan.FB), 2100.0, 12));

        // t-1007: Alpine Lakes Weekend
        instances.addAll(instances("t-1007",
            List.of("2026-06-05", "2026-06-16", "2026-07-02"),
            List.of(3, 5), List.of(MealPlan.BB, MealPlan.HB), 890.0, 20));

        // t-1008: Coral Bay Escape
        instances.addAll(instances("t-1008",
            List.of("2026-06-08", "2026-06-21", "2026-07-10"),
            List.of(5, 7, 10), List.of(MealPlan.BB, MealPlan.AI), 1520.0, 20));

        // t-1009: Rainforest River Lodge
        instances.addAll(instances("t-1009",
            List.of("2026-06-11", "2026-06-24", "2026-07-14"),
            List.of(4, 6, 8), List.of(MealPlan.BB, MealPlan.HB), 1180.0, 20));

        // t-1010: Adriatic Sunset Cruise
        instances.addAll(instances("t-1010",
            List.of("2026-06-13", "2026-06-30", "2026-07-18"),
            List.of(5, 7), List.of(MealPlan.HB, MealPlan.FB), 1790.0, 20));

        // t-1011: Tuscan Countryside Drive
        instances.addAll(instances("t-1011",
            List.of("2026-06-07", "2026-06-19", "2026-07-05"),
            List.of(3, 5, 7), List.of(MealPlan.BB, MealPlan.HB), 1040.0, 15));

        // t-1012: Sacred Valley Trek Plus
        instances.addAll(instances("t-1012",
            List.of("2026-06-15", "2026-07-03", "2026-07-22"),
            List.of(7, 10, 12), List.of(MealPlan.FB), 2240.0, 12));

        // t-1013: Dolomite Panorama Camp
        instances.addAll(instances("t-1013",
            List.of("2026-06-18", "2026-07-08", "2026-07-26"),
            List.of(5, 7, 9), List.of(MealPlan.HB, MealPlan.FB), 1320.0, 20));

        // t-1014: Caribbean Family Resort
        instances.addAll(instances("t-1014",
            List.of("2026-06-22", "2026-07-12", "2026-07-30"),
            List.of(5, 7, 10), List.of(MealPlan.BB, MealPlan.AI), 1680.0, 20));

        return instances;
    }

    private List<TourInstance> instances(String tourId, List<String> dates,
                                          List<Integer> durations, List<MealPlan> mealPlans,
                                          double basePrice, int capacity) {
        List<TourInstance> result = new ArrayList<>();
        int seq = 1;
        for (String dateStr : dates) {
            LocalDate start = LocalDate.parse(dateStr);
            int firstDuration = durations.get(0);
            LocalDate end = start.plusDays(firstDuration);

            List<PricingOption> pricing = buildPricing(durations, basePrice);

            TourInstance ti = TourInstance.builder()
                .id("ti-" + tourId.replace("t-", "") + "-" + seq++)
                .tourId(tourId)
                .startDate(start)
                .endDate(end)
                .totalCapacity(capacity)
                .availableSlots(capacity)
                .durations(durations)
                .mealPlans(mealPlans)
                .pricing(pricing)
                .freeCancellationDays(10)
                .build();

            result.add(ti);
        }
        return result;
    }

    private List<PricingOption> buildPricing(List<Integer> durations, double basePrice) {
        List<PricingOption> pricing = new ArrayList<>();
        for (int dur : durations) {
            // price scales with duration: base for first duration, +35% per extra 3 days roughly
            double factor = 1.0 + (dur - durations.get(0)) * 0.05;
            BigDecimal price = BigDecimal.valueOf(Math.round(basePrice * factor));
            pricing.add(PricingOption.builder()
                .duration(dur)
                .pricePerPerson(price)
                .mealSupplementsPerDay(Map.of(
                    MealPlan.HB, BigDecimal.valueOf(30),
                    MealPlan.FB, BigDecimal.valueOf(50),
                    MealPlan.AI, BigDecimal.valueOf(80)
                ))
                .build());
        }
        return pricing;
    }

    private Tour tour(String id, String name, Destination destination, Hotel hotel,
                      TourType type, double rating, int reviews, int maxGuests,
                      String summary, int freeCancelDays, String travelAgentId) {
        return Tour.builder()
            .id(id)
            .name(name)
            .destination(destination)
            .hotel(hotel)
            .tourType(type)
            .rating(rating)
            .reviewCount(reviews)
            .imageUrls(List.of("https://example.com/tours/" + id + "/1.jpg"))
            .summary(summary)
            .freeCancellationDays(freeCancelDays)
            .travelAgentId(travelAgentId)
            .guestQuantity(GuestQuantityInfo.builder()
                .maxAdults(maxGuests)
                .maxChildren(0)
                .maxTotal(maxGuests)
                .build())
            .build();
    }

    private Destination dest(String city, String country) {
        return Destination.builder().city(city).country(country).build();
    }

    private Hotel hotel(String name, String description) {
        if (name == null) return null;
        return Hotel.builder().name(name).description(description).starRating(4).build();
    }
}
