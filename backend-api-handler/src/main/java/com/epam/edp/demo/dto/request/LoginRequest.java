package com.epam.edp.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login request DTO.
 * Intentionally NO @Email, @Size, or @ValidPassword annotations.
 * Login must NOT reveal password policy to attackers.
 * All failures return the same generic "Invalid email or password".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}

