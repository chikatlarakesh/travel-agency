package com.epam.edp.demo.service;

import com.epam.edp.demo.entity.PasswordResetToken;
import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.exception.BadRequestException;
import com.epam.edp.demo.repository.PasswordResetTokenRepository;
import com.epam.edp.demo.repository.UserRepository;
import com.epam.edp.demo.util.PasswordValidator;
import com.epam.edp.demo.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates the three-step password recovery flow:
 * <ol>
 *   <li>Initiate – generate a 6-digit OTP and send it via SES.</li>
 *   <li>Verify   – validate the OTP without consuming it.</li>
 *   <li>Reset    – validate the OTP and update the password.</li>
 * </ol>
 *
 * <p><b>Security invariant:</b> the {@code initiatePasswordReset} method always
 * returns successfully regardless of whether the email exists, preventing
 * account enumeration.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    /** OTP validity window. */
    private static final int CODE_EXPIRY_MINUTES = 15;

    /** Upper bound for a 6-digit numeric code (exclusive). */
    private static final int CODE_UPPER_BOUND = 1_000_000;

    private static final String INVALID_CODE_MSG = "Invalid or expired verification code.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;

    private final SecureRandom secureRandom = new SecureRandom();

    // -----------------------------------------------------------------------
    // Step 1 – Initiate password reset
    // -----------------------------------------------------------------------

    /**
     * Sends a 6-digit OTP to the user's registered email address.
     * <p>
     * If the email is not registered, this method returns silently (no error,
     * no email sent) to prevent account enumeration attacks.
     * </p>
     *
     * @param email the email address supplied by the user
     */
    public void initiatePasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            // Silent return – do NOT reveal whether the address is registered.
            log.debug("password-reset.initiate.not-found email={}", SecurityUtils.maskEmail(email));
            return;
        }

        User user = userOpt.get();

        // Invalidate any previous unused tokens for this email.
        tokenRepository.deleteByEmail(email);

        // Generate a cryptographically secure 6-digit OTP.
        String code = String.format("%06d", secureRandom.nextInt(CODE_UPPER_BOUND));
        String codeHash = SecurityUtils.hashToken(code);

        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID().toString())
                .email(email)
                .codeHash(codeHash)
                .expiresAt(Instant.now().plus(CODE_EXPIRY_MINUTES, ChronoUnit.MINUTES))
                .used(false)
                .createdAt(Instant.now())
                .build();

        tokenRepository.save(token);

        try {
            emailService.sendPasswordResetCode(email, user.getFirstName(), code);
            log.info("password-reset.initiate.success email={}", SecurityUtils.maskEmail(email));
        } catch (Exception ex) {
            // Log the failure but NEVER propagate it up:
            // - Prevents account enumeration (caller always gets 200)
            // - Avoids leaking AWS/SES configuration errors to the client
            log.error("password-reset.email.failed email={} error={}",
                    SecurityUtils.maskEmail(email), ex.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Step 2 – Verify OTP (without consuming it)
    // -----------------------------------------------------------------------

    /**
     * Validates the OTP without marking it as used, so the client can then
     * call {@link #resetPassword} in a separate request.
     *
     * @param email            the user's email address
     * @param verificationCode the 6-digit OTP entered by the user
     * @throws BadRequestException if the code is invalid or expired
     */
    public void verifyCode(String email, String verificationCode) {
        findValidToken(email, verificationCode);
        log.info("password-reset.verify-code.success email={}", SecurityUtils.maskEmail(email));
    }

    // -----------------------------------------------------------------------
    // Step 3 – Reset password
    // -----------------------------------------------------------------------

    /**
     * Validates the OTP and, if correct, updates the user's password with
     * a new BCrypt hash. Marks the token as used to prevent replay attacks.
     *
     * @param email            the user's email address
     * @param verificationCode the 6-digit OTP entered by the user
     * @param newPassword      the desired new password (must satisfy complexity rules)
     * @throws BadRequestException if the code is invalid/expired or the password fails validation
     */
    public void resetPassword(String email, String verificationCode, String newPassword) {
        // Validate password complexity before touching the database.
        List<String> passwordErrors = passwordValidator.validate(newPassword);
        if (!passwordErrors.isEmpty()) {
            throw new BadRequestException(String.join("; ", passwordErrors));
        }

        PasswordResetToken token = findValidToken(email, verificationCode);

        // Retrieve the user – the token already proves the email is registered.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException(INVALID_CODE_MSG));

        // Update password hash (BCrypt).
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Consume the token to prevent replay.
        token.setUsed(true);
        tokenRepository.save(token);

        log.info("password-reset.reset.success email={}", SecurityUtils.maskEmail(email));
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Retrieves and validates an active (non-used, non-expired) token that
     * matches the supplied code.
     *
     * @throws BadRequestException for any invalid state (not found, expired, wrong code)
     */
    private PasswordResetToken findValidToken(String email, String verificationCode) {
        PasswordResetToken token = tokenRepository
                .findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BadRequestException(INVALID_CODE_MSG));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            log.debug("password-reset.token.expired email={}", SecurityUtils.maskEmail(email));
            throw new BadRequestException(INVALID_CODE_MSG);
        }

        String expectedHash = SecurityUtils.hashToken(verificationCode);
        if (!expectedHash.equals(token.getCodeHash())) {
            log.debug("password-reset.token.wrong-code email={}", SecurityUtils.maskEmail(email));
            throw new BadRequestException(INVALID_CODE_MSG);
        }

        return token;
    }
}

