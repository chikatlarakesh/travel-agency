package com.epam.edp.demo.scheduler;

import com.epam.edp.demo.entity.Booking;
import com.epam.edp.demo.entity.TourInstance;
import com.epam.edp.demo.enums.BookingStatus;
import com.epam.edp.demo.repository.BookingRepository;
import com.epam.edp.demo.repository.TourInstanceRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BookingStatusSchedulerTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private TourInstanceRepository tourInstanceRepository;

    private BookingStatusScheduler scheduler;

    @Before
    public void setUp() {
        scheduler = new BookingStatusScheduler(bookingRepository, tourInstanceRepository);
    }

    // =========================================================================
    // startConfirmedTours
    // =========================================================================

    @Test
    public void startConfirmedTours_startDateArrived_transitionsToStarted() {
        Booking booking = booking("b-1", BookingStatus.CONFIRMED, "t-1", "i-1",
                LocalDate.now().minusDays(1).toString()); // startDate was yesterday
        TourInstance instance = instance("i-1", "t-1",
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(6));

        when(bookingRepository.findByState(BookingStatus.CONFIRMED)).thenReturn(List.of(booking));
        when(tourInstanceRepository.findById("i-1")).thenReturn(Optional.of(instance));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.startConfirmedTours();

        ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(cap.capture());
        assertEquals(BookingStatus.STARTED, cap.getValue().getState());
    }

    @Test
    public void startConfirmedTours_startDateInFuture_noTransition() {
        Booking booking = booking("b-1", BookingStatus.CONFIRMED, "t-1", "i-1",
                LocalDate.now().plusDays(5).toString());
        TourInstance instance = instance("i-1", "t-1",
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(12));

        when(bookingRepository.findByState(BookingStatus.CONFIRMED)).thenReturn(List.of(booking));
        when(tourInstanceRepository.findById("i-1")).thenReturn(Optional.of(instance));

        scheduler.startConfirmedTours();

        verify(bookingRepository, never()).save(any());
    }

    @Test
    public void startConfirmedTours_startDateIsToday_transitionsToStarted() {
        Booking booking = booking("b-1", BookingStatus.CONFIRMED, "t-1", "i-1",
                LocalDate.now().toString());
        TourInstance instance = instance("i-1", "t-1",
                LocalDate.now(), LocalDate.now().plusDays(7));

        when(bookingRepository.findByState(BookingStatus.CONFIRMED)).thenReturn(List.of(booking));
        when(tourInstanceRepository.findById("i-1")).thenReturn(Optional.of(instance));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.startConfirmedTours();

        ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(cap.capture());
        assertEquals(BookingStatus.STARTED, cap.getValue().getState());
    }

    @Test
    public void startConfirmedTours_noConfirmedBookings_noSave() {
        when(bookingRepository.findByState(BookingStatus.CONFIRMED)).thenReturn(Collections.emptyList());

        scheduler.startConfirmedTours();

        verify(bookingRepository, never()).save(any());
    }

    @Test
    public void startConfirmedTours_instanceNotFound_skipsBooking() {
        Booking booking = booking("b-1", BookingStatus.CONFIRMED, "t-1", "i-missing",
                LocalDate.now().toString());

        when(bookingRepository.findByState(BookingStatus.CONFIRMED)).thenReturn(List.of(booking));
        when(tourInstanceRepository.findById("i-missing")).thenReturn(Optional.empty());

        scheduler.startConfirmedTours();

        verify(bookingRepository, never()).save(any());
    }

    @Test
    public void startConfirmedTours_multipleBookings_transitionsOnlyEligible() {
        Booking past = booking("b-1", BookingStatus.CONFIRMED, "t-1", "i-1",
                LocalDate.now().minusDays(1).toString());
        Booking future = booking("b-2", BookingStatus.CONFIRMED, "t-2", "i-2",
                LocalDate.now().plusDays(3).toString());

        TourInstance pastInstance   = instance("i-1", "t-1",
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(6));
        TourInstance futureInstance = instance("i-2", "t-2",
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(10));

        when(bookingRepository.findByState(BookingStatus.CONFIRMED)).thenReturn(List.of(past, future));
        when(tourInstanceRepository.findById("i-1")).thenReturn(Optional.of(pastInstance));
        when(tourInstanceRepository.findById("i-2")).thenReturn(Optional.of(futureInstance));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.startConfirmedTours();

        verify(bookingRepository, times(1)).save(any());
    }

    // =========================================================================
    // finishStartedTours
    // =========================================================================

    @Test
    public void finishStartedTours_endDatePassed_transitionsToFinished() {
        Booking booking = booking("b-1", BookingStatus.STARTED, "t-1", "i-1",
                LocalDate.now().minusDays(8).toString());
        TourInstance instance = instance("i-1", "t-1",
                LocalDate.now().minusDays(8), LocalDate.now().minusDays(1));

        when(bookingRepository.findByState(BookingStatus.STARTED)).thenReturn(List.of(booking));
        when(tourInstanceRepository.findById("i-1")).thenReturn(Optional.of(instance));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.finishStartedTours();

        ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(cap.capture());
        assertEquals(BookingStatus.FINISHED, cap.getValue().getState());
    }

    @Test
    public void finishStartedTours_endDateInFuture_noTransition() {
        Booking booking = booking("b-1", BookingStatus.STARTED, "t-1", "i-1",
                LocalDate.now().minusDays(2).toString());
        TourInstance instance = instance("i-1", "t-1",
                LocalDate.now().minusDays(2), LocalDate.now().plusDays(5));

        when(bookingRepository.findByState(BookingStatus.STARTED)).thenReturn(List.of(booking));
        when(tourInstanceRepository.findById("i-1")).thenReturn(Optional.of(instance));

        scheduler.finishStartedTours();

        verify(bookingRepository, never()).save(any());
    }

    @Test
    public void finishStartedTours_endDateIsToday_transitionsToFinished() {
        Booking booking = booking("b-1", BookingStatus.STARTED, "t-1", "i-1",
                LocalDate.now().minusDays(7).toString());
        TourInstance instance = instance("i-1", "t-1",
                LocalDate.now().minusDays(7), LocalDate.now());

        when(bookingRepository.findByState(BookingStatus.STARTED)).thenReturn(List.of(booking));
        when(tourInstanceRepository.findById("i-1")).thenReturn(Optional.of(instance));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.finishStartedTours();

        ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(cap.capture());
        assertEquals(BookingStatus.FINISHED, cap.getValue().getState());
    }

    @Test
    public void finishStartedTours_noStartedBookings_noSave() {
        when(bookingRepository.findByState(BookingStatus.STARTED)).thenReturn(Collections.emptyList());

        scheduler.finishStartedTours();

        verify(bookingRepository, never()).save(any());
    }

    @Test
    public void finishStartedTours_instanceNotFound_skipsBooking() {
        Booking booking = booking("b-1", BookingStatus.STARTED, "t-1", "i-missing",
                LocalDate.now().minusDays(10).toString());

        when(bookingRepository.findByState(BookingStatus.STARTED)).thenReturn(List.of(booking));
        when(tourInstanceRepository.findById("i-missing")).thenReturn(Optional.empty());

        scheduler.finishStartedTours();

        verify(bookingRepository, never()).save(any());
    }

    @Test
    public void finishStartedTours_fallbackByTourIdAndDate_whenNoInstanceId() {
        Booking booking = new Booking();
        booking.setId("b-1");
        booking.setUserId("u-1");
        booking.setTourId("t-1");
        booking.setTourInstanceId(null);          // no direct instance id
        booking.setState(BookingStatus.STARTED);
        booking.setDate(LocalDate.now().minusDays(8).toString());

        TourInstance instance = instance("i-1", "t-1",
                LocalDate.now().minusDays(8), LocalDate.now().minusDays(1));

        when(bookingRepository.findByState(BookingStatus.STARTED)).thenReturn(List.of(booking));
        when(tourInstanceRepository.findByTourId("t-1")).thenReturn(List.of(instance));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.finishStartedTours();

        ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(cap.capture());
        assertEquals(BookingStatus.FINISHED, cap.getValue().getState());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Booking booking(String id, BookingStatus status, String tourId,
                             String instanceId, String date) {
        Booking b = new Booking();
        b.setId(id);
        b.setUserId("u-001");
        b.setTourId(tourId);
        b.setTourInstanceId(instanceId);
        b.setState(status);
        b.setDate(date);
        return b;
    }

    private TourInstance instance(String id, String tourId,
                                   LocalDate startDate, LocalDate endDate) {
        return TourInstance.builder()
                .id(id)
                .tourId(tourId)
                .startDate(startDate)
                .endDate(endDate)
                .availableSlots(10)
                .totalCapacity(10)
                .build();
    }
}
