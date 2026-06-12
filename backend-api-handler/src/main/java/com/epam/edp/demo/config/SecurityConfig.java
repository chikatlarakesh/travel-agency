package com.epam.edp.demo.config;

import com.epam.edp.demo.oauth2.CookieOAuth2AuthorizationRequestRepository;
import com.epam.edp.demo.oauth2.CustomOAuth2UserService;
import com.epam.edp.demo.oauth2.CustomOidcUserService;
import com.epam.edp.demo.oauth2.OAuth2AuthenticationFailureHandler;
import com.epam.edp.demo.oauth2.OAuth2AuthenticationSuccessHandler;
import com.epam.edp.demo.security.JwtAuthenticationFilter;
import com.epam.edp.demo.security.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** BCrypt work factor — 2^12 rounds. Balances security vs CPU cost. */
    private static final int BCRYPT_STRENGTH = 12;
    /** CORS preflight cache duration in seconds (1 hour). */
    private static final long CORS_MAX_AGE_SECONDS = 3600L;
    private static final String ROLE_ADMIN = "ADMIN";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;

    // OAuth2 social login beans — injected only when oauth2-client starter is on the classpath
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2FailureHandler;
    private final CookieOAuth2AuthorizationRequestRepository cookieAuthRequestRepo;

    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RateLimitFilter rateLimitFilter,
                          CustomOAuth2UserService customOAuth2UserService,
                          CustomOidcUserService customOidcUserService,
                          OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler,
                          OAuth2AuthenticationFailureHandler oAuth2FailureHandler,
                          CookieOAuth2AuthorizationRequestRepository cookieAuthRequestRepo) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter         = rateLimitFilter;
        this.customOAuth2UserService = customOAuth2UserService;
        this.customOidcUserService   = customOidcUserService;
        this.oAuth2SuccessHandler    = oAuth2SuccessHandler;
        this.oAuth2FailureHandler    = oAuth2FailureHandler;
        this.cookieAuthRequestRepo   = cookieAuthRequestRepo;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            /*
             * Session policy: STATELESS for JWT API calls.
             * The OAuth2 authorization code flow temporarily needs to persist state
             * between the provider redirect and the callback; this is handled by
             * CookieOAuth2AuthorizationRequestRepository instead of HTTP sessions,
             * so STATELESS is retained.
             */
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    // ── Existing public endpoints (unchanged) ──────────────────────
                    "/api/v1/auth/sign-up",
                    "/api/v1/auth/sign-in",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/logout",
                    "/api/v1/auth/forgot-password",
                    "/api/v1/auth/verify-code",
                    "/api/v1/auth/reset-password",
                    "/api/v1/auth/captcha/generate",
                    "/api/v1/auth/initiate-registration",
                    "/api/v1/auth/verify-registration-code",
                    "/api/v1/auth/complete-registration",
                    "/api/v1/users/*/email/confirm",
                    "/api/v1/tours/**",
                    "/api/hello",
                    "/",
                    "/actuator/health",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    // ── OAuth2 social login endpoints (US-16) ──────────────────────
                    "/oauth2/**",                          // initiates Google redirect
                    "/login/oauth2/**",                    // Google callback
                    "/api/v1/auth/oauth2/**"               // complete-signup endpoint
                ).permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole(ROLE_ADMIN)
                .requestMatchers("/api/v1/agent/**").hasAnyRole(ROLE_ADMIN, "TRAVEL_AGENT")
                .requestMatchers("/api/v1/travel-agent/**").hasAnyRole(ROLE_ADMIN, "TRAVEL_AGENT")
                .requestMatchers("/actuator/**").hasRole(ROLE_ADMIN)
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"message\":\"Unauthorized\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"message\":\"Forbidden\"}");
                })
            )
            // ── OAuth2 login (additive — does not affect JWT path) ─────────────
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(ae -> ae
                    .baseUri("/oauth2/authorization")
                    .authorizationRequestRepository(cookieAuthRequestRepo)
                )
                .redirectionEndpoint(re -> re
                    .baseUri("/login/oauth2/code/*")
                )
                .userInfoEndpoint(ui -> ui
                    // Plain OAuth2 providers (e.g. Facebook)
                    .userService(customOAuth2UserService)
                    // OIDC providers (e.g. Google) — MUST be registered separately.
                    // Without this, Spring uses the default OidcUserService which returns
                    // DefaultOidcUser, causing ClassCastException in the success handler.
                    .oidcUserService(customOidcUserService)
                )
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler)
            )
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("POST", "GET", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(CORS_MAX_AGE_SECONDS);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}