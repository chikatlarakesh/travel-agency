package com.epam.edp.demo.dto.booking.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for retrieving documents from a booking.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetrieveDocumentsResponseDTO {

    private List<DocumentResponseDTO> payments;
    private List<GuestDocumentsResponseDTO> guestDocuments;
}

