package com.epam.edp.demo.dto.booking;

import com.epam.edp.demo.enums.DocumentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to upload a document to a booking")
public class UploadDocumentRequestDTO {

    @NotNull(message = "type is required")
    @Schema(description = "Type of document being uploaded", example = "PASSPORT", required = true)
    private DocumentType type;

    @Schema(description = "Original file name", example = "Passport_JohnDoe.pdf")
    private String fileName;

    @Schema(description = "URL or base64-encoded file content", required = true)
    private String fileUrl;
}
