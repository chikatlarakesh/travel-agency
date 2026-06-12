package com.epam.edp.demo.exception;

public class AuthFailedException extends RuntimeException {

    public AuthFailedException() {
        super("Authentication failed");
    }
}
