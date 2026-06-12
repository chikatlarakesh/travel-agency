package com.epam.edp.demo.exception;

public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(String bookingId) {
        super("Booking not found: " + bookingId);
    }
}
