package com.epam.edp.demo.service.impl;

import com.epam.edp.demo.config.RabbitMqConfig;
import com.epam.edp.demo.dto.event.ReviewStatsEvent;
import com.epam.edp.demo.dto.tour.DestinationListResponseDTO;
import com.epam.edp.demo.dto.tour.ReviewDTO;
import com.epam.edp.demo.dto.tour.ReviewListResponseDTO;
import com.epam.edp.demo.dto.tour.TourDetailsDTO;
import com.epam.edp.demo.dto.tour.TourFeedbackRequestDTO;
import com.epam.edp.demo.dto.tour.TourListResponseDTO;
import com.epam.edp.demo.dto.tour.TourSummaryDTO;
import com.epam.edp.demo.dto.user.MessageResponseDTO;
import com.epam.edp.demo.entity.Booking;
import com.epam.edp.demo.entity.GuestQuantityInfo;
import com.epam.edp.demo.entity.PricingOption;
import com.epam.edp.demo.entity.Review;
import com.epam.edp.demo.entity.Tour;
import com.epam.edp.demo.entity.TourInstance;
import com.epam.edp.demo.enums.BookingStatus;
import com.epam.edp.demo.enums.TourType;
import com.epam.edp.demo.exception.BadRequestException;
import com.epam.edp.demo.exception.FeedbackAlreadyExistsException;
import com.epam.edp.demo.exception.TourNotFoundException;
import com.epam.edp.demo.mapper.ReviewMapper;
import com.epam.edp.demo.mapper.TourMapper;
import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.repository.BookingRepository;
import com.epam.edp.demo.repository.MongoReviewRepository;
import com.epam.edp.demo.repository.TourInstanceRepository;
import com.epam.edp.demo.repository.TourRepository;
import com.epam.edp.demo.repository.TravelAgentRepository;
import com.epam.edp.demo.repository.UserRepository;
import com.epam.edp.demo.service.ContentModerationService;
import com.epam.edp.demo.service.TourService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourServiceImpl implements TourService {

    private static final int MIN_QUERY_LENGTH = 3;
    private static final String SORT_RATING_DESC = "RATING_DESC";
    private static final String SORT_RATING_ASC = "RATING_ASC";

    private final TourRepository tourRepository;
    private final TourInstanceRepository tourInstanceRepository;
    private final MongoReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TravelAgentRepository travelAgentRepository;
    private final TourMapper tourMapper;
    private final ReviewMapper reviewMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ContentModerationService contentModerationService;

    @Override
    public DestinationListResponseDTO getDestinations(String query) {
        if (query == null || query.length() < MIN_QUERY_LENGTH) {
            return new DestinationListResponseDTO(List.of());
        }
        List<String> matched = tourRepository.searchDestinations(query).stream()
                .filter(t -> t.getDestination() != null)
                .map(t -> t.getDestination().getCity() + ", " + t.getDestination().getCountry())
                .distinct()
                .sorted()
                .toList();
        return new DestinationListResponseDTO(matched);
    }

    @Override
    public TourListResponseDTO getAvailableTours(int page, int pageSize, String destination, String startDate,
                                                  String duration, int adults, int children, String mealPlan,
                                                  String tourType, String sortBy) {
        List<TourInstance> matchingInstances = tourInstanceRepository.findAll().stream()
                .filter(i -> matchesStartDate(i, startDate))
                .filter(i -> matchesDuration(i, duration))
                .filter(i -> matchesMealPlan(i, mealPlan))
                .filter(i -> i.getAvailableSlots() >= adults + children)
                .toList();

        Map<String, Tour> tourCache = new LinkedHashMap<>();
        List<TourWithInstance> candidates = buildCandidates(tourCache, matchingInstances, destination, tourType, adults, children);

        Map<String, TourWithInstance> deduped = deduplicateByEarliestDate(candidates);

        List<TourWithInstance> results = new ArrayList<>(deduped.values());
        sort(results, sortBy);

        int totalItems = results.size();
        int totalPages = totalItems == 0 ? 1 : (int) Math.ceil((double) totalItems / pageSize);
        int safePage   = Math.min(page, totalPages);
        int fromIndex  = (safePage - 1) * pageSize;
        int toIndex    = Math.min(fromIndex + pageSize, totalItems);

        List<TourSummaryDTO> paginated = results.subList(fromIndex, toIndex).stream()
                .map(e -> tourMapper.toTourSummaryDTO(e.tour(), e.instance()))
                .toList();

        return tourMapper.toTourListResponseDTO(paginated, safePage, pageSize, totalPages, totalItems);
    }

    @Override
    public TourDetailsDTO getTourById(String id) {
        Tour tour = tourRepository.findById(id)
                .orElseThrow(() -> new TourNotFoundException(id));
        List<TourInstance> instances = tourInstanceRepository.findByTourId(id);
        return tourMapper.toTourDetailsDTO(tour, instances);
    }

    @Override
    @Cacheable(value = "reviews", key = "#tourId + ':' + #page + ':' + #pageSize + ':' + #sortBy")
    public ReviewListResponseDTO getReviews(String tourId, int page, int pageSize, String sortBy) {
        tourRepository.findById(tourId)
                .orElseThrow(() -> new TourNotFoundException(tourId));

        // Only show published reviews on the public tour page
        List<Review> reviews = new ArrayList<>(reviewRepository.findByTourIdAndVisibility(tourId, "PUBLISHED"));
        sortReviews(reviews, sortBy);

        int totalItems = reviews.size();
        if (totalItems == 0) {
            return new ReviewListResponseDTO(List.of(), page, pageSize, 0, 0);
        }

        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        int safePage   = Math.min(page, totalPages);
        int fromIndex  = (safePage - 1) * pageSize;
        int toIndex    = Math.min(fromIndex + pageSize, totalItems);

        List<ReviewDTO> paginated = reviews.subList(fromIndex, toIndex).stream()
                .map(reviewMapper::toDTO)
                .toList();

        return new ReviewListResponseDTO(paginated, safePage, pageSize, totalPages, totalItems);
    }

    @Override
    public MessageResponseDTO submitFeedback(String tourId, TourFeedbackRequestDTO request, String userId) {
        Tour tourEntity = tourRepository.findById(tourId)
                .orElseThrow(() -> new TourNotFoundException(tourId));

        int rating = request.getRating();
        if (rating <= 3 && (request.getComment() == null || request.getComment().isBlank())) {
            throw new BadRequestException("Comment is required when rating is 3 or below");
        }

        List<Booking> bookings = bookingRepository.findByUserIdAndTourId(userId, tourId);
        boolean hasValidBooking = bookings.stream()
                .anyMatch(b -> b.getState() == BookingStatus.STARTED
                        || b.getState() == BookingStatus.FINISHED);
        if (!hasValidBooking) {
            throw new BadRequestException("You must have an active or completed booking for this tour to submit feedback");
        }

        // Upsert: update existing review if present, otherwise create a new one
        java.util.Optional<Review> existing = reviewRepository.findByUserIdAndTourId(userId, tourId);
        Review review = existing.orElseGet(() -> {
            Review r = new Review();
            r.setUserId(userId);
            r.setTourId(tourId);
            r.setVisibility("PUBLISHED");
            return r;
        });
        review.setRate(rating);
        review.setReviewContent(request.getComment());
        review.setCreatedAt(java.time.Instant.now().toString());

        // Populate author info from User profile
        userRepository.findById(userId).ifPresent(user -> {
            String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " " +
                    (user.getLastName() != null ? user.getLastName() : "")).trim();
            review.setAuthorName(fullName.isEmpty() ? "Traveler" : fullName);
            review.setAuthorImageUrl(user.getImageUrl());
        });

        // Denormalize tour metadata for admin filtering and auto-flag prohibited content
        if (tourEntity != null) {
            review.setTourName(tourEntity.getName());
            review.setTourType(tourEntity.getTourType() != null ? tourEntity.getTourType().name() : null);
        }

        String contentToCheck = (request.getComment() != null ? request.getComment() : "");
        if (contentModerationService.containsProhibitedContent(contentToCheck)) {
            review.setFlagged(true);
            review.setFlagReason(contentModerationService.getFlagReason(contentToCheck));
            log.warn("review.auto-flagged userId={} tourId={} reason={}", userId, tourId, review.getFlagReason());
        } else {
            review.setFlagged(false);
            review.setFlagReason(null);
        }

        reviewRepository.save(review);
        log.info("Feedback upserted by user {} for tour {}", userId, tourId);

        // Publish review stats event so report-app can update feedback metrics
        try {
            String agentId = tourEntity != null ? tourEntity.getTravelAgentId() : null;
            ReviewStatsEvent reviewEvent = ReviewStatsEvent.builder()
                    .tourId(tourId)
                    .tourName(tourEntity != null ? tourEntity.getName() : "")
                    .travelAgentId(agentId)
                    .userId(userId)
                    .rating(rating)
                    .eventTimestamp(java.time.Instant.now())
                    .build();
            rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.REVIEW_ROUTING, reviewEvent);
        } catch (Exception ex) {
            log.warn("rabbitmq.review.publish.failed tourId={} reason={}", tourId, ex.getMessage());
        }

        return new MessageResponseDTO(existing.isPresent()
                ? "Your feedback has been updated."
                : "Your feedback has been submitted.");
    }

    @Override
    public ReviewDTO getMyFeedback(String tourId, String userId) {
        Review review = reviewRepository.findByUserIdAndTourId(userId, tourId)
                .orElseThrow(() -> new TourNotFoundException("No feedback found for this tour"));
        ReviewDTO dto = new ReviewDTO();
        dto.setRate(review.getRate());
        dto.setReviewContent(review.getReviewContent());
        dto.setAuthorName(review.getAuthorName());
        dto.setAuthorImageUrl(review.getAuthorImageUrl());
        dto.setCreatedAt(review.getCreatedAt() != null ? review.getCreatedAt().toString() : null);
        return dto;
    }

    private List<TourWithInstance> buildCandidates(Map<String, Tour> tourCache, List<TourInstance> instances,
                                                     String destination, String tourType, int adults, int children) {
        return instances.stream()
                .map(instance -> {
                    Tour tour = tourCache.computeIfAbsent(instance.getTourId(),
                            id -> tourRepository.findById(id).orElse(null));
                    return tour != null ? new TourWithInstance(tour, instance) : null;
                })
                .filter(Objects::nonNull)
                .filter(e -> matchesDestination(e.tour(), destination))
                .filter(e -> matchesTourType(e.tour(), tourType))
                .filter(e -> matchesGuests(e.tour(), adults, children))
                .toList();
    }

    private Map<String, TourWithInstance> deduplicateByEarliestDate(List<TourWithInstance> candidates) {
        Map<String, TourWithInstance> deduped = new LinkedHashMap<>();
        for (TourWithInstance entry : candidates) {
            deduped.merge(entry.tour().getId(), entry, this::chooseBestInstance);
        }
        return deduped;
    }

    private TourWithInstance chooseBestInstance(TourWithInstance existing, TourWithInstance challenger) {
        LocalDate existingDate = existing.instance().getStartDate();
        if (existingDate == null) {
            return challenger;
        }
        LocalDate challengerDate = challenger.instance().getStartDate();
        if (challengerDate == null) {
            return existing;
        }
        return challengerDate.isBefore(existingDate) ? challenger : existing;
    }

    private boolean matchesStartDate(TourInstance instance, String startDate) {
        if (startDate == null || startDate.isBlank()) {
            return true;
        }
        if (instance.getStartDate() == null) {
            return false;
        }
        LocalDate requested = LocalDate.parse(startDate);
        return !instance.getStartDate().isBefore(requested);
    }

    private boolean matchesDuration(TourInstance instance, String duration) {
        if (duration == null || duration.isBlank()) {
            return true;
        }
        if (instance.getDurations() == null) {
            return false;
        }
        int days = parseDurationDays(duration);
        return days > 0 && instance.getDurations().contains(days);
    }

    private boolean matchesMealPlan(TourInstance instance, String mealPlan) {
        if (mealPlan == null || mealPlan.isBlank()) {
            return true;
        }
        if (instance.getMealPlans() == null) {
            return false;
        }
        return instance.getMealPlans().stream()
                .anyMatch(mp -> mp.name().equalsIgnoreCase(mealPlan));
    }

    private boolean matchesDestination(Tour tour, String destination) {
        if (destination == null || destination.isBlank()
                || "Any destination".equalsIgnoreCase(destination)) {
            return true;
        }
        if (tour.getDestination() == null) {
            return false;
        }
        String formatted = tour.getDestination().getCity() + ", " + tour.getDestination().getCountry();
        return formatted.toLowerCase().contains(destination.toLowerCase());
    }

    private boolean matchesGuests(Tour tour, int adults, int children) {
        GuestQuantityInfo gq = tour.getGuestQuantity();
        if (gq == null) {
            return true;
        }
        return gq.getMaxAdults() >= adults && gq.getMaxChildren() >= children;
    }

    private boolean matchesTourType(Tour tour, String tourType) {
        if (tourType == null || tourType.isBlank()) {
            return true;
        }
        if (tour.getTourType() == null) {
            return false;
        }
        try {
            return tour.getTourType() == TourType.valueOf(tourType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void sort(List<TourWithInstance> entries, String sortBy) {
        if (sortBy == null) {
            sortBy = SORT_RATING_DESC;
        }
        switch (sortBy) {
            case SORT_RATING_ASC:
                entries.sort(Comparator.comparingDouble(e -> nullSafeRating(e.tour())));
                break;
            case "PRICE_ASC":
                entries.sort(Comparator.comparingInt(e -> minPrice(e.instance())));
                break;
            case "PRICE_DESC":
                entries.sort(Comparator.<TourWithInstance>comparingInt(e -> minPrice(e.instance())).reversed());
                break;
            default:
                entries.sort(Comparator.<TourWithInstance>comparingDouble(e -> nullSafeRating(e.tour())).reversed());
        }
    }

    private void sortReviews(List<Review> reviews, String sortBy) {
        if (sortBy == null) {
            sortBy = SORT_RATING_DESC;
        }
        switch (sortBy) {
            case SORT_RATING_ASC:
                reviews.sort(Comparator.comparingInt(Review::getRate));
                break;
            case "NEWEST":
                reviews.sort(Comparator.comparing(Review::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed());
                break;
            case "OLDEST":
                reviews.sort(Comparator.comparing(Review::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder())));
                break;
            default:
                reviews.sort(Comparator.comparingInt(Review::getRate).reversed());
        }
    }

    private int parseDurationDays(String duration) {
        try {
            return Integer.parseInt(duration.trim().split("\\s+")[0]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int minPrice(TourInstance instance) {
        if (instance.getPricing() == null || instance.getPricing().isEmpty()) {
            return Integer.MAX_VALUE;
        }
        return instance.getPricing().stream()
                .map(PricingOption::getPricePerPerson)
                .filter(Objects::nonNull)
                .mapToInt(BigDecimal::intValue)
                .min()
                .orElse(Integer.MAX_VALUE);
    }

    private double nullSafeRating(Tour tour) {
        return tour.getRating() != null ? tour.getRating() : 0.0;
    }

    private record TourWithInstance(Tour tour, TourInstance instance) {}
}
