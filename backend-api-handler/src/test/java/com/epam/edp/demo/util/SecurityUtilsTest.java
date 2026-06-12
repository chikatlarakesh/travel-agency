package com.epam.edp.demo.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityUtilsTest {

    @Test
    @DisplayName("hashToken: produces consistent SHA-256 hex output")
    void hashToken_consistentOutput() {
        String hash1 = SecurityUtils.hashToken("my-token");
        String hash2 = SecurityUtils.hashToken("my-token");
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 = 32 bytes = 64 hex chars
    }

    @Test
    @DisplayName("hashToken: different inputs produce different hashes")
    void hashToken_differentInputs() {
        assertThat(SecurityUtils.hashToken("token-a"))
                .isNotEqualTo(SecurityUtils.hashToken("token-b"));
    }

    @Test
    @DisplayName("maskEmail: masks correctly")
    void maskEmail_masksCorrectly() {
        assertThat(SecurityUtils.maskEmail("user@example.com")).isEqualTo("u***@example.com");
        assertThat(SecurityUtils.maskEmail("ab@x.co")).isEqualTo("a***@x.co");
    }

    @Test
    @DisplayName("maskEmail: handles edge cases")
    void maskEmail_edgeCases() {
        assertThat(SecurityUtils.maskEmail(null)).isEqualTo("***");
        assertThat(SecurityUtils.maskEmail("noemail")).isEqualTo("***");
        assertThat(SecurityUtils.maskEmail("a@x.com")).isEqualTo("***@x.com");
    }

    @Test
    @DisplayName("extractClientIp: uses X-Forwarded-For when from trusted proxy")
    void extractClientIp_usesXFF() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.50, 70.41.3.18");
        request.setRemoteAddr("10.0.0.1"); // Trusted proxy (K8s internal)

        assertThat(SecurityUtils.extractClientIp(request)).isEqualTo("203.0.113.50");
    }

    @Test
    @DisplayName("extractClientIp: ignores X-Forwarded-For from untrusted source (spoofing prevention)")
    void extractClientIp_ignoresXffFromUntrustedSource() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "1.2.3.4");
        request.setRemoteAddr("203.0.113.99"); // Public IP — not a trusted proxy

        // Should return remoteAddr, NOT the spoofed XFF value
        assertThat(SecurityUtils.extractClientIp(request)).isEqualTo("203.0.113.99");
    }

    @Test
    @DisplayName("extractClientIp: falls back to remoteAddr when no XFF header")
    void extractClientIp_fallback() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");

        assertThat(SecurityUtils.extractClientIp(request)).isEqualTo("192.168.1.100");
    }

    @Test
    @DisplayName("createRefreshTokenCookie: sets security attributes")
    void createRefreshTokenCookie_securityAttributes() {
        ResponseCookie cookie = SecurityUtils.createRefreshTokenCookie("test-token", 604800);

        assertThat(cookie.getName()).isEqualTo("refreshToken");
        assertThat(cookie.getValue()).isEqualTo("test-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(604800);
    }

    @Test
    @DisplayName("createExpiredCookie: clears cookie with maxAge=0")
    void createExpiredCookie_clearsCorrectly() {
        ResponseCookie cookie = SecurityUtils.createExpiredCookie();

        assertThat(cookie.getName()).isEqualTo("refreshToken");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(0);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
    }
}

