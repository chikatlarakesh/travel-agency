package com.epam.edp.demo.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeEmailRequestDTO {

    @NotBlank(message = "New email is required")
    @Email(message = "Must be a valid email address")
    private String newEmail;

    @NotBlank(message = "Password is required")
    private String password;
}
