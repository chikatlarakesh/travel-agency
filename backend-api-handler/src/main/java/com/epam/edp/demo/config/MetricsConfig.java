package com.epam.edp.demo.config;

import org.springframework.context.annotation.Configuration;

/**
 * Metrics configuration for auth-related observability.
 *
 * <p>Custom Micrometer counters are registered directly in the services that use them
 * (TokenService, RateLimitFilter, AuthService) via MeterRegistry injection.
 * This avoids orphaned bean declarations and keeps metric ownership co-located with logic.</p>
 *
 * <p>Metrics exposed via /actuator/prometheus for Grafana/Alertmanager:
 * <ul>
 *   <li>auth.login.success.count — successful logins</li>
 *   <li>auth.login.failure.count — failed login attempts</li>
 *   <li>auth.account.locked.count — account lockout events</li>
 *   <li>auth.token.refresh.count — token refresh operations</li>
 *   <li>auth.token.reuse.detected.count — refresh token reuse detections (possible theft)</li>
 *   <li>auth.ratelimit.exceeded.count — rate limit violations</li>
 * </ul></p>
 */
@Configuration
public class MetricsConfig {
    // Counters are registered inline in services via MeterRegistry.counter(...)
    // No orphaned beans — each service owns its metrics.
}
