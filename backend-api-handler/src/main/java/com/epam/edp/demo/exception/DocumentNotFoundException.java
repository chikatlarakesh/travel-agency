package com.epam.edp.demo.exception;

/**
 * Exception thrown when a booking document is not found.
 */
public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(String documentId) {
        super("Document not found: " + documentId);
    }

    public DocumentNotFoundException(String bookingId, String documentId) {
        super("Document " + documentId + " not found for booking " + bookingId);
    }
}

