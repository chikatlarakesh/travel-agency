package com.epam.edp.demo.service.impl;

import com.epam.edp.demo.dto.booking.BookedTourListResponseDTO;
import com.epam.edp.demo.dto.booking.CreateBookingRequestDTO;
import com.epam.edp.demo.dto.booking.CreateBookingResponseDTO;
import com.epam.edp.demo.dto.booking.GuestsDTO;
import com.epam.edp.demo.dto.booking.PersonDetailDTO;
import com.epam.edp.demo.dto.booking.UpdateBookingRequestDTO;
import com.epam.edp.demo.entity.Booking;
import com.epam.edp.demo.entity.Destination;
import com.epam.edp.demo.entity.GuestCount;
import com.epam.edp.demo.entity.Tour;
import com.epam.edp.demo.entity.TourInstance;
import com.epam.edp.demo.entity.TravelAgent;
import com.epam.edp.demo.enums.BookingStatus;
import com.epam.edp.demo.exception.BadRequestException;
import com.epam.edp.demo.exception.BookingNotFoundException;
import com.epam.edp.demo.exception.CancellationNotAllowedException;
import com.epam.edp.demo.exception.OverbookingException;
import com.epam.edp.demo.exception.TourNotFoundException;
import com.epam.edp.demo.exception.UnauthorizedException;
import com.epam.edp.demo.mapper.BookingMapper;
import com.epam.edp.demo.repository.BookingRepository;
import com.epam.edp.demo.repository.MongoReviewRepository;
import com.epam.edp.demo.repository.TourInstanceRepository;
import com.epam.edp.demo.repository.TourRepository;
import com.epam.edp.demo.repository.TravelAgentRepository;
import com.epam.edp.demo.repository.UserRepository;
import com.epam.edp.demo.security.JwtTokenProvider;
import com.epam.edp.demo.util.DateUtil;
import com.epam.edp.demo.util.MealPlanFormatter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BookingServiceImplTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private TourRepository tourRepository;
    @Mock private TourInstanceRepository tourInstanceRepository;
    @Mock private TravelAgentRepository travelAgentRepository;
    @Mock private UserRepository userRepository;
    @Mock private BookingMapper bookingMapper;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private MealPlanFormatter mealPlanFormatter;
    @Mock private DateUtil dateUtil;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private com.epam.edp.demo.repository.BookingDocumentRepository bookingDocumentRepository;
    @Mock private MongoReviewRepository reviewRepository;
    @Mock private RabbitTemplate rabbitTemplate;

    private BookingServiceImpl service;

    private static final String VALID_TOKEN = "Bearer valid.jwt.token";
    private static final String RAW_TOKEN  = "valid.jwt.token";
    private static final String USER_ID    = "u-001";
    private static final String TOUR_ID    = "t-001";
    private static final String INSTANCE_ID = "i-001";
    private static final String BOOKING_ID  = "b-001";

    @Before
    public void setUp() {
        service = new BookingServiceImpl(
                bookingRepository, tourRepository, tourInstanceRepository,
                travelAgentRepository, userRepository, bookingMapper, jwtTokenProvider,
                mealPlanFormatter, dateUtil, mongoTemplate, bookingDocumentRepository,
                reviewRepository, rabbitTemplate);
    }

    // =========================================================================
    // createBooking
    // =========================================================================

    @Test
    public void createBooking_success_returnsFreeDate() {
        stubValidToken();
        Tour tour = tour(TOUR_ID);
        TourInstance instance = instance(INSTANCE_ID, TOUR_ID, LocalDate.of(2027, 6, 15));
        TravelAgent agent = agent("a-001");

        when(tourRepository.existsById(TOUR_ID)).thenReturn(true);
        when(tourRepository.findById(TOUR_ID)).thenReturn(Optional.of(tour));
        when(tourInstanceRepository.findByTourId(TOUR_ID)).thenReturn(List.of(instance));
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(), eq(TourInstance.class)))
                .thenReturn(instance);
        when(travelAgentRepository.findAny()).thenReturn(agent);
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(dateUtil.computeFreeCancellationDate("2027-06-15", 7)).thenReturn("2027-06-08");
        when(mealPlanFormatter.format("BB")).thenReturn("Bed & Breakfast");

        CreateBookingResponseDTO result = service.createBooking(createRequest(USER_ID, TOUR_ID, "2027-06-15"), VALID_TOKEN);

        assertNotNull(result);
        assertEquals("2027-06-08", result.getFreeCancelation());
        assertNotNull(result.getDetails());

        ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(cap.capture());
        Booking saved = cap.getValue();
        assertEquals(USER_ID, saved.getUserId());
        assertEquals(TOUR_ID, saved.getTourId());
        assertEquals(INSTANCE_ID, saved.getTourInstanceId());
        assertEquals(BookingStatus.BOOKED, saved.getState());
    }

    @Test(expected = UnauthorizedException.class)
    public void createBooking_nullToken_throwsUnauthorized() {
        service.createBooking(createRequest(USER_ID, TOUR_ID, "2027-06-15"), null);
    }

    @Test(expected = UnauthorizedException.class)
    public void createBooking_invalidToken_throwsUnauthorized() {
        when(jwtTokenProvider.validateToken("bad.token")).thenReturn(false);
        service.createBooking(createRequest(USER_ID, TOUR_ID, "2027-06-15"), "Bearer bad.token");
    }

    @Test(expected = BadRequestException.class)
    public void createBooking_blankUserId_throwsBadRequest() {
        stubValidToken();
        CreateBookingRequestDTO req = createRequest("", TOUR_ID, "2027-06-15");
        service.createBooking(req, VALID_TOKEN);
    }

    @Test(expected = TourNotFoundException.class)
    public void createBooking_tourNotFound_throwsTourNotFound() {
        stubValidToken();
        // existsById returns false by default — triggers TourNotFoundException
        service.createBooking(createRequest(USER_ID, TOUR_ID, "2027-06-15"), VALID_TOKEN);
    }

    @Test(expected = BadRequestException.class)
    public void createBooking_noMatchingInstance_throwsBadRequest() {
        stubValidToken();
        // Instance has a different startDate
        TourInstance instance = instance(INSTANCE_ID, TOUR_ID, LocalDate.of(2027, 7, 1));

        when(tourRepository.existsById(TOUR_ID)).thenReturn(true);
        when(tourInstanceRepository.findByTourId(TOUR_ID)).thenReturn(List.of(instance));

        service.createBooking(createRequest(USER_ID, TOUR_ID, "2027-06-15"), VALID_TOKEN);
    }

    @Test(expected = OverbookingException.class)
    public void createBooking_noSlots_throwsOverbooking() {
        stubValidToken();
        TourInstance instance = instance(INSTANCE_ID, TOUR_ID, LocalDate.of(2027, 6, 15));

        when(tourRepository.existsById(TOUR_ID)).thenReturn(true);
        when(tourInstanceRepository.findByTourId(TOUR_ID)).thenReturn(List.of(instance));
        // findAndModify returns null → no slot available
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(), eq(TourInstance.class)))
                .thenReturn(null);

        service.createBooking(createRequest(USER_ID, TOUR_ID, "2027-06-15"), VALID_TOKEN);
    }

    // =========================================================================
    // getBookings
    // =========================================================================

    @Test
    public void getBookings_validToken_returnsList() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn(USER_ID);
        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED, "2027-06-15");
        Tour tour = tour(TOUR_ID);
        TravelAgent agent = agent("a-001");

        when(bookingRepository.findByUserId(USER_ID)).thenReturn(List.of(booking));
        when(tourRepository.findById(TOUR_ID)).thenReturn(Optional.of(tour));
        when(tourInstanceRepository.findByTourId(TOUR_ID)).thenReturn(Collections.emptyList());
        when(travelAgentRepository.findById("a-001")).thenReturn(Optional.of(agent));
        when(bookingMapper.toBookedTourDTO(any(), any(), any(), any())).thenReturn(new com.epam.edp.demo.dto.booking.BookedTourDTO());

        BookedTourListResponseDTO result = service.getBookings(USER_ID, VALID_TOKEN);

        assertNotNull(result);
        assertEquals(1, result.getBookings().size());
    }

    @Test
    public void getBookings_noBookings_returnsEmptyList() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn(USER_ID);
        when(bookingRepository.findByUserId(USER_ID)).thenReturn(Collections.emptyList());

        BookedTourListResponseDTO result = service.getBookings(USER_ID, VALID_TOKEN);

        assertNotNull(result);
        assertEquals(0, result.getBookings().size());
    }

    @Test(expected = UnauthorizedException.class)
    public void getBookings_noToken_throwsUnauthorized() {
        service.getBookings(USER_ID, null);
    }

    // =========================================================================
    // cancelBooking
    // =========================================================================

    @Test
    public void cancelBooking_success_setsStateCanceled() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn(USER_ID);

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED,
                LocalDate.now().plusDays(20).toString());
        booking.setTourInstanceId(INSTANCE_ID);
        booking.setGuests(new GuestCount(2, 0));

        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cancelBooking(BOOKING_ID, VALID_TOKEN);

        ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(cap.capture());
        assertEquals(BookingStatus.CANCELED, cap.getValue().getState());
        assertEquals(USER_ID, cap.getValue().getCanceledBy());
        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(TourInstance.class));
    }

    @Test(expected = BookingNotFoundException.class)
    public void cancelBooking_notFound_throwsBookingNotFound() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn(USER_ID);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

        service.cancelBooking(BOOKING_ID, VALID_TOKEN);
    }

    @Test(expected = UnauthorizedException.class)
    public void cancelBooking_differentUser_throwsUnauthorized() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn("other-user");

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED,
                LocalDate.now().plusDays(20).toString());
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.cancelBooking(BOOKING_ID, VALID_TOKEN);
    }

    @Test(expected = CancellationNotAllowedException.class)
    public void cancelBooking_alreadyCanceled_throwsCancellationNotAllowed() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn(USER_ID);

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.CANCELED,
                LocalDate.now().plusDays(20).toString());
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.cancelBooking(BOOKING_ID, VALID_TOKEN);
    }

    @Test(expected = CancellationNotAllowedException.class)
    public void cancelBooking_tourStarted_throwsCancellationNotAllowed() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn(USER_ID);

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.STARTED,
                LocalDate.now().plusDays(20).toString());
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.cancelBooking(BOOKING_ID, VALID_TOKEN);
    }

    @Test(expected = CancellationNotAllowedException.class)
    public void cancelBooking_within10Days_throwsCancellationNotAllowed() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn(USER_ID);

        // Tour starts in 5 days — within the 10-day threshold
        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED,
                LocalDate.now().plusDays(5).toString());
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.cancelBooking(BOOKING_ID, VALID_TOKEN);
    }

    @Test(expected = CancellationNotAllowedException.class)
    public void cancelBooking_exactly10DaysBeforeToday_throwsCancellationNotAllowed() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn(USER_ID);

        // Latest cancellation date is today — LocalDate.now().isAfter(today) == false
        // but we need to be strictly BEFORE. At exactly 10 days the deadline has passed.
        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED,
                LocalDate.now().plusDays(9).toString());
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.cancelBooking(BOOKING_ID, VALID_TOKEN);
    }

    @Test
    public void cancelBooking_exactly11DaysBeforeStart_succeeds() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn(USER_ID);

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED,
                LocalDate.now().plusDays(11).toString());
        booking.setTourInstanceId(null); // no slot restore needed
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cancelBooking(BOOKING_ID, VALID_TOKEN);

        verify(bookingRepository).save(any(Booking.class));
        verify(mongoTemplate, never()).updateFirst(any(), any(), eq(TourInstance.class));
    }

    @Test(expected = UnauthorizedException.class)
    public void cancelBooking_noToken_throwsUnauthorized() {
        service.cancelBooking(BOOKING_ID, null);
    }

    // =========================================================================
    // confirmBooking
    // =========================================================================

    @Test
    public void confirmBooking_success_setsStateConfirmed() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn(USER_ID);

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED, "2027-06-15");
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.confirmBooking(BOOKING_ID, VALID_TOKEN);

        ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(cap.capture());
        assertEquals(BookingStatus.CONFIRMED, cap.getValue().getState());
    }

    @Test(expected = BookingNotFoundException.class)
    public void confirmBooking_notFound_throwsBookingNotFound() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn(USER_ID);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

        service.confirmBooking(BOOKING_ID, VALID_TOKEN);
    }

    @Test(expected = UnauthorizedException.class)
    public void confirmBooking_differentUser_throwsUnauthorized() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn("other-user");
        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED, "2027-06-15");
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.confirmBooking(BOOKING_ID, VALID_TOKEN);
    }

    @Test(expected = BadRequestException.class)
    public void confirmBooking_notInBookedState_throwsBadRequest() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn(USER_ID);
        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.CONFIRMED, "2027-06-15");
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.confirmBooking(BOOKING_ID, VALID_TOKEN);
    }

    @Test(expected = BadRequestException.class)
    public void confirmBooking_canceledState_throwsBadRequest() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn(USER_ID);
        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.CANCELED, "2027-06-15");
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.confirmBooking(BOOKING_ID, VALID_TOKEN);
    }

    @Test(expected = UnauthorizedException.class)
    public void confirmBooking_noToken_throwsUnauthorized() {
        service.confirmBooking(BOOKING_ID, null);
    }

    // =========================================================================
    // updateBooking
    // =========================================================================

    @Test
    public void updateBooking_success_updatesMealPlanGuestsAndPersonalDetails() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn(USER_ID);

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED, "2027-06-15");
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateBookingRequestDTO request = new UpdateBookingRequestDTO();
        GuestsDTO guests = new GuestsDTO();
        guests.setAdult(3);
        guests.setChildren(1);
        request.setGuests(guests);
        request.setMealPlan("HB");
        PersonDetailDTO person = new PersonDetailDTO();
        person.setFirstName("Jane");
        person.setLastName("Doe");
        request.setPersonalDetails(List.of(person));

        service.updateBooking(BOOKING_ID, request, VALID_TOKEN);

        ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(cap.capture());
        Booking saved = cap.getValue();
        assertEquals("HB", saved.getMealPlan());
        assertEquals(3, saved.getGuests().getAdult());
    }

    @Test
    public void updateBooking_confirmedState_succeeds() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn(USER_ID);

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.CONFIRMED, "2027-06-15");
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateBookingRequestDTO request = new UpdateBookingRequestDTO();
        GuestsDTO guests = new GuestsDTO();
        guests.setAdult(2);
        guests.setChildren(0);
        request.setGuests(guests);
        PersonDetailDTO person = new PersonDetailDTO();
        person.setFirstName("Alice");
        person.setLastName("Smith");
        request.setPersonalDetails(List.of(person));

        service.updateBooking(BOOKING_ID, request, VALID_TOKEN);

        verify(bookingRepository).save(any(Booking.class));
    }

    @Test(expected = BookingNotFoundException.class)
    public void updateBooking_notFound_throwsBookingNotFound() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn(USER_ID);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

        service.updateBooking(BOOKING_ID, new UpdateBookingRequestDTO(), VALID_TOKEN);
    }

    @Test(expected = UnauthorizedException.class)
    public void updateBooking_differentUser_throwsUnauthorized() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn("other-user");
        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED, "2027-06-15");
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.updateBooking(BOOKING_ID, new UpdateBookingRequestDTO(), VALID_TOKEN);
    }

    @Test(expected = BadRequestException.class)
    public void updateBooking_canceledState_throwsBadRequest() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn(USER_ID);
        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.CANCELED, "2027-06-15");
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.updateBooking(BOOKING_ID, new UpdateBookingRequestDTO(), VALID_TOKEN);
    }

    @Test(expected = BadRequestException.class)
    public void updateBooking_startedState_throwsBadRequest() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn(USER_ID);
        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.STARTED, "2027-06-15");
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.updateBooking(BOOKING_ID, new UpdateBookingRequestDTO(), VALID_TOKEN);
    }

    @Test(expected = BadRequestException.class)
    public void updateBooking_finishedState_throwsBadRequest() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn(USER_ID);
        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.FINISHED, "2027-06-15");
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.updateBooking(BOOKING_ID, new UpdateBookingRequestDTO(), VALID_TOKEN);
    }

    @Test(expected = UnauthorizedException.class)
    public void updateBooking_noToken_throwsUnauthorized() {
        service.updateBooking(BOOKING_ID, new UpdateBookingRequestDTO(), null);
    }

    // =========================================================================
    // cancelBooking - additional edge cases
    // =========================================================================

    @Test(expected = CancellationNotAllowedException.class)
    public void cancelBooking_finishedState_throwsCancellationNotAllowed() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn(USER_ID);
        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.FINISHED,
                LocalDate.now().plusDays(20).toString());
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.cancelBooking(BOOKING_ID, VALID_TOKEN);
    }

    // =========================================================================
    // createBooking - additional edge cases
    // =========================================================================

    @Test(expected = BadRequestException.class)
    public void createBooking_invalidDateFormat_throwsBadRequest() {
        stubValidToken();
        when(tourRepository.existsById(TOUR_ID)).thenReturn(true);
        CreateBookingRequestDTO req = createRequest(USER_ID, TOUR_ID, "not-a-date");
        service.createBooking(req, VALID_TOKEN);
    }

    @Test(expected = BadRequestException.class)
    public void createBooking_pastDate_throwsBadRequest() {
        stubValidToken();
        when(tourRepository.existsById(TOUR_ID)).thenReturn(true);
        CreateBookingRequestDTO req = createRequest(USER_ID, TOUR_ID,
                LocalDate.now().minusDays(1).toString());
        service.createBooking(req, VALID_TOKEN);
    }

    @Test(expected = BadRequestException.class)
    public void createBooking_existingActiveBooking_throwsBadRequest() {
        stubValidToken();
        TourInstance instance = instance(INSTANCE_ID, TOUR_ID, LocalDate.of(2027, 6, 15));
        Booking existingBooking = new Booking();
        existingBooking.setState(BookingStatus.BOOKED);

        when(tourRepository.existsById(TOUR_ID)).thenReturn(true);
        when(tourInstanceRepository.findByTourId(TOUR_ID)).thenReturn(List.of(instance));
        when(bookingRepository.findByUserIdAndTourInstanceId(USER_ID, INSTANCE_ID))
                .thenReturn(List.of(existingBooking));

        service.createBooking(createRequest(USER_ID, TOUR_ID, "2027-06-15"), VALID_TOKEN);
    }

    // =========================================================================
    // getBookings - additional edge cases
    // =========================================================================

    @Test(expected = UnauthorizedException.class)
    public void getBookings_callerIdMismatch_throwsUnauthorized() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn("different-user");

        service.getBookings(USER_ID, VALID_TOKEN);
    }


    // =========================================================================
    // Helpers
    // =========================================================================

    private void stubValidToken() {
        when(jwtTokenProvider.validateToken(RAW_TOKEN)).thenReturn(true);
    }

    private static final String AGENT_EMAIL = "priya.sharma@travelagency.com";

    private void stubAgentLookup() {
        when(jwtTokenProvider.validateToken(RAW_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken(RAW_TOKEN)).thenReturn(AGENT_EMAIL);
        TravelAgent agent = new TravelAgent();
        agent.setId("a-001");
        when(travelAgentRepository.findByEmailIgnoreCase(AGENT_EMAIL)).thenReturn(Optional.of(agent));
    }

    private Tour tour(String id) {
        Tour tour = new Tour();
        tour.setId(id);
        tour.setName("Test Tour");
        tour.setFreeCancellationDays(7);
        tour.setDestination(new Destination("Rome", "Italy"));
        return tour;
    }

    private TourInstance instance(String id, String tourId, LocalDate startDate) {
        return TourInstance.builder()
                .id(id)
                .tourId(tourId)
                .startDate(startDate)
                .endDate(startDate.plusDays(7))
                .availableSlots(10)
                .totalCapacity(10)
                .build();
    }

    private TravelAgent agent(String id) {
        TravelAgent a = new TravelAgent();
        a.setId(id);
        a.setName("Alice Smith");
        return a;
    }

    private Booking booking(String id, String userId, String tourId,
                             BookingStatus status, String date) {
        Booking b = new Booking();
        b.setId(id);
        b.setUserId(userId);
        b.setTourId(tourId);
        b.setState(status);
        b.setDate(date);
        b.setDuration("7 days");
        b.setMealPlan("BB");
        b.setGuests(new GuestCount(1, 0));
        b.setTravelAgentId("a-001");
        return b;
    }

    private CreateBookingRequestDTO createRequest(String userId, String tourId, String date) {
        GuestsDTO guests = new GuestsDTO();
        guests.setAdult(2);
        guests.setChildren(0);

        PersonDetailDTO person = new PersonDetailDTO();
        person.setFirstName("John");
        person.setLastName("Doe");

        CreateBookingRequestDTO req = new CreateBookingRequestDTO();
        req.setUserId(userId);
        req.setTourId(tourId);
        req.setDate(date);
        req.setDuration("7 days");
        req.setMealPlan("BB");
        req.setGuests(guests);
        req.setPersonalDetails(List.of(person));
        return req;
    }

    // =========================================================================
    // Sprint 2: confirmBookingV2
    // =========================================================================

    @Test
    public void confirmBookingV2_success_setsStateConfirmed() {
        stubAgentLookup();

        com.epam.edp.demo.entity.BookingDocument doc = new com.epam.edp.demo.entity.BookingDocument();
        doc.setId("doc-1");
        doc.setVerified(true);

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED, "2027-06-15");
        booking.setTravelAgentId("a-001");
        booking.setDocuments(new java.util.ArrayList<>(List.of(doc)));
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(bookingDocumentRepository.findByBookingId(BOOKING_ID)).thenReturn(List.of(doc));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toAgentDetailDTO(any(), any(), any(), any())).thenReturn(new com.epam.edp.demo.dto.booking.TravelAgentBookingDetailDTO());

        com.epam.edp.demo.dto.booking.ConfirmBookingRequestDTO request = new com.epam.edp.demo.dto.booking.ConfirmBookingRequestDTO();
        request.setNotes("All good");

        com.epam.edp.demo.dto.booking.TravelAgentBookingDetailDTO result =
                service.confirmBookingV2(BOOKING_ID, request, VALID_TOKEN);

        assertNotNull(result);
        ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(cap.capture());
        assertEquals(BookingStatus.CONFIRMED, cap.getValue().getState());
    }

    @Test(expected = com.epam.edp.demo.exception.DocumentVerificationException.class)
    public void confirmBookingV2_noDocuments_throwsDocumentVerificationException() {
        stubAgentLookup();

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED, "2027-06-15");
        booking.setTravelAgentId("a-001");
        booking.setDocuments(new java.util.ArrayList<>());
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.confirmBookingV2(BOOKING_ID, new com.epam.edp.demo.dto.booking.ConfirmBookingRequestDTO(), VALID_TOKEN);
    }

    @Test(expected = com.epam.edp.demo.exception.DocumentVerificationException.class)
    public void confirmBookingV2_unverifiedDocument_throwsDocumentVerificationException() {
        stubAgentLookup();

        com.epam.edp.demo.entity.BookingDocument unverified = new com.epam.edp.demo.entity.BookingDocument();
        unverified.setId("doc-1");
        unverified.setVerified(false);

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED, "2027-06-15");
        booking.setTravelAgentId("a-001");
        booking.setDocuments(new java.util.ArrayList<>(List.of(unverified)));
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.confirmBookingV2(BOOKING_ID, new com.epam.edp.demo.dto.booking.ConfirmBookingRequestDTO(), VALID_TOKEN);
    }

    @Test(expected = UnauthorizedException.class)
    public void confirmBookingV2_notAssignedAgent_throwsUnauthorized() {
        stubValidToken();

        service.confirmBookingV2(BOOKING_ID, new com.epam.edp.demo.dto.booking.ConfirmBookingRequestDTO(), VALID_TOKEN);
    }

    @Test(expected = com.epam.edp.demo.exception.InvalidBookingStateException.class)
    public void confirmBookingV2_alreadyConfirmed_throwsInvalidState() {
        stubAgentLookup();

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.CONFIRMED, "2027-06-15");
        booking.setTravelAgentId("a-001");
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.confirmBookingV2(BOOKING_ID, new com.epam.edp.demo.dto.booking.ConfirmBookingRequestDTO(), VALID_TOKEN);
    }

    // =========================================================================
    // Sprint 2: cancelBookingV2
    // =========================================================================

    @Test
    public void cancelBookingV2_success_refundEligible() {
        stubAgentLookup();

        // Tour starts in 20 days → well within the 10-day free cancellation window
        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED,
                LocalDate.now().plusDays(20).toString());
        booking.setTravelAgentId("a-001");
        booking.setTourInstanceId(INSTANCE_ID);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toAgentDetailDTO(any(), any(), any(), any())).thenReturn(new com.epam.edp.demo.dto.booking.TravelAgentBookingDetailDTO());

        com.epam.edp.demo.dto.booking.CancelBookingRequestDTO request =
                new com.epam.edp.demo.dto.booking.CancelBookingRequestDTO(
                        com.epam.edp.demo.enums.CancellationReason.CUSTOMERS_EMERGENCY, "Emergency");

        com.epam.edp.demo.dto.booking.TravelAgentBookingDetailDTO result =
                service.cancelBookingV2(BOOKING_ID, request, VALID_TOKEN);

        assertNotNull(result);
        ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(cap.capture());
        assertEquals(BookingStatus.CANCELED, cap.getValue().getState());
        assertEquals("a-001", cap.getValue().getCanceledBy());
        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(TourInstance.class));
    }

    @Test
    public void cancelBookingV2_notRefundEligible_withinDeadline() {
        stubAgentLookup();

        // Tour starts in 5 days — past the 10-day cancellation window
        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.CONFIRMED,
                LocalDate.now().plusDays(5).toString());
        booking.setTravelAgentId("a-001");
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toAgentDetailDTO(any(), any(), any(), any())).thenReturn(new com.epam.edp.demo.dto.booking.TravelAgentBookingDetailDTO());

        com.epam.edp.demo.dto.booking.CancelBookingRequestDTO request =
                new com.epam.edp.demo.dto.booking.CancelBookingRequestDTO(
                        com.epam.edp.demo.enums.CancellationReason.WEATHER_CONDITIONS, null);

        service.cancelBookingV2(BOOKING_ID, request, VALID_TOKEN);

        ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(cap.capture());
        assertEquals(BookingStatus.CANCELED, cap.getValue().getState());
        // refundEligible should be false — captured in cancellation details
        assertNotNull(cap.getValue().getCancellation());
        assertEquals(false, cap.getValue().getCancellation().isRefundEligible());
    }

    @Test(expected = com.epam.edp.demo.exception.InvalidBookingStateException.class)
    public void cancelBookingV2_startedState_throwsInvalidState() {
        stubAgentLookup();

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.STARTED, "2027-06-15");
        booking.setTravelAgentId("a-001");
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.cancelBookingV2(BOOKING_ID,
                new com.epam.edp.demo.dto.booking.CancelBookingRequestDTO(
                        com.epam.edp.demo.enums.CancellationReason.OTHER, null),
                VALID_TOKEN);
    }

    @Test(expected = UnauthorizedException.class)
    public void cancelBookingV2_notAssignedAgent_throwsUnauthorized() {
        stubValidToken();

        service.cancelBookingV2(BOOKING_ID,
                new com.epam.edp.demo.dto.booking.CancelBookingRequestDTO(
                        com.epam.edp.demo.enums.CancellationReason.OTHER, null),
                VALID_TOKEN);
    }

    // =========================================================================
    // Sprint 2: uploadDocument
    // =========================================================================

    @Test
    public void uploadDocument_success_addsDocumentToBooking() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn(USER_ID);

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED, "2027-06-15");
        booking.setTravelAgentId("a-001");
        booking.setDocuments(new java.util.ArrayList<>());
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toAgentDetailDTO(any(), any(), any(), any())).thenReturn(new com.epam.edp.demo.dto.booking.TravelAgentBookingDetailDTO());

        com.epam.edp.demo.dto.booking.UploadDocumentRequestDTO request =
                new com.epam.edp.demo.dto.booking.UploadDocumentRequestDTO(
                        com.epam.edp.demo.enums.DocumentType.PASSPORT,
                        "passport.pdf",
                        "https://example.com/passport.pdf");

        com.epam.edp.demo.dto.booking.TravelAgentBookingDetailDTO result =
                service.uploadDocument(BOOKING_ID, request, VALID_TOKEN);

        assertNotNull(result);
        ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(cap.capture());
        assertEquals(1, cap.getValue().getDocuments().size());
        assertEquals(com.epam.edp.demo.enums.DocumentType.PASSPORT,
                cap.getValue().getDocuments().get(0).getType());
    }

    @Test(expected = com.epam.edp.demo.exception.InvalidBookingStateException.class)
    public void uploadDocument_canceledBooking_throwsInvalidState() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn(USER_ID);

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.CANCELED, "2027-06-15");
        booking.setTravelAgentId("a-001");
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.uploadDocument(BOOKING_ID,
                new com.epam.edp.demo.dto.booking.UploadDocumentRequestDTO(
                        com.epam.edp.demo.enums.DocumentType.VISA, "visa.pdf", "url"),
                VALID_TOKEN);
    }

    @Test(expected = UnauthorizedException.class)
    public void uploadDocument_notOwnerOrAgent_throwsUnauthorized() {
        stubValidToken();
        when(jwtTokenProvider.getUserIdFromToken(RAW_TOKEN)).thenReturn("stranger");

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED, "2027-06-15");
        booking.setTravelAgentId("a-001");
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.uploadDocument(BOOKING_ID,
                new com.epam.edp.demo.dto.booking.UploadDocumentRequestDTO(
                        com.epam.edp.demo.enums.DocumentType.PASSPORT, "p.pdf", "url"),
                VALID_TOKEN);
    }

    // =========================================================================
    // Sprint 2: verifyDocument
    // =========================================================================

    @Test
    public void verifyDocument_approve_setsVerifiedTrue() {
        stubAgentLookup();

        com.epam.edp.demo.entity.BookingDocument doc = new com.epam.edp.demo.entity.BookingDocument();
        doc.setId("doc-1");
        doc.setVerified(false);

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED, "2027-06-15");
        booking.setTravelAgentId("a-001");
        booking.setDocuments(new java.util.ArrayList<>(List.of(doc)));
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toAgentDetailDTO(any(), any(), any(), any())).thenReturn(new com.epam.edp.demo.dto.booking.TravelAgentBookingDetailDTO());

        com.epam.edp.demo.dto.booking.DocumentVerificationRequestDTO request =
                new com.epam.edp.demo.dto.booking.DocumentVerificationRequestDTO("doc-1", "APPROVE", null);

        service.verifyDocument(BOOKING_ID, request, VALID_TOKEN);

        ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(cap.capture());
        assertEquals(true, cap.getValue().getDocuments().get(0).isVerified());
        assertEquals("a-001", cap.getValue().getDocuments().get(0).getVerifiedBy());
    }

    @Test
    public void verifyDocument_reject_setsVerifiedFalse() {
        stubAgentLookup();

        com.epam.edp.demo.entity.BookingDocument doc = new com.epam.edp.demo.entity.BookingDocument();
        doc.setId("doc-1");
        doc.setVerified(true);

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED, "2027-06-15");
        booking.setTravelAgentId("a-001");
        booking.setDocuments(new java.util.ArrayList<>(List.of(doc)));
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toAgentDetailDTO(any(), any(), any(), any())).thenReturn(new com.epam.edp.demo.dto.booking.TravelAgentBookingDetailDTO());

        com.epam.edp.demo.dto.booking.DocumentVerificationRequestDTO request =
                new com.epam.edp.demo.dto.booking.DocumentVerificationRequestDTO("doc-1", "REJECT", "Expired");

        service.verifyDocument(BOOKING_ID, request, VALID_TOKEN);

        ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(cap.capture());
        assertEquals(false, cap.getValue().getDocuments().get(0).isVerified());
    }

    @Test(expected = com.epam.edp.demo.exception.DocumentVerificationException.class)
    public void verifyDocument_documentNotFound_throwsDocumentVerificationException() {
        stubAgentLookup();

        com.epam.edp.demo.entity.BookingDocument doc = new com.epam.edp.demo.entity.BookingDocument();
        doc.setId("doc-1");
        doc.setVerified(false);

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED, "2027-06-15");
        booking.setTravelAgentId("a-001");
        booking.setDocuments(new java.util.ArrayList<>(List.of(doc)));
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.verifyDocument(BOOKING_ID,
                new com.epam.edp.demo.dto.booking.DocumentVerificationRequestDTO("non-existent", "APPROVE", null),
                VALID_TOKEN);
    }

    @Test(expected = com.epam.edp.demo.exception.DocumentVerificationException.class)
    public void verifyDocument_noDocuments_throwsDocumentVerificationException() {
        stubAgentLookup();

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED, "2027-06-15");
        booking.setTravelAgentId("a-001");
        booking.setDocuments(null);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.verifyDocument(BOOKING_ID,
                new com.epam.edp.demo.dto.booking.DocumentVerificationRequestDTO("doc-1", "APPROVE", null),
                VALID_TOKEN);
    }

    @Test(expected = BadRequestException.class)
    public void verifyDocument_invalidAction_throwsBadRequest() {
        stubAgentLookup();

        com.epam.edp.demo.entity.BookingDocument doc = new com.epam.edp.demo.entity.BookingDocument();
        doc.setId("doc-1");
        doc.setVerified(false);

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED, "2027-06-15");
        booking.setTravelAgentId("a-001");
        booking.setDocuments(new java.util.ArrayList<>(List.of(doc)));
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.verifyDocument(BOOKING_ID,
                new com.epam.edp.demo.dto.booking.DocumentVerificationRequestDTO("doc-1", "INVALID_ACTION", null),
                VALID_TOKEN);
    }

    // =========================================================================
    // Sprint 2: editBooking
    // =========================================================================

    @Test
    public void editBooking_success_updatesGuestsMealPlanDuration() {
        stubAgentLookup();

        com.epam.edp.demo.entity.CustomerApproval approval =
                com.epam.edp.demo.entity.CustomerApproval.builder()
                        .approvalMode(com.epam.edp.demo.enums.ApprovalMode.OFFLINE)
                        .approvalGiven(true)
                        .build();

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED, "2027-06-15");
        booking.setTravelAgentId("a-001");
        booking.setCustomerApproval(approval);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toAgentDetailDTO(any(), any(), any(), any())).thenReturn(new com.epam.edp.demo.dto.booking.TravelAgentBookingDetailDTO());

        com.epam.edp.demo.dto.booking.EditBookingRequestDTO request =
                new com.epam.edp.demo.dto.booking.EditBookingRequestDTO(3, 1, "HB", 10);

        service.editBooking(BOOKING_ID, request, VALID_TOKEN);

        ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(cap.capture());
        assertEquals(3, cap.getValue().getGuests().getAdult());
        assertEquals(1, cap.getValue().getGuests().getChildren());
        assertEquals("HB", cap.getValue().getMealPlan());
        assertEquals("10", cap.getValue().getDuration());
        // approval must be reset after edit
        assertNull(cap.getValue().getCustomerApproval());
    }

    @Test(expected = com.epam.edp.demo.exception.InvalidBookingStateException.class)
    public void editBooking_notBookedState_throwsInvalidState() {
        stubAgentLookup();

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.CONFIRMED, "2027-06-15");
        booking.setTravelAgentId("a-001");
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.editBooking(BOOKING_ID, new com.epam.edp.demo.dto.booking.EditBookingRequestDTO(), VALID_TOKEN);
    }

    @Test(expected = BadRequestException.class)
    public void editBooking_noCustomerApproval_throwsBadRequest() {
        stubAgentLookup();

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED, "2027-06-15");
        booking.setTravelAgentId("a-001");
        booking.setCustomerApproval(null);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.editBooking(BOOKING_ID, new com.epam.edp.demo.dto.booking.EditBookingRequestDTO(), VALID_TOKEN);
    }

    @Test(expected = BadRequestException.class)
    public void editBooking_approvalDenied_throwsBadRequest() {
        stubAgentLookup();

        com.epam.edp.demo.entity.CustomerApproval denied =
                com.epam.edp.demo.entity.CustomerApproval.builder()
                        .approvalMode(com.epam.edp.demo.enums.ApprovalMode.OFFLINE)
                        .approvalGiven(false)
                        .build();

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED, "2027-06-15");
        booking.setTravelAgentId("a-001");
        booking.setCustomerApproval(denied);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.editBooking(BOOKING_ID, new com.epam.edp.demo.dto.booking.EditBookingRequestDTO(), VALID_TOKEN);
    }

    // =========================================================================
    // Sprint 2: recordCustomerApproval
    // =========================================================================

    @Test
    public void recordCustomerApproval_success_setsApproval() {
        stubAgentLookup();

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED, "2027-06-15");
        booking.setTravelAgentId("a-001");
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingMapper.toAgentDetailDTO(any(), any(), any(), any())).thenReturn(new com.epam.edp.demo.dto.booking.TravelAgentBookingDetailDTO());

        com.epam.edp.demo.dto.booking.CustomerApprovalRequestDTO request =
                new com.epam.edp.demo.dto.booking.CustomerApprovalRequestDTO(
                        com.epam.edp.demo.enums.ApprovalMode.OFFLINE, true, "Approved by phone");

        service.recordCustomerApproval(BOOKING_ID, request, VALID_TOKEN);

        ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(cap.capture());
        assertNotNull(cap.getValue().getCustomerApproval());
        assertEquals(true, cap.getValue().getCustomerApproval().isApprovalGiven());
    }

    @Test(expected = com.epam.edp.demo.exception.InvalidBookingStateException.class)
    public void recordCustomerApproval_notBookedState_throwsInvalidState() {
        stubAgentLookup();

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.CONFIRMED, "2027-06-15");
        booking.setTravelAgentId("a-001");
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        service.recordCustomerApproval(BOOKING_ID,
                new com.epam.edp.demo.dto.booking.CustomerApprovalRequestDTO(
                        com.epam.edp.demo.enums.ApprovalMode.OFFLINE, true, null),
                VALID_TOKEN);
    }

    // =========================================================================
    // Sprint 2: getAgentBookings
    // =========================================================================

    @Test
    public void getAgentBookings_noFilter_returnsAllAgentBookings() {
        stubAgentLookup();

        Booking b1 = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED, "2027-06-15");
        b1.setTravelAgentId("a-001");
        Booking b2 = booking("b-002", USER_ID, TOUR_ID, BookingStatus.CONFIRMED, "2027-07-01");
        b2.setTravelAgentId("a-001");

        when(bookingRepository.findByTravelAgentId("a-001")).thenReturn(List.of(b1, b2));
        when(bookingMapper.toAgentDetailDTO(any(), any(), any(), any())).thenReturn(new com.epam.edp.demo.dto.booking.TravelAgentBookingDetailDTO());

        com.epam.edp.demo.dto.booking.AgentBookingListResponseDTO result =
                service.getAgentBookings(null, VALID_TOKEN);

        assertNotNull(result);
        assertEquals(2, result.getTotal());
    }

    @Test
    public void getAgentBookings_withStatusFilter_returnsFiltered() {
        stubAgentLookup();

        Booking b = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED, "2027-06-15");
        b.setTravelAgentId("a-001");

        when(bookingRepository.findByTravelAgentIdAndState("a-001", BookingStatus.BOOKED))
                .thenReturn(List.of(b));
        when(bookingMapper.toAgentDetailDTO(any(), any(), any(), any())).thenReturn(new com.epam.edp.demo.dto.booking.TravelAgentBookingDetailDTO());

        com.epam.edp.demo.dto.booking.AgentBookingListResponseDTO result =
                service.getAgentBookings("BOOKED", VALID_TOKEN);

        assertEquals(1, result.getTotal());
    }

    @Test(expected = BadRequestException.class)
    public void getAgentBookings_invalidStatusFilter_throwsBadRequest() {
        stubAgentLookup();

        service.getAgentBookings("INVALID_STATUS", VALID_TOKEN);
    }

    // =========================================================================
    // Sprint 2: getAgentBookingById
    // =========================================================================

    @Test
    public void getAgentBookingById_success_returnsDetail() {
        stubAgentLookup();

        Booking booking = booking(BOOKING_ID, USER_ID, TOUR_ID, BookingStatus.BOOKED, "2027-06-15");
        booking.setTravelAgentId("a-001");
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(bookingMapper.toAgentDetailDTO(any(), any(), any(), any())).thenReturn(new com.epam.edp.demo.dto.booking.TravelAgentBookingDetailDTO());

        com.epam.edp.demo.dto.booking.TravelAgentBookingDetailDTO result =
                service.getAgentBookingById(BOOKING_ID, VALID_TOKEN);

        assertNotNull(result);
    }

    @Test(expected = UnauthorizedException.class)
    public void getAgentBookingById_notAssignedAgent_throwsUnauthorized() {
        stubValidToken();

        service.getAgentBookingById(BOOKING_ID, VALID_TOKEN);
    }

    @Test(expected = BookingNotFoundException.class)
    public void getAgentBookingById_notFound_throwsBookingNotFound() {
        stubAgentLookup();
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.empty());

        service.getAgentBookingById(BOOKING_ID, VALID_TOKEN);
    }
}


