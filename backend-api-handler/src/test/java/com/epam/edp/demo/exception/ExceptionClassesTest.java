package com.epam.edp.demo.exception;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@RunWith(JUnit4.class)
public class ExceptionClassesTest {

    // ── AuthFailedException ──────────────────────────────────────────────────

    @Test
    public void authFailedException_defaultMessage() {
        AuthFailedException ex = new AuthFailedException();
        assertEquals("Authentication failed", ex.getMessage());
    }

    // ── BookingNotFoundException ─────────────────────────────────────────────

    @Test
    public void bookingNotFoundException_containsId() {
        BookingNotFoundException ex = new BookingNotFoundException("b-123");
        assertNotNull(ex.getMessage());
        assertEquals("Booking not found: b-123", ex.getMessage());
    }

    // ── CancellationNotAllowedException ─────────────────────────────────────

    @Test
    public void cancellationNotAllowedException_setsMessage() {
        CancellationNotAllowedException ex = new CancellationNotAllowedException("too late");
        assertEquals("too late", ex.getMessage());
    }

    // ── OverbookingException ─────────────────────────────────────────────────

    @Test
    public void overbookingException_setsMessage() {
        OverbookingException ex = new OverbookingException("no slots");
        assertEquals("no slots", ex.getMessage());
    }

    // ── RateLimitExceededException ───────────────────────────────────────────

    @Test
    public void rateLimitExceededException_setsRetryAfterAndMessage() {
        RateLimitExceededException ex = new RateLimitExceededException(60);
        assertEquals("Rate limit exceeded", ex.getMessage());
        assertEquals(60, ex.getRetryAfterSeconds());
    }

    // ── BadRequestException ──────────────────────────────────────────────────

    @Test
    public void badRequestException_messageOnly() {
        BadRequestException ex = new BadRequestException("invalid input");
        assertEquals("invalid input", ex.getMessage());
    }

    @Test
    public void badRequestException_messageAndCause() {
        Throwable cause = new RuntimeException("root cause");
        BadRequestException ex = new BadRequestException("invalid", cause);
        assertEquals("invalid", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    // ── UnauthorizedException ────────────────────────────────────────────────

    @Test
    public void unauthorizedException_setsMessage() {
        UnauthorizedException ex = new UnauthorizedException("not allowed");
        assertEquals("not allowed", ex.getMessage());
    }
}
