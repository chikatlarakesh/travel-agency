package com.epam.edp.demo.service;

import com.epam.edp.demo.config.JwtConfig;
import com.epam.edp.demo.dto.response.AuthResponse;
import com.epam.edp.demo.exception.AuthFailedException;
import com.epam.edp.demo.entity.RefreshToken;
import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.repository.RefreshTokenRepository;
import com.epam.edp.demo.repository.UserRepository;
import com.epam.edp.demo.security.JwtTokenProvider;
import com.epam.edp.demo.util.SecurityUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Manages JWT access tokens and opaque refresh tokens.
 * Implements token rotation with replay detection and atomic revocation.
 */
@Slf4j
@Service
public class TokenService {

    private static final String FIELD_REVOKED = "revoked";

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtConfig jwtConfig;
    private final MongoTemplate mongoTemplate;

    // Metrics
    private final Counter tokenRefreshCounter;
    private final Counter tokenReuseDetectedCounter;

    public TokenService(JwtTokenProvider jwtTokenProvider,
                        RefreshTokenRepository refreshTokenRepository,
                        UserRepository userRepository,
                        JwtConfig jwtConfig,
                        MongoTemplate mongoTemplate,
                        MeterRegistry meterRegistry) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtConfig = jwtConfig;
        this.mongoTemplate = mongoTemplate;
        this.tokenRefreshCounter = meterRegistry.counter("auth.token.refresh.count");
        this.tokenReuseDetectedCounter = meterRegistry.counter("auth.token.reuse.detected.count");
    }

    /**
     * Issue a new access token + refresh token pair.
     * The refresh token is stored as a SHA-256 hash in MongoDB — never raw.
     */
    public AuthResponse issueTokens(User user, String deviceInfo) {
        // Generate JWT access token
        String accessToken = jwtTokenProvider.generateAccessToken(user);

        // Generate opaque refresh token
        String rawRefreshToken = UUID.randomUUID().toString();
        String tokenHash = SecurityUtils.hashToken(rawRefreshToken);

        // Persist hashed refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(tokenHash)
                .userId(user.getId())
                .deviceInfo(deviceInfo)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtConfig.getRefreshTokenExpiry()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtConfig.getAccessTokenExpiry())
                .refreshTokenRaw(rawRefreshToken)  // For HttpOnly cookie — excluded from JSON
                .build();
    }

    /**
     * Refresh tokens with rotation and replay detection.
     *
     * <p>Uses atomic findAndModify to prevent race conditions:
     * Only one concurrent request can successfully consume a refresh token.
     * The second request will find the token already revoked → triggers reuse detection.</p>
     *
     * <p>If a revoked token is presented, it means either:
     *   - An attacker stole and used a token that was already rotated, or
     *   - A legitimate user is trying a token that an attacker already used.
     * In both cases, revoke the ENTIRE token family for this user — force full re-auth.</p>
     */
    public AuthResponse refreshTokens(String rawRefreshToken) {
        String tokenHash = SecurityUtils.hashToken(rawRefreshToken);

        // Atomic operation: find token where hash matches AND revoked=false, then set revoked=true.
        // If two concurrent requests race, only one gets the non-null result.
        RefreshToken consumed = atomicConsumeToken(tokenHash);

        if (consumed == null) {
            // Token was not found with revoked=false. Either it doesn't exist, is already revoked, or was just consumed.
            // Check if it exists at all to distinguish "not found" from "reuse detected"
            RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash).orElse(null);

            if (existing == null) {
                log.warn("auth.token.refresh.notfound");
                throw new AuthFailedException();
            }

            // Token exists but is already revoked — REPLAY/REUSE DETECTED
            log.error("auth.token.reuse.detected userId={}", existing.getUserId());
            tokenReuseDetectedCounter.increment();
            revokeAllUserTokens(existing.getUserId());
            throw new AuthFailedException();
        }

        // Expired check (belt-and-suspenders — TTL index should handle, but be defensive)
        if (consumed.getExpiresAt().isBefore(Instant.now())) {
            log.debug("auth.token.refresh.expired userId={}", consumed.getUserId());
            throw new AuthFailedException();
        }

        // Fetch user
        User user = userRepository.findById(consumed.getUserId())
                .orElseThrow(() -> {
                    log.error("auth.token.refresh.user.notfound userId={}", consumed.getUserId());
                    return new AuthFailedException();
                });

        // Issue new token pair
        AuthResponse newTokens = issueTokens(user, consumed.getDeviceInfo());

        // Link old token to new one (audit trail)
        consumed.setReplacedByTokenHash(SecurityUtils.hashToken(newTokens.getRefreshTokenRaw()));
        refreshTokenRepository.save(consumed);

        tokenRefreshCounter.increment();
        log.debug("auth.token.refresh.success userId={}", user.getId());
        return newTokens;
    }

    /**
     * Atomically consume a refresh token: finds by hash where revoked=false and sets revoked=true.
     * Returns the token BEFORE modification (with revoked=false), or null if no match.
     * This prevents race conditions where two concurrent requests both succeed.
     */
    private RefreshToken atomicConsumeToken(String tokenHash) {
        Query query = new Query(Criteria.where("tokenHash").is(tokenHash)
                .and(FIELD_REVOKED).is(false));
        Update update = new Update().set(FIELD_REVOKED, true);
        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(false);

        return mongoTemplate.findAndModify(query, update, options, RefreshToken.class);
    }

    /**
     * Revoke a single refresh token (used on logout).
     */
    public void revokeRefreshToken(String rawRefreshToken) {
        String tokenHash = SecurityUtils.hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    /**
     * Revoke ALL active refresh tokens for a user.
     * Called on replay detection — nuclear option to protect the account.
     */
    public void revokeAllUserTokens(String userId) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        activeTokens.forEach(token -> token.setRevoked(true));
        refreshTokenRepository.saveAll(activeTokens);
        log.warn("auth.token.family.revoked userId={} count={}", userId, activeTokens.size());
    }
}

