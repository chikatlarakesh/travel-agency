package com.epam.edp.demo.dto.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to verify (or reject) a specific booking document")
public class DocumentVerificationRequestDTO {

    @NotBlank(message = "documentId is required")
    @Schema(description = "ID of the document within the booking's documents list", required = true)
    private String documentId;

    @NotBlank(message = "action is required (APPROVE or REJECT)")
    @Schema(description = "Verification action: APPROVE or REJECT", example = "APPROVE", required = true,
            allowableValues = {"APPROVE", "REJECT"})
    private String action;

    @Schema(description = "Optional note explaining the verification decision")
    private String note;
}
