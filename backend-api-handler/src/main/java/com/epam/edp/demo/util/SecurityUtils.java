package com.epam.edp.demo.util;

import com.epam.edp.demo.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public final class SecurityUtils {

    /** Bearer token prefix used in the Authorization header. Shared constant prevents duplication. */
    public static final String BEARER_PREFIX = "Bearer ";

    /** Cookie name used for storing the refresh token. */
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    /** SameSite policy for refresh token cookie. */
    private static final String SAME_SITE_STRICT = "Strict";

    /** Path scope for the refresh token cookie. */
    private static final String AUTH_PATH = "/api/v1/auth";

    /** RFC 1918 private network prefixes + loopback addresses. */
    private static final List<String> TRUSTED_PROXY_PREFIXES = List.of(
            "10.", "192.168.", "127.0.0.1", "::1",
            "172.16.", "172.17.", "172.18.", "172.19.",
            "172.20.", "172.21.", "172.22.", "172.23.",
            "172.24.", "172.25.", "172.26.", "172.27.",
            "172.28.", "172.29.", "172.30.", "172.31."
    );

    private SecurityUtils() {}

    /**
     * Returns the authenticated user's ID from the current security context.
     * The principal is set as the userId string by JwtAuthenticationFilter.
     */
    public static String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("Authentication required");
        }
        return authentication.getPrincipal().toString();
    }

    /**
     * Computes SHA-256 hex hash of the given token string.
     */
    public static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Masks an email for safe logging: "user@example.com" → "u***@example.com"
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + email.substring(atIndex);
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    /**
     * Extracts real client IP, trusting X-Forwarded-For only from private/trusted proxies.
     */
    public static String extractClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank() && isTrustedProxy(remoteAddr)) {
            return xff.split(",")[0].trim();
        }
        return remoteAddr;
    }

    private static boolean isTrustedProxy(String ip) {
        if (ip == null) {
            return false;
        }
        return TRUSTED_PROXY_PREFIXES.stream()
                .anyMatch(prefix -> ip.startsWith(prefix) || ip.equals(prefix));
    }

    /**
     * Creates a secure HttpOnly cookie for the refresh token.
     */
    public static ResponseCookie createRefreshTokenCookie(String token, int maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(true)
                .sameSite(SAME_SITE_STRICT)
                .path(AUTH_PATH)
                .maxAge(maxAgeSeconds)
                .build();
    }

    /**
     * Creates an expired cookie to clear the refresh token on logout.
     */
    public static ResponseCookie createExpiredCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite(SAME_SITE_STRICT)
                .path(AUTH_PATH)
                .maxAge(0)
                .build();
    }
}
