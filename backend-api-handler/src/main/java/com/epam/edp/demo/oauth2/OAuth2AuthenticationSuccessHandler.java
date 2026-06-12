package com.epam.edp.demo.oauth2;

import com.epam.edp.demo.config.JwtConfig;
import com.epam.edp.demo.dto.response.AuthResponse;
import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.service.TokenService;
import com.epam.edp.demo.util.SecurityUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Handles a successful OAuth2 authentication event.
 *
 * <h3>Branch logic</h3>
 * <dl>
 *   <dt>Case A — Existing user ({@code pendingOnboarding = false})</dt>
 *   <dd>Issues JWT access token + refresh token via the <em>existing</em>
 *   {@link TokenService#issueTokens} method.  Sets the refresh token as an HttpOnly cookie
 *   using the <em>existing</em> {@link SecurityUtils#createRefreshTokenCookie} helper.
 *   Redirects to: {@code /oauth2/redirect?token=&amp;role=&amp;userName=&amp;email=}</dd>
 *
 *   <dt>Case B — New user ({@code pendingOnboarding = true})</dt>
 *   <dd>Generates a short-lived HMAC-signed onboarding token via {@link OnboardingTokenService}.
 *   No JWT is issued and no account is created.
 *   Redirects to: {@code /oauth2/signup?onboarding_token=&amp;email=&amp;firstName=&amp;lastName=}</dd>
 * </dl>
 *
 * <h3>Impact on existing code</h3>
 * None — this handler is only wired into the OAuth2 login chain via
 * {@link com.epam.edp.demo.config.SecurityConfig}.  All existing auth flows are untouched.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenService tokenService;
    private final JwtConfig jwtConfig;
    private final OnboardingTokenService onboardingTokenService;
    private final CookieOAuth2AuthorizationRequestRepository cookieRepo;

    @Value("${app.oauth2.redirectUri:http://localhost:3000/oauth2/redirect}")
    private String defaultRedirectUri;

    @Value("${app.oauth2.signupUri:http://localhost:3000/oauth2/signup}")
    private String defaultSignupUri;

    // ------------------------------------------------------------------ //
    //  AuthenticationSuccessHandler contract                              //
    // ------------------------------------------------------------------ //

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (response.isCommitted()) {
            log.debug("oauth2.success_handler.response_already_committed");
            return;
        }

        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();

        // Clean up OAuth2 state cookies — they are no longer needed.
        cookieRepo.clearCookies(request, response);
        super.clearAuthenticationAttributes(request);

        if (principal.isPendingOnboarding()) {
            handleOnboarding(principal, request, response);
        } else {
            handleExistingUser(principal, request, response);
        }
    }

    // ------------------------------------------------------------------ //
    //  Case A — Existing user                                             //
    // ------------------------------------------------------------------ //

    private void handleExistingUser(OAuth2UserPrincipal principal,
                                    HttpServletRequest request,
                                    HttpServletResponse response) throws IOException {
        User user = principal.getUser();

        // Reuse the existing token service — no separate JWT logic.
        String deviceInfo = request.getHeader("User-Agent");
        AuthResponse authResponse = tokenService.issueTokens(user, deviceInfo);

        // Set the refresh token cookie exactly as the regular sign-in flow does.
        ResponseCookie refreshCookie = SecurityUtils.createRefreshTokenCookie(
                authResponse.getRefreshTokenRaw(), jwtConfig.getRefreshTokenExpiry());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        String userName = buildUserName(user);
        String redirectUrl = UriComponentsBuilder.fromUriString(resolveRedirectUri(request))
                .queryParam("token",    authResponse.getAccessToken())
                .queryParam("role",     user.getRole())
                .queryParam("userName", userName)
                .queryParam("email",    user.getEmail())
                .build().toUriString();

        log.info("oauth2.login.redirect userId={} role={}", user.getId(), user.getRole());
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    // ------------------------------------------------------------------ //
    //  Case B — New user (onboarding pending)                            //
    // ------------------------------------------------------------------ //

    private void handleOnboarding(OAuth2UserPrincipal principal,
                                  HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {
        User transientUser = principal.getUser();

        String onboardingToken = onboardingTokenService.generate(
                transientUser.getEmail(),
                transientUser.getProvider(),
                transientUser.getProviderId(),
                transientUser.getFirstName(),
                transientUser.getLastName(),
                transientUser.getImageUrl());

        String redirectUrl = UriComponentsBuilder.fromUriString(defaultSignupUri)
                .queryParam("onboarding_token", onboardingToken)
                .queryParam("email",      transientUser.getEmail())
                .queryParam("firstName",  transientUser.getFirstName())
                .queryParam("lastName",   transientUser.getLastName())
                .build().toUriString();

        log.info("oauth2.onboarding.redirect email={}",
                SecurityUtils.maskEmail(transientUser.getEmail()));
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                            //
    // ------------------------------------------------------------------ //

    /**
     * Resolves the post-login redirect URI.
     * Prefers the {@code redirect_uri} cookie set by the frontend, falls back to
     * the application-level default.
     */
    private String resolveRedirectUri(HttpServletRequest request) {
        return CookieUtils.getCookie(request, CookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_COOKIE)
                .map(Cookie::getValue)
                .filter(v -> !v.isBlank())
                .orElse(defaultRedirectUri);
    }

    private static String buildUserName(User user) {
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last  = user.getLastName()  != null ? user.getLastName()  : "";
        return (first + " " + last).trim();
    }
}

