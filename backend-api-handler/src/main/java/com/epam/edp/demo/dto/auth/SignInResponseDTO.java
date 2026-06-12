package com.epam.edp.demo.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignInResponseDTO {
    private String idToken;
    private String role;
    private String userName;
    private String email;
}
