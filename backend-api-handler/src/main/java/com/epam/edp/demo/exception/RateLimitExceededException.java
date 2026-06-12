package com.epam.edp.demo.exception;

import lombok.Getter;

/**
 * Thrown when IP-based rate limit is exceeded.
 */
@Getter
public class RateLimitExceededException extends RuntimeException {

    private final int retryAfterSeconds;

    public RateLimitExceededException(int retryAfterSeconds) {
        super("Rate limit exceeded");
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
