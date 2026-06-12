package com.epam.edp.demo.entity;

import com.epam.edp.demo.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Entity representing a document uploaded for a booking.
 */
@Document(collection = "booking_documents")
@CompoundIndex(name = "idx_bookingId_type", def = "{'bookingId': 1, 'documentType': 1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDocument {

    @Id
    private String id;

    @Indexed
    private String bookingId;

    /** Sprint 2: Detailed document type (PASSPORT, VISA, PAYMENT_RECEIPT, GUEST_DOCUMENT, OTHER) */
    private DocumentType type;

    /** Upload-document-be: Category (PAYMENT, GUEST_DOCUMENT) */
    private BookingDocumentCategory documentType;

    /** Original file name provided by the user. */
    private String fileName;

    /** File type (pdf, jpg, png, etc.) */
    private String fileType;

    /** URL or base64-encoded file content (Sprint 2). */
    private String fileUrl;

    /** Base64 content (upload-document-be). */
    private String content;

    /** For guest documents, the name of the guest this document belongs to. */
    private String guestName;

    /** User who uploaded this document. */
    private String uploadedBy;

    /** Timestamp when document was uploaded. */
    private Instant uploadedAt;

    /** Whether a travel agent has verified this document. */
    private boolean verified;

    /** ID of the agent who verified this document. */
    private String verifiedBy;

    /** Timestamp when document was verified. */
    private Instant verifiedAt;

    /** Category for upload-document-be feature. */
    public enum BookingDocumentCategory {
        PAYMENT,
        GUEST_DOCUMENT
    }
}

