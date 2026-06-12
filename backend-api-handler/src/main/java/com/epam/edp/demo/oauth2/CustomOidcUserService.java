package com.epam.edp.demo.oauth2;

import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.entity.enums.AccountStatus;
import com.epam.edp.demo.repository.UserRepository;
import com.epam.edp.demo.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Custom OIDC user service — handles providers that use OpenID Connect, primarily Google.
 *
 * <h3>Why a separate OIDC service?</h3>
 * Spring Security internally distinguishes between plain OAuth2 and OIDC:
 * <ul>
 *   <li>OIDC providers (Google) → {@code OidcUserService} → must return {@link OidcUser}</li>
 *   <li>Plain OAuth2 (Facebook) → {@code DefaultOAuth2UserService} → returns {@code OAuth2User}</li>
 * </ul>
 * Without this service, Spring's default {@code OidcUserService} runs and returns
 * {@code DefaultOidcUser}, which causes a {@code ClassCastException} when the success
 * handler casts the principal to {@link OAuth2UserPrincipal}.
 *
 * <h3>Onboarding logic</h3>
 * <ul>
 *   <li><b>Existing email</b> — associate provider info, update last-login, allow login.
 *       {@code pendingOnboarding = false}.</li>
 *   <li><b>New email</b> — build a transient (unsaved!) {@link User} object and set
 *       {@code pendingOnboarding = true}.  The success handler will redirect to the
 *       onboarding page; the account is only created after the user confirms.</li>
 * </ul>
 *
 * <h3>Impact on existing code</h3>
 * None — this service is only invoked by Spring Security's OAuth2 login filter when a
 * user authenticates via Google.  The email/password and registration flows are untouched.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private static final String ERR_MISSING_EMAIL    = "oauth2_missing_email";
    private static final String ERR_ACCOUNT_DISABLED = "oauth2_account_disabled";
    private static final String ERR_ACCOUNT_LOCKED   = "oauth2_account_locked";

    /**
     * Spring's built-in OIDC delegate — validates the ID token and fetches user-info.
     * Created locally because it has no Spring-managed dependencies of its own.
     */
    private final OidcUserService delegate = new OidcUserService();

    private final UserRepository userRepository;

    // ------------------------------------------------------------------ //
    //  OAuth2UserService<OidcUserRequest, OidcUser> contract              //
    // ------------------------------------------------------------------ //

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {

        // 1. Let Spring validate the ID token and assemble the OIDC claims.
        OidcUser oidcUser = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 2. Map provider-specific claims to our abstraction.
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                registrationId, oidcUser.getAttributes());

        // 3. Email is mandatory — should always be present with openid+email scopes.
        String email = userInfo.getEmail().orElseThrow(() -> {
            log.warn("oauth2.oidc.missing_email provider={}", registrationId);
            return new OAuth2AuthenticationException(
                    new OAuth2Error(ERR_MISSING_EMAIL),
                    "Email not provided by '" + registrationId + "'. "
                            + "Ensure the 'email' scope is included.");
        });

        String provider   = registrationId.toUpperCase();
        String providerId = userInfo.getId();

        // 4. Look up whether this email already exists in our database.
        return userRepository.findByEmail(email)
                .map(existing -> {
                    // Case A: known email — validate, associate, allow login.
                    validateAccountStatus(existing, provider);
                    User updated = associateProvider(existing, provider, providerId, userInfo);
                    log.info("oauth2.oidc.login.success email={} provider={}",
                            SecurityUtils.maskEmail(email), provider);
                    return (OidcUser) new OAuth2UserPrincipal(updated, oidcUser, false);
                })
                .orElseGet(() -> {
                    // Case B: unknown email — build transient user, require onboarding.
                    User transientUser = buildTransientUser(email, provider, providerId, userInfo);
                    log.info("oauth2.oidc.onboarding_pending email={} provider={}",
                            SecurityUtils.maskEmail(email), provider);
                    return new OAuth2UserPrincipal(transientUser, oidcUser, true);
                });
    }

    // ------------------------------------------------------------------ //
    //  Case A helpers                                                      //
    // ------------------------------------------------------------------ //

    /**
     * Associates the Google provider info with the existing account if not yet linked,
     * and updates the last-login timestamp.  The user IS saved to the database here
     * because the account already exists.
     */
    private User associateProvider(User user, String provider, String providerId,
                                   OAuth2UserInfo userInfo) {
        boolean modified = false;

        if (user.getProviderId() == null || user.getProviderId().isBlank()) {
            user.setProvider(provider);
            user.setProviderId(providerId);
            modified = true;
            log.info("oauth2.oidc.account_linked email={} provider={}",
                    SecurityUtils.maskEmail(user.getEmail()), provider);
        }

        if (user.getImageUrl() == null || user.getImageUrl().isBlank()) {
            userInfo.getImageUrl().ifPresent(url -> {
                user.setImageUrl(url);
            });
            modified = true;
        }

        user.setLastLoginAt(Instant.now());
        return userRepository.save(user);
    }

    // ------------------------------------------------------------------ //
    //  Case B helpers                                                      //
    // ------------------------------------------------------------------ //

    /**
     * Builds a <em>transient</em> {@link User} object that is intentionally <strong>NOT
     * saved to the database</strong>.  It carries the information collected from Google
     * forward to the success handler, which will then sign an onboarding token and
     * redirect the user to the confirmation page.
     *
     * <p>The account is only created after the frontend calls
     * {@code POST /api/v1/auth/oauth2/complete-signup}.
     */
    private User buildTransientUser(String email, String provider, String providerId,
                                    OAuth2UserInfo userInfo) {
        return User.builder()
                .id(UUID.randomUUID().toString()) // placeholder id — NOT persisted yet
                .email(email)
                .firstName(userInfo.getFirstName().orElse(""))
                .lastName(userInfo.getLastName().orElse(""))
                .provider(provider)
                .providerId(providerId)
                .imageUrl(userInfo.getImageUrl().orElse(null))
                // role, passwordHash, accountStatus intentionally omitted — not a real account yet
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Account status validation                                           //
    // ------------------------------------------------------------------ //

    private void validateAccountStatus(User user, String provider) {
        if (user.getAccountStatus() == AccountStatus.DISABLED) {
            log.warn("oauth2.oidc.account_disabled userId={} provider={}", user.getId(), provider);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(ERR_ACCOUNT_DISABLED),
                    "This account has been disabled. Please contact support.");
        }
        if (user.getAccountStatus() == AccountStatus.LOCKED) {
            log.warn("oauth2.oidc.account_locked userId={} provider={}", user.getId(), provider);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(ERR_ACCOUNT_LOCKED),
                    "This account is temporarily locked. Please try again later.");
        }
    }
}

