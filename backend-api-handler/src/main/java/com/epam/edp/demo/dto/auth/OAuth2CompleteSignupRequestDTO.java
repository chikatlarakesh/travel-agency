package com.epam.edp.demo.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for {@code POST /api/v1/auth/oauth2/complete-signup}.
 *
 * <p>The frontend receives the {@code onboardingToken} from the redirect URL
 * after a successful Google authentication of a first-time user and sends it
 * back here to finalise account creation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2CompleteSignupRequestDTO {

    @NotBlank(message = "Onboarding token is required.")
    private String onboardingToken;
}

