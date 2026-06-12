package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.booking.document.RetrieveDocumentsResponseDTO;
import com.epam.edp.demo.dto.booking.document.UpdateDocumentRequestDTO;
import com.epam.edp.demo.dto.booking.document.UploadDocumentsRequestDTO;

/**
 * Service for managing booking documents (payment receipts and guest documents).
 */
public interface BookingDocumentService {

    /**
     * Upload documents for a booking.
     * Fails with 409 Conflict if documents already exist for this booking.
     *
     * @param bookingId           the booking ID
     * @param request             the upload request containing payments and guest documents
     * @param authorizationHeader Bearer token of the requesting user
     * @throws com.epam.edp.demo.exception.BookingNotFoundException if booking does not exist
     * @throws com.epam.edp.demo.exception.UnauthorizedException   if caller is not the booking owner
     * @throws com.epam.edp.demo.exception.DocumentsAlreadyExistException if documents already exist
     */
    void uploadDocuments(String bookingId, UploadDocumentsRequestDTO request, String authorizationHeader);

    /**
     * Update a single document by ID.
     *
     * @param bookingId           the booking ID
     * @param documentId          the document ID to update
     * @param request             the update request with new document data
     * @param authorizationHeader Bearer token of the requesting user
     * @throws com.epam.edp.demo.exception.BookingNotFoundException  if booking does not exist
     * @throws com.epam.edp.demo.exception.DocumentNotFoundException if document does not exist
     * @throws com.epam.edp.demo.exception.UnauthorizedException    if caller is not the booking owner
     */
    void updateDocument(String bookingId, String documentId, UpdateDocumentRequestDTO request, String authorizationHeader);

    /**
     * Retrieve all documents for a booking.
     *
     * @param bookingId           the booking ID
     * @param authorizationHeader Bearer token of the requesting user
     * @return documents grouped by payments and guest documents
     * @throws com.epam.edp.demo.exception.BookingNotFoundException if booking does not exist
     * @throws com.epam.edp.demo.exception.UnauthorizedException   if caller is not the booking owner
     */
    RetrieveDocumentsResponseDTO getDocuments(String bookingId, String authorizationHeader);

    /**
     * Retrieve all documents for a booking — accessible by the assigned travel agent.
     *
     * @param bookingId           the booking ID
     * @param authorizationHeader Bearer token of the requesting travel agent
     */
    RetrieveDocumentsResponseDTO getDocumentsForAgent(String bookingId, String authorizationHeader);

    /**
     * Delete a specific document from a booking.
     *
     * @param bookingId           the booking ID
     * @param documentId          the document ID to delete
     * @param authorizationHeader Bearer token of the requesting user
     * @throws com.epam.edp.demo.exception.BookingNotFoundException  if booking does not exist
     * @throws com.epam.edp.demo.exception.DocumentNotFoundException if document does not exist
     * @throws com.epam.edp.demo.exception.UnauthorizedException    if caller is not the booking owner
     */
    void deleteDocument(String bookingId, String documentId, String authorizationHeader);

    /**
     * Get the count of documents for a booking.
     *
     * @param bookingId the booking ID
     * @return number of documents
     */
    long getDocumentCount(String bookingId);
}

