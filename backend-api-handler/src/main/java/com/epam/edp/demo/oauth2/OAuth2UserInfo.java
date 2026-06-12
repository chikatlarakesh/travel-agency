package com.epam.edp.demo.oauth2;

import java.util.Map;
import java.util.Optional;

/**
 * Provider-agnostic view of the user-info returned by any OAuth2/OIDC provider.
 *
 * <p>Each concrete subclass maps the provider-specific attribute names (e.g.
 * {@code given_name} for Google vs. {@code first_name} for Facebook) to a common
 * interface used by both user services.  Adding a new provider requires only a new
 * subclass and a single {@code case} entry in {@link OAuth2UserInfoFactory}.
 */
public abstract class OAuth2UserInfo {

    protected final Map<String, Object> attributes;

    protected OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    /** Provider-issued stable unique identifier for the user (never null). */
    public abstract String getId();

    /** Verified email address.  May be absent if the provider did not grant email scope. */
    public abstract Optional<String> getEmail();

    /** Given (first) name. */
    public abstract Optional<String> getFirstName();

    /** Family (last) name. */
    public abstract Optional<String> getLastName();

    /** Profile picture URL. */
    public abstract Optional<String> getImageUrl();

    /** Raw provider attribute map (available for provider-specific consumers). */
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}

