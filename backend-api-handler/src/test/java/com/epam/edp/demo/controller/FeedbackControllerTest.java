package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.user.MessageResponseDTO;
import com.epam.edp.demo.exception.BadRequestException;
import com.epam.edp.demo.exception.FeedbackAlreadyExistsException;
import com.epam.edp.demo.exception.TourNotFoundException;
import com.epam.edp.demo.security.JwtAuthenticationFilter;
import com.epam.edp.demo.security.JwtTokenProvider;
import com.epam.edp.demo.security.RateLimitFilter;
import com.epam.edp.demo.service.TourService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-layer tests for POST /api/v1/tours/{id}/feedbacks.
 *
 * Uses @WebMvcTest — no MongoDB connection required.
 * TourService is mocked; Spring Security filters are disabled via addFilters=false
 * so tests exercise the controller + GlobalExceptionHandler in isolation.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(value = TourController.class, excludeAutoConfiguration = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
public class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TourService tourService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitFilter rateLimitFilter;

    // ── Success scenarios ─────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_validHighRating_returns201WithMessage() throws Exception {
        when(tourService.submitFeedback(eq("tour-1"), any(), anyString()))
                .thenReturn(new MessageResponseDTO("Your feedback has been submitted."));

        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 5, \"comment\": \"Amazing experience!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Your feedback has been submitted."));
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_highRatingNoComment_returns201() throws Exception {
        when(tourService.submitFeedback(eq("tour-1"), any(), anyString()))
                .thenReturn(new MessageResponseDTO("Your feedback has been submitted."));

        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 4}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Your feedback has been submitted."));
    }

    // ── DTO annotation failures ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_missingRating_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\": \"Nice trip\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_ratingZero_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_ratingSix_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 6}"))
                .andExpect(status().isBadRequest());
    }

    // ── Service-exception propagation ────────────────────────────────────────

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_tourNotFound_returns404() throws Exception {
        when(tourService.submitFeedback(eq("bad-tour"), any(), anyString()))
                .thenThrow(new TourNotFoundException("bad-tour"));

        mockMvc.perform(post("/api/v1/tours/bad-tour/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 5}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_commentRequiredForLowRating_returns400() throws Exception {
        when(tourService.submitFeedback(eq("tour-1"), any(), anyString()))
                .thenThrow(new BadRequestException("Comment is required when rating is 3 or below"));

        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Comment is required when rating is 3 or below"));
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_duplicateFeedback_returns409() throws Exception {
        when(tourService.submitFeedback(eq("tour-1"), any(), anyString()))
                .thenThrow(new FeedbackAlreadyExistsException());

        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 5}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_noValidBooking_returns400() throws Exception {
        when(tourService.submitFeedback(eq("tour-1"), any(), anyString()))
                .thenThrow(new BadRequestException(
                        "You must have an active or completed booking for this tour to submit feedback"));

        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_serviceThrowsRuntimeException_returns500() throws Exception {
        when(tourService.submitFeedback(eq("tour-1"), any(), anyString()))
                .thenThrow(new RuntimeException("Unexpected DB error"));

        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 5}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal server error"));
    }

    // ── Section 1: Validation edge cases ─────────────────────────────────────

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_ratingNegativeOne_returns400() throws Exception {
        // @Min(1) rejects -1 before the controller body runs
        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": -1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_nullRatingInJson_returns400WithRatingRequiredMessage() throws Exception {
        // Explicit null rating → @NotNull fires with "Rating is required"
        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": null, \"comment\": \"Hello\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Rating is required"));
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_emptyJsonBody_returns400WithRatingRequiredMessage() throws Exception {
        // {} — rating key absent; @NotNull fires with "Rating is required"
        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Rating is required"));
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_malformedJson_returns400() throws Exception {
        // Unparseable JSON → HttpMessageNotReadableException → 400
        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{rating: no_quotes}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_ratingAsString_returns400() throws Exception {
        // "five" cannot be coerced into Integer → 400
        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": \"five\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_wrongContentType_returnsError() throws Exception {
        // text/plain triggers HttpMediaTypeNotSupportedException, which is caught by
        // GlobalExceptionHandler's generic @ExceptionHandler(Exception.class) → 500.
        // The important assertion is that the endpoint does NOT return 2xx for wrong content types.
        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{\"rating\": 5}"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_veryLongComment_returns201() throws Exception {
        // No @Size constraint on comment — 2000-char comment must be accepted
        String longComment = "A".repeat(2000);
        when(tourService.submitFeedback(eq("tour-1"), any(), anyString()))
                .thenReturn(new MessageResponseDTO("Your feedback has been submitted."));

        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 5, \"comment\": \"" + longComment + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Your feedback has been submitted."));
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_xssScriptTagInComment_returns201() throws Exception {
        // HTML/script is stored as plain text — no rejection at this layer
        when(tourService.submitFeedback(eq("tour-1"), any(), anyString()))
                .thenReturn(new MessageResponseDTO("Your feedback has been submitted."));

        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 5, \"comment\": \"<script>alert('xss')<\\/script>\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Your feedback has been submitted."));
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_emojiInComment_returns201() throws Exception {
        // Multi-byte emoji must not cause any parsing or validation error
        when(tourService.submitFeedback(eq("tour-1"), any(), anyString()))
                .thenReturn(new MessageResponseDTO("Your feedback has been submitted."));

        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 5, \"comment\": \"Amazing! \\uD83C\\uDF0D\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_specialCharsInComment_returns201() throws Exception {
        // Escaped quotes inside a JSON string must parse correctly
        when(tourService.submitFeedback(eq("tour-1"), any(), anyString()))
                .thenReturn(new MessageResponseDTO("Your feedback has been submitted."));

        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 4, \"comment\": \"Great! \\\"Highly\\\" recommend it.\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_rating1WithComment_returns201() throws Exception {
        // Minimum valid rating (1) + required comment → passes DTO validation
        when(tourService.submitFeedback(eq("tour-1"), any(), anyString()))
                .thenReturn(new MessageResponseDTO("Your feedback has been submitted."));

        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 1, \"comment\": \"Very poor experience.\"}"))
                .andExpect(status().isCreated());
    }

    // ── Section 2: Authorization ──────────────────────────────────────────────

    @Test
    public void submitFeedback_noAuthentication_returns401() throws Exception {
        // No @WithMockUser → security context empty →
        // SecurityUtils.getCurrentUserId() throws UnauthorizedException → 401
        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 5}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    public void submitFeedback_noAuthentication_serviceNeverCalled() throws Exception {
        // Unauthenticated requests must never reach the service layer
        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 5}"))
                .andExpect(status().isUnauthorized());

        verify(tourService, never()).submitFeedback(any(), any(), any());
    }

    // ── Section 3: Response structure ────────────────────────────────────────

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_successResponse_hasMessageFieldNotErrorField() throws Exception {
        // 201 body must use "message" key, not "error"
        when(tourService.submitFeedback(eq("tour-1"), any(), anyString()))
                .thenReturn(new MessageResponseDTO("Your feedback has been submitted."));

        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_404Response_containsExactTourIdInErrorMessage() throws Exception {
        // TourNotFoundException formats the message as "Tour not found: <id>"
        when(tourService.submitFeedback(eq("t-xyz"), any(), anyString()))
                .thenThrow(new TourNotFoundException("t-xyz"));

        mockMvc.perform(post("/api/v1/tours/t-xyz/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 5}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Tour not found: t-xyz"));
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_409Response_containsFixedDuplicateMessage() throws Exception {
        // FeedbackAlreadyExistsException hardcodes its message
        when(tourService.submitFeedback(eq("tour-1"), any(), anyString()))
                .thenThrow(new FeedbackAlreadyExistsException());

        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 5}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("You have already submitted feedback for this tour"));
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_dtoValidationError_usesMessageKeyNotErrorKey() throws Exception {
        // MethodArgumentNotValidException handler returns {"message":"..."}, NOT {"error":"..."}
        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Rating must be between 1 and 5"))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_successResponse_contentTypeIsJson() throws Exception {
        when(tourService.submitFeedback(eq("tour-1"), any(), anyString()))
                .thenReturn(new MessageResponseDTO("Your feedback has been submitted."));

        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 5}"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_errorResponse_contentTypeIsJson() throws Exception {
        when(tourService.submitFeedback(eq("tour-1"), any(), anyString()))
                .thenThrow(new TourNotFoundException("tour-1"));

        mockMvc.perform(post("/api/v1/tours/tour-1/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 5}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(username = "user-123")
    public void submitFeedback_tourIdPathVariable_forwardedCorrectlyToService() throws Exception {
        // Verifies the {id} path variable is extracted and passed to the service unchanged
        when(tourService.submitFeedback(eq("specific-tour-999"), any(), anyString()))
                .thenReturn(new MessageResponseDTO("Your feedback has been submitted."));

        mockMvc.perform(post("/api/v1/tours/specific-tour-999/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\": 5}"))
                .andExpect(status().isCreated());

        verify(tourService).submitFeedback(eq("specific-tour-999"), any(), anyString());
    }
}


