package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.user.ChangeEmailRequestDTO;
import com.epam.edp.demo.dto.user.ConfirmEmailRequestDTO;
import com.epam.edp.demo.dto.user.MessageResponseDTO;
import com.epam.edp.demo.dto.user.UpdatePasswordRequestDTO;
import com.epam.edp.demo.dto.user.UpdateUserImageRequestDTO;
import com.epam.edp.demo.dto.user.UpdateUserNameRequestDTO;
import com.epam.edp.demo.dto.user.UserDTO;
import com.epam.edp.demo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile management")
@SecurityRequirement(name = "Bearer")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get user profile")
    @ApiResponse(responseCode = "200", description = "User info retrieved")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(userService.getUserById(id, authorization));
    }

    @Operation(summary = "Update first and last name")
    @ApiResponse(responseCode = "200", description = "Name updated successfully")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PutMapping("/{id}/name")
    public ResponseEntity<MessageResponseDTO> updateName(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserNameRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(userService.updateName(id, request, authorization));
    }

    @Operation(summary = "Upload profile avatar (Base64)")
    @ApiResponse(responseCode = "200", description = "Avatar updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid image data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PutMapping("/{id}/image")
    public ResponseEntity<MessageResponseDTO> updateImage(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserImageRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(userService.updateImage(id, request, authorization));
    }

    @Operation(summary = "Change password (requires current password)")
    @ApiResponse(responseCode = "200", description = "Password updated successfully")
    @ApiResponse(responseCode = "400", description = "Current password incorrect or new password invalid")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PutMapping("/{id}/password")
    public ResponseEntity<MessageResponseDTO> updatePassword(
            @PathVariable String id,
            @Valid @RequestBody UpdatePasswordRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(userService.updatePassword(id, request, authorization));
    }

    @Operation(summary = "Initiate email change (sends confirmation link to new address)")
    @ApiResponse(responseCode = "200", description = "Confirmation link sent")
    @ApiResponse(responseCode = "400", description = "Password incorrect or email already in use")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PutMapping("/{id}/email")
    public ResponseEntity<MessageResponseDTO> initiateEmailChange(
            @PathVariable String id,
            @Valid @RequestBody ChangeEmailRequestDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ResponseEntity.ok(userService.initiateEmailChange(id, request, authorization));
    }

    @Operation(summary = "Confirm email change via token from confirmation link")
    @ApiResponse(responseCode = "200", description = "Email changed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid, expired, or already-used token")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PostMapping("/{id}/email/confirm")
    public ResponseEntity<MessageResponseDTO> confirmEmailChange(
            @PathVariable String id,
            @Valid @RequestBody ConfirmEmailRequestDTO request) {
        return ResponseEntity.ok(userService.confirmEmailChange(id, request.getConfirmationToken()));
    }
}
