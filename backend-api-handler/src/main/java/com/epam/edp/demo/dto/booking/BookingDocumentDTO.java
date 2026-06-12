package com.epam.edp.demo.dto.booking;

import com.epam.edp.demo.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDocumentDTO {
    private String id;
    private DocumentType type;
    private String fileUrl;
    private String fileName;
    private boolean verified;
    private Instant verifiedAt;
    private String verifiedBy;
    private Instant uploadedAt;
}
