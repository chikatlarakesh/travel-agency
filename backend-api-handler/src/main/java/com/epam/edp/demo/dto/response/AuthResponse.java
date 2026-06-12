package com.epam.edp.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private int expiresIn;

    /** Raw refresh token — used ONLY for HttpOnly cookie, NEVER serialized to JSON. */
    @JsonIgnore
    private String refreshTokenRaw;

    private String role;
    private String userName;
    private String email;
}
