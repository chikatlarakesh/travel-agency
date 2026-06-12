package com.epam.edp.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtTokenProvider);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("valid Bearer token → sets SecurityContext authentication")
    void doFilter_validBearerToken_setsAuthentication() throws ServletException, IOException {
        request.setRequestURI("/api/protected");
        request.addHeader("Authorization", "Bearer valid-jwt-token");

        when(jwtTokenProvider.validateToken("valid-jwt-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid-jwt-token")).thenReturn("user-123");
        when(jwtTokenProvider.getRoleFromToken("valid-jwt-token")).thenReturn("CUSTOMER");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo("user-123");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority").containsExactly("ROLE_CUSTOMER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("missing Authorization header → passes through unauthenticated")
    void doFilter_missingAuthHeader_continuesUnauthenticated() throws ServletException, IOException {
        request.setRequestURI("/api/protected");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("invalid token → passes through unauthenticated")
    void doFilter_invalidToken_continuesUnauthenticated() throws ServletException, IOException {
        request.setRequestURI("/api/protected");
        request.addHeader("Authorization", "Bearer invalid-token");

        when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("non-Bearer prefix (e.g., 'Token xxx') → skipped, passes through")
    void doFilter_nonBearerPrefix_continuesUnauthenticated() throws ServletException, IOException {
        request.setRequestURI("/api/protected");
        request.addHeader("Authorization", "Token some-token");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("shouldNotFilter: /api/v1/auth/sign-in → true (skipped)")
    void shouldNotFilter_loginPath_returnsTrue() {
        request.setRequestURI("/api/v1/auth/sign-in");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter: /api/v1/auth/sign-up → true (skipped)")
    void shouldNotFilter_registerPath_returnsTrue() {
        request.setRequestURI("/api/v1/auth/sign-up");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter: /api/v1/auth/refresh → true (skipped)")
    void shouldNotFilter_refreshPath_returnsTrue() {
        request.setRequestURI("/api/v1/auth/refresh");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter: /api/v1/auth/logout → true (skipped)")
    void shouldNotFilter_logoutPath_returnsTrue() {
        request.setRequestURI("/api/v1/auth/logout");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter: /api/v1/tours/available → false (filter runs; GET is permitAll via SecurityConfig)")
    void shouldNotFilter_toursPath_returnsFalse() {
        // Tours paths are NO LONGER skipped so that POST /tours/{id}/feedbacks can receive
        // an authenticated principal. GET endpoints remain open via SecurityConfig.permitAll().
        request.setRequestURI("/api/v1/tours/available");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    @DisplayName("shouldNotFilter: /api/v1/tours/{id}/feedbacks → false (filter runs to set auth for feedback)")
    void shouldNotFilter_feedbacksPath_returnsFalse() {
        request.setRequestURI("/api/v1/tours/tour-1/feedbacks");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    @DisplayName("tours path with valid Bearer token → authentication is set in SecurityContext")
    void doFilter_toursPathWithValidToken_setsAuthentication() throws ServletException, IOException {
        request.setRequestURI("/api/v1/tours/tour-1/feedbacks");
        request.addHeader("Authorization", "Bearer valid-jwt-token");

        when(jwtTokenProvider.validateToken("valid-jwt-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid-jwt-token")).thenReturn("user-42");
        when(jwtTokenProvider.getRoleFromToken("valid-jwt-token")).thenReturn("CUSTOMER");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo("user-42");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("tours path without token → passes through unauthenticated (GET endpoints are permitAll)")
    void doFilter_toursPathNoToken_continuesUnauthenticated() throws ServletException, IOException {
        request.setRequestURI("/api/v1/tours/available");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).validateToken(anyString());
    }

    @Test
    @DisplayName("shouldNotFilter: /actuator/health → true (skipped)")
    void shouldNotFilter_actuatorPath_returnsTrue() {
        request.setRequestURI("/actuator/health");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter: /api/protected → false (filtered)")
    void shouldNotFilter_protectedPath_returnsFalse() {
        request.setRequestURI("/api/protected");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }
}

