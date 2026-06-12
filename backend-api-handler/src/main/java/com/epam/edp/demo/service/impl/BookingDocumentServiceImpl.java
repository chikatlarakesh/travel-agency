package com.epam.edp.demo.service.impl;

import com.epam.edp.demo.dto.booking.document.*;
import com.epam.edp.demo.entity.Booking;
import com.epam.edp.demo.entity.BookingDocument;
import com.epam.edp.demo.entity.PersonDetail;
import com.epam.edp.demo.exception.BadRequestException;
import com.epam.edp.demo.exception.BookingNotFoundException;
import com.epam.edp.demo.exception.DocumentNotFoundException;
import com.epam.edp.demo.exception.DocumentsAlreadyExistException;
import com.epam.edp.demo.exception.UnauthorizedException;
import com.epam.edp.demo.repository.BookingDocumentRepository;
import com.epam.edp.demo.repository.BookingRepository;
import com.epam.edp.demo.repository.TravelAgentRepository;
import com.epam.edp.demo.security.JwtTokenProvider;
import com.epam.edp.demo.service.BookingDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of BookingDocumentService.
 * Handles document upload, retrieval, and deletion for bookings.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingDocumentServiceImpl implements BookingDocumentService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final BookingDocumentRepository documentRepository;
    private final BookingRepository bookingRepository;
    private final TravelAgentRepository travelAgentRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void uploadDocuments(String bookingId, UploadDocumentsRequestDTO request, String authorizationHeader) {
        String userId = extractUserIdFromToken(authorizationHeader);
        Booking booking = getBookingAndVerifyOwner(bookingId, userId);

        // ═══════════════════════════════════════════════════════════════════════
        // CHECK IF DOCUMENTS ALREADY EXIST
        // If they do, skip strict validation and just append the new documents.
        // ═══════════════════════════════════════════════════════════════════════
        List<BookingDocument> existingDocuments = documentRepository.findByBookingId(bookingId);
        if (!existingDocuments.isEmpty()) {
            // Append mode: just save whatever is provided without full re-validation
            List<BookingDocument> documentsToAppend = new ArrayList<>();
            if (request.getPayments() != null) {
                for (DocumentUploadDTO payment : request.getPayments()) {
                    if (payment.getFileName() != null && !payment.getFileName().isBlank()
                            && payment.getBase64encodedDocument() != null && !payment.getBase64encodedDocument().isBlank()) {
                        documentsToAppend.add(BookingDocument.builder()
                                .id(UUID.randomUUID().toString())
                                .bookingId(bookingId)
                                .documentType(BookingDocument.BookingDocumentCategory.PAYMENT)
                                .fileName(payment.getFileName())
                                .fileType(payment.getType())
                                .content(payment.getBase64encodedDocument())
                                .uploadedBy(userId)
                                .uploadedAt(Instant.now())
                                .build());
                    }
                }
            }
            if (request.getGuestDocuments() != null) {
                for (GuestDocumentsUploadDTO guestDocs : request.getGuestDocuments()) {
                    if (guestDocs.getDocuments() != null) {
                        for (DocumentUploadDTO doc : guestDocs.getDocuments()) {
                            if (doc.getFileName() != null && !doc.getFileName().isBlank()
                                    && doc.getBase64encodedDocument() != null && !doc.getBase64encodedDocument().isBlank()) {
                                documentsToAppend.add(BookingDocument.builder()
                                        .id(UUID.randomUUID().toString())
                                        .bookingId(bookingId)
                                        .documentType(BookingDocument.BookingDocumentCategory.GUEST_DOCUMENT)
                                        .fileName(doc.getFileName())
                                        .fileType(doc.getType())
                                        .content(doc.getBase64encodedDocument())
                                        .guestName(guestDocs.getUserName())
                                        .uploadedBy(userId)
                                        .uploadedAt(Instant.now())
                                        .build());
                            }
                        }
                    }
                }
            }
            if (!documentsToAppend.isEmpty()) {
                documentRepository.saveAll(documentsToAppend);
                log.info("documents.appended bookingId={} count={} uploadedBy={}",
                        bookingId, documentsToAppend.size(), userId);
            }
            return;
        }

        // ═══════════════════════════════════════════════════════════════════════
        // HARD VALIDATION: Require payment receipt + passport for ALL travelers
        // ═══════════════════════════════════════════════════════════════════════

        // 1. Validate at least one payment receipt
        if (request.getPayments() == null || request.getPayments().isEmpty()) {
            throw new BadRequestException("At least one payment receipt is required");
        }
        
        // Validate each payment has content
        for (DocumentUploadDTO payment : request.getPayments()) {
            if (payment.getFileName() == null || payment.getFileName().isBlank()) {
                throw new BadRequestException("Payment document fileName is required");
            }
            if (payment.getBase64encodedDocument() == null || payment.getBase64encodedDocument().isBlank()) {
                throw new BadRequestException("Payment document content is required");
            }
        }

        // 2. Calculate total number of travelers from booking
        int totalTravelers = 0;
        if (booking.getGuests() != null) {
            totalTravelers = booking.getGuests().getAdult() + booking.getGuests().getChildren();
        }
        
        // 3. Validate guest documents count matches total travelers
        if (request.getGuestDocuments() == null || request.getGuestDocuments().isEmpty()) {
            throw new BadRequestException(
                    String.format("Passport documents required for all %d travelers", totalTravelers));
        }
        
        int guestDocumentEntries = request.getGuestDocuments().size();
        if (guestDocumentEntries != totalTravelers) {
            throw new BadRequestException(
                    String.format("Expected passport documents for %d travelers, but received for %d",
                            totalTravelers, guestDocumentEntries));
        }

        // 4. Validate each guest has at least one document and guest names match booking
        Set<String> expectedGuestNames = buildExpectedGuestNames(booking);
        Set<String> providedGuestNames = new HashSet<>();
        
        for (GuestDocumentsUploadDTO guestDocs : request.getGuestDocuments()) {
            if (guestDocs.getUserName() == null || guestDocs.getUserName().isBlank()) {
                throw new BadRequestException("Guest userName is required for each guest document entry");
            }
            if (guestDocs.getDocuments() == null || guestDocs.getDocuments().isEmpty()) {
                throw new BadRequestException(
                        String.format("At least one passport document is required for guest: %s", 
                                guestDocs.getUserName()));
            }
            
            // Validate each document has content
            for (DocumentUploadDTO doc : guestDocs.getDocuments()) {
                if (doc.getFileName() == null || doc.getFileName().isBlank()) {
                    throw new BadRequestException(
                            String.format("Document fileName is required for guest: %s", guestDocs.getUserName()));
                }
                if (doc.getBase64encodedDocument() == null || doc.getBase64encodedDocument().isBlank()) {
                    throw new BadRequestException(
                            String.format("Document content is required for guest: %s", guestDocs.getUserName()));
                }
            }
            
            providedGuestNames.add(guestDocs.getUserName().toLowerCase().trim());
        }

        // 5. Validate guest names match the booking's personalDetails (if available)
        if (!expectedGuestNames.isEmpty()) {
            for (String providedName : providedGuestNames) {
                if (!expectedGuestNames.contains(providedName)) {
                    throw new BadRequestException(
                            String.format("Guest name '%s' does not match any traveler in the booking", 
                                    providedName));
                }
            }
            
            // Check if all expected guests are covered
            Set<String> missingGuests = new HashSet<>(expectedGuestNames);
            missingGuests.removeAll(providedGuestNames);
            if (!missingGuests.isEmpty()) {
                throw new BadRequestException(
                        String.format("Missing passport documents for travelers: %s", 
                                String.join(", ", missingGuests)));
            }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // VALIDATION PASSED - Save documents
        // ═══════════════════════════════════════════════════════════════════════


        List<BookingDocument> documentsToSave = new ArrayList<>();

        // Process payment documents
        for (DocumentUploadDTO payment : request.getPayments()) {
            BookingDocument doc = BookingDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .bookingId(bookingId)
                    .documentType(BookingDocument.BookingDocumentCategory.PAYMENT)
                    .fileName(payment.getFileName())
                    .fileType(payment.getType())
                    .content(payment.getBase64encodedDocument())
                    .guestName(null)
                    .uploadedBy(userId)
                    .uploadedAt(Instant.now())
                    .build();
            documentsToSave.add(doc);
        }

        // Process guest documents
        for (GuestDocumentsUploadDTO guestDocs : request.getGuestDocuments()) {
            for (DocumentUploadDTO doc : guestDocs.getDocuments()) {
                BookingDocument document = BookingDocument.builder()
                        .id(UUID.randomUUID().toString())
                        .bookingId(bookingId)
                        .documentType(BookingDocument.BookingDocumentCategory.GUEST_DOCUMENT)
                        .fileName(doc.getFileName())
                        .fileType(doc.getType())
                        .content(doc.getBase64encodedDocument())
                        .guestName(guestDocs.getUserName())
                        .uploadedBy(userId)
                        .uploadedAt(Instant.now())
                        .build();
                documentsToSave.add(document);
            }
        }

        documentRepository.saveAll(documentsToSave);
        log.info("documents.uploaded bookingId={} count={} uploadedBy={}",
                bookingId, documentsToSave.size(), userId);
    }

    @Override
    public void updateDocument(String bookingId, String documentId, UpdateDocumentRequestDTO request, String authorizationHeader) {
        String userId = extractUserIdFromToken(authorizationHeader);
        getBookingAndVerifyOwner(bookingId, userId);

        // Find the document
        BookingDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(bookingId, documentId));

        // Verify document belongs to this booking
        if (!document.getBookingId().equals(bookingId)) {
            throw new DocumentNotFoundException(bookingId, documentId);
        }

        // Validate request
        if (request.getFileName() == null || request.getFileName().isBlank()) {
            throw new BadRequestException("Document fileName is required");
        }
        if (request.getBase64encodedDocument() == null || request.getBase64encodedDocument().isBlank()) {
            throw new BadRequestException("Document content is required");
        }

        // Update the document
        document.setFileName(request.getFileName());
        document.setFileType(request.getType());
        document.setContent(request.getBase64encodedDocument());
        document.setUploadedAt(Instant.now());
        document.setUploadedBy(userId);

        documentRepository.save(document);
        log.info("document.updated bookingId={} documentId={} updatedBy={}", bookingId, documentId, userId);
    }
    
    /**
     * Build a set of expected guest names from the booking's personalDetails.
     * Names are stored lowercase for case-insensitive comparison.
     */
    private Set<String> buildExpectedGuestNames(Booking booking) {
        Set<String> names = new HashSet<>();
        if (booking.getPersonalDetails() != null) {
            for (PersonDetail detail : booking.getPersonalDetails()) {
                String fullName = (detail.getFirstName() + " " + detail.getLastName())
                        .toLowerCase().trim();
                names.add(fullName);
            }
        }
        return names;
    }

    @Override
    public RetrieveDocumentsResponseDTO getDocuments(String bookingId, String authorizationHeader) {
        String userId = extractUserIdFromToken(authorizationHeader);
        getBookingAndVerifyOwner(bookingId, userId);

        List<BookingDocument> allDocuments = documentRepository.findByBookingId(bookingId);

        // Separate payments and guest documents
        List<DocumentResponseDTO> payments = allDocuments.stream()
                .filter(d -> d.getDocumentType() == BookingDocument.BookingDocumentCategory.PAYMENT)
                .map(d -> new DocumentResponseDTO(d.getId(), d.getFileName(), d.getContent(), d.getFileType()))
                .collect(Collectors.toList());

        // Group guest documents by guest name
        Map<String, List<BookingDocument>> guestDocsGrouped = allDocuments.stream()
                .filter(d -> d.getDocumentType() == BookingDocument.BookingDocumentCategory.GUEST_DOCUMENT)
                .collect(Collectors.groupingBy(
                        d -> d.getGuestName() != null ? d.getGuestName() : "Unknown",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<GuestDocumentsResponseDTO> guestDocuments = guestDocsGrouped.entrySet().stream()
                .map(entry -> new GuestDocumentsResponseDTO(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(d -> new DocumentResponseDTO(d.getId(), d.getFileName(), d.getContent(), d.getFileType()))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        log.debug("documents.retrieved bookingId={} payments={} guestDocGroups={}",
                bookingId, payments.size(), guestDocuments.size());

        return new RetrieveDocumentsResponseDTO(payments, guestDocuments);
    }

    @Override
    public RetrieveDocumentsResponseDTO getDocumentsForAgent(String bookingId, String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new UnauthorizedException("Valid authentication token required");
        }
        String email = jwtTokenProvider.getEmailFromToken(token);
        String agentId = travelAgentRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Not a registered travel agent"))
                .getId();
        // Verify booking is assigned to this agent
        bookingRepository.findByIdAndTravelAgentId(bookingId, agentId)
                .orElseThrow(() -> new UnauthorizedException("You are not the assigned agent for this booking"));

        List<BookingDocument> allDocuments = documentRepository.findByBookingId(bookingId);

        List<DocumentResponseDTO> payments = allDocuments.stream()
                .filter(d -> d.getDocumentType() == BookingDocument.BookingDocumentCategory.PAYMENT)
                .map(d -> new DocumentResponseDTO(d.getId(), d.getFileName(), d.getContent(), d.getFileType()))
                .collect(Collectors.toList());

        Map<String, List<BookingDocument>> guestDocsGrouped = allDocuments.stream()
                .filter(d -> d.getDocumentType() == BookingDocument.BookingDocumentCategory.GUEST_DOCUMENT)
                .collect(Collectors.groupingBy(
                        d -> d.getGuestName() != null ? d.getGuestName() : "Unknown",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<GuestDocumentsResponseDTO> guestDocuments = guestDocsGrouped.entrySet().stream()
                .map(entry -> new GuestDocumentsResponseDTO(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(d -> new DocumentResponseDTO(d.getId(), d.getFileName(), d.getContent(), d.getFileType()))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        return new RetrieveDocumentsResponseDTO(payments, guestDocuments);
    }

    @Override
    public void deleteDocument(String bookingId, String documentId, String authorizationHeader) {
        String userId = extractUserIdFromToken(authorizationHeader);
        getBookingAndVerifyOwner(bookingId, userId);

        BookingDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(bookingId, documentId));

        // Verify document belongs to this booking
        if (!document.getBookingId().equals(bookingId)) {
            throw new DocumentNotFoundException(bookingId, documentId);
        }

        documentRepository.delete(document);
        log.info("document.deleted bookingId={} documentId={} deletedBy={}", bookingId, documentId, userId);
    }

    @Override
    public long getDocumentCount(String bookingId) {
        return documentRepository.countByBookingId(bookingId);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Booking getBookingAndVerifyOwner(String bookingId, String userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (!booking.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to access documents for this booking");
        }

        return booking;
    }

    private String extractUserIdFromToken(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new UnauthorizedException("Valid authentication token required");
        }
        return jwtTokenProvider.getUserIdFromToken(token);
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            return authorizationHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}

