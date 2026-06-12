package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.auth.SignUpRequestDTO;
import com.epam.edp.demo.dto.request.LoginRequest;
import com.epam.edp.demo.dto.response.AuthResponse;
import com.epam.edp.demo.exception.AuthFailedException;
import com.epam.edp.demo.exception.BadRequestException;
import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.entity.enums.AccountStatus;
import com.epam.edp.demo.repository.UserRepository;
import com.epam.edp.demo.util.PasswordValidator;
import com.epam.edp.demo.service.CaptchaService;
import com.epam.edp.demo.service.RoleAssignmentService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {

    @Mock private UserRepository userRepository;
    @Mock private AccountLockService accountLockService;
    @Mock private TokenService tokenService;
    @Mock private HttpServletRequest httpRequest;
    @Mock private RoleAssignmentService roleAssignmentService;
    @Mock private CaptchaService captchaService;

    private AuthService authService;
    private PasswordEncoder passwordEncoder;

    private static final String RAW_PASSWORD = "TestPassword1!";

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(12);
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        PasswordValidator passwordValidator = new PasswordValidator();
        authService = new AuthService(userRepository, passwordEncoder, accountLockService,
                tokenService, passwordValidator, meterRegistry, roleAssignmentService, captchaService);

        lenient().when(httpRequest.getHeader("User-Agent")).thenReturn("TestAgent");
        lenient().when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    private User createUser() {
        return User.builder()
                .id("user-1")
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode(RAW_PASSWORD))
                .accountStatus(AccountStatus.ACTIVE)
                .failedAttempts(0)
                .build();
    }

    @Test
    @DisplayName("login: valid credentials returns tokens")
    void login_validCredentials_returnsTokens() {
        User user = createUser();
        LoginRequest request = new LoginRequest("test@example.com", RAW_PASSWORD);
        AuthResponse expectedResponse = AuthResponse.builder()
                .accessToken("jwt").tokenType("Bearer").expiresIn(900).build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(accountLockService.isLocked(user)).thenReturn(false);
        when(tokenService.issueTokens(any(), anyString())).thenReturn(expectedResponse);
        when(userRepository.save(any())).thenReturn(user);

        AuthResponse result = authService.login(request, httpRequest);

        assertThat(result.getAccessToken()).isEqualTo("jwt");
        verify(accountLockService).resetFailedAttempts(user);
        verify(tokenService).issueTokens(eq(user), anyString());
    }

    @Test
    @DisplayName("login: wrong password throws AuthFailed and increments failed attempts")
    void login_wrongPassword_throwsAndIncrements() {
        User user = createUser();
        LoginRequest request = new LoginRequest("test@example.com", "WrongPass1!");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(AuthFailedException.class);

        verify(accountLockService).recordFailedAttempt(user);
        verify(tokenService, never()).issueTokens(any(), anyString());
    }

    @Test
    @DisplayName("login: non-existent user throws AuthFailed (constant time via DUMMY_HASH)")
    void login_nonExistentUser_throwsConstantTime() {
        LoginRequest request = new LoginRequest("nobody@example.com", "SomePass1!");

        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(AuthFailedException.class);

        // Should NOT attempt to record failed attempt for non-existent user
        verify(accountLockService, never()).recordFailedAttempt(any());
    }

    @Test
    @DisplayName("login: locked account throws same AuthFailed (no info leakage)")
    void login_lockedAccount_throwsSameError() {
        User user = createUser();
        LoginRequest request = new LoginRequest("test@example.com", RAW_PASSWORD);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(accountLockService.isLocked(user)).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(AuthFailedException.class);

       
        verify(tokenService, never()).issueTokens(any(), anyString());
    }

    @Test
    @DisplayName("login: DISABLED account throws same AuthFailed (no info leakage)")
    void login_disabledAccount_throwsSameError() {
        User user = createUser();
        user.setAccountStatus(AccountStatus.DISABLED);
        LoginRequest request = new LoginRequest("test@example.com", RAW_PASSWORD);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(accountLockService.isLocked(user)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, httpRequest))
                .isInstanceOf(AuthFailedException.class);

        // Should NOT issue tokens for disabled account
        verify(tokenService, never()).issueTokens(any(), anyString());
    }

    @Test
    @DisplayName("login: expired lock allows login")
    void login_lockExpired_allowsLogin() {
        User user = createUser();
        LoginRequest request = new LoginRequest("test@example.com", RAW_PASSWORD);
        AuthResponse expectedResponse = AuthResponse.builder()
                .accessToken("jwt").tokenType("Bearer").expiresIn(900).build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(accountLockService.isLocked(user)).thenReturn(false); // Lock expired, auto-reset
        when(tokenService.issueTokens(any(), anyString())).thenReturn(expectedResponse);
        when(userRepository.save(any())).thenReturn(user);

        AuthResponse result = authService.login(request, httpRequest);
        assertThat(result.getAccessToken()).isEqualTo("jwt");
    }

    @Test
    @DisplayName("login: success resets failed attempts counter")
    void login_success_resetsCounter() {
        User user = createUser();
        user.setFailedAttempts(3);
        LoginRequest request = new LoginRequest("test@example.com", RAW_PASSWORD);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(accountLockService.isLocked(user)).thenReturn(false);
        when(tokenService.issueTokens(any(), anyString()))
                .thenReturn(AuthResponse.builder().accessToken("jwt").expiresIn(900).build());
        when(userRepository.save(any())).thenReturn(user);

        authService.login(request, httpRequest);

        verify(accountLockService).resetFailedAttempts(user);
    }



    @Test
    @DisplayName("login: success updates lastLoginAt timestamp")
    void login_success_updatesLastLoginAt() {
        User user = createUser();
        LoginRequest request = new LoginRequest("test@example.com", RAW_PASSWORD);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(accountLockService.isLocked(user)).thenReturn(false);
        when(tokenService.issueTokens(any(), anyString()))
                .thenReturn(AuthResponse.builder().accessToken("jwt").expiresIn(900).build());
        when(userRepository.save(any())).thenReturn(user);

        authService.login(request, httpRequest);

        assertThat(user.getLastLoginAt()).isNotNull();
        verify(userRepository).save(user);
    }

    // ========================
    // LOGOUT
    // ========================

    @Test
    @DisplayName("logout: valid token delegates to tokenService.revokeRefreshToken")
    void logout_validToken_delegatesToTokenService() {
        authService.logout("some-refresh-token");
        verify(tokenService).revokeRefreshToken("some-refresh-token");
    }

    @Test
    @DisplayName("logout: null token does not call revokeRefreshToken")
    void logout_nullToken_doesNotThrow() {
        authService.logout(null);
        verify(tokenService, never()).revokeRefreshToken(any());
    }

    @Test
    @DisplayName("logout: blank token does not call revokeRefreshToken")
    void logout_blankToken_doesNotThrow() {
        authService.logout("   ");
        verify(tokenService, never()).revokeRefreshToken(any());
    }

    // ========================
    // REFRESH
    // ========================

    @Test
    @DisplayName("refresh: delegates to tokenService.refreshTokens")
    void refresh_delegatesToTokenService() {
        AuthResponse expected = AuthResponse.builder().accessToken("new-jwt").expiresIn(900).build();
        when(tokenService.refreshTokens("raw-token")).thenReturn(expected);

        AuthResponse result = authService.refresh("raw-token");

        assertThat(result.getAccessToken()).isEqualTo("new-jwt");
        verify(tokenService).refreshTokens("raw-token");
    }

    // ========================
    // REGISTRATION — password policy tests
    // ========================

    private SignUpRequestDTO createSignUpRequest(String password) {
        SignUpRequestDTO req = new SignUpRequestDTO();
        req.setFirstName("Test");
        req.setLastName("User");
        req.setEmail("newuser@example.com");
        req.setPassword(password);
        return req;
    }

    @Test
    @DisplayName("register: weak password (no uppercase) throws BadRequest")
    void register_noUppercase_throwsBadRequest() {
        assertThatThrownBy(() -> authService.register(createSignUpRequest("abcdefg1!")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("uppercase");
    }

    @Test
    @DisplayName("register: weak password (too short) throws BadRequest")
    void register_tooShort_throwsBadRequest() {
        assertThatThrownBy(() -> authService.register(createSignUpRequest("Ab1!")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least 8");
    }

    @Test
    @DisplayName("register: weak password (no special char) throws BadRequest")
    void register_noSpecialChar_throwsBadRequest() {
        assertThatThrownBy(() -> authService.register(createSignUpRequest("Abcdefg1")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("special character");
    }

    @Test
    @DisplayName("register: weak password (no digit) throws BadRequest")
    void register_noDigit_throwsBadRequest() {
        assertThatThrownBy(() -> authService.register(createSignUpRequest("Abcdefgh!")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("digit");
    }

    @Test
    @DisplayName("register: invalid email format throws BadRequest")
    void register_invalidEmailFormat_throwsBadRequest() {
        SignUpRequestDTO req = createSignUpRequest("StrongP1!");
        req.setEmail("notanemail");
        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid email format");
    }

    @Test
    @DisplayName("register: valid data succeeds")
    void register_validData_succeeds() {
        SignUpRequestDTO req = createSignUpRequest("StrongP1!");
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = authService.register(req);
        assertThat(result.getMessage()).contains("Account created successfully");
        verify(userRepository).save(any(User.class));
    }
}

