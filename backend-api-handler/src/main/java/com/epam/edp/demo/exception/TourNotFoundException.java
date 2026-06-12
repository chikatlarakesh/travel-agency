package com.epam.edp.demo.exception;

public class TourNotFoundException extends RuntimeException {

    public TourNotFoundException(String id) {
        super("Tour not found: " + id);
    }
}
