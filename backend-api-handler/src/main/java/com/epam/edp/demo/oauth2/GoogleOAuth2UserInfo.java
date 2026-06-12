package com.epam.edp.demo.oauth2;

import java.util.Map;
import java.util.Optional;

/**
 * {@link OAuth2UserInfo} implementation for Google OpenID Connect.
 *
 * <p>Google returns standard OIDC claims:
 * {@code sub} (unique user ID), {@code email}, {@code given_name},
 * {@code family_name}, {@code picture}.
 */
public class GoogleOAuth2UserInfo extends OAuth2UserInfo {

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    /** Google OIDC {@code sub} claim — globally unique, stable identifier. */
    @Override
    public String getId() {
        Object sub = attributes.get("sub");
        return sub != null ? sub.toString() : "";
    }

    @Override
    public Optional<String> getEmail() {
        return Optional.ofNullable((String) attributes.get("email"));
    }

    @Override
    public Optional<String> getFirstName() {
        return Optional.ofNullable((String) attributes.get("given_name"));
    }

    @Override
    public Optional<String> getLastName() {
        return Optional.ofNullable((String) attributes.get("family_name"));
    }

    @Override
    public Optional<String> getImageUrl() {
        return Optional.ofNullable((String) attributes.get("picture"));
    }
}

