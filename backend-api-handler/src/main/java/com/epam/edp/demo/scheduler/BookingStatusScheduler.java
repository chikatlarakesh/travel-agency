package com.epam.edp.demo.scheduler;

import com.epam.edp.demo.entity.Booking;
import com.epam.edp.demo.entity.TourInstance;
import com.epam.edp.demo.enums.BookingStatus;
import com.epam.edp.demo.repository.BookingRepository;
import com.epam.edp.demo.repository.TourInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Periodically transitions booking states according to the lifecycle diagram:
 *
 * <pre>
 *   CONFIRMED  → STARTED   when tour start date has arrived
 *   STARTED    → FINISHED  when tour end date has arrived
 * </pre>
 *
 * Runs daily at midnight (00:00) in production.
 * The BOOKED → CONFIRMED transition is a manual travel-agent action and is not automated here.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingStatusScheduler {

    private final BookingRepository bookingRepository;
    private final TourInstanceRepository tourInstanceRepository;

    /**
     * Transitions CONFIRMED bookings to STARTED when their tour start date has arrived.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void startConfirmedTours() {
        LocalDate today = LocalDate.now();
        List<Booking> confirmed = bookingRepository.findByState(BookingStatus.CONFIRMED);

        int updated = 0;
        for (Booking booking : confirmed) {
            Optional<TourInstance> instanceOpt = resolveInstance(booking);
            if (instanceOpt.isEmpty()) {
                continue;
            }

            LocalDate startDate = instanceOpt.get().getStartDate();
            if (startDate != null && !today.isBefore(startDate)) {
                booking.setState(BookingStatus.STARTED);
                bookingRepository.save(booking);
                updated++;
                log.debug("booking.started bookingId={} startDate={}", booking.getId(), startDate);
            }
        }
        if (updated > 0) {
            log.info("scheduler.startConfirmedTours updated={}", updated);
        }
    }

    /**
     * Transitions STARTED bookings to FINISHED when their tour end date has passed.
     */
    @Scheduled(cron = "0 5 0 * * *")
    public void finishStartedTours() {
        LocalDate today = LocalDate.now();
        List<Booking> started = bookingRepository.findByState(BookingStatus.STARTED);

        int updated = 0;
        for (Booking booking : started) {
            Optional<TourInstance> instanceOpt = resolveInstance(booking);
            if (instanceOpt.isEmpty()) {
                continue;
            }

            LocalDate endDate = instanceOpt.get().getEndDate();
            if (endDate != null && !today.isBefore(endDate)) {
                booking.setState(BookingStatus.FINISHED);
                bookingRepository.save(booking);
                updated++;
                log.debug("booking.finished bookingId={} endDate={}", booking.getId(), endDate);
            }
        }
        if (updated > 0) {
            log.info("scheduler.finishStartedTours updated={}", updated);
        }
    }

    /**
     * Resolves the TourInstance for a booking, preferring {@code tourInstanceId}
     * and falling back to a date-based lookup.
     */
    private Optional<TourInstance> resolveInstance(Booking booking) {
        if (booking.getTourInstanceId() != null) {
            return tourInstanceRepository.findById(booking.getTourInstanceId());
        }
        // Fallback: find by tourId + startDate match
        if (booking.getTourId() == null || booking.getDate() == null) {
            return Optional.empty();
        }
        LocalDate bookingDate = LocalDate.parse(booking.getDate());
        return tourInstanceRepository.findByTourId(booking.getTourId())
                .stream()
                .filter(i -> bookingDate.equals(i.getStartDate()))
                .findFirst();
    }
}
