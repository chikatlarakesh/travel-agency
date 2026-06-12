package com.epam.edp.demo.controller;

import com.epam.edp.demo.config.JacksonConfig;
import com.epam.edp.demo.config.JwtConfig;
import com.epam.edp.demo.dto.auth.SignUpResponseDTO;
import com.epam.edp.demo.dto.response.AuthResponse;
import com.epam.edp.demo.exception.AuthFailedException;
import com.epam.edp.demo.exception.EmailAlreadyExistsException;
import com.epam.edp.demo.exception.GlobalExceptionHandler;
import com.epam.edp.demo.mapper.UserMapper;
import com.epam.edp.demo.security.JwtAuthenticationFilter;
import com.epam.edp.demo.security.JwtTokenProvider;
import com.epam.edp.demo.security.RateLimitFilter;
import com.epam.edp.demo.service.AuthService;
import com.epam.edp.demo.service.PasswordResetService;
import com.epam.edp.demo.service.RegistrationService;
import com.epam.edp.demo.oauth2.OnboardingTokenService;
import com.epam.edp.demo.service.RoleAssignmentService;
import com.epam.edp.demo.service.TokenService;
import com.epam.edp.demo.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AuthController.class, excludeAutoConfiguration = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, UserMapper.class, JacksonConfig.class})
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtConfig jwtConfig;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitFilter rateLimitFilter;

    @MockBean
    private PasswordResetService passwordResetService;

    @MockBean
    private RegistrationService registrationService;

    @MockBean
    private OnboardingTokenService onboardingTokenService;

    @MockBean
    private RoleAssignmentService roleAssignmentService;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void signIn_validCredentials_returns200WithToken() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("test-jwt-token")
                .role("CUSTOMER")
                .userName("Jhonson Doe")
                .email("jhonson_doe@nomail.com")
                .refreshTokenRaw("refresh-token")
                .build();
        when(authService.login(any(), any())).thenReturn(authResponse);
        when(jwtConfig.getRefreshTokenExpiry()).thenReturn(604800);

        mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"jhonson_doe@nomail.com\",\"password\":\"Password1!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idToken").isNotEmpty())
                .andExpect(jsonPath("$.email").value("jhonson_doe@nomail.com"));
    }

    @Test
    void signIn_wrongPassword_returns400() throws Exception {
        when(authService.login(any(), any())).thenThrow(new AuthFailedException());

        mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"jhonson_doe@nomail.com\",\"password\":\"WrongPass1!\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signIn_unknownEmail_returns400() throws Exception {
        when(authService.login(any(), any())).thenThrow(new AuthFailedException());

        mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"SomePass1!\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signIn_weakPassword_returns400WithValidationMessage() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"palash@gmail.com\",\"password\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void signIn_numericPassword_returns400WithTypeMessage() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"palash@gmail.com\",\"password\":123456}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("password must be a valid string."));
    }

    @Test
    void signIn_missingEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"ValidP1!ss\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signUp_newEmail_returns201() throws Exception {
        when(authService.register(any())).thenReturn(new SignUpResponseDTO("User registered successfully"));

        mockMvc.perform(post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Alice\",\"lastName\":\"Test\","
                                + "\"email\":\"alice.test.integration@nomail.com\","
                                + "\"password\":\"AliceP1!ss\","
                                + "\"captchaId\":\"test-id\",\"captchaAnswer\":\"TEST\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void signUp_duplicateEmail_returns409() throws Exception {
        when(authService.register(any())).thenThrow(new EmailAlreadyExistsException("jhonson_doe@nomail.com"));

        mockMvc.perform(post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Dup\",\"lastName\":\"User\","
                                + "\"email\":\"jhonson_doe@nomail.com\","
                                + "\"password\":\"DupPass1!\","
                                + "\"captchaId\":\"test-id\",\"captchaAnswer\":\"TEST\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void signUp_missingFirstName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastName\":\"User\","
                                + "\"email\":\"new.user2@nomail.com\","
                                + "\"password\":\"ValidP1!ss\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signUp_weakPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Weak\",\"lastName\":\"User\","
                                + "\"email\":\"weak.pwd@nomail.com\","
                                + "\"password\":\"abc\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signUp_invalidEmailFormat_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Bad\",\"lastName\":\"Email\","
                                + "\"email\":\"not-an-email\","
                                + "\"password\":\"ValidP1!ss\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signUp_numericFirstNameToken_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":123,\"lastName\":\"User\","
                                + "\"email\":\"num.firstname@nomail.com\","
                                + "\"password\":\"ValidP1!ss\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.details[0]").value("firstName must be of type string"));
    }

    @Test
    void signUp_numericLastNameToken_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Alice\",\"lastName\":456,"
                                + "\"email\":\"num.lastname@nomail.com\","
                                + "\"password\":\"ValidP1!ss\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.details[0]").value("lastName must be of type string"));
    }

    @Test
    void refresh_validCookie_returns200WithNewTokens() throws Exception {
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("new-access-token")
                .role("CUSTOMER")
                .userName("Alice Test")
                .email("alice@example.com")
                .refreshTokenRaw("new-refresh-token")
                .build();
        when(authService.refresh("valid-refresh-token")).thenReturn(authResponse);
        when(jwtConfig.getRefreshTokenExpiry()).thenReturn(604800);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idToken").value("new-access-token"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void refresh_missingCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_blankCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", "   ")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_withCookie_returns200WithMessage() throws Exception {
        doNothing().when(authService).logout(any());
        when(jwtConfig.getRefreshTokenExpiry()).thenReturn(604800);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("refreshToken", "some-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    void logout_withoutCookie_returns200WithMessage() throws Exception {
        doNothing().when(authService).logout(any());

        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
}
