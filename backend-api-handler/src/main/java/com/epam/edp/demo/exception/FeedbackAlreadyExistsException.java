package com.epam.edp.demo.exception;

public class FeedbackAlreadyExistsException extends RuntimeException {

    public FeedbackAlreadyExistsException() {
        super("You have already submitted feedback for this tour");
    }
}
