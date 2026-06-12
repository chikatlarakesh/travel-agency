package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.auth.SignUpRequestDTO;
import com.epam.edp.demo.dto.auth.SignUpResponseDTO;
import com.epam.edp.demo.dto.request.LoginRequest;
import com.epam.edp.demo.dto.response.AuthResponse;
import com.epam.edp.demo.exception.AuthFailedException;
import com.epam.edp.demo.exception.BadRequestException;
import com.epam.edp.demo.exception.EmailAlreadyExistsException;
import com.epam.edp.demo.service.CaptchaService;
import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.entity.enums.AccountStatus;
import com.epam.edp.demo.repository.UserRepository;
import com.epam.edp.demo.util.PasswordValidator;
import com.epam.edp.demo.util.SecurityUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Core authentication service handling registration, login, and logout.
 * Implements timing-attack mitigation and account lockout protection.
 */
@Slf4j
@Service
public class AuthService {

    private static final String DUMMY_HASH =
            "$2a$12$LJ3m4sMKfJzM0pXoRlbzWOSJcVf6IXHWdG8vJxIE3VqPqKfGhR6mO";
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountLockService accountLockService;
    private final TokenService tokenService;
    private final PasswordValidator passwordValidator;
    private final RoleAssignmentService roleAssignmentService;
    private final CaptchaService captchaService;
    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter accountLockedCounter;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AccountLockService accountLockService,
                       TokenService tokenService,
                       PasswordValidator passwordValidator,
                       MeterRegistry meterRegistry,
                       RoleAssignmentService roleAssignmentService,
                       CaptchaService captchaService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountLockService = accountLockService;
        this.tokenService = tokenService;
        this.passwordValidator = passwordValidator;
        this.roleAssignmentService = roleAssignmentService;
        this.captchaService = captchaService;
        this.loginSuccessCounter = meterRegistry.counter("auth.login.success.count");
        this.loginFailureCounter = meterRegistry.counter("auth.login.failure.count");
        this.accountLockedCounter = meterRegistry.counter("auth.account.locked.count");
    }

    /**
     * Register a new user (POST /api/v1/auth/sign-up).
     * Defense-in-depth: validates even though controller uses @Valid.
     */
    public SignUpResponseDTO register(SignUpRequestDTO request) {
        validateSignUpRequest(request);
        captchaService.validate(request.getCaptchaId(), request.getCaptchaAnswer());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        String role = roleAssignmentService.determineRole(request.getEmail());

        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .failedAttempts(0)
                .build();

        try {
            userRepository.save(user);
        } catch (DuplicateKeyException e) {
            // Race condition: concurrent registration with same email
            log.info("auth.register.concurrent email={}", SecurityUtils.maskEmail(request.getEmail()), e);
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        log.info("auth.register.success email={}", SecurityUtils.maskEmail(request.getEmail()));
        return new SignUpResponseDTO("Account created successfully");
    }


    /**
     * Authenticate user and issue JWT tokens.
     * Implements constant-time comparison to prevent timing attacks.
     */
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = SecurityUtils.extractClientIp(httpRequest);
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        // Always compare password even if user not found (timing attack mitigation)
        String hashToCheck = (user != null) ? user.getPasswordHash() : DUMMY_HASH;
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), hashToCheck);

        if (user == null || !passwordMatches) {
            handleLoginFailure(user, request.getEmail(), clientIp);
            throw new AuthFailedException();
        }

        if (accountLockService.isLocked(user)) {
            accountLockedCounter.increment();
            log.warn("auth.login.locked userId={} ip={}", user.getId(), clientIp);
            throw new AuthFailedException();
        }

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            log.warn("auth.login.disabled userId={} status={} ip={}", user.getId(), user.getAccountStatus(), clientIp);
            throw new AuthFailedException();
        }

        return completeLogin(user, httpRequest, clientIp);
    }

    /**
     * Invalidate refresh token on logout.
     */
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            tokenService.revokeRefreshToken(rawRefreshToken);
        }
        log.info("auth.logout.success");
    }

    /**
     * Refresh access token using refresh token rotation.
     */
    public AuthResponse refresh(String rawRefreshToken) {
        return tokenService.refreshTokens(rawRefreshToken);
    }

    private void handleLoginFailure(User user, String email, String clientIp) {
        if (user != null) {
            accountLockService.recordFailedAttempt(user);
        }
        loginFailureCounter.increment();
        log.warn("auth.login.failure email={} ip={}", SecurityUtils.maskEmail(email), clientIp);
    }

    private AuthResponse completeLogin(User user, HttpServletRequest httpRequest, String clientIp) {
        accountLockService.resetFailedAttempts(user);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String deviceInfo = httpRequest.getHeader("User-Agent");
        AuthResponse response = tokenService.issueTokens(user, deviceInfo);
        response.setRole(user.getRole());
        response.setUserName(user.getFirstName() + " " + user.getLastName());
        response.setEmail(user.getEmail());

        loginSuccessCounter.increment();
        log.info("auth.login.success userId={} ip={}", user.getId(), clientIp);
        return response;
    }


    private void validateSignUpRequest(SignUpRequestDTO request) {
        requireNonBlank(request.getFirstName(), "First name");
        requireNonBlank(request.getLastName(), "Last name");
        requireNonBlank(request.getEmail(), "Email");
        if (!request.getEmail().matches(EMAIL_REGEX)) {
            throw new BadRequestException("Invalid email format");
        }
        requireNonBlank(request.getPassword(), "Password");
        List<String> passwordErrors = passwordValidator.validate(request.getPassword());
        if (!passwordErrors.isEmpty()) {
            throw new BadRequestException(String.join("; ", passwordErrors));
        }
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " is required");
        }
    }
}

