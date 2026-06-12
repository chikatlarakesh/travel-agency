package com.epam.edp.demo.security;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimitFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        // 3 max attempts in 15 min window — low limit for testing
        filter = new RateLimitFilter(3, 15, new SimpleMeterRegistry());
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("under limit → request passes through")
    void doFilter_underLimit_allowsRequest() throws ServletException, IOException {
        request.setRequestURI("/api/v1/auth/sign-in");
        request.setRemoteAddr("10.0.0.1");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("exceed limit → returns 429 with Retry-After header and JSON body")
    void doFilter_exceedsLimit_returns429WithRetryAfter() throws ServletException, IOException {
        request.setRequestURI("/api/v1/auth/sign-in");
        request.setRemoteAddr("10.0.0.2");

        // Exhaust the bucket (3 attempts)
        for (int i = 0; i < 3; i++) {
            MockHttpServletResponse okResponse = new MockHttpServletResponse();
            filter.doFilterInternal(request, okResponse, filterChain);
        }

        // 4th attempt should be blocked
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        FilterChain blockedChain = mock(FilterChain.class);
        filter.doFilterInternal(request, blockedResponse, blockedChain);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
        assertThat(blockedResponse.getHeader("Retry-After")).isEqualTo("900"); // 15 * 60
        assertThat(blockedResponse.getContentAsString()).contains("Too many requests");
        // The blocked chain should NOT have been invoked
        verify(blockedChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("non-rate-limited path → passes through regardless")
    void shouldNotFilter_nonAuthPath_returnsTrue() {
        request.setRequestURI("/api/v1/auth/refresh");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter: /api/v1/auth/sign-in → false (rate limited)")
    void shouldNotFilter_loginPath_returnsFalse() {
        request.setRequestURI("/api/v1/auth/sign-in");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    @DisplayName("shouldNotFilter: /api/v1/auth/sign-up → false (rate limited)")
    void shouldNotFilter_registerPath_returnsFalse() {
        request.setRequestURI("/api/v1/auth/sign-up");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    @DisplayName("different IPs get separate rate limit buckets")
    void doFilter_differentIps_separateBuckets() throws ServletException, IOException {
        request.setRequestURI("/api/v1/auth/sign-in");

        // Exhaust bucket for IP-A
        request.setRemoteAddr("10.0.0.10");
        for (int i = 0; i < 3; i++) {
            filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);
        }

        // IP-A is now blocked
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilterInternal(request, blockedResponse, filterChain);
        assertThat(blockedResponse.getStatus()).isEqualTo(429);

        // IP-B should still pass
        request.setRemoteAddr("10.0.0.11");
        MockHttpServletResponse allowedResponse = new MockHttpServletResponse();
        filter.doFilterInternal(request, allowedResponse, filterChain);
        assertThat(allowedResponse.getStatus()).isEqualTo(200);
    }
}

