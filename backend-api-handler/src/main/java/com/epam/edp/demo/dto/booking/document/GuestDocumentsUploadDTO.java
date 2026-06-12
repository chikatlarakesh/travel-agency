package com.epam.edp.demo.dto.booking.document;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for guest documents in upload request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuestDocumentsUploadDTO {

    @NotBlank(message = "User name is required")
    private String userName;

    @NotEmpty(message = "Documents list cannot be empty")
    @Valid
    private List<DocumentUploadDTO> documents;
}

