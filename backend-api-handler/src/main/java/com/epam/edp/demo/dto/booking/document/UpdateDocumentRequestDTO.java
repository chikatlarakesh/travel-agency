package com.epam.edp.demo.dto.booking.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a single document via PATCH.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateDocumentRequestDTO {
    private String fileName;
    private String type;
    private String base64encodedDocument;
}

