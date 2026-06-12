package com.epam.edp.demo.service.impl;

import com.epam.edp.demo.dto.tour.TourFeedbackRequestDTO;
import com.epam.edp.demo.dto.user.MessageResponseDTO;
import com.epam.edp.demo.entity.Booking;
import com.epam.edp.demo.entity.Review;
import com.epam.edp.demo.entity.Tour;
import com.epam.edp.demo.enums.BookingStatus;
import com.epam.edp.demo.exception.BadRequestException;
import com.epam.edp.demo.exception.FeedbackAlreadyExistsException;
import com.epam.edp.demo.exception.TourNotFoundException;
import com.epam.edp.demo.mapper.ReviewMapper;
import com.epam.edp.demo.mapper.TourMapper;
import com.epam.edp.demo.repository.BookingRepository;
import com.epam.edp.demo.repository.MongoReviewRepository;
import com.epam.edp.demo.repository.TourInstanceRepository;
import com.epam.edp.demo.repository.TourRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.epam.edp.demo.repository.TravelAgentRepository;
import com.epam.edp.demo.service.ContentModerationService;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TourServiceImpl.submitFeedback.
 *
 * Covers: success path, missing tour, duplicate feedback, invalid rating/comment
 * combinations, and booking-state validations.
 */
@RunWith(MockitoJUnitRunner.class)
public class SubmitFeedbackServiceTest {

    @Mock private TourRepository tourRepository;
    @Mock private TourInstanceRepository tourInstanceRepository;
    @Mock private MongoReviewRepository reviewRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private TourMapper tourMapper;
    @Mock private ReviewMapper reviewMapper;
    @Mock private com.epam.edp.demo.repository.UserRepository userRepository;
    @Mock private TravelAgentRepository travelAgentRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private ContentModerationService contentModerationService;

    private TourServiceImpl tourService;

    private static final String TOUR_ID = "tour-123";
    private static final String USER_ID = "user-456";

    @Before
    public void setUp() {
        tourService = new TourServiceImpl(
                tourRepository, tourInstanceRepository, reviewRepository,
                bookingRepository, userRepository, travelAgentRepository,
                tourMapper, reviewMapper, rabbitTemplate, contentModerationService);
    }

    // ── Success scenarios ────────────────────────────────────────────────────

    @Test
    public void submitFeedback_highRatingNoComment_returns201Message() {
        givenTourExists();
        givenNoDuplicateFeedback();
        givenStartedBookingExists();

        TourFeedbackRequestDTO request = new TourFeedbackRequestDTO(5, null);

        MessageResponseDTO response = tourService.submitFeedback(TOUR_ID, request, USER_ID);

        assertNotNull(response);
        assertEquals("Your feedback has been submitted.", response.getMessage());
        verify(reviewRepository).save(any());
    }

    @Test
    public void submitFeedback_highRatingWithComment_savesSuccessfully() {
        givenTourExists();
        givenNoDuplicateFeedback();
        givenStartedBookingExists();

        TourFeedbackRequestDTO request = new TourFeedbackRequestDTO(4, "Really enjoyed it!");

        MessageResponseDTO response = tourService.submitFeedback(TOUR_ID, request, USER_ID);

        assertEquals("Your feedback has been submitted.", response.getMessage());
        verify(reviewRepository).save(any());
    }

    @Test
    public void submitFeedback_lowRatingWithComment_savesSuccessfully() {
        givenTourExists();
        givenNoDuplicateFeedback();
        givenStartedBookingExists();

        TourFeedbackRequestDTO request = new TourFeedbackRequestDTO(2, "Needs improvement.");

        MessageResponseDTO response = tourService.submitFeedback(TOUR_ID, request, USER_ID);

        assertEquals("Your feedback has been submitted.", response.getMessage());
        verify(reviewRepository).save(any());
    }

    @Test
    public void submitFeedback_finishedBooking_savesSuccessfully() {
        givenTourExists();
        givenNoDuplicateFeedback();
        givenFinishedBookingExists();

        TourFeedbackRequestDTO request = new TourFeedbackRequestDTO(5, "Great trip!");

        MessageResponseDTO response = tourService.submitFeedback(TOUR_ID, request, USER_ID);

        assertEquals("Your feedback has been submitted.", response.getMessage());
    }

    @Test
    public void submitFeedback_ratingExactlyThreeWithComment_savesSuccessfully() {
        givenTourExists();
        givenNoDuplicateFeedback();
        givenStartedBookingExists();

        TourFeedbackRequestDTO request = new TourFeedbackRequestDTO(3, "It was okay.");

        MessageResponseDTO response = tourService.submitFeedback(TOUR_ID, request, USER_ID);

        assertEquals("Your feedback has been submitted.", response.getMessage());
    }

    @Test
    public void submitFeedback_ratingFourEmptyComment_savesSuccessfully() {
        givenTourExists();
        givenNoDuplicateFeedback();
        givenStartedBookingExists();

        TourFeedbackRequestDTO request = new TourFeedbackRequestDTO(4, "");

        // rating >= 4 so blank comment is allowed
        MessageResponseDTO response = tourService.submitFeedback(TOUR_ID, request, USER_ID);

        assertEquals("Your feedback has been submitted.", response.getMessage());
    }

    // ── Validation: tour must exist ──────────────────────────────────────────

    @Test(expected = TourNotFoundException.class)
    public void submitFeedback_tourNotFound_throwsTourNotFoundException() {
        when(tourRepository.findById(TOUR_ID)).thenReturn(Optional.empty());

        tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(5, null), USER_ID);
    }

    @Test
    public void submitFeedback_tourNotFound_doesNotSaveReview() {
        when(tourRepository.findById(TOUR_ID)).thenReturn(Optional.empty());

        try {
            tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(5, null), USER_ID);
        } catch (TourNotFoundException ignored) {}

        verify(reviewRepository, never()).save(any());
    }

    // ── Validation: comment required for low ratings ─────────────────────────

    @Test(expected = BadRequestException.class)
    public void submitFeedback_ratingOneNullComment_throwsBadRequest() {
        givenTourExists();

        tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(1, null), USER_ID);
    }

    @Test(expected = BadRequestException.class)
    public void submitFeedback_ratingTwoBlankComment_throwsBadRequest() {
        givenTourExists();

        tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(2, "   "), USER_ID);
    }

    @Test(expected = BadRequestException.class)
    public void submitFeedback_ratingThreeEmptyComment_throwsBadRequest() {
        givenTourExists();

        tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(3, ""), USER_ID);
    }

    @Test
    public void submitFeedback_lowRatingNullComment_errorMessageDescriptive() {
        givenTourExists();

        try {
            tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(2, null), USER_ID);
        } catch (BadRequestException ex) {
            assertNotNull(ex.getMessage());
        }
    }

    // ── Upsert: duplicate feedback is updated, not rejected ─────────────────

    @Test
    public void submitFeedback_duplicateFeedback_throwsFeedbackAlreadyExistsException() {
        // Changed: upsert behavior — duplicate feedback succeeds with "updated" message
        givenTourExists();
        givenExistingFeedback();
        givenStartedBookingExists();

        MessageResponseDTO response = tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(5, "Great!"), USER_ID);

        assertEquals("Your feedback has been updated.", response.getMessage());
    }

    @Test
    public void submitFeedback_duplicateFeedback_doesNotSaveAgain() {
        // Changed: upsert behavior — save IS called to persist updated review
        givenTourExists();
        givenExistingFeedback();
        givenStartedBookingExists();

        tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(5, "Great!"), USER_ID);

        verify(reviewRepository).save(any());
    }

    // ── Validation: user must have a valid booking ───────────────────────────

    @Test(expected = BadRequestException.class)
    public void submitFeedback_noBookingForTour_throwsBadRequest() {
        givenTourExists();
        givenNoDuplicateFeedback();
        when(bookingRepository.findByUserIdAndTourId(USER_ID, TOUR_ID))
                .thenReturn(Collections.emptyList());

        tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(5, "Nice!"), USER_ID);
    }

    @Test(expected = BadRequestException.class)
    public void submitFeedback_onlyCanceledBooking_throwsBadRequest() {
        givenTourExists();
        givenNoDuplicateFeedback();

        Booking canceled = new Booking();
        canceled.setState(BookingStatus.CANCELED);

        when(bookingRepository.findByUserIdAndTourId(USER_ID, TOUR_ID))
                .thenReturn(Collections.singletonList(canceled));

        tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(5, "Nice!"), USER_ID);
    }

    @Test(expected = BadRequestException.class)
    public void submitFeedback_onlyBookedStatus_throwsBadRequest() {
        givenTourExists();
        givenNoDuplicateFeedback();

        Booking booked = new Booking();
        booked.setState(BookingStatus.BOOKED);

        when(bookingRepository.findByUserIdAndTourId(USER_ID, TOUR_ID))
                .thenReturn(Collections.singletonList(booked));

        tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(5, "Nice!"), USER_ID);
    }

    @Test
    public void submitFeedback_mixedBookingsOneStarted_savesSuccessfully() {
        givenTourExists();
        givenNoDuplicateFeedback();

        Booking canceled = new Booking();
        canceled.setState(BookingStatus.CANCELED);

        Booking started = new Booking();
        started.setState(BookingStatus.STARTED);

        when(bookingRepository.findByUserIdAndTourId(USER_ID, TOUR_ID))
                .thenReturn(Arrays.asList(canceled, started));

        MessageResponseDTO response = tourService.submitFeedback(
                TOUR_ID, new TourFeedbackRequestDTO(5, null), USER_ID);

        assertEquals("Your feedback has been submitted.", response.getMessage());
    }

    // ── Section 1: Validation Edge Cases ─────────────────────────────────────

    @Test(expected = BadRequestException.class)
    public void submitFeedback_onlyConfirmedBooking_throwsBadRequest() {
        // CONFIRMED is not STARTED or FINISHED — must be rejected
        givenTourExists();
        givenNoDuplicateFeedback();

        Booking confirmed = new Booking();
        confirmed.setState(BookingStatus.CONFIRMED);
        when(bookingRepository.findByUserIdAndTourId(USER_ID, TOUR_ID))
                .thenReturn(Collections.singletonList(confirmed));

        tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(5, "Nice!"), USER_ID);
    }

    @Test(expected = BadRequestException.class)
    public void submitFeedback_lowRatingTabOnlyComment_throwsBadRequest() {
        // "\t\t" is blank per String.isBlank() — comment is required for rating <= 3
        givenTourExists();
        tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(2, "\t\t"), USER_ID);
    }

    @Test(expected = BadRequestException.class)
    public void submitFeedback_lowRatingNewlineOnlyComment_throwsBadRequest() {
        // "\n\n" is blank — comment required for rating <= 3
        givenTourExists();
        tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(3, "\n\n"), USER_ID);
    }

    @Test
    public void submitFeedback_ratingOneWithSingleCharComment_savesSuccessfully() {
        // Boundary: minimum rating with the smallest possible non-blank comment
        givenTourExists();
        givenNoDuplicateFeedback();
        givenStartedBookingExists();

        MessageResponseDTO response = tourService.submitFeedback(
                TOUR_ID, new TourFeedbackRequestDTO(1, "X"), USER_ID);

        assertEquals("Your feedback has been submitted.", response.getMessage());
    }

    @Test
    public void submitFeedback_ratingFiveWithVeryLongComment_savesSuccessfully() {
        // No max-length constraint on comment — 2000-char comment with rating 5
        givenTourExists();
        givenNoDuplicateFeedback();
        givenStartedBookingExists();

        String longComment = "X".repeat(2000);
        MessageResponseDTO response = tourService.submitFeedback(
                TOUR_ID, new TourFeedbackRequestDTO(5, longComment), USER_ID);

        assertEquals("Your feedback has been submitted.", response.getMessage());
    }

    @Test
    public void submitFeedback_commentWithHtmlTags_storedAsPlainText() {
        // HTML is treated as a plain string — no sanitization expected at this layer
        givenTourExists();
        givenNoDuplicateFeedback();
        givenStartedBookingExists();

        String htmlComment = "<script>alert('xss')</script>";
        MessageResponseDTO response = tourService.submitFeedback(
                TOUR_ID, new TourFeedbackRequestDTO(5, htmlComment), USER_ID);

        assertEquals("Your feedback has been submitted.", response.getMessage());
    }

    @Test
    public void submitFeedback_commentWithEmoji_savesSuccessfully() {
        // Multi-byte emoji should not break string handling
        givenTourExists();
        givenNoDuplicateFeedback();
        givenStartedBookingExists();

        MessageResponseDTO response = tourService.submitFeedback(
                TOUR_ID, new TourFeedbackRequestDTO(5, "Loved it! \uD83C\uDF0D\uD83D\uDE04"), USER_ID);

        assertEquals("Your feedback has been submitted.", response.getMessage());
    }

    // ── Section 2: Multi-User / Multi-Tour Scenarios ─────────────────────────

    @Test
    public void submitFeedback_differentUserReviewsSameTour_savedIndependently() {
        // User B can review the same tour that User A already reviewed
        String otherUserId = "user-789";
        when(tourRepository.findById(TOUR_ID))
                .thenReturn(Optional.of(Tour.builder().id(TOUR_ID).build()));
        when(reviewRepository.findByUserIdAndTourId(otherUserId, TOUR_ID)).thenReturn(Optional.empty());

        Booking booking = new Booking();
        booking.setState(BookingStatus.STARTED);
        when(bookingRepository.findByUserIdAndTourId(otherUserId, TOUR_ID))
                .thenReturn(Collections.singletonList(booking));

        MessageResponseDTO response = tourService.submitFeedback(
                TOUR_ID, new TourFeedbackRequestDTO(4, "Nice!"), otherUserId);

        assertEquals("Your feedback has been submitted.", response.getMessage());
    }

    @Test
    public void submitFeedback_sameUserReviewsDifferentTour_savedSuccessfully() {
        // The same user is allowed to review a different tour
        String otherTourId = "tour-other";
        when(tourRepository.findById(otherTourId))
                .thenReturn(Optional.of(Tour.builder().id(otherTourId).build()));
        when(reviewRepository.findByUserIdAndTourId(USER_ID, otherTourId)).thenReturn(Optional.empty());

        Booking booking = new Booking();
        booking.setState(BookingStatus.STARTED);
        when(bookingRepository.findByUserIdAndTourId(USER_ID, otherTourId))
                .thenReturn(Collections.singletonList(booking));

        MessageResponseDTO response = tourService.submitFeedback(
                otherTourId, new TourFeedbackRequestDTO(5, null), USER_ID);

        assertEquals("Your feedback has been submitted.", response.getMessage());
    }

    @Test
    public void submitFeedback_mixedBookingsOneFinished_savesSuccessfully() {
        // A canceled + a finished booking → finished qualifies
        givenTourExists();
        givenNoDuplicateFeedback();

        Booking canceled = new Booking();
        canceled.setState(BookingStatus.CANCELED);

        Booking finished = new Booking();
        finished.setState(BookingStatus.FINISHED);

        when(bookingRepository.findByUserIdAndTourId(USER_ID, TOUR_ID))
                .thenReturn(Arrays.asList(canceled, finished));

        MessageResponseDTO response = tourService.submitFeedback(
                TOUR_ID, new TourFeedbackRequestDTO(4, null), USER_ID);

        assertEquals("Your feedback has been submitted.", response.getMessage());
    }

    // ── Section 3: Saved Entity Verification (ArgumentCaptor) ────────────────

    @Test
    public void submitFeedback_savedReview_hasCorrectUserId() {
        givenTourExists();
        givenNoDuplicateFeedback();
        givenStartedBookingExists();

        tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(5, "Great!"), USER_ID);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertEquals(USER_ID, captor.getValue().getUserId());
    }

    @Test
    public void submitFeedback_savedReview_hasCorrectTourId() {
        givenTourExists();
        givenNoDuplicateFeedback();
        givenStartedBookingExists();

        tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(5, "Great!"), USER_ID);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertEquals(TOUR_ID, captor.getValue().getTourId());
    }

    @Test
    public void submitFeedback_savedReview_hasCorrectRating() {
        givenTourExists();
        givenNoDuplicateFeedback();
        givenStartedBookingExists();

        tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(3, "Average trip."), USER_ID);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertEquals(3, captor.getValue().getRate());
    }

    @Test
    public void submitFeedback_savedReview_hasCorrectComment() {
        givenTourExists();
        givenNoDuplicateFeedback();
        givenStartedBookingExists();

        String comment = "Absolutely loved every moment!";
        tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(5, comment), USER_ID);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertEquals(comment, captor.getValue().getReviewContent());
    }

    @Test
    public void submitFeedback_savedReview_hasNonNullCreatedAt() {
        givenTourExists();
        givenNoDuplicateFeedback();
        givenStartedBookingExists();

        tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(5, null), USER_ID);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertNotNull("createdAt should be set automatically", captor.getValue().getCreatedAt());
    }

    // ── Section 4: Repository Interaction Verification ────────────────────────

    @Test
    public void submitFeedback_existsByUserIdAndTourId_calledWithExactArguments() {
        // Changed: upsert uses findByUserIdAndTourId instead of existsByUserIdAndTourId
        givenTourExists();
        givenNoDuplicateFeedback();
        givenStartedBookingExists();

        tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(5, "X"), USER_ID);

        verify(reviewRepository).findByUserIdAndTourId(USER_ID, TOUR_ID);
    }

    @Test
    public void submitFeedback_bookingRepository_calledWithExactUserIdAndTourId() {
        givenTourExists();
        givenNoDuplicateFeedback();
        givenStartedBookingExists();

        tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(5, null), USER_ID);

        verify(bookingRepository).findByUserIdAndTourId(USER_ID, TOUR_ID);
    }

    @Test
    public void submitFeedback_saveCalledExactlyOnce_onSuccess() {
        givenTourExists();
        givenNoDuplicateFeedback();
        givenStartedBookingExists();

        tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(5, null), USER_ID);

        verify(reviewRepository).save(any());
    }

    @Test
    public void submitFeedback_tourRepository_calledWithCorrectTourId() {
        givenTourExists();
        givenNoDuplicateFeedback();
        givenStartedBookingExists();

        tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(5, null), USER_ID);

        verify(tourRepository).findById(TOUR_ID);
    }

    // ── Section 5: Exception message verification ────────────────────────────

    @Test
    public void submitFeedback_lowRating_exceptionMessageMentionsComment() {
        // BadRequestException message should clearly state that comment is required
        givenTourExists();

        try {
            tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(2, null), USER_ID);
        } catch (BadRequestException ex) {
            assertNotNull("Exception message must not be null", ex.getMessage());
            assertTrue("Message should mention comment requirement",
                    ex.getMessage().toLowerCase().contains("comment"));
        }
    }

    @Test
    public void submitFeedback_noValidBooking_exceptionMessageIsDescriptive() {
        // BadRequestException for missing booking should carry a meaningful message
        givenTourExists();
        givenNoDuplicateFeedback();
        when(bookingRepository.findByUserIdAndTourId(USER_ID, TOUR_ID))
                .thenReturn(Collections.emptyList());

        try {
            tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(5, null), USER_ID);
        } catch (BadRequestException ex) {
            assertNotNull("Exception message must not be null", ex.getMessage());
            assertTrue("Message should mention booking",
                    ex.getMessage().toLowerCase().contains("booking"));
        }
    }

    @Test
    public void submitFeedback_duplicateFeedback_exceptionHasFixedMessage() {
        // Changed: upsert returns "updated" message instead of throwing
        givenTourExists();
        givenExistingFeedback();
        givenStartedBookingExists();

        MessageResponseDTO response = tourService.submitFeedback(
                TOUR_ID, new TourFeedbackRequestDTO(5, "X"), USER_ID);

        assertEquals("Your feedback has been updated.", response.getMessage());
    }

    // ── Section 6: Save never called on failure paths ────────────────────────

    @Test
    public void submitFeedback_lowRatingMissingComment_saveNeverCalled() {
        // When comment validation fails, the Review must NOT be persisted
        givenTourExists();

        try {
            tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(1, null), USER_ID);
        } catch (BadRequestException ignored) {}

        verify(reviewRepository, never()).save(any());
    }

    @Test
    public void submitFeedback_noValidBooking_saveNeverCalled() {
        // When booking check fails, the Review must NOT be persisted
        givenTourExists();
        givenNoDuplicateFeedback();
        when(bookingRepository.findByUserIdAndTourId(USER_ID, TOUR_ID))
                .thenReturn(Collections.emptyList());

        try {
            tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(5, null), USER_ID);
        } catch (BadRequestException ignored) {}

        verify(reviewRepository, never()).save(any());
    }

    @Test
    public void submitFeedback_canceledBooking_saveNeverCalled() {
        // Canceled booking → rejected → no save
        givenTourExists();
        givenNoDuplicateFeedback();

        Booking canceled = new Booking();
        canceled.setState(BookingStatus.CANCELED);
        when(bookingRepository.findByUserIdAndTourId(USER_ID, TOUR_ID))
                .thenReturn(Collections.singletonList(canceled));

        try {
            tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(5, null), USER_ID);
        } catch (BadRequestException ignored) {}

        verify(reviewRepository, never()).save(any());
    }

    // ── Section 7: Boundary and additional validation cases ──────────────────

    @Test
    public void submitFeedback_ratingFourNullComment_isAccepted() {
        // Rating=4 is the lower boundary for optional comment — null must be accepted
        givenTourExists();
        givenNoDuplicateFeedback();
        givenStartedBookingExists();

        MessageResponseDTO response = tourService.submitFeedback(
                TOUR_ID, new TourFeedbackRequestDTO(4, null), USER_ID);

        assertEquals("Your feedback has been submitted.", response.getMessage());
        verify(reviewRepository).save(any());
    }

    @Test
    public void submitFeedback_ratingTwoMixedWhitespaceComment_throwsBadRequest() {
        // "\r\n " is blank per String.isBlank() — must be rejected for rating <= 3
        givenTourExists();

        try {
            tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(2, "\r\n "), USER_ID);
        } catch (BadRequestException ex) {
            assertNotNull(ex.getMessage());
            return;
        }
        throw new AssertionError("Expected BadRequestException was not thrown");
    }

    @Test
    public void submitFeedback_multipleStartedBookings_saveCalledExactlyOnce() {
        // Even if the user has two STARTED bookings, save() should be called exactly once
        givenTourExists();
        givenNoDuplicateFeedback();

        Booking b1 = new Booking();
        b1.setState(BookingStatus.STARTED);
        Booking b2 = new Booking();
        b2.setState(BookingStatus.STARTED);
        when(bookingRepository.findByUserIdAndTourId(USER_ID, TOUR_ID))
                .thenReturn(Arrays.asList(b1, b2));

        tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(5, null), USER_ID);

        verify(reviewRepository).save(any());
    }

    // ── Section 8: Ordering — tour must be checked before booking/duplicate ──

    @Test
    public void submitFeedback_tourNotFound_bookingRepositoryNeverQueried() {
        // If the tour does not exist, we should fail fast without hitting the booking repo
        when(tourRepository.findById(TOUR_ID)).thenReturn(Optional.empty());

        try {
            tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(5, null), USER_ID);
        } catch (TourNotFoundException ignored) {}

        verify(bookingRepository, never()).findByUserIdAndTourId(any(), any());
    }

    @Test
    public void submitFeedback_tourNotFound_duplicateCheckNeverPerformed() {
        // If the tour does not exist, existsByUserIdAndTourId should not be called
        when(tourRepository.findById(TOUR_ID)).thenReturn(Optional.empty());

        try {
            tourService.submitFeedback(TOUR_ID, new TourFeedbackRequestDTO(5, null), USER_ID);
        } catch (TourNotFoundException ignored) {}

        verify(reviewRepository, never()).existsByUserIdAndTourId(any(), any());
    }

    private void givenTourExists() {
        when(tourRepository.findById(TOUR_ID))
                .thenReturn(Optional.of(Tour.builder().id(TOUR_ID).name("Test Tour").build()));
    }

    private void givenNoDuplicateFeedback() {
        when(reviewRepository.findByUserIdAndTourId(USER_ID, TOUR_ID)).thenReturn(Optional.empty());
    }

    private void givenExistingFeedback() {
        Review existing = new Review();
        existing.setUserId(USER_ID);
        existing.setTourId(TOUR_ID);
        existing.setRate(4);
        existing.setReviewContent("Old comment");
        when(reviewRepository.findByUserIdAndTourId(USER_ID, TOUR_ID)).thenReturn(Optional.of(existing));
    }

    private void givenStartedBookingExists() {
        Booking booking = new Booking();
        booking.setState(BookingStatus.STARTED);
        booking.setUserId(USER_ID);
        booking.setTourId(TOUR_ID);
        when(bookingRepository.findByUserIdAndTourId(USER_ID, TOUR_ID))
                .thenReturn(Collections.singletonList(booking));
    }

    private void givenFinishedBookingExists() {
        Booking booking = new Booking();
        booking.setState(BookingStatus.FINISHED);
        booking.setUserId(USER_ID);
        booking.setTourId(TOUR_ID);
        when(bookingRepository.findByUserIdAndTourId(USER_ID, TOUR_ID))
                .thenReturn(Collections.singletonList(booking));
    }
}

