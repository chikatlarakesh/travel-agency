package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.auth.SignUpResponseDTO;
import com.epam.edp.demo.entity.RegistrationToken;
import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.entity.enums.AccountStatus;
import com.epam.edp.demo.service.CaptchaService;
import com.epam.edp.demo.exception.BadRequestException;
import com.epam.edp.demo.exception.EmailAlreadyExistsException;
import com.epam.edp.demo.exception.EmailDeliveryException;
import com.epam.edp.demo.repository.RegistrationTokenRepository;
import com.epam.edp.demo.repository.UserRepository;
import com.epam.edp.demo.util.PasswordValidator;
import com.epam.edp.demo.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the three-step email-verified registration flow:
 * <ol>
 *   <li>Initiate  – validate email uniqueness, generate a 6-digit OTP and send it.</li>
 *   <li>Verify    – validate the OTP without consuming it (optional pre-check).</li>
 *   <li>Complete  – validate the OTP, set credentials, and create the user account.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    /** OTP validity window (minutes). */
    private static final int CODE_EXPIRY_MINUTES = 15;

    /** Upper bound for a 6-digit numeric code (exclusive). */
    private static final int CODE_UPPER_BOUND = 1_000_000;

    private static final String INVALID_CODE_MSG = "Invalid or expired verification code.";

    private final UserRepository userRepository;
    private final RegistrationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final RoleAssignmentService roleAssignmentService;
    private final CaptchaService captchaService;

    private final SecureRandom secureRandom = new SecureRandom();

    // -----------------------------------------------------------------------
    // Step 1 – Initiate registration
    // -----------------------------------------------------------------------

    /**
     * Validates the email is not already taken, then sends a 6-digit OTP to it.
     *
     * @param firstName prospective user's first name
     * @param lastName  prospective user's last name
     * @param email     email address to verify
     * @throws EmailAlreadyExistsException if the address is already registered
     */
    public void initiateRegistration(String firstName, String lastName, String email,
                                      String captchaId, String captchaAnswer) {
        captchaService.validate(captchaId, captchaAnswer);
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        // Invalidate any previous pending tokens for this email.
        tokenRepository.deleteByEmail(email);

        // Generate a cryptographically secure 6-digit OTP.
        String code = String.format("%06d", secureRandom.nextInt(CODE_UPPER_BOUND));
        String codeHash = SecurityUtils.hashToken(code);

        RegistrationToken token = RegistrationToken.builder()
                .id(UUID.randomUUID().toString())
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .codeHash(codeHash)
                .expiresAt(Instant.now().plus(CODE_EXPIRY_MINUTES, ChronoUnit.MINUTES))
                .used(false)
                .createdAt(Instant.now())
                .build();

        tokenRepository.save(token);

        try {
            emailService.sendRegistrationVerificationCode(email, firstName, code);
            log.info("registration.initiate.otp-sent email={}", SecurityUtils.maskEmail(email));
        } catch (Exception ex) {
            log.error("registration.email.failed email={} error={}",
                    SecurityUtils.maskEmail(email), ex.getMessage());
            throw new EmailDeliveryException("Failed to send verification email to " + SecurityUtils.maskEmail(email), ex);
        }
    }

    // -----------------------------------------------------------------------
    // Step 2 – Verify OTP (without consuming it)
    // -----------------------------------------------------------------------

    /**
     * Validates the OTP without marking it as used.
     *
     * @param email            the prospective user's email
     * @param verificationCode the 6-digit OTP entered by the user
     * @throws BadRequestException if the code is invalid or expired
     */
    public void verifyRegistrationCode(String email, String verificationCode) {
        findValidToken(email, verificationCode);
        log.info("registration.verify-code.success email={}", SecurityUtils.maskEmail(email));
    }

    // -----------------------------------------------------------------------
    // Step 3 – Complete registration
    // -----------------------------------------------------------------------

    /**
     * Validates the OTP, creates the user account, and marks the token as used.
     *
     * @param email            verified email address
     * @param verificationCode the 6-digit OTP entered by the user
     * @param password         desired password (must satisfy complexity rules)
     * @return confirmation message DTO
     * @throws BadRequestException         if the code is invalid/expired or password fails validation
     * @throws EmailAlreadyExistsException if a race condition caused a duplicate account
     */
    public SignUpResponseDTO completeRegistration(String email, String verificationCode, String password) {
        // Validate password complexity before touching the database.
        List<String> passwordErrors = passwordValidator.validate(password);
        if (!passwordErrors.isEmpty()) {
            throw new BadRequestException(String.join("; ", passwordErrors));
        }

        RegistrationToken token = findValidToken(email, verificationCode);

        // Double-check email uniqueness (handles race conditions between step 1 and step 3).
        if (userRepository.existsByEmail(email)) {
            token.setUsed(true);
            tokenRepository.save(token);
            throw new EmailAlreadyExistsException(email);
        }

        String role = roleAssignmentService.determineRole(email);

        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .firstName(token.getFirstName())
                .lastName(token.getLastName())
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .failedAttempts(0)
                .build();

        try {
            userRepository.save(user);
        } catch (DuplicateKeyException e) {
            log.info("registration.concurrent-duplicate email={}", SecurityUtils.maskEmail(email), e);
            throw new EmailAlreadyExistsException(email);
        }

        // Consume the token to prevent replay.
        token.setUsed(true);
        tokenRepository.save(token);

        log.info("registration.complete.success email={}", SecurityUtils.maskEmail(email));
        return new SignUpResponseDTO("Account created successfully");
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private RegistrationToken findValidToken(String email, String verificationCode) {
        RegistrationToken token = tokenRepository
                .findTopByEmailAndUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BadRequestException(INVALID_CODE_MSG));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            log.debug("registration.token.expired email={}", SecurityUtils.maskEmail(email));
            throw new BadRequestException(INVALID_CODE_MSG);
        }

        String expectedHash = SecurityUtils.hashToken(verificationCode);
        if (!expectedHash.equals(token.getCodeHash())) {
            log.debug("registration.token.wrong-code email={}", SecurityUtils.maskEmail(email));
            throw new BadRequestException(INVALID_CODE_MSG);
        }

        return token;
    }
}

