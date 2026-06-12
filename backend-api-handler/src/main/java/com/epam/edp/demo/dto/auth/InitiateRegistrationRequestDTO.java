package com.epam.edp.demo.dto.auth;

import com.epam.edp.demo.validation.StrictStringDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for POST /api/v1/auth/initiate-registration (step 1).
 * Captures personal details and the email to which the OTP will be sent.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiateRegistrationRequestDTO {

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Captcha ID is required")
    private String captchaId;

    @NotBlank(message = "Captcha answer is required")
    private String captchaAnswer;
}

