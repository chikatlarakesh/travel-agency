package com.epam.edp.demo.dto.booking.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for guest documents in response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuestDocumentsResponseDTO {

    private String userName;
    private List<DocumentResponseDTO> documents;
}

