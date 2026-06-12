package com.epam.edp.demo.captcha;

import lombok.Value;

import java.time.Instant;

/**
 * Immutable holder for a pending CAPTCHA challenge.
 * Stored in the Caffeine cache keyed by captchaId.
 * Caffeine enforces TTL expiry; {@code expiresAt} is kept as a
 * defence-in-depth guard (e.g. against lazy eviction edge cases).
 */
@Value
public class CaptchaEntry {
    String answer;
    Instant expiresAt;
}
