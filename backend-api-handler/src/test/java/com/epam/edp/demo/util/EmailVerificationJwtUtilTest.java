package com.epam.edp.demo.util;

import com.epam.edp.demo.config.JwtConfig;
import io.jsonwebtoken.Jwts;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(JUnit4.class)
public class EmailVerificationJwtUtilTest {

    private JwtConfig config;
    private EmailVerificationJwtUtil util;

    @Before
    public void setUp() throws Exception {
        config = new JwtConfig();
        config.init();  // generates ephemeral dev key pair
        util = new EmailVerificationJwtUtil(config);
    }

    @Test
    public void generateAndValidate_roundTrip_returnsUserId() {
        String userId = "user-123";
        String email = "user@example.com";

        String token = util.generateVerificationToken(userId, email);
        assertNotNull(token);

        String extractedId = util.validateAndGetUserId(token);
        assertEquals(userId, extractedId);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateAndGetUserId_invalidToken_throwsIllegalArgument() {
        util.validateAndGetUserId("not.a.valid.jwt.token");
    }

    @Test(expected = IllegalArgumentException.class)
    public void validateAndGetUserId_wrongPurpose_throwsIllegalArgument() {
        String token = Jwts.builder()
                .subject("u-1")
                .claim("email", "e@x.com")
                .claim("purpose", "password-reset")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(config.getRsaPrivateKey())
                .compact();

        util.validateAndGetUserId(token);
    }
}
