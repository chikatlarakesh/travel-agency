package com.epam.edp.demo.dto.auth;

import com.epam.edp.demo.validation.StrictStringDeserializer;
import com.epam.edp.demo.validation.ValidPassword;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignUpRequestDTO {

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

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 16, message = "Password must be between 8 and 16 characters")
    @ValidPassword
    private String password;

    @NotBlank(message = "CAPTCHA ID is required")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String captchaId;

    @NotBlank(message = "CAPTCHA answer is required")
    @JsonDeserialize(using = StrictStringDeserializer.class)
    private String captchaAnswer;
}
