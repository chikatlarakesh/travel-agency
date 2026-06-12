package com.epam.edp.demo.oauth2;

import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.entity.enums.AccountStatus;
import com.epam.edp.demo.repository.UserRepository;
import com.epam.edp.demo.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Custom OAuth2 user service for <em>plain OAuth2</em> providers that do not use OIDC —
 * currently structure-ready for Facebook.
 *
 * <p>Google uses OIDC and is handled by {@link CustomOidcUserService}.  This service
 * is registered as the fallback for any non-OIDC registration via
 * {@code .userInfoEndpoint(ui -> ui.userService(this))}.
 *
 * <h3>Impact on existing code</h3>
 * None — invoked only by Spring Security's OAuth2 login filter for non-OIDC providers.
 * The JWT/password auth path is unaffected.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final String ERR_MISSING_EMAIL    = "oauth2_missing_email";
    private static final String ERR_ACCOUNT_DISABLED = "oauth2_account_disabled";
    private static final String ERR_ACCOUNT_LOCKED   = "oauth2_account_locked";

    private final UserRepository userRepository;

    // ------------------------------------------------------------------ //
    //  DefaultOAuth2UserService override                                  //
    // ------------------------------------------------------------------ //

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                registrationId, oAuth2User.getAttributes());

        String email = userInfo.getEmail().orElseThrow(() -> {
            log.warn("oauth2.missing_email provider={}", registrationId);
            return new OAuth2AuthenticationException(
                    new OAuth2Error(ERR_MISSING_EMAIL),
                    "Email not provided by '" + registrationId + "'.");
        });

        String provider   = registrationId.toUpperCase();
        String providerId = userInfo.getId();

        return userRepository.findByEmail(email)
                .map(existing -> {
                    validateAccountStatus(existing, provider);
                    User updated = associateProvider(existing, provider, providerId, userInfo);
                    log.info("oauth2.login.success email={} provider={}",
                            SecurityUtils.maskEmail(email), provider);
                    return (OAuth2User) new OAuth2UserPrincipal(updated, oAuth2User.getAttributes(), false);
                })
                .orElseGet(() -> {
                    User transientUser = buildTransientUser(email, provider, providerId, userInfo);
                    log.info("oauth2.onboarding_pending email={} provider={}",
                            SecurityUtils.maskEmail(email), provider);
                    return new OAuth2UserPrincipal(transientUser, oAuth2User.getAttributes(), true);
                });
    }

    // ------------------------------------------------------------------ //
    //  Helpers (identical business logic to CustomOidcUserService)        //
    // ------------------------------------------------------------------ //

    private User associateProvider(User user, String provider, String providerId,
                                   OAuth2UserInfo userInfo) {
        if (user.getProviderId() == null || user.getProviderId().isBlank()) {
            user.setProvider(provider);
            user.setProviderId(providerId);
            log.info("oauth2.account_linked email={} provider={}",
                    SecurityUtils.maskEmail(user.getEmail()), provider);
        }
        if (user.getImageUrl() == null || user.getImageUrl().isBlank()) {
            userInfo.getImageUrl().ifPresent(user::setImageUrl);
        }
        user.setLastLoginAt(Instant.now());
        return userRepository.save(user);
    }

    private User buildTransientUser(String email, String provider, String providerId,
                                    OAuth2UserInfo userInfo) {
        return User.builder()
                .id(UUID.randomUUID().toString())
                .email(email)
                .firstName(userInfo.getFirstName().orElse(""))
                .lastName(userInfo.getLastName().orElse(""))
                .provider(provider)
                .providerId(providerId)
                .imageUrl(userInfo.getImageUrl().orElse(null))
                .build();
    }

    private void validateAccountStatus(User user, String provider) {
        if (user.getAccountStatus() == AccountStatus.DISABLED) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(ERR_ACCOUNT_DISABLED), "Account has been disabled.");
        }
        if (user.getAccountStatus() == AccountStatus.LOCKED) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(ERR_ACCOUNT_LOCKED), "Account is temporarily locked.");
        }
    }
}

