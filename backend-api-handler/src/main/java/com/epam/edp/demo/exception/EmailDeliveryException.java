package com.epam.edp.demo.exception;

/**
 * Thrown when a transactional email cannot be delivered (e.g. SES is unavailable or
 * rejects the request). Maps to HTTP 503 Service Unavailable so the client can retry.
 */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}

