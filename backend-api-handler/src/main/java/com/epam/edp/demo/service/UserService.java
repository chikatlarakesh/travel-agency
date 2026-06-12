package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.user.ChangeEmailRequestDTO;
import com.epam.edp.demo.dto.user.MessageResponseDTO;
import com.epam.edp.demo.dto.user.UpdatePasswordRequestDTO;
import com.epam.edp.demo.dto.user.UpdateUserImageRequestDTO;
import com.epam.edp.demo.dto.user.UpdateUserNameRequestDTO;
import com.epam.edp.demo.dto.user.UserDTO;

public interface UserService {

    UserDTO getUserById(String userId, String authorizationHeader);

    MessageResponseDTO updateName(String userId, UpdateUserNameRequestDTO request, String authorizationHeader);

    MessageResponseDTO updateImage(String userId, UpdateUserImageRequestDTO request, String authorizationHeader);

    MessageResponseDTO updatePassword(String userId, UpdatePasswordRequestDTO request, String authorizationHeader);

    MessageResponseDTO initiateEmailChange(String userId, ChangeEmailRequestDTO request, String authorizationHeader);

    MessageResponseDTO confirmEmailChange(String userId, String confirmationToken);
}
