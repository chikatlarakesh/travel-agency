package com.epam.edp.demo.exception;
import com.epam.edp.demo.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    @Test
    @DisplayName("AuthFailedException returns 400 with generic message")
    void handleAuthFailed_returns400Generic() {
        ResponseEntity<Map<String, String>> response = handler.handleAuthFailed(new AuthFailedException());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Wrong password or email");
    }
    @Test
    @DisplayName("EmailAlreadyExistsException returns 409")
    void handleEmailConflict_returns409() {
        ResponseEntity<ErrorResponse> response =
                handler.handleEmailExists(new EmailAlreadyExistsException("test@example.com"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getError()).isEqualTo("Email already exists");
    }
    @Test
    @DisplayName("TourNotFoundException returns 404")
    void handleTourNotFound_returns404() {
        ResponseEntity<ErrorResponse> response =
                handler.handleTourNotFound(new TourNotFoundException("t-99"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    @Test
    @DisplayName("UnauthorizedException returns 401")
    void handleUnauthorized_returns401() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnauthorized(new UnauthorizedException("No token"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getError()).isEqualTo("No token");
    }
    @Test
    @DisplayName("BadRequestException returns 400")
    void handleBadRequest_returns400() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBadRequest(new BadRequestException("Missing field"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError()).isEqualTo("Missing field");
    }
    @Test
    @DisplayName("RateLimitExceededException returns 429 with Retry-After")
    void handleRateLimit_returns429() {
        ResponseEntity<ErrorResponse> response = handler.handleRateLimit(new RateLimitExceededException(900));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("900");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).contains("Too many requests");
    }
    @Test
    @DisplayName("Generic exception returns 500 with no internal details")
    void handleGeneric_returns500NoDetails() {
        ResponseEntity<ErrorResponse> response = handler.handleGeneric(new RuntimeException("DB crashed"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Internal server error");
        assertThat(response.getBody().getError()).doesNotContain("DB crashed");
    }

    @Test
    @DisplayName("BookingNotFoundException returns 404")
    void handleBookingNotFound_returns404() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBookingNotFound(new BookingNotFoundException("b-99"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("OverbookingException returns 409")
    void handleOverbooking_returns409() {
        ResponseEntity<ErrorResponse> response =
                handler.handleOverbooking(new OverbookingException("No slots"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getError()).isEqualTo("No slots");
    }

    @Test
    @DisplayName("CancellationNotAllowedException returns 400")
    void handleCancellationNotAllowed_returns400() {
        ResponseEntity<ErrorResponse> response =
                handler.handleCancellationNotAllowed(new CancellationNotAllowedException("Too late"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError()).isEqualTo("Too late");
    }

    @Test
    @DisplayName("MissingServletRequestParameterException returns 400 with parameter name")
    void handleMissingParam_returns400WithParamName() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("userId", "String");
        ResponseEntity<ErrorResponse> response = handler.handleMissingParam(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError()).contains("userId");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException returns 400 with validation details")
    void handleValidation_returns400WithDetails() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("obj", "email", "must not be blank");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message")).isEqualTo("must not be blank");
    }

    // ──────────────────────────────────────────────────────────────
    // handleMessageNotReadable — new coverage for numeric name fields
    // ──────────────────────────────────────────────────────────────

    private static HttpMessageNotReadableException notReadable(Throwable cause) {
        return new HttpMessageNotReadableException("bad request", cause,
                new MockHttpInputMessage(new byte[0]));
    }

    @Test
    @DisplayName("Numeric firstName token: returns 400 with 'firstName must be of type string'")
    void handleMessageNotReadable_numericFirstName_returns400WithFieldMessage() {
        InvalidFormatException cause = InvalidFormatException.from(null, "must be of type string", 123, String.class);
        cause.prependPath(null, "firstName");

        ResponseEntity<?> response = handler.handleMessageNotReadable(notReadable(cause));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body.getError()).isEqualTo("Validation failed");
        assertThat(body.getDetails()).containsExactly("firstName must be of type string");
    }

    @Test
    @DisplayName("Numeric lastName token: returns 400 with 'lastName must be of type string'")
    void handleMessageNotReadable_numericLastName_returns400WithFieldMessage() {
        InvalidFormatException cause = InvalidFormatException.from(null, "must be of type string", 456, String.class);
        cause.prependPath(null, "lastName");

        ResponseEntity<?> response = handler.handleMessageNotReadable(notReadable(cause));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body.getError()).isEqualTo("Validation failed");
        assertThat(body.getDetails()).containsExactly("lastName must be of type string");
    }

    @Test
    @DisplayName("InvalidFormatException without path: returns 400 with generic type message")
    void handleMessageNotReadable_noPath_returnsGenericTypeMessage() {
        // no prependPath call → path is empty
        InvalidFormatException cause = InvalidFormatException.from(null, "must be of type string", 99, String.class);

        ResponseEntity<?> response = handler.handleMessageNotReadable(notReadable(cause));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body.getError()).isEqualTo("Validation failed");
        assertThat(body.getDetails()).containsExactly("Value must be of type string");
    }

    @Test
    @DisplayName("InvalidFormatException targeting non-String type: returns 400 with 'Malformed JSON request'")
    void handleMessageNotReadable_nonStringTargetType_returnsMalformedJsonMessage() {
        InvalidFormatException cause = InvalidFormatException.from(null, "bad value", "notAnInt", Integer.class);

        ResponseEntity<?> response = handler.handleMessageNotReadable(notReadable(cause));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body.getError()).isEqualTo("Malformed JSON request");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException with non-InvalidFormatException cause: returns 400 'Malformed JSON request'")
    void handleMessageNotReadable_genericCause_returnsMalformedJsonMessage() {
        ResponseEntity<?> response = handler.handleMessageNotReadable(
                notReadable(new RuntimeException("completely broken JSON")));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body.getError()).isEqualTo("Malformed JSON request");
    }

    // ──────────────────────────────────────────────────────────────
    // Bug fix: numeric lastName in PUT /users/{id}/name — QA regression
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Numeric lastName via MismatchedInputException: returns 'lastName must be a valid string.' (not 'Password')")
    @SuppressWarnings("unchecked")
    void handleMessageNotReadable_numericLastNameViaMismatch_returnsLastNameMessage() {
        // Simulates JacksonConfig.StrictStringDeserializer calling ctxt.reportInputMismatch(String.class, "Expected a string value ...")
        MismatchedInputException cause = MismatchedInputException.from(
                (com.fasterxml.jackson.core.JsonParser) null,
                String.class,
                "Expected a string value but got: VALUE_NUMBER_INT");
        cause.prependPath(null, "lastName");

        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error: Expected a string value but got: VALUE_NUMBER_INT",
                cause,
                new MockHttpInputMessage(new byte[0]));

        ResponseEntity<?> response = handler.handleMessageNotReadable(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body.getError()).isEqualTo("lastName must be a valid string.");
    }

    @Test
    @DisplayName("Numeric firstName via MismatchedInputException: returns 'firstName must be a valid string.' (not 'Password')")
    @SuppressWarnings("unchecked")
    void handleMessageNotReadable_numericFirstNameViaMismatch_returnsFirstNameMessage() {
        MismatchedInputException cause = MismatchedInputException.from(
                (com.fasterxml.jackson.core.JsonParser) null,
                String.class,
                "Expected a string value but got: VALUE_NUMBER_INT");
        cause.prependPath(null, "firstName");

        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error: Expected a string value but got: VALUE_NUMBER_INT",
                cause,
                new MockHttpInputMessage(new byte[0]));

        ResponseEntity<?> response = handler.handleMessageNotReadable(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body.getError()).isEqualTo("firstName must be a valid string.");
    }

    // ──────────────────────────────────────────────────────────────
    // Bug fix: unknown fields in request body — QA regression
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Unknown field in request body: returns 400 'Request body contains unexpected fields.'")
    @SuppressWarnings("unchecked")
    void handleMessageNotReadable_unknownField_returns400WithUnexpectedFieldsMessage() {
        UnrecognizedPropertyException cause = UnrecognizedPropertyException.from(
                mock(com.fasterxml.jackson.core.JsonParser.class),
                Object.class,
                "password",
                null);

        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error: Unrecognized field \"password\"",
                cause,
                new MockHttpInputMessage(new byte[0]));

        ResponseEntity<?> response = handler.handleMessageNotReadable(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertThat(body.getError()).isEqualTo("Request body contains unexpected fields.");
    }
}
