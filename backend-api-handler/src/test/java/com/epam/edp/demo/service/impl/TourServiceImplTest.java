package com.epam.edp.demo.service.impl;

import com.epam.edp.demo.dto.tour.DestinationListResponseDTO;
import com.epam.edp.demo.dto.tour.ReviewDTO;
import com.epam.edp.demo.dto.tour.ReviewListResponseDTO;
import com.epam.edp.demo.dto.tour.TourDetailsDTO;
import com.epam.edp.demo.dto.tour.TourListResponseDTO;
import com.epam.edp.demo.dto.tour.TourSummaryDTO;
import com.epam.edp.demo.entity.Destination;
import com.epam.edp.demo.entity.GuestQuantityInfo;
import com.epam.edp.demo.entity.PricingOption;
import com.epam.edp.demo.entity.Review;
import com.epam.edp.demo.entity.Tour;
import com.epam.edp.demo.entity.TourInstance;
import com.epam.edp.demo.enums.MealPlan;
import com.epam.edp.demo.enums.TourType;
import com.epam.edp.demo.exception.TourNotFoundException;
import com.epam.edp.demo.mapper.ReviewMapper;
import com.epam.edp.demo.mapper.TourMapper;
import com.epam.edp.demo.repository.BookingRepository;
import com.epam.edp.demo.repository.MongoReviewRepository;
import com.epam.edp.demo.repository.TourInstanceRepository;
import com.epam.edp.demo.repository.TourRepository;
import com.epam.edp.demo.repository.UserRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.epam.edp.demo.repository.TravelAgentRepository;
import com.epam.edp.demo.service.ContentModerationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Service-layer unit tests for TourServiceImpl.getTourById and getReviews.
 * Exercises pagination, sorting, edge cases, and exception propagation.
 * Kept separate from TourServiceImplTest to avoid touching unrelated tests.
 */
@RunWith(MockitoJUnitRunner.class)
public class TourServiceImplTest {

    @Mock private TourRepository tourRepository;
    @Mock private TourInstanceRepository tourInstanceRepository;
    @Mock private MongoReviewRepository reviewRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private TourMapper tourMapper;
    @Mock private ReviewMapper reviewMapper;
    @Mock private UserRepository userRepository;
    @Mock private TravelAgentRepository travelAgentRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private ContentModerationService contentModerationService;

    private TourServiceImpl tourService;

    @Before
    public void setUp() {
        tourService = new TourServiceImpl(
                tourRepository, tourInstanceRepository, reviewRepository,
                bookingRepository, userRepository, travelAgentRepository,
                tourMapper, reviewMapper, rabbitTemplate, contentModerationService);
    }

    // ΓöÇΓöÇ getTourById ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ

    @Test
    public void getTourById_found_returnsMapperResult() {
        Tour tour = Tour.builder().id("t-1").name("Garden Resort").build();
        List<TourInstance> instances = Collections.singletonList(
                TourInstance.builder().id("i-1").tourId("t-1").build());
        TourDetailsDTO expected = new TourDetailsDTO();
        expected.setId("t-1");

        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(tourInstanceRepository.findByTourId("t-1")).thenReturn(instances);
        when(tourMapper.toTourDetailsDTO(tour, instances)).thenReturn(expected);

        TourDetailsDTO result = tourService.getTourById("t-1");

        assertSame(expected, result);
        verify(tourInstanceRepository).findByTourId("t-1");
        verify(tourMapper).toTourDetailsDTO(tour, instances);
    }

    @Test(expected = TourNotFoundException.class)
    public void getTourById_notFound_throwsTourNotFoundException() {
        when(tourRepository.findById("missing")).thenReturn(Optional.empty());
        tourService.getTourById("missing");
    }

    @Test
    public void getTourById_notFound_exceptionMessageContainsTourId() {
        when(tourRepository.findById("bad-id")).thenReturn(Optional.empty());

        try {
            tourService.getTourById("bad-id");
            fail("Expected TourNotFoundException");
        } catch (TourNotFoundException ex) {
            assertTrue("Exception message should contain the tour id",
                    ex.getMessage().contains("bad-id"));
        }
    }

    @Test
    public void getTourById_tourWithNoInstances_passesEmptyListToMapper() {
        Tour tour = Tour.builder().id("t-no-instances").build();

        when(tourRepository.findById("t-no-instances")).thenReturn(Optional.of(tour));
        when(tourInstanceRepository.findByTourId("t-no-instances"))
                .thenReturn(Collections.emptyList());
        when(tourMapper.toTourDetailsDTO(tour, Collections.emptyList()))
                .thenReturn(new TourDetailsDTO());

        TourDetailsDTO result = tourService.getTourById("t-no-instances");

        assertNotNull(result);
        verify(tourMapper).toTourDetailsDTO(tour, Collections.emptyList());
    }

    @Test
    public void getTourById_tourWithMultipleInstances_allInstancesPassedToMapper() {
        Tour tour = Tour.builder().id("t-multi").build();
        List<TourInstance> instances = Arrays.asList(
                TourInstance.builder().id("i-1").tourId("t-multi").build(),
                TourInstance.builder().id("i-2").tourId("t-multi").build(),
                TourInstance.builder().id("i-3").tourId("t-multi").build());
        TourDetailsDTO expected = new TourDetailsDTO();

        when(tourRepository.findById("t-multi")).thenReturn(Optional.of(tour));
        when(tourInstanceRepository.findByTourId("t-multi")).thenReturn(instances);
        when(tourMapper.toTourDetailsDTO(tour, instances)).thenReturn(expected);

        TourDetailsDTO result = tourService.getTourById("t-multi");

        assertSame(expected, result);
        verify(tourMapper).toTourDetailsDTO(tour, instances);
    }

    @Test
    public void getTourById_repositoryThrowsException_propagatesException() {
        when(tourRepository.findById("t-err"))
                .thenThrow(new RuntimeException("DB error"));

        try {
            tourService.getTourById("t-err");
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
            assertEquals("DB error", ex.getMessage());
        }
    }

    // ΓöÇΓöÇ getReviews ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ

    @Test
    public void getReviews_twoReviews_returnsCorrectPaginationMetadata() {
        Tour tour = Tour.builder().id("t-1").build();
        Review r1 = makeReview("r-1", "t-1", "u-1", "Alice", null, "2024-08-01", 5, "Great!");
        Review r2 = makeReview("r-2", "t-1", "u-2", "Bob",   null, "2024-07-01", 3, "OK");

        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(reviewRepository.findByTourIdAndVisibility("t-1", "PUBLISHED")).thenReturn(Arrays.asList(r1, r2));
        when(reviewMapper.toDTO(r1)).thenReturn(buildReviewDTO("Alice", 5));
        when(reviewMapper.toDTO(r2)).thenReturn(buildReviewDTO("Bob", 3));

        ReviewListResponseDTO result = tourService.getReviews("t-1", 1, 4, "RATING_DESC");

        assertEquals(2, result.getTotalItems());
        assertEquals(1, result.getTotalPages());
        assertEquals(1, result.getPage());
        assertEquals(4, result.getPageSize());
        assertEquals(2, result.getReviews().size());
    }

    @Test(expected = TourNotFoundException.class)
    public void getReviews_tourNotFound_throwsTourNotFoundException() {
        when(tourRepository.findById("bad")).thenReturn(Optional.empty());
        tourService.getReviews("bad", 1, 4, "RATING_DESC");
    }

    @Test
    public void getReviews_emptyReviewList_returnsZeroTotalsAndEmptyReviews() {
        Tour tour = Tour.builder().id("t-empty").build();

        when(tourRepository.findById("t-empty")).thenReturn(Optional.of(tour));
        when(reviewRepository.findByTourIdAndVisibility("t-empty", "PUBLISHED")).thenReturn(Collections.emptyList());

        ReviewListResponseDTO result = tourService.getReviews("t-empty", 1, 4, "RATING_DESC");

        assertEquals(0, result.getTotalItems());
        assertEquals(0, result.getTotalPages());
        assertTrue(result.getReviews().isEmpty());
    }

    @Test
    public void getReviews_defaultSortDesc_highestRatingReturnedFirst() {
        Tour tour = Tour.builder().id("t-1").build();
        Review low  = new Review("r-1", "t-1", "u-1", "Alice", null, "2024-01-01", 2, "Poor", null, null, "PUBLISHED", false, null);
        Review high = new Review("r-2", "t-1", "u-2", "Bob",   null, "2024-01-02", 5, "Great", null, null, "PUBLISHED", false, null);

        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(reviewRepository.findByTourIdAndVisibility("t-1", "PUBLISHED")).thenReturn(Arrays.asList(low, high));
        when(reviewMapper.toDTO(high)).thenReturn(buildReviewDTO("Bob",   5));
        when(reviewMapper.toDTO(low)).thenReturn(buildReviewDTO("Alice", 2));

        ReviewListResponseDTO result = tourService.getReviews("t-1", 1, 4, "RATING_DESC");

        assertEquals(5, result.getReviews().get(0).getRate());
        assertEquals(2, result.getReviews().get(1).getRate());
    }

    @Test
    public void getReviews_sortAsc_lowestRatingReturnedFirst() {
        Tour tour = Tour.builder().id("t-1").build();
        Review high = new Review("r-1", "t-1", "u-2", "Bob",   null, "2024-01-02", 5, "Great", null, null, "PUBLISHED", false, null);
        Review low  = new Review("r-2", "t-1", "u-1", "Alice", null, "2024-01-01", 2, "Poor", null, null, "PUBLISHED", false, null);

        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(reviewRepository.findByTourIdAndVisibility("t-1", "PUBLISHED")).thenReturn(Arrays.asList(high, low));
        when(reviewMapper.toDTO(low)).thenReturn(buildReviewDTO("Alice", 2));
        when(reviewMapper.toDTO(high)).thenReturn(buildReviewDTO("Bob",  5));

        ReviewListResponseDTO result = tourService.getReviews("t-1", 1, 4, "RATING_ASC");

        assertEquals(2, result.getReviews().get(0).getRate());
        assertEquals(5, result.getReviews().get(1).getRate());
    }

    @Test
    public void getReviews_sortNewest_latestCreatedAtReturnedFirst() {
        Tour tour = Tour.builder().id("t-1").build();
        Review older = new Review("r-1", "t-1", "u-1", "Alice", null, "2024-01-01", 4, "Good", null, null, "PUBLISHED", false, null);
        Review newer = new Review("r-2", "t-1", "u-2", "Bob",   null, "2024-12-01", 4, "Also Good", null, null, "PUBLISHED", false, null);

        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(reviewRepository.findByTourIdAndVisibility("t-1", "PUBLISHED")).thenReturn(Arrays.asList(older, newer));
        when(reviewMapper.toDTO(newer)).thenReturn(buildReviewDTOWithName("Bob",   4));
        when(reviewMapper.toDTO(older)).thenReturn(buildReviewDTOWithName("Alice", 4));

        ReviewListResponseDTO result = tourService.getReviews("t-1", 1, 4, "NEWEST");

        assertEquals("Bob",   result.getReviews().get(0).getAuthorName());
        assertEquals("Alice", result.getReviews().get(1).getAuthorName());
    }

    @Test
    public void getReviews_sortOldest_earliestCreatedAtReturnedFirst() {
        Tour tour = Tour.builder().id("t-1").build();
        Review older = new Review("r-1", "t-1", "u-1", "Alice", null, "2024-01-01", 4, "Good", null, null, "PUBLISHED", false, null);
        Review newer = new Review("r-2", "t-1", "u-2", "Bob",   null, "2024-12-01", 4, "Also Good", null, null, "PUBLISHED", false, null);

        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(reviewRepository.findByTourIdAndVisibility("t-1", "PUBLISHED")).thenReturn(Arrays.asList(newer, older));
        when(reviewMapper.toDTO(older)).thenReturn(buildReviewDTOWithName("Alice", 4));
        when(reviewMapper.toDTO(newer)).thenReturn(buildReviewDTOWithName("Bob",   4));

        ReviewListResponseDTO result = tourService.getReviews("t-1", 1, 4, "OLDEST");

        assertEquals("Alice", result.getReviews().get(0).getAuthorName());
        assertEquals("Bob",   result.getReviews().get(1).getAuthorName());
    }

    @Test
    public void getReviews_pageExceedsTotalPages_clampsToLastPage() {
        Tour tour = Tour.builder().id("t-1").build();
        Review r1 = new Review("r-1", "t-1", "u-1", "Alice", null, "2024-01-01", 5, "Great!", null, null, "PUBLISHED", false, null);
        Review r2 = new Review("r-2", "t-1", "u-2", "Bob",   null, "2024-01-02", 4, "Good", null, null, "PUBLISHED", false, null);

        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(reviewRepository.findByTourIdAndVisibility("t-1", "PUBLISHED")).thenReturn(Arrays.asList(r1, r2));
        when(reviewMapper.toDTO(r1)).thenReturn(buildReviewDTO("Alice", 5));
        when(reviewMapper.toDTO(r2)).thenReturn(buildReviewDTO("Bob",   4));

        // 2 reviews, pageSize=4 ΓåÆ totalPages=1; requesting page=10 ΓåÆ clamped to 1
        ReviewListResponseDTO result = tourService.getReviews("t-1", 10, 4, "RATING_DESC");

        assertEquals(1, result.getPage());
        assertEquals(2, result.getTotalItems());
        assertEquals(2, result.getReviews().size());
    }

    @Test
    public void getReviews_secondPageOfTwo_returnsOnlyRemainingItem() {
        Tour tour = Tour.builder().id("t-1").build();
        Review r1 = new Review("r-1", "t-1", "u-1", "A", null, "2024-01-01", 5, "", null, null, "PUBLISHED", false, null);
        Review r2 = new Review("r-2", "t-1", "u-2", "B", null, "2024-01-01", 4, "", null, null, "PUBLISHED", false, null);
        Review r3 = new Review("r-3", "t-1", "u-3", "C", null, "2024-01-01", 3, "", null, null, "PUBLISHED", false, null);

        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(reviewRepository.findByTourIdAndVisibility("t-1", "PUBLISHED")).thenReturn(Arrays.asList(r1, r2, r3));
        // Only stub the review that will be on page 2 (r3)
        when(reviewMapper.toDTO(r3)).thenReturn(buildReviewDTO("C", 3));

        // pageSize=2, 3 items ΓåÆ 2 pages; page 2 contains only the third item (rate=3)
        ReviewListResponseDTO result = tourService.getReviews("t-1", 2, 2, "RATING_DESC");

        assertEquals(2, result.getTotalPages());
        assertEquals(3, result.getTotalItems());
        assertEquals(1, result.getReviews().size());
        assertEquals(3, result.getReviews().get(0).getRate());
    }

    @Test
    public void getReviews_nullCreatedAt_doesNotThrowOnNewestSort() {
        Tour tour = Tour.builder().id("t-1").build();
        Review r = new Review("r-1", "t-1", "u-1", "Alice", null, null, 5, "Great!", null, null, "PUBLISHED", false, null);

        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(reviewRepository.findByTourIdAndVisibility("t-1", "PUBLISHED")).thenReturn(Collections.singletonList(r));
        when(reviewMapper.toDTO(r)).thenReturn(buildReviewDTO("Alice", 5));

        // nullsLast comparator should handle null createdAt without NPE
        ReviewListResponseDTO result = tourService.getReviews("t-1", 1, 4, "NEWEST");

        assertEquals(1, result.getTotalItems());
    }

    @Test
    public void getReviews_nullCreatedAt_doesNotThrowOnOldestSort() {
        Tour tour = Tour.builder().id("t-1").build();
        Review r = new Review("r-1", "t-1", "u-1", "Alice", null, null, 5, "Great!", null, null, "PUBLISHED", false, null);

        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(reviewRepository.findByTourIdAndVisibility("t-1", "PUBLISHED")).thenReturn(Collections.singletonList(r));
        when(reviewMapper.toDTO(r)).thenReturn(buildReviewDTO("Alice", 5));

        ReviewListResponseDTO result = tourService.getReviews("t-1", 1, 4, "OLDEST");

        assertEquals(1, result.getTotalItems());
    }

    @Test
    public void getReviews_reviewRepositoryThrowsException_propagatesException() {
        Tour tour = Tour.builder().id("t-1").build();

        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(reviewRepository.findByTourIdAndVisibility("t-1", "PUBLISHED"))
                .thenThrow(new RuntimeException("DB read failed"));

        try {
            tourService.getReviews("t-1", 1, 4, "RATING_DESC");
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
            assertEquals("DB read failed", ex.getMessage());
        }
    }

    // ΓöÇΓöÇ helpers ΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇΓöÇ

    private Review makeReview(String id, String tourId, String userId, String authorName, String authorImageUrl,
            String createdAt, int rate, String reviewContent) {
        return new Review(id, tourId, userId, authorName, authorImageUrl, createdAt, rate, reviewContent, null, null, "PUBLISHED", false, null);
    }

    private ReviewDTO buildReviewDTO(String author, int rate) {
        ReviewDTO dto = new ReviewDTO();
        dto.setAuthorName(author);
        dto.setRate(rate);
        return dto;
    }

    private ReviewDTO buildReviewDTOWithName(String author, int rate) {
        ReviewDTO dto = new ReviewDTO();
        dto.setAuthorName(author);
        dto.setRate(rate);
        return dto;
    }

    // ── getDestinations ──────────────────────────────────────────────────────

    @Test
    public void getDestinations_nullQuery_returnsEmpty() {
        DestinationListResponseDTO result = tourService.getDestinations(null);
        assertTrue(result.getDestinations().isEmpty());
    }

    @Test
    public void getDestinations_queryTooShort_returnsEmpty() {
        DestinationListResponseDTO result = tourService.getDestinations("ab");
        assertTrue(result.getDestinations().isEmpty());
    }

    @Test
    public void getDestinations_exactlyThreeChars_returnsMatchingDestination() {
        Tour tour = Tour.builder().id("t-1").destination(new Destination("Rome", "Italy")).build();
        when(tourRepository.searchDestinations("rom")).thenReturn(Collections.singletonList(tour));

        DestinationListResponseDTO result = tourService.getDestinations("rom");

        assertEquals(1, result.getDestinations().size());
        assertEquals("Rome, Italy", result.getDestinations().get(0));
    }

    @Test
    public void getDestinations_caseInsensitiveMatch_returnsResult() {
        Tour tour = Tour.builder().id("t-1").destination(new Destination("Paris", "France")).build();
        when(tourRepository.searchDestinations("PaRi")).thenReturn(Collections.singletonList(tour));

        DestinationListResponseDTO result = tourService.getDestinations("PaRi");

        assertEquals(1, result.getDestinations().size());
    }

    @Test
    public void getDestinations_noMatch_returnsEmpty() {
        when(tourRepository.searchDestinations("xyz123")).thenReturn(Collections.emptyList());

        DestinationListResponseDTO result = tourService.getDestinations("xyz123");

        assertTrue(result.getDestinations().isEmpty());
    }

    @Test
    public void getDestinations_nullDestinationOnTour_notIncluded() {
        Tour tour = Tour.builder().id("t-1").destination(null).build();
        when(tourRepository.searchDestinations("paris")).thenReturn(Collections.singletonList(tour));

        DestinationListResponseDTO result = tourService.getDestinations("paris");

        assertTrue(result.getDestinations().isEmpty());
    }

    @Test
    public void getDestinations_multipleToursWithSameDest_deduplicatesAndSorts() {
        Tour t1 = Tour.builder().id("t-1").destination(new Destination("Madrid", "Spain")).build();
        Tour t2 = Tour.builder().id("t-2").destination(new Destination("Madrid", "Spain")).build();
        Tour t3 = Tour.builder().id("t-3").destination(new Destination("Barcelona", "Spain")).build();
        when(tourRepository.searchDestinations("spain")).thenReturn(Arrays.asList(t1, t2, t3));

        DestinationListResponseDTO result = tourService.getDestinations("spain");

        assertEquals(2, result.getDestinations().size());
        assertEquals("Barcelona, Spain", result.getDestinations().get(0));
        assertEquals("Madrid, Spain", result.getDestinations().get(1));
    }

    // ── getAvailableTours ─────────────────────────────────────────────────────

    @Test
    public void getAvailableTours_noInstances_returnsEmptyPaginatedResult() {
        TourListResponseDTO expected = new TourListResponseDTO();
        when(tourInstanceRepository.findAll()).thenReturn(Collections.emptyList());
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, null, null, null, 1, 0, null, null, null);

        assertSame(expected, result);
    }

    @Test
    public void getAvailableTours_singleMatchingTour_returnsMappedResult() {
        TourInstance instance = TourInstance.builder().id("i-1").tourId("t-1")
                .startDate(LocalDate.now().plusDays(10)).availableSlots(10).build();
        Tour tour = Tour.builder().id("t-1").rating(4.5).build();
        TourSummaryDTO summaryDTO = new TourSummaryDTO();
        TourListResponseDTO expected = new TourListResponseDTO();

        when(tourInstanceRepository.findAll()).thenReturn(Collections.singletonList(instance));
        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(tourMapper.toTourSummaryDTO(tour, instance)).thenReturn(summaryDTO);
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, null, null, null, 1, 0, null, null, null);

        assertSame(expected, result);
    }

    @Test
    public void getAvailableTours_startDateFilter_excludesInstanceBeforeRequestedDate() {
        // Instance startDate is yesterday; filter requests today → excluded
        TourInstance instance = TourInstance.builder().id("i-1").tourId("t-1")
                .startDate(LocalDate.now().minusDays(1)).availableSlots(10).build();
        TourListResponseDTO expected = new TourListResponseDTO();

        when(tourInstanceRepository.findAll()).thenReturn(Collections.singletonList(instance));
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, null, LocalDate.now().toString(), null, 1, 0, null, null, null);

        assertSame(expected, result);
    }

    @Test
    public void getAvailableTours_startDateFilter_instanceWithNullDate_excluded() {
        TourInstance instance = TourInstance.builder().id("i-1").tourId("t-1")
                .startDate(null).availableSlots(10).build();
        TourListResponseDTO expected = new TourListResponseDTO();

        when(tourInstanceRepository.findAll()).thenReturn(Collections.singletonList(instance));
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, null, LocalDate.now().toString(), null, 1, 0, null, null, null);

        assertSame(expected, result);
    }

    @Test
    public void getAvailableTours_durationFilter_matchingDuration_includes() {
        TourInstance instance = TourInstance.builder().id("i-1").tourId("t-1")
                .startDate(LocalDate.now().plusDays(10)).availableSlots(10)
                .durations(Collections.singletonList(7)).build();
        Tour tour = Tour.builder().id("t-1").rating(4.0).build();
        TourSummaryDTO summaryDTO = new TourSummaryDTO();
        TourListResponseDTO expected = new TourListResponseDTO();

        when(tourInstanceRepository.findAll()).thenReturn(Collections.singletonList(instance));
        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(tourMapper.toTourSummaryDTO(tour, instance)).thenReturn(summaryDTO);
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, null, null, "7 days", 1, 0, null, null, null);

        assertSame(expected, result);
    }

    @Test
    public void getAvailableTours_durationFilter_nonMatchingDuration_excludes() {
        TourInstance instance = TourInstance.builder().id("i-1").tourId("t-1")
                .startDate(LocalDate.now().plusDays(10)).availableSlots(10)
                .durations(Collections.singletonList(14)).build();
        TourListResponseDTO expected = new TourListResponseDTO();

        when(tourInstanceRepository.findAll()).thenReturn(Collections.singletonList(instance));
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, null, null, "7 days", 1, 0, null, null, null);

        assertSame(expected, result);
    }

    @Test
    public void getAvailableTours_durationFilter_nullDurationList_excludes() {
        // Instance has null durations list; any duration filter should exclude it
        TourInstance instance = TourInstance.builder().id("i-1").tourId("t-1")
                .startDate(LocalDate.now().plusDays(10)).availableSlots(10)
                .durations(null).build();
        TourListResponseDTO expected = new TourListResponseDTO();

        when(tourInstanceRepository.findAll()).thenReturn(Collections.singletonList(instance));
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, null, null, "7 days", 1, 0, null, null, null);

        assertSame(expected, result);
    }

    @Test
    public void getAvailableTours_durationFilter_invalidDurationString_parsedAsZero_excludes() {
        // "abc" → parseDurationDays returns 0 → days > 0 is false → instance excluded
        TourInstance instance = TourInstance.builder().id("i-1").tourId("t-1")
                .startDate(LocalDate.now().plusDays(10)).availableSlots(10)
                .durations(Collections.singletonList(7)).build();
        TourListResponseDTO expected = new TourListResponseDTO();

        when(tourInstanceRepository.findAll()).thenReturn(Collections.singletonList(instance));
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, null, null, "abc", 1, 0, null, null, null);

        assertSame(expected, result);
    }

    @Test
    public void getAvailableTours_mealPlanFilter_matchingMealPlan_includes() {
        TourInstance instance = TourInstance.builder().id("i-1").tourId("t-1")
                .startDate(LocalDate.now().plusDays(10)).availableSlots(10)
                .mealPlans(Collections.singletonList(MealPlan.BB)).build();
        Tour tour = Tour.builder().id("t-1").rating(4.0).build();
        TourSummaryDTO summaryDTO = new TourSummaryDTO();
        TourListResponseDTO expected = new TourListResponseDTO();

        when(tourInstanceRepository.findAll()).thenReturn(Collections.singletonList(instance));
        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(tourMapper.toTourSummaryDTO(tour, instance)).thenReturn(summaryDTO);
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, null, null, null, 1, 0, "BB", null, null);

        assertSame(expected, result);
    }

    @Test
    public void getAvailableTours_mealPlanFilter_nonMatchingMealPlan_excludes() {
        TourInstance instance = TourInstance.builder().id("i-1").tourId("t-1")
                .startDate(LocalDate.now().plusDays(10)).availableSlots(10)
                .mealPlans(Collections.singletonList(MealPlan.BB)).build();
        TourListResponseDTO expected = new TourListResponseDTO();

        when(tourInstanceRepository.findAll()).thenReturn(Collections.singletonList(instance));
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, null, null, null, 1, 0, "AI", null, null);

        assertSame(expected, result);
    }

    @Test
    public void getAvailableTours_notEnoughSlots_excludes() {
        // Request adults=4, children=2 → total=6, but only 3 slots
        TourInstance instance = TourInstance.builder().id("i-1").tourId("t-1")
                .startDate(LocalDate.now().plusDays(10)).availableSlots(3).build();
        TourListResponseDTO expected = new TourListResponseDTO();

        when(tourInstanceRepository.findAll()).thenReturn(Collections.singletonList(instance));
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, null, null, null, 4, 2, null, null, null);

        assertSame(expected, result);
    }

    @Test
    public void getAvailableTours_anyDestinationParam_passesAllTours() {
        TourInstance instance = TourInstance.builder().id("i-1").tourId("t-1")
                .startDate(LocalDate.now().plusDays(10)).availableSlots(10).build();
        Tour tour = Tour.builder().id("t-1").rating(4.0).destination(new Destination("Rome", "Italy")).build();
        TourSummaryDTO summaryDTO = new TourSummaryDTO();
        TourListResponseDTO expected = new TourListResponseDTO();

        when(tourInstanceRepository.findAll()).thenReturn(Collections.singletonList(instance));
        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(tourMapper.toTourSummaryDTO(tour, instance)).thenReturn(summaryDTO);
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, "Any destination", null, null, 1, 0, null, null, null);

        assertSame(expected, result);
    }

    @Test
    public void getAvailableTours_destinationFilter_nullTourDestination_excludes() {
        TourInstance instance = TourInstance.builder().id("i-1").tourId("t-1")
                .startDate(LocalDate.now().plusDays(10)).availableSlots(10).build();
        Tour tour = Tour.builder().id("t-1").destination(null).build();
        TourListResponseDTO expected = new TourListResponseDTO();

        when(tourInstanceRepository.findAll()).thenReturn(Collections.singletonList(instance));
        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, "Paris, France", null, null, 1, 0, null, null, null);

        assertSame(expected, result);
    }

    @Test
    public void getAvailableTours_tourTypeFilter_matching_includes() {
        TourInstance instance = TourInstance.builder().id("i-1").tourId("t-1")
                .startDate(LocalDate.now().plusDays(10)).availableSlots(10).build();
        Tour tour = Tour.builder().id("t-1").rating(4.0).tourType(TourType.RESORT).build();
        TourSummaryDTO summaryDTO = new TourSummaryDTO();
        TourListResponseDTO expected = new TourListResponseDTO();

        when(tourInstanceRepository.findAll()).thenReturn(Collections.singletonList(instance));
        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(tourMapper.toTourSummaryDTO(tour, instance)).thenReturn(summaryDTO);
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, null, null, null, 1, 0, null, "RESORT", null);

        assertSame(expected, result);
    }

    @Test
    public void getAvailableTours_tourTypeFilter_invalidType_excludes() {
        TourInstance instance = TourInstance.builder().id("i-1").tourId("t-1")
                .startDate(LocalDate.now().plusDays(10)).availableSlots(10).build();
        Tour tour = Tour.builder().id("t-1").tourType(TourType.RESORT).build();
        TourListResponseDTO expected = new TourListResponseDTO();

        when(tourInstanceRepository.findAll()).thenReturn(Collections.singletonList(instance));
        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, null, null, null, 1, 0, null, "INVALID_TYPE", null);

        assertSame(expected, result);
    }

    @Test
    public void getAvailableTours_tourTypeFilter_nullTourType_excludes() {
        TourInstance instance = TourInstance.builder().id("i-1").tourId("t-1")
                .startDate(LocalDate.now().plusDays(10)).availableSlots(10).build();
        Tour tour = Tour.builder().id("t-1").tourType(null).build();
        TourListResponseDTO expected = new TourListResponseDTO();

        when(tourInstanceRepository.findAll()).thenReturn(Collections.singletonList(instance));
        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, null, null, null, 1, 0, null, "RESORT", null);

        assertSame(expected, result);
    }

    @Test
    public void getAvailableTours_guestQuantity_notEnoughAdults_excludes() {
        TourInstance instance = TourInstance.builder().id("i-1").tourId("t-1")
                .startDate(LocalDate.now().plusDays(10)).availableSlots(10).build();
        Tour tour = Tour.builder().id("t-1")
                .guestQuantity(new GuestQuantityInfo(2, 0, 2)).build();
        TourListResponseDTO expected = new TourListResponseDTO();

        when(tourInstanceRepository.findAll()).thenReturn(Collections.singletonList(instance));
        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        // 5 adults requested but max is 2 → excluded
        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, null, null, null, 5, 0, null, null, null);

        assertSame(expected, result);
    }

    @Test
    public void getAvailableTours_tourNotFoundInCache_filteredOut() {
        TourInstance instance = TourInstance.builder().id("i-1").tourId("t-missing")
                .startDate(LocalDate.now().plusDays(10)).availableSlots(10).build();
        TourListResponseDTO expected = new TourListResponseDTO();

        when(tourInstanceRepository.findAll()).thenReturn(Collections.singletonList(instance));
        when(tourRepository.findById("t-missing")).thenReturn(Optional.empty());
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, null, null, null, 1, 0, null, null, null);

        assertSame(expected, result);
    }

    @Test
    public void getAvailableTours_deduplication_twoInstancesSameTour_picksEarlier() {
        LocalDate earlyDate = LocalDate.now().plusDays(5);
        LocalDate laterDate = LocalDate.now().plusDays(15);
        TourInstance laterInstance = TourInstance.builder().id("i-later").tourId("t-1")
                .startDate(laterDate).availableSlots(10).build();
        TourInstance earlyInstance = TourInstance.builder().id("i-early").tourId("t-1")
                .startDate(earlyDate).availableSlots(10).build();
        Tour tour = Tour.builder().id("t-1").rating(4.0).build();
        TourSummaryDTO summaryDTO = new TourSummaryDTO();
        TourListResponseDTO expected = new TourListResponseDTO();

        // Return later first so deduplication must replace it with the earlier one
        when(tourInstanceRepository.findAll()).thenReturn(Arrays.asList(laterInstance, earlyInstance));
        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(tourMapper.toTourSummaryDTO(tour, earlyInstance)).thenReturn(summaryDTO);
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, null, null, null, 1, 0, null, null, null);

        assertSame(expected, result);
        verify(tourMapper).toTourSummaryDTO(tour, earlyInstance);
    }

    @Test
    public void getAvailableTours_chooseBestInstance_existingDateNull_picksChallenger() {
        TourInstance nullDateInstance = TourInstance.builder().id("i-null").tourId("t-1")
                .startDate(null).availableSlots(10).build();
        TourInstance realDateInstance = TourInstance.builder().id("i-real").tourId("t-1")
                .startDate(LocalDate.now().plusDays(5)).availableSlots(10).build();
        Tour tour = Tour.builder().id("t-1").rating(4.0).build();
        TourSummaryDTO summaryDTO = new TourSummaryDTO();
        TourListResponseDTO expected = new TourListResponseDTO();

        when(tourInstanceRepository.findAll()).thenReturn(Arrays.asList(nullDateInstance, realDateInstance));
        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(tourMapper.toTourSummaryDTO(tour, realDateInstance)).thenReturn(summaryDTO);
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, null, null, null, 1, 0, null, null, null);

        assertSame(expected, result);
        verify(tourMapper).toTourSummaryDTO(tour, realDateInstance);
    }

    @Test
    public void getAvailableTours_sortRatingAsc_sortsBranch() {
        TourInstance i1 = TourInstance.builder().id("i-1").tourId("t-1")
                .startDate(LocalDate.now().plusDays(10)).availableSlots(10).build();
        TourInstance i2 = TourInstance.builder().id("i-2").tourId("t-2")
                .startDate(LocalDate.now().plusDays(10)).availableSlots(10).build();
        Tour tour1 = Tour.builder().id("t-1").rating(3.0).build();
        Tour tour2 = Tour.builder().id("t-2").rating(5.0).build();
        TourListResponseDTO expected = new TourListResponseDTO();

        when(tourInstanceRepository.findAll()).thenReturn(Arrays.asList(i1, i2));
        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour1));
        when(tourRepository.findById("t-2")).thenReturn(Optional.of(tour2));
        when(tourMapper.toTourSummaryDTO(any(), any())).thenReturn(new TourSummaryDTO());
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, null, null, null, 1, 0, null, null, "RATING_ASC");

        assertSame(expected, result);
    }

    @Test
    public void getAvailableTours_sortPriceAsc_sortsBranch() {
        PricingOption p1 = PricingOption.builder().pricePerPerson(new BigDecimal("500")).build();
        PricingOption p2 = PricingOption.builder().pricePerPerson(new BigDecimal("300")).build();
        TourInstance i1 = TourInstance.builder().id("i-1").tourId("t-1")
                .startDate(LocalDate.now().plusDays(10)).availableSlots(10)
                .pricing(Collections.singletonList(p1)).build();
        TourInstance i2 = TourInstance.builder().id("i-2").tourId("t-2")
                .startDate(LocalDate.now().plusDays(10)).availableSlots(10)
                .pricing(Collections.singletonList(p2)).build();
        Tour tour1 = Tour.builder().id("t-1").rating(4.0).build();
        Tour tour2 = Tour.builder().id("t-2").rating(3.0).build();
        TourListResponseDTO expected = new TourListResponseDTO();

        when(tourInstanceRepository.findAll()).thenReturn(Arrays.asList(i1, i2));
        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour1));
        when(tourRepository.findById("t-2")).thenReturn(Optional.of(tour2));
        when(tourMapper.toTourSummaryDTO(any(), any())).thenReturn(new TourSummaryDTO());
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, null, null, null, 1, 0, null, null, "PRICE_ASC");

        assertSame(expected, result);
    }

    @Test
    public void getAvailableTours_sortPriceDesc_sortsBranch() {
        PricingOption p1 = PricingOption.builder().pricePerPerson(new BigDecimal("500")).build();
        PricingOption p2 = PricingOption.builder().pricePerPerson(new BigDecimal("300")).build();
        TourInstance i1 = TourInstance.builder().id("i-1").tourId("t-1")
                .startDate(LocalDate.now().plusDays(10)).availableSlots(10)
                .pricing(Collections.singletonList(p1)).build();
        TourInstance i2 = TourInstance.builder().id("i-2").tourId("t-2")
                .startDate(LocalDate.now().plusDays(10)).availableSlots(10)
                .pricing(Collections.singletonList(p2)).build();
        Tour tour1 = Tour.builder().id("t-1").rating(4.0).build();
        Tour tour2 = Tour.builder().id("t-2").rating(3.0).build();
        TourListResponseDTO expected = new TourListResponseDTO();

        when(tourInstanceRepository.findAll()).thenReturn(Arrays.asList(i1, i2));
        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour1));
        when(tourRepository.findById("t-2")).thenReturn(Optional.of(tour2));
        when(tourMapper.toTourSummaryDTO(any(), any())).thenReturn(new TourSummaryDTO());
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        TourListResponseDTO result = tourService.getAvailableTours(
                1, 6, null, null, null, 1, 0, null, null, "PRICE_DESC");

        assertSame(expected, result);
    }

    @Test
    public void getAvailableTours_pagination_pageExceedsTotalPages_clampsToLastPage() {
        TourInstance instance = TourInstance.builder().id("i-1").tourId("t-1")
                .startDate(LocalDate.now().plusDays(10)).availableSlots(10).build();
        Tour tour = Tour.builder().id("t-1").rating(4.0).build();
        TourSummaryDTO summaryDTO = new TourSummaryDTO();
        TourListResponseDTO expected = new TourListResponseDTO();

        when(tourInstanceRepository.findAll()).thenReturn(Collections.singletonList(instance));
        when(tourRepository.findById("t-1")).thenReturn(Optional.of(tour));
        when(tourMapper.toTourSummaryDTO(tour, instance)).thenReturn(summaryDTO);
        when(tourMapper.toTourListResponseDTO(any(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(expected);

        // 1 item, pageSize=6 → totalPages=1; requesting page=5 → clamped to 1
        TourListResponseDTO result = tourService.getAvailableTours(
                5, 6, null, null, null, 1, 0, null, null, null);

        assertSame(expected, result);
    }
}
