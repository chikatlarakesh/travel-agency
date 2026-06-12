package com.epam.edp.demo.exception;

import com.epam.edp.demo.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String GENERIC_AUTH_ERROR = "Wrong password or email";
    private static final String INTERNAL_ERROR_MESSAGE = "Internal server error";

    @ExceptionHandler(AuthFailedException.class)
    public ResponseEntity<Map<String, String>> handleAuthFailed(AuthFailedException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", GENERIC_AUTH_ERROR));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized access attempt: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(CaptchaValidationException.class)
    public ResponseEntity<ErrorResponse> handleCaptchaValidation(CaptchaValidationException ex) {
        log.warn("captcha.validation.rejected message={}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        log.debug("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailExists(EmailAlreadyExistsException ex) {
        log.debug("Email already exists: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("Email already exists"));
    }

    @ExceptionHandler(TourNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTourNotFound(TourNotFoundException ex) {
        log.debug("Tour not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleBookingNotFound(BookingNotFoundException ex) {
        log.debug("Booking not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDocumentNotFound(DocumentNotFoundException ex) {
        log.debug("Document not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(DocumentsAlreadyExistException.class)
    public ResponseEntity<ErrorResponse> handleDocumentsAlreadyExist(DocumentsAlreadyExistException ex) {
        log.debug("Documents already exist: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(OverbookingException.class)
    public ResponseEntity<ErrorResponse> handleOverbooking(OverbookingException ex) {
        log.warn("Overbooking attempt: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(FeedbackAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleFeedbackAlreadyExists(FeedbackAlreadyExistsException ex) {
        log.debug("Duplicate feedback attempt: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        log.debug("User not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(CancellationNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleCancellationNotAllowed(CancellationNotAllowedException ex) {
        log.debug("Cancellation not allowed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        log.debug("Missing request parameter: {}", ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("Required parameter '" + ex.getParameterName() + "' is missing"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        log.debug("Validation failed: {}", details);
        String message = details.isEmpty() ? "Validation failed" : details.get(0);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        if (details.size() > 1) {
            body.put("details", details);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.debug("Malformed request body: {}", ex.getMessage());
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof UnrecognizedPropertyException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of("Request body contains unexpected fields."));
        }
        if (cause instanceof InvalidFormatException invalidFormatException
                && String.class.equals(invalidFormatException.getTargetType())) {
            String fieldName = extractFieldName(invalidFormatException);
            String message = (fieldName == null || fieldName.isBlank())
                    ? "Value must be of type string"
                    : fieldName + " must be of type string";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of("Validation failed", List.of(message)));
        }

        String message = "Malformed JSON request";
        if (ex.getMessage() != null && ex.getMessage().contains("Expected a string value")) {
            String fieldName = null;
            if (cause instanceof JsonMappingException jme && !jme.getPath().isEmpty()) {
                fieldName = jme.getPath().get(jme.getPath().size() - 1).getFieldName();
            }
            message = (fieldName != null && !fieldName.isBlank())
                    ? fieldName + " must be a valid string."
                    : "Value must be a valid string.";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(message));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex) {
        log.warn("Rate limit exceeded");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(ErrorResponse.of("Too many requests"));
    }

    @ExceptionHandler(InvalidBookingStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBookingState(InvalidBookingStateException ex) {
        log.debug("Invalid booking state transition: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(DocumentVerificationException.class)
    public ResponseEntity<ErrorResponse> handleDocumentVerification(DocumentVerificationException ex) {
        log.debug("Document verification error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ex.getMessage()));
    }

    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<ErrorResponse> handleEmailDelivery(EmailDeliveryException ex) {
        log.error("Email delivery failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of("Unable to send verification email. Please try again later."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(INTERNAL_ERROR_MESSAGE));
    }

    private String extractFieldName(InvalidFormatException invalidFormatException) {
        if (invalidFormatException.getPath().isEmpty()) {
            return null;
        }
        JsonMappingException.Reference reference = invalidFormatException.getPath()
                .get(invalidFormatException.getPath().size() - 1);
        return reference.getFieldName();
    }
}