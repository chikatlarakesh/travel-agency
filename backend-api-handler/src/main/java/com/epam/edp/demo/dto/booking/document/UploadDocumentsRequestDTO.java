package com.epam.edp.demo.dto.booking.document;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for uploading documents to a booking.
 * Contains payment receipts and guest documents (passports, etc.)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadDocumentsRequestDTO {

    @Valid
    private List<DocumentUploadDTO> payments;

    @Valid
    private List<GuestDocumentsUploadDTO> guestDocuments;
}

