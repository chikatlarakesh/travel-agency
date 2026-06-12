package com.epam.edp.demo.exception;

/**
 * Thrown when CAPTCHA validation fails during registration.
 * Extends {@link BadRequestException} so it maps to HTTP 400 via the
 * global handler, but has its own dedicated handler for richer logging.
 */
public class CaptchaValidationException extends BadRequestException {

    public CaptchaValidationException(String message) {
        super(message);
    }
}
