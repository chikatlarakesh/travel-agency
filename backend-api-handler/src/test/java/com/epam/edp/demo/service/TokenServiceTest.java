package com.epam.edp.demo.service;

import com.epam.edp.demo.config.JwtConfig;
import com.epam.edp.demo.dto.response.AuthResponse;
import com.epam.edp.demo.exception.AuthFailedException;
import com.epam.edp.demo.entity.RefreshToken;
import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.entity.enums.AccountStatus;
import com.epam.edp.demo.repository.RefreshTokenRepository;
import com.epam.edp.demo.repository.UserRepository;
import com.epam.edp.demo.security.JwtTokenProvider;
import com.epam.edp.demo.util.SecurityUtils;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private JwtConfig jwtConfig;
    @Mock private MongoTemplate mongoTemplate;

    private TokenService tokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(jwtTokenProvider, refreshTokenRepository, userRepository,
                jwtConfig, mongoTemplate, new SimpleMeterRegistry());
        testUser = User.builder()
                .id("user-1")
                .email("test@example.com")
                .passwordHash("$2a$12$hash")
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("issueTokens: generates JWT and stores hashed refresh token")
    void issueTokens_generatesJwtAndStoresHash() {
        when(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("jwt-access-token");
        when(jwtConfig.getAccessTokenExpiry()).thenReturn(900);
        when(jwtConfig.getRefreshTokenExpiry()).thenReturn(604800);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = tokenService.issueTokens(testUser, "Chrome/Windows");

        assertThat(response.getAccessToken()).isEqualTo("jwt-access-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(900);
        assertThat(response.getRefreshTokenRaw()).isNotBlank();

        // Verify stored token hash is NOT the raw token
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.getTokenHash()).isNotEqualTo(response.getRefreshTokenRaw());
        assertThat(saved.getTokenHash()).isEqualTo(SecurityUtils.hashToken(response.getRefreshTokenRaw()));
        assertThat(saved.getUserId()).isEqualTo("user-1");
        assertThat(saved.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("refreshTokens: valid token issues new pair and revokes old atomically")
    void refreshTokens_validToken_issuesNewPairAndRevokesOld() {
        String rawToken = "old-refresh-token";
        String tokenHash = SecurityUtils.hashToken(rawToken);

        RefreshToken stored = RefreshToken.builder()
                .tokenHash(tokenHash)
                .userId("user-1")
                .deviceInfo("Chrome")
                .issuedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .expiresAt(Instant.now().plus(6, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        // Atomic findAndModify returns the token (before modification)
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(RefreshToken.class))).thenReturn(stored);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken(testUser)).thenReturn("new-jwt");
        when(jwtConfig.getAccessTokenExpiry()).thenReturn(900);
        when(jwtConfig.getRefreshTokenExpiry()).thenReturn(604800);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = tokenService.refreshTokens(rawToken);

        assertThat(response.getAccessToken()).isEqualTo("new-jwt");
        // The stored token should have replacedByTokenHash set
        assertThat(stored.getReplacedByTokenHash()).isNotNull();
    }

    @Test
    @DisplayName("refreshTokens: revoked token triggers reuse detection and revokes family")
    void refreshTokens_revokedToken_detectsReuse_revokesFamily() {
        String rawToken = "reused-token";
        String tokenHash = SecurityUtils.hashToken(rawToken);

        RefreshToken stored = RefreshToken.builder()
                .tokenHash(tokenHash)
                .userId("user-1")
                .revoked(true) // Already revoked — reuse!
                .build();

        // Atomic findAndModify returns null (no match: token already revoked)
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(RefreshToken.class))).thenReturn(null);
        // Lookup finds the token exists but is revoked
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.findByUserIdAndRevokedFalse("user-1")).thenReturn(List.of());

        assertThatThrownBy(() -> tokenService.refreshTokens(rawToken))
                .isInstanceOf(AuthFailedException.class);

        // Verify family revocation was attempted
        verify(refreshTokenRepository).findByUserIdAndRevokedFalse("user-1");
    }

    @Test
    @DisplayName("refreshTokens: expired token throws AuthFailedException")
    void refreshTokens_expiredToken_throws() {
        String rawToken = "expired-token";
        String tokenHash = SecurityUtils.hashToken(rawToken);

        RefreshToken stored = RefreshToken.builder()
                .tokenHash(tokenHash)
                .userId("user-1")
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        // Atomic consume succeeds (token was not yet revoked)
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(RefreshToken.class))).thenReturn(stored);

        assertThatThrownBy(() -> tokenService.refreshTokens(rawToken))
                .isInstanceOf(AuthFailedException.class);
    }

    @Test
    @DisplayName("refreshTokens: unknown token throws AuthFailedException")
    void refreshTokens_notFound_throws() {
        // Atomic findAndModify returns null
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(RefreshToken.class))).thenReturn(null);
        // Token not found at all
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.refreshTokens("unknown"))
                .isInstanceOf(AuthFailedException.class);
    }

    @Test
    @DisplayName("revokeRefreshToken: marks token as revoked")
    void revokeRefreshToken_marksRevoked() {
        String rawToken = "token-to-revoke";
        String hash = SecurityUtils.hashToken(rawToken);
        RefreshToken stored = RefreshToken.builder().tokenHash(hash).revoked(false).build();

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        tokenService.revokeRefreshToken(rawToken);

        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    @DisplayName("revokeAllUserTokens: revokes all non-revoked tokens")
    void revokeAllUserTokens_revokesAll() {
        RefreshToken t1 = RefreshToken.builder().tokenHash("h1").userId("user-1").revoked(false).build();
        RefreshToken t2 = RefreshToken.builder().tokenHash("h2").userId("user-1").revoked(false).build();

        when(refreshTokenRepository.findByUserIdAndRevokedFalse("user-1")).thenReturn(List.of(t1, t2));

        tokenService.revokeAllUserTokens("user-1");

        assertThat(t1.isRevoked()).isTrue();
        assertThat(t2.isRevoked()).isTrue();
        verify(refreshTokenRepository).saveAll(List.of(t1, t2));
    }

    @Test
    @DisplayName("refreshTokens: user deleted from DB throws AuthFailedException")
    void refreshTokens_userDeleted_throws() {
        String rawToken = "valid-token";
        String tokenHash = SecurityUtils.hashToken(rawToken);

        RefreshToken stored = RefreshToken.builder()
                .tokenHash(tokenHash)
                .userId("deleted-user")
                .expiresAt(Instant.now().plus(6, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(RefreshToken.class))).thenReturn(stored);
        when(userRepository.findById("deleted-user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.refreshTokens(rawToken))
                .isInstanceOf(AuthFailedException.class);
    }

    @Test
    @DisplayName("revokeRefreshToken: non-existent token does not throw")
    void revokeRefreshToken_nonExistent_doesNotThrow() {
        String rawToken = "nonexistent-token";
        String hash = SecurityUtils.hashToken(rawToken);

        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.empty());

        // Should not throw — graceful no-op
        tokenService.revokeRefreshToken(rawToken);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("revokeAllUserTokens: no active tokens handles gracefully")
    void revokeAllUserTokens_noActiveTokens_handlesGracefully() {
        when(refreshTokenRepository.findByUserIdAndRevokedFalse("user-1")).thenReturn(List.of());

        tokenService.revokeAllUserTokens("user-1");

        verify(refreshTokenRepository).saveAll(List.of());
    }
}

