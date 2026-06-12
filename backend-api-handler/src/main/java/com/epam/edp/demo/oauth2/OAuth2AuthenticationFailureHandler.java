package com.epam.edp.demo.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Handles OAuth2 authentication failures.
 *
 * <p>Instead of exposing internal error details, this handler:
 * <ol>
 *   <li>Logs the raw exception at WARN level (server-side only).</li>
 *   <li>Clears the OAuth2 state cookies to prevent stale state.</li>
 *   <li>Redirects the browser to the frontend with a safe, generic error code
 *       as a query parameter so the UI can display an appropriate message.</li>
 * </ol>
 *
 * <h3>Safe error codes (no sensitive details exposed)</h3>
 * <ul>
 *   <li>{@code missing_email}    — provider did not supply an email address</li>
 *   <li>{@code account_locked}   — account is temporarily locked</li>
 *   <li>{@code account_disabled} — account has been disabled</li>
 *   <li>{@code authentication_failed} — generic fallback</li>
 * </ul>
 *
 * <h3>Impact on existing code</h3>
 * None — only invoked by the OAuth2 login chain.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final CookieOAuth2AuthorizationRequestRepository cookieRepo;

    @Value("${app.oauth2.signupUri:http://localhost:3000/oauth2/signup}")
    private String defaultSignupUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String safeCode = toSafeErrorCode(exception);

        log.warn("oauth2.login.failure code={} detail={}", safeCode, exception.getMessage());

        cookieRepo.clearCookies(request, response);

        // Redirect to the frontend signup/error page — keeps the UX in the SPA.
        String redirectUrl = UriComponentsBuilder.fromUriString(defaultSignupUri)
                .queryParam("error", safeCode)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    // ------------------------------------------------------------------ //
    //  Helper                                                             //
    // ------------------------------------------------------------------ //

    /**
     * Maps internal exceptions to safe, non-sensitive error codes suitable for
     * query parameters in a redirect URL.
     */
    private static String toSafeErrorCode(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauthEx) {
            return switch (oauthEx.getError().getErrorCode()) {
                case "oauth2_missing_email"    -> "missing_email";
                case "oauth2_account_locked"   -> "account_locked";
                case "oauth2_account_disabled" -> "account_disabled";
                default                        -> "authentication_failed";
            };
        }
        return "authentication_failed";
    }
}

