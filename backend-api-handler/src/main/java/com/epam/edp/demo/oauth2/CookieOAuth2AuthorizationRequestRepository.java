package com.epam.edp.demo.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * Cookie-backed {@link AuthorizationRequestRepository} for the OAuth2 authorization code flow.
 *
 * <h3>Why this exists</h3>
 * Spring Security's default implementation stores the OAuth2 authorization request in an
 * HTTP session.  This application uses {@code SessionCreationPolicy.STATELESS} and is
 * deployed on multi-replica Kubernetes pods, so session storage is not available.
 *
 * <p>This implementation stores the serialized {@link OAuth2AuthorizationRequest} in a
 * short-lived (3-minute) HttpOnly cookie.  The cookie is created when the user initiates
 * the authorization request and deleted once the provider callback is processed.
 *
 * <p>The OAuth2 {@code state} parameter provides CSRF protection independently of the
 * storage mechanism, so cookie-based storage does not weaken security.
 *
 * <h3>Impact on existing code</h3>
 * None — this bean is only consulted by Spring Security's OAuth2 login filter, which
 * is only active when {@code .oauth2Login()} is configured.  The JWT authentication
 * path is unaffected.
 */
@Slf4j
@Component
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    /** Cookie name for the serialized OAuth2AuthorizationRequest. */
    public static final String AUTH_REQUEST_COOKIE = "oauth2_auth_req";

    /** Cookie name for an optional post-login redirect URI supplied by the frontend. */
    public static final String REDIRECT_URI_COOKIE = "oauth2_redirect_uri";

    /** OAuth2 state survives at most this many seconds (3 minutes). */
    private static final int COOKIE_TTL_SECONDS = 180;

    // ------------------------------------------------------------------ //
    //  AuthorizationRequestRepository contract                            //
    // ------------------------------------------------------------------ //

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieUtils.getCookie(request, AUTH_REQUEST_COOKIE)
                .map(c -> CookieUtils.deserialize(c, OAuth2AuthorizationRequest.class))
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            clearCookies(request, response);
            return;
        }

        CookieUtils.addCookie(response, AUTH_REQUEST_COOKIE,
                CookieUtils.serialize(authorizationRequest), COOKIE_TTL_SECONDS);

        // Persist an optional post-login redirect URI from the initiating frontend page.
        String redirectUri = request.getParameter("redirect_uri");
        if (redirectUri != null && !redirectUri.isBlank()) {
            CookieUtils.addCookie(response, REDIRECT_URI_COOKIE, redirectUri, COOKIE_TTL_SECONDS);
        }

        log.debug("oauth2.auth_request.saved state={}", authorizationRequest.getState());
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
        clearCookies(request, response);
        return authRequest;
    }

    // ------------------------------------------------------------------ //
    //  Helper                                                             //
    // ------------------------------------------------------------------ //

    /** Deletes both OAuth2 cookies.  Called by success/failure handlers when the flow ends. */
    public void clearCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.deleteCookie(request, response, AUTH_REQUEST_COOKIE);
        CookieUtils.deleteCookie(request, response, REDIRECT_URI_COOKIE);
    }
}

