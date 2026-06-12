package com.epam.edp.demo.oauth2;

import com.epam.edp.demo.exception.BadRequestException;

import java.util.Map;

/**
 * Factory that returns the correct {@link OAuth2UserInfo} subclass for a given
 * Spring Security registration ID (provider).
 *
 * <p>To add a new provider, create a new {@link OAuth2UserInfo} subclass and add
 * one {@code case} here — no other changes are needed.
 */
public final class OAuth2UserInfoFactory {

    private OAuth2UserInfoFactory() {}

    /**
     * Returns a provider-specific {@link OAuth2UserInfo} wrapping the raw attribute map.
     *
     * @param registrationId the Spring Security registration ID (e.g. {@code "google"})
     * @param attributes     raw attributes from the OAuth2/OIDC user-info endpoint
     * @throws BadRequestException if the provider is not supported
     */
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId,
                                                   Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google"   -> new GoogleOAuth2UserInfo(attributes);
            // Facebook: add FacebookOAuth2UserInfo here when credentials are available
            default -> throw new BadRequestException(
                    "Social login provider '" + registrationId + "' is not yet supported.");
        };
    }
}

