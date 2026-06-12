package com.epam.edp.demo.exception;

public class OverbookingException extends RuntimeException {

    public OverbookingException(String message) {
        super(message);
    }
}
