package com.epam.edp.demo.dto.booking.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for a document in response (with id, fileName and base64 content).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponseDTO {

    private String id;
    private String fileName;
    private String content;
    private String fileType;
}

