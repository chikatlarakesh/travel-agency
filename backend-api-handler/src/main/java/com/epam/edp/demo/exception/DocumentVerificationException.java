package com.epam.edp.demo.exception;

/**
 * Thrown when a document verification operation cannot be completed
 * (e.g. document not found in booking, or document already verified).
 */
public class DocumentVerificationException extends RuntimeException {

    public DocumentVerificationException(String message) {
        super(message);
    }
}
