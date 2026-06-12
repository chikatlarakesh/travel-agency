package com.epam.edp.demo.dto.booking.document;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for a single document upload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadDTO {

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "Document type is required")
    private String type;

    @NotBlank(message = "Base64 encoded document is required")
    private String base64encodedDocument;
}

