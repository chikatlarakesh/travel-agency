package com.epam.edp.demo.exception;

/**
 * Thrown when a requested booking status transition is not permitted by the
 * lifecycle state machine (e.g. BOOKED → STARTED is forbidden).
 */
public class InvalidBookingStateException extends RuntimeException {

    public InvalidBookingStateException(String message) {
        super(message);
    }

    public InvalidBookingStateException(Object fromState, Object toState) {
        super("Cannot transition booking from " + fromState + " to " + toState);
    }
}
