package com.epam.edp.demo.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for POST /api/v1/users/{id}/email/confirm.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload required to confirm an e-mail change")
public class ConfirmEmailRequestDTO {

    @NotBlank(message = "confirmationToken is required")
    @Schema(description = "One-time confirmation token sent to the new e-mail address",
            example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String confirmationToken;
}

