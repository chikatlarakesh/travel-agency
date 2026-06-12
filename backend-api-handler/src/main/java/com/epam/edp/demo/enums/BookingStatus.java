package com.epam.edp.demo.enums;

/**
 * Lifecycle states of a booking.
 *
 * Transition flow (happy path):
 *   BOOKED → CONFIRMED → STARTED → FINISHED
 *
 * Any state may transition to CANCELED subject to business rules
 * (e.g. freeCancellationDays on the associated TourInstance).
 */
public enum BookingStatus {
    /** Reservation created; awaiting confirmation. */
    BOOKED,

    /** Confirmed by the travel agent or system. */
    CONFIRMED,

    /** Tour has commenced. */
    STARTED,

    /** Tour has concluded successfully. */
    FINISHED,

    /** Booking was canceled by guest or agent. */
    CANCELED
}
