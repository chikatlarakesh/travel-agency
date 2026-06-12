package com.epam.edp.demo.service.impl;

import com.epam.edp.demo.dto.user.ChangeEmailRequestDTO;
import com.epam.edp.demo.dto.user.MessageResponseDTO;
import com.epam.edp.demo.dto.user.UpdatePasswordRequestDTO;
import com.epam.edp.demo.dto.user.UpdateUserImageRequestDTO;
import com.epam.edp.demo.dto.user.UpdateUserNameRequestDTO;
import com.epam.edp.demo.dto.user.UserDTO;
import com.epam.edp.demo.entity.EmailVerificationToken;
import com.epam.edp.demo.entity.User;
import com.epam.edp.demo.exception.BadRequestException;
import com.epam.edp.demo.exception.EmailAlreadyExistsException;
import com.epam.edp.demo.exception.UnauthorizedException;
import com.epam.edp.demo.exception.UserNotFoundException;
import com.epam.edp.demo.repository.EmailVerificationTokenRepository;
import com.epam.edp.demo.repository.UserRepository;
import com.epam.edp.demo.security.JwtTokenProvider;
import com.epam.edp.demo.service.EmailService;
import com.epam.edp.demo.service.UserService;
import com.epam.edp.demo.util.PasswordValidator;
import com.epam.edp.demo.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int MAX_IMAGE_SIZE_BYTES = 5_242_880; // 5 MB

    @Value("${app.email.token.expiry-seconds:86400}")
    private long emailTokenExpirySeconds;

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final EmailService emailService;

    @Override
    public UserDTO getUserById(String userId, String authorizationHeader) {
        String callerId = extractCallerId(authorizationHeader);
        User user = findUser(userId);
        authorizeOwnerOrAdmin(callerId, userId, authorizationHeader);

        return new UserDTO(user.getFirstName(), user.getLastName(), user.getImageUrl(), user.getRole());
    }

    @Override
    public MessageResponseDTO updateName(String userId, UpdateUserNameRequestDTO request, String authorizationHeader) {
        String callerId = extractCallerId(authorizationHeader);
        User user = findUser(userId);
        authorizeOwnerOrAdmin(callerId, userId, authorizationHeader);

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        userRepository.save(user);

        log.info("user.name.updated userId={}", userId);
        return new MessageResponseDTO("Your account has been updated successfully");
    }

    @Override
    public MessageResponseDTO updateImage(String userId, UpdateUserImageRequestDTO request, String authorizationHeader) {
        String callerId = extractCallerId(authorizationHeader);
        User user = findUser(userId);
        authorizeOwnerOrAdmin(callerId, userId, authorizationHeader);

        // For the current setup, persist avatar directly in MongoDB as a data URI.
        // This keeps the flow independent from external object storage.
        String normalizedImageData = normalizeAndValidateImage(request.getImageBase64());
        user.setImageUrl(normalizedImageData);
        userRepository.save(user);

        log.info("user.image.updated userId={}", userId);
        return new MessageResponseDTO("Your account has been updated successfully");
    }

    @Override
    public MessageResponseDTO updatePassword(String userId, UpdatePasswordRequestDTO request, String authorizationHeader) {
        String callerId = extractCallerId(authorizationHeader);
        User user = findUser(userId);
        authorizeOwnerOnly(callerId, userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        List<String> errors = passwordValidator.validate(request.getNewPassword());
        if (!errors.isEmpty()) {
            throw new BadRequestException(errors.get(0));
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must differ from the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("user.password.updated userId={}", userId);
        return new MessageResponseDTO("Your password has been updated successfully");
    }

    @Override
    public MessageResponseDTO initiateEmailChange(String userId, ChangeEmailRequestDTO request, String authorizationHeader) {
        String callerId = extractCallerId(authorizationHeader);
        User user = findUser(userId);
        authorizeOwnerOnly(callerId, userId);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Password is incorrect");
        }

        String newEmail = request.getNewEmail().trim().toLowerCase();

        if (user.getEmail().equalsIgnoreCase(newEmail)) {
            throw new BadRequestException("New email must differ from the current email");
        }

        if (userRepository.existsByEmail(newEmail)) {
            throw new EmailAlreadyExistsException(newEmail);
        }

        // Revoke any existing pending tokens for this user
        emailTokenRepository.deleteAllByUserId(userId);

        String rawToken = UUID.randomUUID().toString();
        String tokenHash = SecurityUtils.hashToken(rawToken);

        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(UUID.randomUUID().toString())
                .tokenHash(tokenHash)
                .userId(userId)
                .newEmail(newEmail)
                .expiresAt(Instant.now().plusSeconds(emailTokenExpirySeconds))
                .used(false)
                .build();

        emailTokenRepository.save(token);
        emailService.sendEmailConfirmation(newEmail, rawToken, userId);

        log.info("user.email.change.initiated userId={}", userId);
        return new MessageResponseDTO("A confirmation link has been sent to your new email address.");
    }

    @Override
    public MessageResponseDTO confirmEmailChange(String userId, String confirmationToken) {
        User user = findUser(userId);

        String tokenHash = SecurityUtils.hashToken(confirmationToken);
        EmailVerificationToken pendingToken = emailTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired confirmation token"));

        if (!pendingToken.getUserId().equals(userId)) {
            throw new UnauthorizedException("Token does not belong to this user");
        }

        if (pendingToken.isUsed()) {
            log.info("user.email.confirmed.already-used userId={}", userId);
            return new MessageResponseDTO("Your email has been changed successfully.");
        }
        if (Instant.now().isAfter(pendingToken.getExpiresAt())) {
            throw new BadRequestException("Confirmation link has expired. Please request a new one.");
        }

        String newEmail = pendingToken.getNewEmail();
        if (userRepository.existsByEmail(newEmail)) {
            if (newEmail.equalsIgnoreCase(user.getEmail())) {
                pendingToken.setUsed(true);
                emailTokenRepository.save(pendingToken);
                log.info("user.email.confirmed.already-updated userId={}", userId);
                return new MessageResponseDTO("Your email has been changed successfully.");
            }
            throw new EmailAlreadyExistsException(newEmail);
        }

        // Atomically update email and mark token used
        user.setEmail(newEmail);
        userRepository.save(user);

        pendingToken.setUsed(true);
        emailTokenRepository.save(pendingToken);

        log.info("user.email.confirmed userId={}", userId);
        return new MessageResponseDTO("Your email has been changed successfully.");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String extractCallerId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(SecurityUtils.BEARER_PREFIX)) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }
        String token = authorizationHeader.substring(SecurityUtils.BEARER_PREFIX.length());
        return jwtTokenProvider.getUserIdFromToken(token);
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    /** Read-only and low-risk mutations: owner or admin. Role is read from JWT — no extra DB query. */
    private void authorizeOwnerOrAdmin(String callerId, String targetUserId, String authorizationHeader) {
        if (!callerId.equals(targetUserId)) {
            String token = authorizationHeader.substring(SecurityUtils.BEARER_PREFIX.length());
            String callerRole = jwtTokenProvider.getRoleFromToken(token);
            if (!"ADMIN".equals(callerRole)) {
                throw new UnauthorizedException("Access denied");
            }
        }
    }

    /** Security-sensitive operations: owner only — admin bypass is intentionally prohibited. */
    private void authorizeOwnerOnly(String callerId, String targetUserId) {
        if (!callerId.equals(targetUserId)) {
            throw new UnauthorizedException("Access denied");
        }
    }

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private String normalizeAndValidateImage(String imageBase64) {
        String raw = imageBase64 == null ? "" : imageBase64.trim();
        String mimeType = null;
        String payload;

        if (raw.startsWith("data:")) {
            if (!raw.contains(",")) {
                throw new BadRequestException("Invalid image format. Please provide a valid Base64-encoded image.");
            }
            String header = raw.split(",", 2)[0]; // e.g. "data:image/png;base64"
            payload = raw.split(",", 2)[1];
            // extract mime type from header
            String headerBody = header.substring("data:".length()); // "image/png;base64"
            mimeType = headerBody.contains(";") ? headerBody.split(";")[0] : headerBody;
            if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
                throw new BadRequestException("Invalid image format. Please provide a valid Base64-encoded image.");
            }
        } else {
            payload = raw;
        }

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(payload);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid image format. Please provide a valid Base64-encoded image.");
        }

        if (!isValidImageBytes(decoded)) {
            throw new BadRequestException("Invalid image format. Please provide a valid Base64-encoded image.");
        }

        if (raw.startsWith("data:image/")) {
            return raw;
        }
        // No data URI prefix — detect format from magic bytes and build one
        return "data:" + detectMimeType(decoded) + ";base64," + payload;
    }

    /**
     * Checks magic bytes to confirm the data is a supported image format.
     * Supported: JPEG, PNG, GIF, WebP.
     */
    private boolean isValidImageBytes(byte[] bytes) {
        if (bytes.length < 4) return false;
        // JPEG: FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) return true;
        // PNG: 89 50 4E 47
        if ((bytes[0] & 0xFF) == 0x89 && (bytes[1] & 0xFF) == 0x50
                && (bytes[2] & 0xFF) == 0x4E && (bytes[3] & 0xFF) == 0x47) return true;
        // GIF: 47 49 46 38 (GIF8)
        if ((bytes[0] & 0xFF) == 0x47 && (bytes[1] & 0xFF) == 0x49
                && (bytes[2] & 0xFF) == 0x46 && (bytes[3] & 0xFF) == 0x38) return true;
        // WebP: RIFF....WEBP (bytes 0-3 = RIFF, bytes 8-11 = WEBP)
        if (bytes.length >= 12
                && (bytes[0] & 0xFF) == 0x52 && (bytes[1] & 0xFF) == 0x49
                && (bytes[2] & 0xFF) == 0x46 && (bytes[3] & 0xFF) == 0x46
                && (bytes[8] & 0xFF) == 0x57 && (bytes[9] & 0xFF) == 0x45
                && (bytes[10] & 0xFF) == 0x42 && (bytes[11] & 0xFF) == 0x50) return true;
        return false;
    }

    private String detectMimeType(byte[] bytes) {
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) return "image/jpeg";
        if ((bytes[0] & 0xFF) == 0x89 && (bytes[1] & 0xFF) == 0x50) return "image/png";
        if ((bytes[0] & 0xFF) == 0x47 && (bytes[1] & 0xFF) == 0x49) return "image/gif";
        return "image/webp";
    }
}
