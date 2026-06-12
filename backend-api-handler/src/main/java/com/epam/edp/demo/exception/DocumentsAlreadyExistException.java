package com.epam.edp.demo.exception;

/**
 * Exception thrown when attempting to POST documents that already exist.
 * Returns 409 Conflict - use PATCH to update individual documents.
 */
public class DocumentsAlreadyExistException extends RuntimeException {

    public DocumentsAlreadyExistException(String bookingId) {
        super(String.format("Documents already exist for booking %s. Use PATCH to update individual documents.", bookingId));
    }
}

