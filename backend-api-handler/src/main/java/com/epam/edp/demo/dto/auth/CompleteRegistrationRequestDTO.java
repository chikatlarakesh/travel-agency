package com.epam.edp.demo.dto.auth;

import com.epam.edp.demo.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for POST /api/v1/auth/complete-registration (step 3).
 * Supplies the verified OTP plus the desired credentials to finalise account creation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteRegistrationRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Verification code is required")
    private String verificationCode;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 16, message = "Password must be between 8 and 16 characters")
    @ValidPassword
    private String password;
}

