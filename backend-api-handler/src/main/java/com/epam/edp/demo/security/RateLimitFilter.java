package com.epam.edp.demo.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.epam.edp.demo.util.SecurityUtils;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * IP-based rate limiting filter for auth endpoints.
 * Uses Bucket4j for token bucket per IP with Caffeine-bounded cache.
 *
 * <p><b>Scalability note:</b> This implementation is per-pod (in-memory).
 * In a multi-pod Kubernetes deployment, each pod maintains its own bucket cache.
 * For global rate limiting across all replicas, replace this with a Redis-backed
 * implementation (e.g., bucket4j-redis or Spring Cloud Gateway rate limiter).</p>
 *
 * <p>The Caffeine cache is bounded to prevent memory exhaustion from IP rotation attacks.
 * Entries expire after the rate limit window, and the cache has a max size of 100,000 entries.</p>
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    /** Only rate-limit these auth endpoints (exact match). */
    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/api/v1/auth/sign-in",
            "/api/v1/auth/sign-up"
    );

    /** Rate-limit these user endpoints (pattern match — path contains variables). */
    private static final List<Pattern> RATE_LIMITED_PATTERNS = List.of(
            Pattern.compile("/api/v1/users/[^/]+/email"),
            Pattern.compile("/api/v1/users/[^/]+/password")
    );

    /** Maximum number of distinct IPs tracked. Prevents memory exhaustion from IP rotation attacks. */
    private static final int MAX_CACHE_SIZE = 100_000;
    private static final int SECONDS_PER_MINUTE = 60;

    private final int maxAttempts;
    private final int windowMinutes;
    private final Cache<String, Bucket> bucketCache;
    private final Counter rateLimitExceededCounter;

    public RateLimitFilter(
            @Value("${security.rate-limit.max-attempts:10}") int maxAttempts,
            @Value("${security.rate-limit.window-minutes:15}") int windowMinutes,
            MeterRegistry meterRegistry) {
        this.maxAttempts = maxAttempts;
        this.windowMinutes = windowMinutes;
        this.rateLimitExceededCounter = meterRegistry.counter("auth.ratelimit.exceeded.count");

        // Bounded cache: evicts least-recently-used entries when full,
        // and auto-expires entries after the rate limit window
        this.bucketCache = Caffeine.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .expireAfterAccess(Duration.ofMinutes(windowMinutes))
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (RATE_LIMITED_PATHS.contains(uri)) return false;
        for (Pattern p : RATE_LIMITED_PATTERNS) {
            if (p.matcher(uri).matches()) return false;
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String clientIp = SecurityUtils.extractClientIp(request);
        Bucket bucket = bucketCache.get(clientIp, k -> createBucket());

        if (!bucket.tryConsume(1)) {
            int retryAfter = windowMinutes * SECONDS_PER_MINUTE;
            rateLimitExceededCounter.increment();
            log.warn("auth.ratelimit.exceeded ip={} endpoint={}", clientIp, request.getRequestURI());

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.getWriter().write(
                    "{\"error\":\"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(maxAttempts)
                .refillIntervally(maxAttempts, Duration.ofMinutes(windowMinutes))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}