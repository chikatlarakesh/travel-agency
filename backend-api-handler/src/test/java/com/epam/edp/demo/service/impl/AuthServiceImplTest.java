package com.epam.edp.demo.service.impl;

import com.epam.edp.demo.dto.auth.SignUpRequestDTO;
import com.epam.edp.demo.dto.auth.SignUpResponseDTO;
import com.epam.edp.demo.enums.UserRole;
import com.epam.edp.demo.exception.BadRequestException;
import com.epam.edp.demo.exception.EmailAlreadyExistsException;
import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.repository.UserRepository;
import com.epam.edp.demo.service.AccountLockService;
import com.epam.edp.demo.service.AuthService;
import com.epam.edp.demo.service.CaptchaService;
import com.epam.edp.demo.service.RoleAssignmentService;
import com.epam.edp.demo.service.TokenService;
import com.epam.edp.demo.util.PasswordValidator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AccountLockService accountLockService;
    @Mock private TokenService tokenService;
    @Mock private RoleAssignmentService roleAssignmentService;
    @Mock private CaptchaService captchaService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder,
                accountLockService, tokenService, new PasswordValidator(), new SimpleMeterRegistry(),
                roleAssignmentService, captchaService);
    }

    // ── register ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: success returns confirmation message")
    void register_success_returnsConfirmationMessage() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongP1!")).thenReturn("hashed");
        when(roleAssignmentService.determineRole("john@example.com")).thenReturn(UserRole.CUSTOMER.name());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        SignUpResponseDTO response = authService.register(
                signUpRequest("John", "Doe", "john@example.com", "StrongP1!"));

        assertThat(response.getMessage()).isEqualTo("Account created successfully");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.CUSTOMER.name());
    }

    @Test
    @DisplayName("register: travel-agent email is assigned TRAVEL_AGENT role")
    void register_travelAgentEmail_assignsTravelAgentRole() {
        when(userRepository.existsByEmail("agent@travelagency.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongP1!")).thenReturn("hashed");
        when(roleAssignmentService.determineRole("agent@travelagency.com")).thenReturn(UserRole.TRAVEL_AGENT.name());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        authService.register(signUpRequest("Alice", "Agent", "agent@travelagency.com", "StrongP1!"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.TRAVEL_AGENT.name());
    }

    @Test
    @DisplayName("register: regular email is assigned CUSTOMER role")
    void register_regularEmail_assignsCustomerRole() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongP1!")).thenReturn("hashed");
        when(roleAssignmentService.determineRole("user@example.com")).thenReturn(UserRole.CUSTOMER.name());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        authService.register(signUpRequest("Bob", "Smith", "user@example.com", "StrongP1!"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.CUSTOMER.name());
    }

    @Test
    @DisplayName("register: duplicate email throws EmailAlreadyExistsException")
    void register_emailAlreadyExists_throwsConflict() {
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                signUpRequest("A", "B", "dup@example.com", "StrongP1!")))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    @DisplayName("register: missing first name throws BadRequestException")
    void register_missingFirstName_throwsBadRequest() {
        assertThatThrownBy(() -> authService.register(
                signUpRequest("", "Doe", "a@b.com", "StrongP1!")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("register: missing email throws BadRequestException")
    void register_missingEmail_throwsBadRequest() {
        assertThatThrownBy(() -> authService.register(
                signUpRequest("John", "Doe", null, "StrongP1!")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("register: missing password throws BadRequestException")
    void register_missingPassword_throwsBadRequest() {
        assertThatThrownBy(() -> authService.register(
                signUpRequest("John", "Doe", "a@b.com", "")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("register: weak password throws BadRequestException")
    void register_weakPassword_throwsBadRequest() {
        assertThatThrownBy(() -> authService.register(
                signUpRequest("John", "Doe", "john@example.com", "weak")))
                .isInstanceOf(BadRequestException.class);
    }


    // ── helpers ───────────────────────────────────────────────────────────

    private SignUpRequestDTO signUpRequest(String first, String last, String email, String password) {
        SignUpRequestDTO r = new SignUpRequestDTO();
        r.setFirstName(first);
        r.setLastName(last);
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }
}
