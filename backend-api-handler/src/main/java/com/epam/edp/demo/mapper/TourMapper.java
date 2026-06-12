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
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class TourMapper {

    private static final String DAYS_SUFFIX = " days";

    private final MealPlanFormatter mealPlanFormatter;

    public TourMapper(MealPlanFormatter mealPlanFormatter) {
        this.mealPlanFormatter = mealPlanFormatter;
    }

    public TourSummaryDTO toTourSummaryDTO(Tour tour, TourInstance instance) {
        if (tour == null || instance == null) {
            return null;
        }
        int freeCancDays = resolveFreeCancellationDays(tour, instance);

        return TourSummaryDTO.builder()
                .id(tour.getId())
                .name(tour.getName())
                .destination(formatDestination(tour.getDestination()))
                .startDate(instance.getStartDate())
                .durations(formatDurations(instance.getDurations()))
                .mealPlans(formatMealPlans(instance.getMealPlans()))
                .price(computeMinPrice(instance.getPricing()))
                .rating(tour.getRating())
                .reviews(tour.getReviewCount())
                .freeCancellation(instance.getStartDate().minusDays(freeCancDays))
                .build();
    }

    public TourListResponseDTO toTourListResponseDTO(
            List<TourSummaryDTO> tours,
            int page,
            int pageSize,
            int totalPages,
            int totalItems) {

        return TourListResponseDTO.builder()
                .tours(tours)
                .page(page)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .totalItems(totalItems)
                .build();
    }

    public TourDetailsDTO toTourDetailsDTO(Tour tour, List<TourInstance> instances) {
        List<Integer> allDurations = instances.stream()
                .filter(i -> i.getDurations() != null)
                .flatMap(i -> i.getDurations().stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        List<MealPlan> allMealPlans = instances.stream()
                .filter(i -> i.getMealPlans() != null)
                .flatMap(i -> i.getMealPlans().stream())
                .distinct()
                .collect(Collectors.toList());

        List<LocalDate> startDates = instances.stream()
                .map(TourInstance::getStartDate)
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        int freeCancDays = resolveFreeCancellationDaysFromInstances(tour, instances);
        Hotel hotel = tour.getHotel();

        return TourDetailsDTO.builder()
                .id(tour.getId())
                .name(tour.getName())
                .destination(formatDestination(tour.getDestination()))
                .rating(tour.getRating())
                .imageUrls(tour.getImageUrls())
                .summary(tour.getSummary())
                .freeCancelationDaysBefore(freeCancDays)
                .durations(formatDurations(allDurations))
                .accomodiation(hotel != null ? hotel.getDescription() : null)
                .hotelName(hotel != null ? hotel.getName() : null)
                .hotelDescription(hotel != null ? hotel.getDescription() : null)
                .mealPlans(formatMealPlans(allMealPlans))
                .customDetails(Collections.emptyMap())
                .startDates(startDates)
                .guestQuantity(toGuestQuantityDTO(tour.getGuestQuantity()))
                .price(buildPriceMap(instances))
                .mealSupplementsPerDay(buildMealSupplementsMap(instances))
                .build();
    }

    private String formatDestination(Destination destination) {
        if (destination == null) {
            return null;
        }
        return destination.getCity() + ", " + destination.getCountry();
    }

    private List<String> formatDurations(List<Integer> durations) {
        if (durations == null || durations.isEmpty()) {
            return Collections.emptyList();
        }
        return durations.stream()
                .map(d -> d + DAYS_SUFFIX)
                .collect(Collectors.toList());
    }

    private List<String> formatMealPlans(List<MealPlan> mealPlans) {
        if (mealPlans == null || mealPlans.isEmpty()) {
            return Collections.emptyList();
        }
        return mealPlans.stream()
                .map(mp -> mealPlanFormatter.format(mp.name()))
                .collect(Collectors.toList());
    }

    private String computeMinPrice(List<PricingOption> pricing) {
        if (pricing == null || pricing.isEmpty()) {
            return null;
        }
        return pricing.stream()
                .map(PricingOption::getPricePerPerson)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .map(min -> "from $" + min.stripTrailingZeros().toPlainString() + " for 1 person")
                .orElse(null);
    }

    private Map<String, String> buildPriceMap(List<TourInstance> instances) {
        Map<String, String> priceMap = new LinkedHashMap<>();
        for (TourInstance instance : instances) {
            if (instance.getPricing() == null) {
                continue;
            }
            for (PricingOption option : instance.getPricing()) {
                if (option.getPricePerPerson() == null) {
                    continue;
                }
                String key = option.getDuration() + DAYS_SUFFIX;
                priceMap.putIfAbsent(key,
                        "$" + option.getPricePerPerson().stripTrailingZeros().toPlainString());
            }
        }
        return priceMap;
    }

    private Map<String, String> buildMealSupplementsMap(List<TourInstance> instances) {
        Map<String, String> supplementsMap = new LinkedHashMap<>();
        for (TourInstance instance : instances) {
            if (instance.getPricing() != null) {
                instance.getPricing().stream()
                        .filter(option -> option.getMealSupplementsPerDay() != null)
                        .forEach(option -> option.getMealSupplementsPerDay().forEach((mealPlan, supplement) -> {
                            if (supplement != null) {
                                supplementsMap.putIfAbsent(mealPlan.name(),
                                        "$" + supplement.stripTrailingZeros().toPlainString());
                            }
                        }));
            }
            if (!supplementsMap.isEmpty()) {
                break;
            }
        }
        return supplementsMap;
    }

    private int resolveFreeCancellationDays(Tour tour, TourInstance instance) {
        return instance.getFreeCancellationDays() > 0
                ? instance.getFreeCancellationDays()
                : tour.getFreeCancellationDays();
    }

    private int resolveFreeCancellationDaysFromInstances(Tour tour, List<TourInstance> instances) {
        return instances.stream()
                .mapToInt(TourInstance::getFreeCancellationDays)
                .filter(days -> days > 0)
                .findFirst()
                .orElse(tour.getFreeCancellationDays());
    }

    private GuestQuantityDTO toGuestQuantityDTO(GuestQuantityInfo info) {
        if (info == null) {
            return new GuestQuantityDTO();
        }
        return GuestQuantityDTO.builder()
                .adultsMaxValue(info.getMaxAdults())
                .childrenMaxValue(info.getMaxChildren())
                .totalMaxVelue(info.getMaxTotal())
                .build();
    }
}
