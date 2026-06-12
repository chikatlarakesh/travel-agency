package com.epam.edp.demo.util;

import com.epam.edp.demo.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Generates and verifies JWT tokens used for email verification.
 * Token contains: userId as subject, purpose claim = "email-verification", 24h expiry.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerificationJwtUtil {

    private static final long EXPIRY_MS = 24 * 60 * 60 * 1000L; // 24 hours
    private static final String PURPOSE = "email-verification";
    private static final String CLAIM_PURPOSE = "purpose";

    private final JwtConfig jwtConfig;

    /**
     * Generate a JWT verification token for the given userId and email.
     */
    public String generateVerificationToken(String userId, String email) {
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim(CLAIM_PURPOSE, PURPOSE)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRY_MS))
                .signWith(jwtConfig.getRsaPrivateKey())
                .compact();
    }

    /**
     * Validate and parse the token. Returns the userId (subject) if valid.
     * Throws IllegalArgumentException if invalid or expired.
     */
    public String validateAndGetUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtConfig.getRsaPublicKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String purpose = claims.get(CLAIM_PURPOSE, String.class);
            if (!PURPOSE.equals(purpose)) {
                throw new IllegalArgumentException("Token is not an email verification token");
            }

            return claims.getSubject();
        } catch (JwtException e) {
            log.debug("Email verification token invalid: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid or expired verification token");
        }
    }
}

