package com.epam.edp.demo.security;

import com.epam.edp.demo.config.JwtConfig;
import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.entity.enums.AccountStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Date;
import java.util.UUID;

import io.jsonwebtoken.Jwts;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static JwtTokenProvider jwtTokenProvider;
    private static User testUser;
    private static KeyPair keyPair;

    @BeforeAll
    static void setUp() throws Exception {
        // Generate RSA key pair for testing
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        keyPair = kpg.generateKeyPair();

        JwtConfig config = new JwtConfig();
        config.setAccessTokenExpiry(900);
        config.setRsaPrivateKey(keyPair.getPrivate());
        config.setRsaPublicKey(keyPair.getPublic());

        jwtTokenProvider = new JwtTokenProvider(config);


        testUser = User.builder()
                .id("user-123")
                .email("test@example.com")
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("generateAccessToken: produces valid JWT with correct claims")
    void generateAccessToken_containsCorrectClaims() {
        String token = jwtTokenProvider.generateAccessToken(testUser);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo("user-123");
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("validateToken: returns true for valid token")
    void validateToken_validToken_returnsTrue() {
        String token = jwtTokenProvider.generateAccessToken(testUser);
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken: returns false for tampered token")
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtTokenProvider.generateAccessToken(testUser);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtTokenProvider.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("validateToken: returns false for garbage input")
    void validateToken_garbageInput_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    @DisplayName("validateToken: returns false for empty string")
    void validateToken_emptyString_returnsFalse() {
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("getUserIdFromToken: returns correct subject")
    void getUserIdFromToken_returnsCorrectId() {
        String token = jwtTokenProvider.generateAccessToken(testUser);
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo("user-123");
    }

    @Test
    @DisplayName("generateAccessToken: each token has unique jti")
    void generateAccessToken_uniqueJti() {
        String token1 = jwtTokenProvider.generateAccessToken(testUser);
        String token2 = jwtTokenProvider.generateAccessToken(testUser);
        // Different tokens (different jti, iat)
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("validateToken: returns false for expired token")
    void validateToken_expiredToken_returnsFalse() {
        // Manually build a JWT that expired 2 minutes ago (well beyond 30s clock skew leeway)
        Date now = new Date();
        Date past = new Date(now.getTime() - 120_000); // 2 min ago
        Date expired = new Date(now.getTime() - 60_000); // 1 min ago

        String expiredToken = Jwts.builder()
                .issuer("auth-service")
                .subject("user-123")
                .claim("email", "test@example.com")
                .id(UUID.randomUUID().toString())
                .issuedAt(past)
                .expiration(expired)
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("validateToken: token signed with different key → returns false")
    void validateToken_differentKey_returnsFalse() throws Exception {
        // Generate a second key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair otherKeyPair = kpg.generateKeyPair();

        JwtConfig otherConfig = new JwtConfig();
        otherConfig.setAccessTokenExpiry(900);
        otherConfig.setRsaPrivateKey(otherKeyPair.getPrivate());
        otherConfig.setRsaPublicKey(otherKeyPair.getPublic());

        JwtTokenProvider otherProvider = new JwtTokenProvider(otherConfig);
        String tokenFromOtherKey = otherProvider.generateAccessToken(testUser);

        // Validate with original provider (different public key) — should fail
        assertThat(jwtTokenProvider.validateToken(tokenFromOtherKey)).isFalse();
    }

    @Test
    @DisplayName("validateToken: returns false for wrong issuer")
    void validateToken_wrongIssuer_returnsFalse() {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 900_000);

        String wrongIssuerToken = Jwts.builder()
                .issuer("other-service")
                .subject("user-123")
                .claim("email", "test@example.com")
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        assertThat(jwtTokenProvider.validateToken(wrongIssuerToken)).isFalse();
    }

    @Test
    @DisplayName("getRoleFromToken: returns correct role claim")
    void getRoleFromToken_returnsCorrectRole() {
        User userWithRole = User.builder()
                .id("user-123")
                .email("test@example.com")
                .role("TRAVEL_AGENT")
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        String token = jwtTokenProvider.generateAccessToken(userWithRole);
        assertThat(jwtTokenProvider.getRoleFromToken(token)).isEqualTo("TRAVEL_AGENT");
    }
}

