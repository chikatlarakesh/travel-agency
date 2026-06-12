package com.epam.edp.demo.mapper;

import com.epam.edp.demo.dto.auth.SignInResponseDTO;
import com.epam.edp.demo.dto.response.AuthResponse;
import com.epam.edp.demo.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public SignInResponseDTO toSignInResponse(User user, String token) {
        return new SignInResponseDTO(
                token,
                user.getRole(),
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail()
        );
    }

    public SignInResponseDTO toSignInResponse(AuthResponse authResponse) {
        return new SignInResponseDTO(
                authResponse.getAccessToken(),
                authResponse.getRole(),
                authResponse.getUserName(),
                authResponse.getEmail()
        );
    }
}
