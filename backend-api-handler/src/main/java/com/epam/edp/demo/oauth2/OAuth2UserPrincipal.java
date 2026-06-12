package com.epam.edp.demo.oauth2;

import com.epam.edp.demo.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Unified Spring Security principal for both plain OAuth2 and OIDC flows.
 *
 * <h3>Why {@link OidcUser} instead of just {@link org.springframework.security.oauth2.core.user.OAuth2User}?</h3>
 * Google uses OpenID Connect (OIDC). Spring Security detects OIDC providers and calls
 * {@code OidcUserService}, whose result must implement {@link OidcUser}.  If our principal
 * only implements {@code OAuth2User}, the cast in the success handler throws a
 * {@code ClassCastException}.  Implementing {@code OidcUser} (which extends
 * {@code OAuth2User}) satisfies both code paths.
 *
 * <h3>The {@code pendingOnboarding} flag</h3>
 * When a Google login arrives for an email that is <em>not yet registered</em>, we must
 * not create a user account immediately (requirement).  Instead, we set
 * {@code pendingOnboarding = true} on this principal and store a transient, <em>not yet
 * persisted</em> {@link User} object.  The success handler inspects this flag:
 * <ul>
 *   <li>{@code false} → issue JWT and return to the app (existing user).</li>
 *   <li>{@code true}  → issue a short-lived onboarding token and redirect to the
 *       signup confirmation page (new user).</li>
 * </ul>
 *
 * <h3>Impact on existing code</h3>
 * None — this class is only instantiated inside the OAuth2 user services and is never
 * referenced outside the {@code oauth2} package (except by {@link OAuth2AuthenticationSuccessHandler}).
 */
@Getter
public class OAuth2UserPrincipal implements OidcUser {

    /** The persisted (or transient-pending) user entity. */
    private final User user;

    /** Raw provider attributes. */
    private final Map<String, Object> attributes;

    /** OIDC ID token — {@code null} for plain OAuth2 (Facebook) flows. */
    private final OidcIdToken idToken;

    /** OIDC user-info response — {@code null} for plain OAuth2 flows. */
    private final OidcUserInfo userInfo;

    /**
     * {@code true} when the OAuth2 email is not yet registered and the user must
     * complete the onboarding confirmation step before an account is created.
     */
    private final boolean pendingOnboarding;

    // ------------------------------------------------------------------ //
    //  Constructors                                                        //
    // ------------------------------------------------------------------ //

    /**
     * Constructor for <strong>OIDC flows</strong> (Google).
     * Preserves the original {@link OidcUser} tokens so Spring Security internals work.
     *
     * @param user              persisted or transient user — never {@code null}
     * @param oidcUser          the raw OidcUser from Spring Security
     * @param pendingOnboarding {@code true} if the user does not yet have a DB account
     */
    public OAuth2UserPrincipal(User user, OidcUser oidcUser, boolean pendingOnboarding) {
        this.user             = user;
        this.attributes       = Collections.unmodifiableMap(oidcUser.getAttributes());
        this.idToken          = oidcUser.getIdToken();
        this.userInfo         = oidcUser.getUserInfo();
        this.pendingOnboarding = pendingOnboarding;
    }

    /**
     * Constructor for <strong>plain OAuth2 flows</strong> (Facebook, future providers).
     * No OIDC token data available.
     *
     * @param user              persisted or transient user — never {@code null}
     * @param attributes        raw provider attributes
     * @param pendingOnboarding {@code true} if the user does not yet have a DB account
     */
    public OAuth2UserPrincipal(User user, Map<String, Object> attributes, boolean pendingOnboarding) {
        this.user             = user;
        this.attributes       = Collections.unmodifiableMap(attributes);
        this.idToken          = null;
        this.userInfo         = null;
        this.pendingOnboarding = pendingOnboarding;
    }

    // ------------------------------------------------------------------ //
    //  Spring Security contracts                                           //
    // ------------------------------------------------------------------ //

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (pendingOnboarding || user.getRole() == null) {
            // Transient user — no role yet; grant an empty authority set.
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    /** Principal name — we use the verified email as the canonical identifier. */
    @Override
    public String getName() {
        return user.getEmail();
    }

    // ------------------------------------------------------------------ //
    //  OidcUser contract                                                   //
    // ------------------------------------------------------------------ //

    @Override
    public Map<String, Object> getClaims() {
        return (idToken != null) ? idToken.getClaims() : attributes;
    }

    /** {@code null} for non-OIDC providers — callers must null-check. */
    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }

    /** {@code null} for non-OIDC providers — callers must null-check. */
    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }
}

