package com.epam.edp.demo.dto.auth;

import com.epam.edp.demo.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignInRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 16, message = "Password must be at least 8 characters long and include uppercase, lowercase, number, and special character.")
    @ValidPassword(message = "Password must be at least 8 characters long and include uppercase, lowercase, number, and special character.")
    private String password;
}
