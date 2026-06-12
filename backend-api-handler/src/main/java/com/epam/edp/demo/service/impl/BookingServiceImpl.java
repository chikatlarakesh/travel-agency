package com.epam.edp.demo.service.impl;

import com.epam.edp.demo.config.RabbitMqConfig;
import com.epam.edp.demo.dto.booking.AgentBookingListResponseDTO;
import com.epam.edp.demo.dto.event.TourStatsEvent;
import com.epam.edp.demo.dto.booking.BookedTourDTO;
import com.epam.edp.demo.dto.booking.BookedTourListResponseDTO;
import com.epam.edp.demo.dto.booking.CancelBookingRequestDTO;
import com.epam.edp.demo.dto.booking.ConfirmBookingRequestDTO;
import com.epam.edp.demo.dto.booking.CreateBookingRequestDTO;
import com.epam.edp.demo.dto.booking.CreateBookingResponseDTO;
import com.epam.edp.demo.dto.booking.CustomerApprovalRequestDTO;
import com.epam.edp.demo.dto.booking.DocumentVerificationRequestDTO;
import com.epam.edp.demo.dto.booking.EditBookingRequestDTO;
import com.epam.edp.demo.dto.booking.TravelAgentBookingDetailDTO;
import com.epam.edp.demo.dto.booking.UploadDocumentRequestDTO;
import com.epam.edp.demo.dto.booking.UpdateBookingRequestDTO;
import com.epam.edp.demo.entity.Booking;
import com.epam.edp.demo.entity.BookingDocument;
import com.epam.edp.demo.entity.CancellationDetails;
import com.epam.edp.demo.entity.ConfirmationDetails;
import com.epam.edp.demo.entity.CustomerApproval;
import com.epam.edp.demo.entity.GuestCount;
import com.epam.edp.demo.entity.PersonDetail;
import com.epam.edp.demo.entity.Tour;
import com.epam.edp.demo.entity.TourInstance;
import com.epam.edp.demo.entity.TravelAgent;
import com.epam.edp.demo.enums.ApprovalMode;
import com.epam.edp.demo.enums.BookingStatus;
import com.epam.edp.demo.exception.BadRequestException;
import com.epam.edp.demo.exception.BookingNotFoundException;
import com.epam.edp.demo.exception.CancellationNotAllowedException;
import com.epam.edp.demo.exception.DocumentVerificationException;
import com.epam.edp.demo.exception.InvalidBookingStateException;
import com.epam.edp.demo.exception.OverbookingException;
import com.epam.edp.demo.exception.TourNotFoundException;
import com.epam.edp.demo.exception.UnauthorizedException;
import com.epam.edp.demo.mapper.BookingMapper;
import com.epam.edp.demo.repository.BookingRepository;
import com.epam.edp.demo.util.SecurityUtils;
import com.epam.edp.demo.repository.BookingDocumentRepository;
import com.epam.edp.demo.repository.MongoReviewRepository;
import com.epam.edp.demo.repository.TourInstanceRepository;
import com.epam.edp.demo.repository.TourRepository;
import com.epam.edp.demo.repository.TravelAgentRepository;
import com.epam.edp.demo.repository.UserRepository;
import com.epam.edp.demo.security.JwtTokenProvider;
import com.epam.edp.demo.service.BookingService;
import com.epam.edp.demo.util.DateUtil;
import com.epam.edp.demo.util.MealPlanFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final int FREE_CANCELLATION_DAYS_THRESHOLD = 10;
    private static final String AVAILABLE_SLOTS_FIELD = "availableSlots";

    private static final Set<BookingStatus> CANCELABLE_STATES =
            Set.of(BookingStatus.BOOKED, BookingStatus.CONFIRMED);

    private final BookingRepository bookingRepository;
    private final TourRepository tourRepository;
    private final TourInstanceRepository tourInstanceRepository;
    private final TravelAgentRepository travelAgentRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final MealPlanFormatter mealPlanFormatter;
    private final DateUtil dateUtil;
    private final MongoTemplate mongoTemplate;
    private final BookingDocumentRepository bookingDocumentRepository;
    private final MongoReviewRepository reviewRepository;
    private final RabbitTemplate rabbitTemplate;

    // =========================================================================
    // Sprint 1 methods (unchanged)
    // =========================================================================

    @Override
    public CreateBookingResponseDTO createBooking(CreateBookingRequestDTO request, String authorizationHeader) {
        extractUserIdFromToken(authorizationHeader);

        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw new BadRequestException("userId is required");
        }

        if (!tourRepository.existsById(request.getTourId())) {
            throw new TourNotFoundException(request.getTourId());
        }

        LocalDate requestedDate;
        try {
            requestedDate = LocalDate.parse(request.getDate());
        } catch (java.time.format.DateTimeParseException e) {
            throw new BadRequestException("Invalid date format: " + request.getDate() + ". Use YYYY-MM-DD", e);
        }

        if (requestedDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot book a tour that has already departed: " + request.getDate());
        }

        TourInstance instance = tourInstanceRepository
                .findByTourId(request.getTourId())
                .stream()
                .filter(i -> requestedDate.equals(i.getStartDate()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "No tour departure found for date: " + request.getDate()));

        List<Booking> existingBookings = bookingRepository
                .findByUserIdAndTourInstanceId(request.getUserId(), instance.getId());
        boolean hasActiveBooking = existingBookings.stream()
                .anyMatch(b -> b.getState() != BookingStatus.CANCELED
                        && b.getState() != BookingStatus.FINISHED);
        if (hasActiveBooking) {
            throw new BadRequestException(
                    "You already have an active booking for this tour departure on " + request.getDate());
        }

        int totalGuests = request.getGuests().getAdult() + request.getGuests().getChildren();
        decrementSlotsOrThrow(instance.getId(), totalGuests);

        Tour tour = tourRepository.findById(request.getTourId())
                .orElseThrow(() -> new TourNotFoundException(request.getTourId()));
        TravelAgent agent = (tour.getTravelAgentId() != null)
                ? travelAgentRepository.findById(tour.getTravelAgentId())
                        .orElseGet(travelAgentRepository::findAny)
                : travelAgentRepository.findAny();

        Booking booking = buildBooking(request, tour, instance, agent);
        bookingRepository.save(booking);
        publishBookingEvent(booking, tour, agent);

        log.info("booking.created bookingId={} userId={} tourId={} instanceId={}",
                booking.getId(), request.getUserId(), request.getTourId(), instance.getId());

        String freeCancellationDate = dateUtil.computeFreeCancellationDate(
                request.getDate(), tour.getFreeCancellationDays());
        String details = buildConfirmationDetails(request, tour);

        return new CreateBookingResponseDTO(freeCancellationDate, details);
    }

    @Override
    public BookedTourListResponseDTO getBookings(String userId, String authorizationHeader) {
        String callerId = extractUserIdFromToken(authorizationHeader);
        if (!callerId.equals(userId)) {
            throw new UnauthorizedException("You can only view your own bookings");
        }

        List<Booking> bookings = bookingRepository.findByUserId(userId);

        List<BookedTourDTO> dtos = bookings.stream()
                .map(this::mapBookingToDTO)
                .toList();

        log.debug("booking.list userId={} count={}", userId, dtos.size());
        return new BookedTourListResponseDTO(dtos);
    }

    @Override
    public void cancelBooking(String bookingId, String authorizationHeader) {
        String callerId = extractUserIdFromToken(authorizationHeader);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (!booking.getUserId().equals(callerId)) {
            throw new UnauthorizedException("You are not authorized to cancel this booking");
        }

        BookingStatus currentState = booking.getState();
        if (currentState == BookingStatus.STARTED
                || currentState == BookingStatus.FINISHED
                || currentState == BookingStatus.CANCELED) {
            throw new CancellationNotAllowedException(
                    "Booking in state " + currentState + " cannot be cancelled");
        }

        LocalDate startDate = LocalDate.parse(booking.getDate());
        LocalDate latestCancellationDate = startDate.minusDays(FREE_CANCELLATION_DAYS_THRESHOLD);
        if (LocalDate.now().isAfter(latestCancellationDate)) {
            throw new CancellationNotAllowedException(
                    "Cancellation is only allowed up to " + FREE_CANCELLATION_DAYS_THRESHOLD
                            + " days before the tour start date. Latest cancellation date was: "
                            + latestCancellationDate);
        }

        if (booking.getTourInstanceId() != null) {
            int totalGuests = (booking.getGuests() != null)
                    ? (booking.getGuests().getAdult() + booking.getGuests().getChildren())
                    : 1;
            restoreSlots(booking.getTourInstanceId(), totalGuests);
        }

        booking.setState(BookingStatus.CANCELED);
        booking.setCanceledBy(callerId);
        bookingRepository.save(booking);

        Tour tour = tourRepository.findById(booking.getTourId()).orElse(null);
        TravelAgent agent = booking.getTravelAgentId() != null
                ? travelAgentRepository.findById(booking.getTravelAgentId()).orElse(null) : null;
        publishBookingEvent(booking, tour, agent);

        log.info("booking.canceled bookingId={} canceledBy={}", bookingId, callerId);
    }

    @Override
    public void confirmBooking(String bookingId, String authorizationHeader) {
        String callerId = extractUserIdFromToken(authorizationHeader);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (!booking.getUserId().equals(callerId)) {
            throw new UnauthorizedException("You are not authorized to confirm this booking");
        }

        if (booking.getState() != BookingStatus.BOOKED) {
            throw new BadRequestException(
                    "Booking can only be confirmed from BOOKED state. Current state: " + booking.getState());
        }

        booking.setState(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        Tour confirmedTour = tourRepository.findById(booking.getTourId()).orElse(null);
        TravelAgent confirmedAgent = booking.getTravelAgentId() != null
                ? travelAgentRepository.findById(booking.getTravelAgentId()).orElse(null) : null;
        publishBookingEvent(booking, confirmedTour, confirmedAgent);

        log.info("booking.confirmed bookingId={} confirmedBy={}", bookingId, callerId);
    }

    @Override
    public BookedTourDTO getBooking(String bookingId, String authorizationHeader) {
        String callerId = extractUserIdFromToken(authorizationHeader);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        validateBookingAccess(booking, callerId);
        return mapBookingToDTO(booking);
    }

    @Override
    public void updateBooking(String bookingId, UpdateBookingRequestDTO request, String authorizationHeader) {
        String callerId = extractUserIdFromToken(authorizationHeader);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        // Allow the booking owner OR the assigned travel agent
        boolean isOwner = booking.getUserId().equals(callerId);
        boolean isAssignedAgent = false;
        if (!isOwner) {
            try {
                String agentId = extractAgentId(authorizationHeader);
                isAssignedAgent = agentId.equals(booking.getTravelAgentId());
            } catch (UnauthorizedException ignored) { /* not an agent */ }
        }
        if (!isOwner && !isAssignedAgent) {
            throw new UnauthorizedException("You are not authorized to update this booking");
        }

        // Only allow updates for BOOKED or CONFIRMED bookings
        if (booking.getState() == BookingStatus.CANCELED || booking.getState() == BookingStatus.STARTED
                || booking.getState() == BookingStatus.FINISHED) {
            throw new BadRequestException(
                    "Booking in state " + booking.getState() + " cannot be updated");
        }

        // When an agent edits, snapshot the ORIGINAL values BEFORE applying changes
        if (isAssignedAgent) {
            booking.setEditSnapshot(com.epam.edp.demo.entity.BookingEditSnapshot.builder()
                    .mealPlan(booking.getMealPlan())
                    .guests(booking.getGuests())
                    .personalDetails(booking.getPersonalDetails())
                    .totalPrice(booking.getTotalPrice())
                    .build());
        }

        if (request.getMealPlan() != null && !request.getMealPlan().isBlank()) {
            booking.setMealPlan(request.getMealPlan());
        }

        if (request.getGuests() != null) {
            booking.setGuests(new GuestCount(request.getGuests().getAdult(), request.getGuests().getChildren()));
        }

        if (request.getPersonalDetails() != null && !request.getPersonalDetails().isEmpty()) {
            booking.setPersonalDetails(request.getPersonalDetails().stream()
                    .map(p -> new PersonDetail(p.getFirstName(), p.getLastName()))
                    .toList());
        }

        if (request.getTotalPrice() != null && request.getTotalPrice() > 0) {
            booking.setTotalPrice(request.getTotalPrice());
        }

        if (isAssignedAgent) {
            booking.setCustomerApproval(CustomerApproval.builder()
                    .approvalMode(com.epam.edp.demo.enums.ApprovalMode.ONLINE)
                    .approvalGiven(false)
                    .approvalDate(Instant.now())
                    .build());
        }

        bookingRepository.save(booking);

        log.info("booking.updated bookingId={} updatedBy={} agentEdit={}", bookingId, callerId, isAssignedAgent);
    }

    // =========================================================================
    // Sprint 2 methods
    // =========================================================================

    @Override
    public TravelAgentBookingDetailDTO confirmBookingV2(String bookingId,
                                                         ConfirmBookingRequestDTO request,
                                                         String authorizationHeader) {
        String agentId = extractAgentId(authorizationHeader);
        Booking booking = requireBookingForAgent(bookingId, agentId);

        validateTransition(booking.getState(), BookingStatus.CONFIRMED);

        // All documents must be uploaded before confirmation
        List<BookingDocument> docs = bookingDocumentRepository.findByBookingId(bookingId);
        if (docs == null || docs.isEmpty()) {
            throw new DocumentVerificationException(
                    "Cannot confirm booking: no documents have been uploaded yet");
        }

        booking.setState(BookingStatus.CONFIRMED);
        booking.setConfirmation(ConfirmationDetails.builder()
                .confirmedBy(agentId)
                .confirmedAt(Instant.now())
                .notes(request.getNotes())
                .build());

        bookingRepository.save(booking);
        log.info("booking.confirmed.v2 bookingId={} agentId={}", bookingId, agentId);

        Tour t2 = tourRepository.findById(booking.getTourId()).orElse(null);
        TravelAgent a2 = travelAgentRepository.findById(agentId).orElse(null);
        publishBookingEvent(booking, t2, a2);

        return mapToAgentDetailDTO(booking);
    }

    @Override
    public TravelAgentBookingDetailDTO cancelBookingV2(String bookingId,
                                                        CancelBookingRequestDTO request,
                                                        String authorizationHeader) {
        String agentId = extractAgentId(authorizationHeader);
        Booking booking = requireBookingForAgent(bookingId, agentId);

        BookingStatus currentState = booking.getState();
        if (!CANCELABLE_STATES.contains(currentState)) {
            throw new InvalidBookingStateException(
                    "Booking in state " + currentState + " cannot be cancelled");
        }

        LocalDate startDate = LocalDate.parse(booking.getDate());
        boolean refundEligible = !LocalDate.now().isAfter(
                startDate.minusDays(FREE_CANCELLATION_DAYS_THRESHOLD));

        // Restore slots when cancelling
        if (booking.getTourInstanceId() != null) {
            int totalGuests = (booking.getGuests() != null)
                    ? (booking.getGuests().getAdult() + booking.getGuests().getChildren())
                    : 1;
            restoreSlots(booking.getTourInstanceId(), totalGuests);
        }

        booking.setState(BookingStatus.CANCELED);
        booking.setCanceledBy(agentId);
        booking.setCancelReason(request.getReason() != null ? request.getReason().name() : null);
        booking.setCancellation(CancellationDetails.builder()
                .reason(request.getReason())
                .reasonNote(request.getReasonNote())
                .canceledBy(agentId)
                .canceledAt(Instant.now())
                .refundEligible(refundEligible)
                .build());

        bookingRepository.save(booking);
        log.info("booking.canceled.v2 bookingId={} agentId={} refundEligible={}",
                bookingId, agentId, refundEligible);

        Tour t3 = tourRepository.findById(booking.getTourId()).orElse(null);
        TravelAgent a3 = travelAgentRepository.findById(agentId).orElse(null);
        publishBookingEvent(booking, t3, a3);

        return mapToAgentDetailDTO(booking);
    }

    @Override
    public TravelAgentBookingDetailDTO editBooking(String bookingId,
                                                    EditBookingRequestDTO request,
                                                    String authorizationHeader) {
        String agentId = extractAgentId(authorizationHeader);
        Booking booking = requireBookingForAgent(bookingId, agentId);

        if (booking.getState() != BookingStatus.BOOKED) {
            throw new InvalidBookingStateException(
                    "Booking can only be edited when in BOOKED state. Current state: " + booking.getState());
        }

        CustomerApproval approval = booking.getCustomerApproval();
        if (approval == null || !approval.isApprovalGiven()) {
            throw new BadRequestException(
                    "Customer approval is required before editing the booking");
        }

        if (request.getAdults() != null || request.getChildren() != null) {
            GuestCount current = booking.getGuests() != null
                    ? booking.getGuests() : new GuestCount(1, 0);
            int adults = request.getAdults() != null ? request.getAdults() : current.getAdult();
            int children = request.getChildren() != null ? request.getChildren() : current.getChildren();
            booking.setGuests(new GuestCount(adults, children));
        }
        if (request.getMealPlan() != null && !request.getMealPlan().isBlank()) {
            booking.setMealPlan(request.getMealPlan());
        }
        if (request.getDuration() != null) {
            booking.setDuration(String.valueOf(request.getDuration()));
        }

        // Reset approval after edit so next edit requires fresh approval
        booking.setCustomerApproval(null);

        bookingRepository.save(booking);
        log.info("booking.edited bookingId={} agentId={}", bookingId, agentId);

        return mapToAgentDetailDTO(booking);
    }

    @Override
    public TravelAgentBookingDetailDTO recordCustomerApproval(String bookingId,
                                                               CustomerApprovalRequestDTO request,
                                                               String authorizationHeader) {
        String agentId = extractAgentId(authorizationHeader);
        Booking booking = requireBookingForAgent(bookingId, agentId);

        if (booking.getState() != BookingStatus.BOOKED) {
            throw new InvalidBookingStateException(
                    "Customer approval can only be recorded for BOOKED bookings. Current state: "
                            + booking.getState());
        }

        booking.setCustomerApproval(CustomerApproval.builder()
                .approvalMode(request.getApprovalMode())
                .approvalGiven(Boolean.TRUE.equals(request.getApprovalGiven()))
                .approvalDate(Instant.now())
                .approvalNote(request.getApprovalNote())
                .build());

        bookingRepository.save(booking);
        log.info("booking.customerApproval bookingId={} approvalGiven={}", bookingId, request.getApprovalGiven());

        return mapToAgentDetailDTO(booking);
    }

    @Override
    public void approveBookingChange(String bookingId, String authorizationHeader) {
        String callerId = extractUserIdFromToken(authorizationHeader);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        if (!booking.getUserId().equals(callerId)) {
            throw new UnauthorizedException("Only the booking owner can approve changes");
        }
        if (booking.getCustomerApproval() == null || booking.getCustomerApproval().isApprovalGiven()) {
            throw new BadRequestException("No pending agent edit to approve");
        }
        booking.setCustomerApproval(CustomerApproval.builder()
                .approvalMode(com.epam.edp.demo.enums.ApprovalMode.ONLINE)
                .approvalGiven(true)
                .approvalDate(Instant.now())
                .build());
        booking.setEditSnapshot(null); // snapshot no longer needed
        bookingRepository.save(booking);
        log.info("booking.changeApproved bookingId={} customerId={}", bookingId, callerId);
    }

    @Override
    public void declineBookingChange(String bookingId, String authorizationHeader) {
        String callerId = extractUserIdFromToken(authorizationHeader);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        if (!booking.getUserId().equals(callerId)) {
            throw new UnauthorizedException("Only the booking owner can decline changes");
        }
        if (booking.getCustomerApproval() == null || booking.getCustomerApproval().isApprovalGiven()) {
            throw new BadRequestException("No pending agent edit to decline");
        }
        // Restore original values from snapshot
        com.epam.edp.demo.entity.BookingEditSnapshot snapshot = booking.getEditSnapshot();
        if (snapshot != null) {
            if (snapshot.getMealPlan() != null)       booking.setMealPlan(snapshot.getMealPlan());
            if (snapshot.getGuests() != null)         booking.setGuests(snapshot.getGuests());
            if (snapshot.getPersonalDetails() != null) booking.setPersonalDetails(snapshot.getPersonalDetails());
            if (snapshot.getTotalPrice() != null)     booking.setTotalPrice(snapshot.getTotalPrice());
            booking.setEditSnapshot(null);
        }
        booking.setCustomerApproval(null);
        bookingRepository.save(booking);
        log.info("booking.changeDeclined bookingId={} customerId={}", bookingId, callerId);
    }

    @Override
    public TravelAgentBookingDetailDTO uploadDocument(String bookingId,
                                                       UploadDocumentRequestDTO request,
                                                       String authorizationHeader) {
        String callerId = extractUserIdFromToken(authorizationHeader);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        validateBookingAccess(booking, callerId);

        if (booking.getState() == BookingStatus.CANCELED
                || booking.getState() == BookingStatus.FINISHED) {
            throw new InvalidBookingStateException(
                    "Documents cannot be uploaded for a " + booking.getState() + " booking");
        }

        BookingDocument doc = BookingDocument.builder()
                .id(UUID.randomUUID().toString())
                .type(request.getType())
                .fileName(request.getFileName())
                .fileUrl(request.getFileUrl())
                .uploadedAt(Instant.now())
                .verified(false)
                .build();

        if (booking.getDocuments() == null) {
            booking.setDocuments(new ArrayList<>());
        }
        booking.getDocuments().add(doc);
        bookingRepository.save(booking);

        log.info("booking.document.uploaded bookingId={} docId={} type={}",
                bookingId, doc.getId(), doc.getType());

        return mapToAgentDetailDTO(booking);
    }

    @Override
    public TravelAgentBookingDetailDTO verifyDocument(String bookingId,
                                                       DocumentVerificationRequestDTO request,
                                                       String authorizationHeader) {
        String agentId = extractAgentId(authorizationHeader);
        Booking booking = requireBookingForAgent(bookingId, agentId);

        if (booking.getDocuments() == null || booking.getDocuments().isEmpty()) {
            throw new DocumentVerificationException("No documents found for booking " + bookingId);
        }

        BookingDocument target = booking.getDocuments().stream()
                .filter(d -> d.getId().equals(request.getDocumentId()))
                .findFirst()
                .orElseThrow(() -> new DocumentVerificationException(
                        "Document not found: " + request.getDocumentId()));

        boolean approve = "APPROVE".equalsIgnoreCase(request.getAction());
        if (!"APPROVE".equalsIgnoreCase(request.getAction())
                && !"REJECT".equalsIgnoreCase(request.getAction())) {
            throw new BadRequestException("action must be APPROVE or REJECT");
        }

        target.setVerified(approve);
        target.setVerifiedBy(agentId);
        target.setVerifiedAt(approve ? Instant.now() : null);

        bookingRepository.save(booking);
        log.info("booking.document.verified bookingId={} docId={} action={}",
                bookingId, request.getDocumentId(), request.getAction());

        return mapToAgentDetailDTO(booking);
    }

    @Override
    public AgentBookingListResponseDTO getAgentBookings(String stateFilter, String authorizationHeader) {
        String agentId = extractAgentId(authorizationHeader);

        List<Booking> bookings;
        if (stateFilter != null && !stateFilter.isBlank()) {
            try {
                BookingStatus status = BookingStatus.valueOf(stateFilter.toUpperCase());
                bookings = bookingRepository.findByTravelAgentIdAndState(agentId, status);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status filter: " + stateFilter);
            }
        } else {
            bookings = bookingRepository.findByTravelAgentId(agentId);
        }

        List<TravelAgentBookingDetailDTO> dtos = bookings.stream()
                .map(this::mapToAgentDetailDTO)
                .toList();

        log.debug("agent.bookings.list agentId={} count={}", agentId, dtos.size());
        return new AgentBookingListResponseDTO(dtos, dtos.size());
    }

    @Override
    public TravelAgentBookingDetailDTO getAgentBookingById(String bookingId, String authorizationHeader) {
        String agentId = extractAgentId(authorizationHeader);
        Booking booking = requireBookingForAgent(bookingId, agentId);
        return mapToAgentDetailDTO(booking);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Booking requireBookingForAgent(String bookingId, String agentId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        if (!agentId.equals(booking.getTravelAgentId())) {
            throw new UnauthorizedException("You are not the assigned agent for this booking");
        }
        return booking;
    }

    private void validateBookingAccess(Booking booking, String callerId) {
        if (!callerId.equals(booking.getUserId())
                && !callerId.equals(booking.getTravelAgentId())) {
            throw new UnauthorizedException("You do not have access to this booking");
        }
    }

    private void validateTransition(BookingStatus from, BookingStatus to) {
        boolean valid = switch (from) {
            case BOOKED -> to == BookingStatus.CONFIRMED || to == BookingStatus.CANCELED;
            case CONFIRMED -> to == BookingStatus.STARTED || to == BookingStatus.CANCELED;
            case STARTED -> to == BookingStatus.FINISHED;
            default -> false;
        };
        if (!valid) {
            throw new InvalidBookingStateException(from, to);
        }
    }

    private TravelAgentBookingDetailDTO mapToAgentDetailDTO(Booking booking) {
        TravelAgentBookingDetailDTO dto = bookingMapper.toAgentDetailDTO(booking,
                tourRepository.findById(booking.getTourId()).orElse(null),
                userRepository.findById(booking.getUserId()).orElse(null),
                tourInstanceRepository.findByTourId(booking.getTourId()));
        // Override documentCount from the dedicated booking_documents collection
        List<com.epam.edp.demo.entity.BookingDocument> docs =
                bookingDocumentRepository.findByBookingId(booking.getId());
        int realCount = (docs != null) ? docs.size() : 0;
        dto.setDocumentCount(realCount);
        dto.setVerifiedDocumentCount((int) (docs != null
                ? docs.stream().filter(com.epam.edp.demo.entity.BookingDocument::isVerified).count()
                : 0));
        return dto;
    }

    private BookedTourDTO mapBookingToDTO(Booking booking) {
        Tour tour = tourRepository.findById(booking.getTourId())
                .orElseThrow(() -> new TourNotFoundException(booking.getTourId()));
        List<TourInstance> instances = tourInstanceRepository.findByTourId(booking.getTourId());
        TravelAgent agent = (booking.getTravelAgentId() != null)
                ? travelAgentRepository.findById(booking.getTravelAgentId())
                        .orElseGet(travelAgentRepository::findAny)
                : travelAgentRepository.findAny();
        BookedTourDTO dto = bookingMapper.toBookedTourDTO(booking, tour, instances, agent);

        // Populate document count from the dedicated booking_documents collection
        List<com.epam.edp.demo.entity.BookingDocument> docs =
                bookingDocumentRepository.findByBookingId(booking.getId());
        if (docs != null && !docs.isEmpty()) {
            List<com.epam.edp.demo.dto.booking.BookingDocumentDTO> docDTOs = docs.stream()
                    .map(d -> com.epam.edp.demo.dto.booking.BookingDocumentDTO.builder()
                            .id(d.getId())
                            .fileName(d.getFileName())
                            .verified(d.isVerified())
                            .uploadedAt(d.getUploadedAt())
                            .build())
                    .toList();
            dto.setDocuments(docDTOs);
        }

        dto.setHasReview(reviewRepository.existsByUserIdAndTourId(booking.getUserId(), booking.getTourId()));

        return dto;
    }

    private Booking buildBooking(CreateBookingRequestDTO request, Tour tour,
                                  TourInstance instance, TravelAgent agent) {
        Booking booking = new Booking();
        booking.setId(UUID.randomUUID().toString());
        booking.setUserId(request.getUserId());
        booking.setTourId(tour.getId());
        booking.setTourInstanceId(instance.getId());
        booking.setState(BookingStatus.BOOKED);
        booking.setDate(request.getDate());
        booking.setDuration(request.getDuration());
        booking.setMealPlan(request.getMealPlan());
        booking.setGuests(new GuestCount(request.getGuests().getAdult(), request.getGuests().getChildren()));
        booking.setPersonalDetails(request.getPersonalDetails().stream()
                .map(p -> new PersonDetail(p.getFirstName(), p.getLastName()))
                .toList());
        booking.setTravelAgentId(agent.getId());
        booking.setTotalPrice(request.getTotalPrice());
        return booking;
    }

    // =========================================================================
    // RabbitMQ event publishing
    // =========================================================================

    private void publishBookingEvent(Booking booking, Tour tour, TravelAgent agent) {
        try {
            int guests = booking.getGuests() != null
                    ? booking.getGuests().getAdult() + booking.getGuests().getChildren() : 1;
            // Revenue: use actual booking total price, fallback to $1,000 per person if not set
            double revenue = booking.getTotalPrice() != null ? booking.getTotalPrice() : guests * 1_000.0;

            TourStatsEvent event = TourStatsEvent.builder()
                    .bookingId(booking.getId())
                    .travelAgentId(booking.getTravelAgentId())
                    .agentName(agent != null ? agent.getName() : "")
                    .agentEmail(agent != null ? agent.getEmail() : "")
                    .tourId(booking.getTourId())
                    .tourName(tour != null ? tour.getName() : "")
                    .country(tour != null && tour.getDestination() != null ? tour.getDestination().getCountry() : "")
                    .city(tour != null && tour.getDestination() != null ? tour.getDestination().getCity() : "")
                    .bookingStatus(booking.getState().name())
                    .touristCount(guests)
                    .revenue(revenue)
                    .eventTimestamp(java.time.Instant.now())
                    .build();

            rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.BOOKING_ROUTING, event);
            log.info("rabbitmq.published bookingId={} status={}", booking.getId(), booking.getState());
        } catch (Exception ex) {
            log.warn("rabbitmq.publish.failed bookingId={} reason={}", booking.getId(), ex.getMessage());
        }
    }

    private void decrementSlotsOrThrow(String instanceId, int guests) {
        Query query = new Query(
                Criteria.where("_id").is(instanceId)
                        .and(AVAILABLE_SLOTS_FIELD).gte(guests));
        Update update = new Update().inc(AVAILABLE_SLOTS_FIELD, -guests);
        TourInstance prior = mongoTemplate.findAndModify(
                query, update,
                FindAndModifyOptions.options().returnNew(false),
                TourInstance.class);
        if (prior == null) {
            throw new OverbookingException("No available slots for the requested tour departure");
        }
    }

    private void restoreSlots(String instanceId, int guests) {
        Query query = new Query(Criteria.where("_id").is(instanceId));
        Update update = new Update().inc(AVAILABLE_SLOTS_FIELD, guests);
        mongoTemplate.updateFirst(query, update, TourInstance.class);
    }

    private String extractUserIdFromToken(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new UnauthorizedException("Valid authentication token required");
        }
        return jwtTokenProvider.getUserIdFromToken(token);
    }

    /**
     * Resolves the travel agent's canonical ID (e.g. "a-004") from the JWT email claim.
     * The JWT subject is a User UUID, but bookings store the travel_agents collection ID.
     */
    private String extractAgentId(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new UnauthorizedException("Valid authentication token required");
        }
        String email = jwtTokenProvider.getEmailFromToken(token);
        return travelAgentRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Not a registered travel agent"))
                .getId();
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith(SecurityUtils.BEARER_PREFIX)) {
            return authorizationHeader.substring(SecurityUtils.BEARER_PREFIX.length());
        }
        return null;
    }

    private String buildConfirmationDetails(CreateBookingRequestDTO request, Tour tour) {
        LocalDate date = LocalDate.parse(request.getDate());
        String formattedDate = date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH));
        String mealPlanDisplay = mealPlanFormatter.format(request.getMealPlan());
        int adults = request.getGuests().getAdult();
        String guestText = adults + " adult" + (adults != 1 ? "s" : "");

        String hotelOrTour = (tour.getHotel() != null && tour.getHotel().getName() != null)
                ? tour.getHotel().getName() : tour.getName();

        return String.format(
                "You have booked at %s, starting date %s (%s), %s for %s successfully. "
                        + "Please upload your travel documents to the booking on the 'My Tours' page "
                        + "and wait for the Travel Agent to contact you.",
                hotelOrTour, formattedDate, request.getDuration(), mealPlanDisplay, guestText);
    }
}
