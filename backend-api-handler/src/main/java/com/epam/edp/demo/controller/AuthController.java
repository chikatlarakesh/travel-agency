package com.epam.edp.demo.controller;

import com.epam.edp.demo.config.JwtConfig;
import com.epam.edp.demo.dto.auth.CompleteRegistrationRequestDTO;
import com.epam.edp.demo.dto.auth.ForgotPasswordRequestDTO;
import com.epam.edp.demo.dto.auth.InitiateRegistrationRequestDTO;
import com.epam.edp.demo.dto.auth.OAuth2CompleteSignupRequestDTO;
import com.epam.edp.demo.dto.auth.ResetPasswordRequestDTO;
import com.epam.edp.demo.dto.auth.SignInRequestDTO;
import com.epam.edp.demo.dto.auth.SignInResponseDTO;
import com.epam.edp.demo.dto.auth.SignUpRequestDTO;
import com.epam.edp.demo.dto.auth.SignUpResponseDTO;
import com.epam.edp.demo.dto.auth.VerifyCodeRequestDTO;
import com.epam.edp.demo.dto.request.LoginRequest;
import com.epam.edp.demo.dto.response.AuthResponse;
import com.epam.edp.demo.dto.response.MessageResponse;
import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.entity.enums.AccountStatus;
import com.epam.edp.demo.exception.BadRequestException;
import com.epam.edp.demo.exception.EmailAlreadyExistsException;
import com.epam.edp.demo.mapper.UserMapper;
import com.epam.edp.demo.oauth2.OnboardingTokenService;
import com.epam.edp.demo.service.AuthService;
import com.epam.edp.demo.service.PasswordResetService;
import com.epam.edp.demo.service.RegistrationService;
import com.epam.edp.demo.service.RoleAssignmentService;
import com.epam.edp.demo.service.TokenService;
import com.epam.edp.demo.repository.UserRepository;
import com.epam.edp.demo.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and session management")
public class AuthController {

    private final AuthService authService;
    private final JwtConfig jwtConfig;
    private final UserMapper userMapper;
    private final PasswordResetService passwordResetService;
    private final RegistrationService registrationService;
    // OAuth2 social login (US-16) — additive fields, do not touch existing logic
    private final OnboardingTokenService onboardingTokenService;
    private final RoleAssignmentService roleAssignmentService;
    private final TokenService tokenService;
    private final UserRepository userRepository;

    @Operation(summary = "Register a new user account")
    @ApiResponse(responseCode = "201", description = "Account created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input or CAPTCHA validation failed")
    @ApiResponse(responseCode = "409", description = "Email already exists")
    @PostMapping("/api/v1/auth/sign-up")
    public ResponseEntity<SignUpResponseDTO> signUp(@Valid @RequestBody SignUpRequestDTO request) {
        log.debug("Processing sign-up request for email={}", SecurityUtils.maskEmail(request.getEmail()));
        SignUpResponseDTO response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Sign in with email and password")
    @ApiResponse(responseCode = "200", description = "Authentication successful")
    @ApiResponse(responseCode = "400", description = "Invalid credentials")
    @ApiResponse(responseCode = "429", description = "Too many requests")
    @PostMapping("/api/v1/auth/sign-in")
    public ResponseEntity<SignInResponseDTO> signIn(
            @Valid @RequestBody SignInRequestDTO request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        LoginRequest loginRequest = new LoginRequest(request.getEmail(), request.getPassword());
        AuthResponse authResponse = authService.login(loginRequest, httpRequest);
        setRefreshTokenCookie(httpResponse, authResponse.getRefreshTokenRaw());
        return ResponseEntity.ok(toSignInResponse(authResponse));
    }

    @Operation(summary = "Refresh access token using refresh token cookie")
    @ApiResponse(responseCode = "200", description = "Tokens refreshed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token")
    @PostMapping("/api/v1/auth/refresh")
    public ResponseEntity<SignInResponseDTO> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse httpResponse) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AuthResponse authResponse = authService.refresh(refreshToken);
        setRefreshTokenCookie(httpResponse, authResponse.getRefreshTokenRaw());
        return ResponseEntity.ok(toSignInResponse(authResponse));
    }

    @Operation(summary = "Logout and invalidate refresh token")
    @ApiResponse(responseCode = "200", description = "Logged out successfully")
    @PostMapping("/api/v1/auth/logout")
    public ResponseEntity<MessageResponse> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse httpResponse) {
        authService.logout(refreshToken);
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, SecurityUtils.createExpiredCookie().toString());
        return ResponseEntity.ok(MessageResponse.of("Logged out successfully"));
    }

    private void setRefreshTokenCookie(HttpServletResponse httpResponse, String rawRefreshToken) {
        ResponseCookie cookie = SecurityUtils.createRefreshTokenCookie(
                rawRefreshToken, jwtConfig.getRefreshTokenExpiry());
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private SignInResponseDTO toSignInResponse(AuthResponse authResponse) {
        return new SignInResponseDTO(
                authResponse.getAccessToken(),
                authResponse.getRole(),
                authResponse.getUserName(),
                authResponse.getEmail()
        );
    }

    // -----------------------------------------------------------------------
    // Email-Verified Registration (3-step flow)
    // -----------------------------------------------------------------------

    @Operation(summary = "Step 1 – Validate email uniqueness and send OTP to the provided address")
    @ApiResponse(responseCode = "200", description = "Verification code sent")
    @ApiResponse(responseCode = "409", description = "Email already in use")
    @PostMapping("/api/v1/auth/initiate-registration")
    public ResponseEntity<MessageResponse> initiateRegistration(
            @Valid @RequestBody InitiateRegistrationRequestDTO request) {
        log.debug("registration.initiate email={}", SecurityUtils.maskEmail(request.getEmail()));
        registrationService.initiateRegistration(
                request.getFirstName(), request.getLastName(), request.getEmail(),
                request.getCaptchaId(), request.getCaptchaAnswer());
        return ResponseEntity.ok(MessageResponse.of(
                "A verification code has been sent to your email address."));
    }

    @Operation(summary = "Step 2 – Validate the 6-digit OTP sent during registration (non-consuming)")
    @ApiResponse(responseCode = "200", description = "Verification code is valid")
    @ApiResponse(responseCode = "400", description = "Invalid or expired verification code")
    @PostMapping("/api/v1/auth/verify-registration-code")
    public ResponseEntity<MessageResponse> verifyRegistrationCode(
            @Valid @RequestBody VerifyCodeRequestDTO request) {
        log.debug("registration.verify-code email={}", SecurityUtils.maskEmail(request.getEmail()));
        registrationService.verifyRegistrationCode(request.getEmail(), request.getVerificationCode());
        return ResponseEntity.ok(MessageResponse.of("Verification code is valid."));
    }

    @Operation(summary = "Step 3 – Set credentials and complete account creation")
    @ApiResponse(responseCode = "201", description = "Account created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid or expired code, or password does not meet requirements")
    @ApiResponse(responseCode = "409", description = "Email already in use")
    @PostMapping("/api/v1/auth/complete-registration")
    public ResponseEntity<SignUpResponseDTO> completeRegistration(
            @Valid @RequestBody CompleteRegistrationRequestDTO request) {
        log.debug("registration.complete email={}", SecurityUtils.maskEmail(request.getEmail()));
        SignUpResponseDTO response = registrationService.completeRegistration(
                request.getEmail(), request.getVerificationCode(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // -----------------------------------------------------------------------
    // Password Recovery (User Story 13)
    // -----------------------------------------------------------------------

    @Operation(summary = "Initiate password recovery – sends a 6-digit OTP to the registered email")
    @ApiResponse(responseCode = "200", description = "If this email exists, a reset link has been sent.")
    @PostMapping("/api/v1/auth/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO request) {
        log.debug("password-reset.forgot-password email={}", SecurityUtils.maskEmail(request.getEmail()));
        passwordResetService.initiatePasswordReset(request.getEmail());
        // Always return 200 to prevent account enumeration.
        return ResponseEntity.ok(MessageResponse.of("If this email exists, a reset link has been sent."));
    }

    @Operation(summary = "Validate the 6-digit OTP sent to the user's email")
    @ApiResponse(responseCode = "200", description = "Verification code validated successfully.")
    @ApiResponse(responseCode = "400", description = "Invalid or expired verification code.")
    @PostMapping("/api/v1/auth/verify-code")
    public ResponseEntity<MessageResponse> verifyCode(
            @Valid @RequestBody VerifyCodeRequestDTO request) {
        log.debug("password-reset.verify-code email={}", SecurityUtils.maskEmail(request.getEmail()));
        passwordResetService.verifyCode(request.getEmail(), request.getVerificationCode());
        return ResponseEntity.ok(MessageResponse.of("Verification code validated successfully."));
    }

    @Operation(summary = "Reset password using the validated OTP")
    @ApiResponse(responseCode = "200", description = "Password reset successfully.")
    @ApiResponse(responseCode = "400", description = "Invalid or expired verification code, or password does not meet requirements.")
    @PostMapping("/api/v1/auth/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request) {
        log.debug("password-reset.reset-password email={}", SecurityUtils.maskEmail(request.getEmail()));
        passwordResetService.resetPassword(
                request.getEmail(),
                request.getVerificationCode(),
                request.getNewPassword());
        return ResponseEntity.ok(MessageResponse.of("Password reset successfully."));
    }

    // -----------------------------------------------------------------------
    // OAuth2 Social Login — Complete Signup (US-16)
    // -----------------------------------------------------------------------

    @Operation(summary = "Complete Google OAuth2 signup for a first-time social login user")
    @ApiResponse(responseCode = "201", description = "Account created and JWT issued")
    @ApiResponse(responseCode = "400", description = "Invalid or expired onboarding token")
    @ApiResponse(responseCode = "409", description = "Email already registered")
    @PostMapping("/api/v1/auth/oauth2/complete-signup")
    public ResponseEntity<SignInResponseDTO> completeOAuth2Signup(
            @Valid @RequestBody OAuth2CompleteSignupRequestDTO request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        log.debug("oauth2.complete-signup.start");

        // 1. Validate the HMAC-signed onboarding token — rejects expired / tampered tokens.
        Map<String, String> payload = onboardingTokenService.validate(request.getOnboardingToken());

        String email      = payload.get("email");
        String provider   = payload.get("provider");
        String providerId = payload.get("providerId");
        String firstName  = payload.get("firstName");
        String lastName   = payload.get("lastName");
        String imageUrl   = payload.get("imageUrl");

        // 2. Guard against race condition: another request may have already created the account.
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Onboarding token is missing the email field.");
        }

        // 3. Determine role using the EXISTING role assignment service.
        String role = roleAssignmentService.determineRole(email);

        // 4. Create the user — passwordHash intentionally absent (OAuth2 account).
        User newUser = User.builder()
                .id(UUID.randomUUID().toString())
                .email(email)
                .firstName(firstName != null ? firstName : "")
                .lastName(lastName  != null ? lastName  : "")
                .provider(provider)
                .providerId(providerId)
                .imageUrl((imageUrl != null && !imageUrl.isBlank()) ? imageUrl : null)
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .failedAttempts(0)
                .build();

        try {
            userRepository.save(newUser);
        } catch (DuplicateKeyException e) {
            log.info("oauth2.complete-signup.concurrent-duplicate email={}",
                    SecurityUtils.maskEmail(email));
            throw new EmailAlreadyExistsException(email);
        }

        // 5. Issue JWT + refresh token using the EXISTING token service.
        String deviceInfo = httpRequest.getHeader("User-Agent");
        AuthResponse authResponse = tokenService.issueTokens(newUser, deviceInfo);
        authResponse.setRole(role);
        authResponse.setUserName((firstName + " " + lastName).trim());
        authResponse.setEmail(email);

        // 6. Set refresh token cookie exactly like regular sign-in.
        ResponseCookie refreshCookie = SecurityUtils.createRefreshTokenCookie(
                authResponse.getRefreshTokenRaw(), jwtConfig.getRefreshTokenExpiry());
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        log.info("oauth2.complete-signup.success email={} role={}",
                SecurityUtils.maskEmail(email), role);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toSignInResponse(authResponse));
    }
}