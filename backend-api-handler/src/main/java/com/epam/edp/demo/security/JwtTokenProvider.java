package com.epam.edp.demo.security;

import com.epam.edp.demo.config.JwtConfig;
import com.epam.edp.demo.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    /** Clock leeway to handle pod clock skew in K8s clusters. */
    private static final Duration CLOCK_SKEW_LEEWAY = Duration.ofSeconds(30);

    /** JWT issuer claim — identifies this auth service. Prevents token confusion in multi-service architecture. */
    private static final String ISSUER = "auth-service";

    private static final String EMAIL_CLAIM = "email";

    private final JwtConfig jwtConfig;

    /**
     * Generate a signed RS256 JWT access token.
     * Claims: iss=auth-service, sub=userId, email, iat, exp, jti (unique ID for replay protection).
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtConfig.getAccessTokenExpiry());

        return Jwts.builder()
                .issuer(ISSUER)
                .subject(user.getId())
                .claim(EMAIL_CLAIM, user.getEmail())
                .claim("role", user.getRole())
                .id(UUID.randomUUID().toString())          // jti — replay protection
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(jwtConfig.getRsaPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Validate token signature, expiration, and issuer.
     * Returns true only if token is well-formed, properly signed, not expired, and from this service.
     */
    public boolean validateToken(String token) {
        try {
            parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired", e);
        } catch (SignatureException e) {
            log.warn("JWT signature validation failed", e);
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token", e);
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token", e);
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty", e);
        } catch (JwtException e) {
            // Covers IncorrectClaimException (wrong issuer), MissingClaimException, etc.
            log.warn("JWT validation failed: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Extract userId (subject) from a valid token.
     */
    public String getUserIdFromToken(String token) {
        return parseClaimsJws(token).getPayload().getSubject();
    }

    /**
     * Extract email claim from a valid token.
     */
    public String getEmailFromToken(String token) {
        return parseClaimsJws(token).getPayload().get(EMAIL_CLAIM, String.class);
    }

    /**
     * Extract role claim from a valid token.
     */
    public String getRoleFromToken(String token) {
        return parseClaimsJws(token).getPayload().get("role", String.class);
    }

    private Jws<Claims> parseClaimsJws(String token) {
        return Jwts.parser()
                .verifyWith(jwtConfig.getRsaPublicKey())
                .requireIssuer(ISSUER)
                .clockSkewSeconds(CLOCK_SKEW_LEEWAY.toSeconds())
                .build()
                .parseSignedClaims(token);
    }
}
