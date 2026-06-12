package com.epam.edp.demo.oauth2;

import com.epam.edp.demo.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Issues and validates short-lived, HMAC-SHA256-signed onboarding tokens.
 *
 * <h3>Purpose</h3>
 * When a new user authenticates via Google but does not yet have an account, the success
 * handler cannot create an account immediately (per requirements).  Instead, it generates
 * an onboarding token that encodes the user's Google-provided information ({@code email},
 * {@code provider}, {@code providerId}, {@code firstName}, {@code lastName},
 * {@code imageUrl}, {@code expiry}).
 *
 * <p>The token is returned to the frontend in the redirect URL.  The frontend presents it
 * to {@code POST /api/v1/auth/oauth2/complete-signup}, which validates the token and
 * creates the final account.
 *
 * <h3>Token format</h3>
 * <pre>
 *   BASE64URL(payload-json) + "." + BASE64URL(HMAC-SHA256(payload-bytes, secret))
 * </pre>
 * The payload is a simple {@code key=value} pipe-delimited string, avoiding JSON
 * parsing complexity and keeping the token compact.
 *
 * <h3>Security properties</h3>
 * <ul>
 *   <li>Tamper detection: HMAC signature verification rejects any modification.</li>
 *   <li>Time-limited: includes an {@code expiry} epoch second, checked on validation.</li>
 *   <li>No DB storage needed: stateless, suitable for horizontally-scaled deployments.</li>
 * </ul>
 *
 * <h3>Impact on existing code</h3>
 * None — this service is a new bean injected only into
 * {@link OAuth2AuthenticationSuccessHandler} and {@link com.epam.edp.demo.controller.AuthController}.
 */
@Slf4j
@Service
public class OnboardingTokenService {

    /** HMAC algorithm used for signing. */
    private static final String HMAC_ALGO = "HmacSHA256";

    /** Delimiter used between payload pairs.  Must not appear in values. */
    private static final String DELIM = "|";

    /** Token time-to-live in seconds (10 minutes). */
    private static final long TTL_SECONDS = 600L;

    private static final String KEY_EMAIL      = "email";
    private static final String KEY_PROVIDER   = "provider";
    private static final String KEY_PROVIDER_ID = "providerId";
    private static final String KEY_FIRST_NAME = "firstName";
    private static final String KEY_LAST_NAME  = "lastName";
    private static final String KEY_IMAGE_URL  = "imageUrl";
    private static final String KEY_EXPIRY     = "expiry";

    @Value("${app.oauth2.onboarding.token-secret:dev-onboarding-secret-change-in-prod}")
    private String tokenSecret;

    // ------------------------------------------------------------------ //
    //  Public API                                                          //
    // ------------------------------------------------------------------ //

    /**
     * Generates a signed onboarding token encoding the given user attributes.
     *
     * @param email      Google-verified email address
     * @param provider   registration ID in uppercase (e.g. {@code "GOOGLE"})
     * @param providerId provider's stable user identifier (Google {@code sub})
     * @param firstName  given name from Google profile
     * @param lastName   family name from Google profile
     * @param imageUrl   profile picture URL (may be null)
     * @return opaque token string safe for URL query parameters
     */
    public String generate(String email, String provider, String providerId,
                           String firstName, String lastName, String imageUrl) {
        long expiry = Instant.now().plusSeconds(TTL_SECONDS).getEpochSecond();

        String payload = buildPayload(email, provider, providerId,
                firstName, lastName, imageUrl, expiry);

        String payloadB64  = b64(payload.getBytes(StandardCharsets.UTF_8));
        String signature   = sign(payloadB64);

        log.debug("oauth2.onboarding_token.generated email={}", maskEmail(email));
        return payloadB64 + "." + signature;
    }

    /**
     * Validates the onboarding token and returns its decoded payload as a map.
     *
     * @param token the token string returned from {@link #generate}
     * @return map with keys: email, provider, providerId, firstName, lastName, imageUrl
     * @throws BadRequestException if the token is malformed, has an invalid signature, or is expired
     */
    public Map<String, String> validate(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Onboarding token is required.");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            log.warn("oauth2.onboarding_token.malformed");
            throw new BadRequestException("Invalid onboarding token.");
        }

        String payloadB64 = parts[0];
        String providedSig = parts[1];
        String expectedSig  = sign(payloadB64);

        if (!constantTimeEquals(expectedSig, providedSig)) {
            log.warn("oauth2.onboarding_token.invalid_signature");
            throw new BadRequestException("Invalid onboarding token.");
        }

        Map<String, String> data = parsePayload(
                new String(Base64.getUrlDecoder().decode(payloadB64), StandardCharsets.UTF_8));

        long expiry = Long.parseLong(data.getOrDefault(KEY_EXPIRY, "0"));
        if (Instant.now().getEpochSecond() > expiry) {
            log.warn("oauth2.onboarding_token.expired email={}",
                    maskEmail(data.getOrDefault(KEY_EMAIL, "?")));
            throw new BadRequestException("Onboarding token has expired. Please sign in with Google again.");
        }

        data.remove(KEY_EXPIRY); // don't expose internal expiry to callers
        return data;
    }

    // ------------------------------------------------------------------ //
    //  Private helpers                                                     //
    // ------------------------------------------------------------------ //

    private String buildPayload(String email, String provider, String providerId,
                                String firstName, String lastName, String imageUrl, long expiry) {
        // LinkedHashMap preserves insertion order for deterministic serialization.
        Map<String, String> map = new LinkedHashMap<>();
        map.put(KEY_EMAIL,       nullSafe(email));
        map.put(KEY_PROVIDER,    nullSafe(provider));
        map.put(KEY_PROVIDER_ID, nullSafe(providerId));
        map.put(KEY_FIRST_NAME,  nullSafe(firstName));
        map.put(KEY_LAST_NAME,   nullSafe(lastName));
        map.put(KEY_IMAGE_URL,   nullSafe(imageUrl));
        map.put(KEY_EXPIRY,      String.valueOf(expiry));

        StringBuilder sb = new StringBuilder();
        map.forEach((k, v) -> sb.append(k).append("=").append(v).append(DELIM));
        return sb.toString();
    }

    private Map<String, String> parsePayload(String payload) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String pair : payload.split("\\|")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                map.put(pair.substring(0, idx), pair.substring(idx + 1));
            }
        }
        return map;
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(
                    tokenSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return b64(raw);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 signing failed", e);
        }
    }

    private static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }

    /** Constant-time string comparison to prevent timing-based signature oracle attacks. */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        return email.charAt(0) + "***" + email.substring(email.indexOf('@'));
    }
}

