package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.booking.BookedTourListResponseDTO;
import com.epam.edp.demo.dto.booking.CreateBookingResponseDTO;
import com.epam.edp.demo.dto.booking.TravelAgentBookingDetailDTO;
import com.epam.edp.demo.exception.BookingNotFoundException;
import com.epam.edp.demo.exception.CancellationNotAllowedException;
import com.epam.edp.demo.exception.GlobalExceptionHandler;
import com.epam.edp.demo.exception.UnauthorizedException;
import com.epam.edp.demo.security.JwtAuthenticationFilter;
import com.epam.edp.demo.security.JwtTokenProvider;
import com.epam.edp.demo.security.RateLimitFilter;
import com.epam.edp.demo.service.BookingDocumentService;
import com.epam.edp.demo.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = BookingController.class, excludeAutoConfiguration = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BookingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private BookingDocumentService documentService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitFilter rateLimitFilter;

    @Test
    void createBooking_validRequest_returns201() throws Exception {
        String requestBody = "{"
                + "\"userId\":\"u-001\","
                + "\"tourId\":\"t-001\","
                + "\"date\":\"2025-06-15\","
                + "\"duration\":\"7 days\","
                + "\"mealPlan\":\"BB\","
                + "\"guests\":{\"adult\":2,\"children\":0},"
                + "\"personalDetails\":[{\"firstName\":\"John\",\"lastName\":\"Doe\"}]"
                + "}";

        CreateBookingResponseDTO response = new CreateBookingResponseDTO("Yes, up to 7 days before", "Booking confirmed");
        when(bookingService.createBooking(any(), eq("Bearer valid-token"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.freeCancelation").isNotEmpty())
                .andExpect(jsonPath("$.details").isNotEmpty());
    }

    @Test
    void createBooking_noToken_returns401() throws Exception {
        String requestBody = "{"
                + "\"userId\":\"u-001\","
                + "\"tourId\":\"t-001\","
                + "\"date\":\"2025-06-15\","
                + "\"duration\":\"7 days\","
                + "\"mealPlan\":\"BB\","
                + "\"guests\":{\"adult\":1,\"children\":0},"
                + "\"personalDetails\":[{\"firstName\":\"John\",\"lastName\":\"Doe\"}]"
                + "}";

        when(bookingService.createBooking(any(), isNull()))
                .thenThrow(new UnauthorizedException("Unauthorized"));

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getBookings_validToken_returns200() throws Exception {
        BookedTourListResponseDTO response = new BookedTourListResponseDTO(Collections.emptyList());
        when(bookingService.getBookings(eq("u-001"), eq("Bearer valid-token"))).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings")
                        .param("userId", "u-001")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookings").isArray());
    }

    @Test
    void getBookings_noToken_returns401() throws Exception {
        when(bookingService.getBookings(eq("u-001"), isNull()))
                .thenThrow(new UnauthorizedException("Unauthorized"));

        mockMvc.perform(get("/api/v1/bookings")
                        .param("userId", "u-001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getBookings_invalidToken_returns401() throws Exception {
        when(bookingService.getBookings(eq("u-001"), eq("Bearer invalid.token.here")))
                .thenThrow(new UnauthorizedException("Unauthorized"));

        mockMvc.perform(get("/api/v1/bookings")
                        .param("userId", "u-001")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // DELETE /api/v1/bookings/{bookingId}  — cancel
    // -----------------------------------------------------------------------

    @Test
    void cancelBooking_validToken_returns204() throws Exception {
        doNothing().when(bookingService).cancelBooking(eq("b-001"), eq("Bearer valid-token"));

        mockMvc.perform(delete("/api/v1/bookings/b-001")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    void cancelBooking_noToken_returns401() throws Exception {
        doThrow(new UnauthorizedException("Unauthorized"))
                .when(bookingService).cancelBooking(eq("b-001"), isNull());

        mockMvc.perform(delete("/api/v1/bookings/b-001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancelBooking_bookingNotFound_returns404() throws Exception {
        doThrow(new BookingNotFoundException("b-999"))
                .when(bookingService).cancelBooking(eq("b-999"), any());

        mockMvc.perform(delete("/api/v1/bookings/b-999")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelBooking_tooLate_returns400() throws Exception {
        doThrow(new CancellationNotAllowedException("Too late to cancel"))
                .when(bookingService).cancelBooking(eq("b-001"), any());

        mockMvc.perform(delete("/api/v1/bookings/b-001")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // PUT /api/v1/bookings/{bookingId}  — update
    // -----------------------------------------------------------------------

    @Test
    void updateBooking_validRequest_returns204() throws Exception {
        doNothing().when(bookingService).updateBooking(eq("b-001"), any(), eq("Bearer valid-token"));

        String requestBody = "{"
                + "\"guests\":{\"adult\":2,\"children\":0},"
                + "\"personalDetails\":[{\"firstName\":\"Jane\",\"lastName\":\"Doe\"}],"
                + "\"mealPlan\":\"HB\""
                + "}";

        mockMvc.perform(put("/api/v1/bookings/b-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .content(requestBody))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateBooking_bookingNotFound_returns404() throws Exception {
        doThrow(new BookingNotFoundException("b-999"))
                .when(bookingService).updateBooking(eq("b-999"), any(), any());

        String requestBody = "{"
                + "\"guests\":{\"adult\":2,\"children\":0},"
                + "\"personalDetails\":[{\"firstName\":\"John\",\"lastName\":\"Doe\"}]"
                + "}";

        mockMvc.perform(put("/api/v1/bookings/b-999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateBooking_unauthorized_returns401() throws Exception {
        doThrow(new UnauthorizedException("Unauthorized"))
                .when(bookingService).updateBooking(eq("b-001"), any(), isNull());

        String requestBody = "{"
                + "\"guests\":{\"adult\":2,\"children\":0},"
                + "\"personalDetails\":[{\"firstName\":\"John\",\"lastName\":\"Doe\"}]"
                + "}";

        mockMvc.perform(put("/api/v1/bookings/b-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // PATCH /api/v1/bookings/{bookingId}/confirm  — confirm
    // -----------------------------------------------------------------------

    @Test
    void confirmBooking_validToken_returns204() throws Exception {
        doNothing().when(bookingService).confirmBooking(eq("b-001"), eq("Bearer valid-token"));

        mockMvc.perform(patch("/api/v1/bookings/b-001/confirm")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    void confirmBooking_bookingNotFound_returns404() throws Exception {
        doThrow(new BookingNotFoundException("b-999"))
                .when(bookingService).confirmBooking(eq("b-999"), any());

        mockMvc.perform(patch("/api/v1/bookings/b-999/confirm")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void confirmBooking_unauthorized_returns401() throws Exception {
        doThrow(new UnauthorizedException("Unauthorized"))
                .when(bookingService).confirmBooking(eq("b-001"), isNull());

        mockMvc.perform(patch("/api/v1/bookings/b-001/confirm"))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // POST /api/v1/bookings/{bookingId}/documents  — upload document (Sprint 2)
    // -----------------------------------------------------------------------

    @Test
    void uploadDocument_validRequest_returns201() throws Exception {
        doNothing().when(documentService).uploadDocuments(eq("b-001"), any(), eq("Bearer valid-token"));

        mockMvc.perform(post("/api/v1/bookings/b-001/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Documents uploaded successfully."));
    }

    @Test
    void uploadDocument_noToken_returns401() throws Exception {
        doThrow(new UnauthorizedException("Unauthorized"))
                .when(documentService).uploadDocuments(eq("b-001"), any(), isNull());

        mockMvc.perform(post("/api/v1/bookings/b-001/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadDocument_bookingNotFound_returns404() throws Exception {
        doThrow(new BookingNotFoundException("b-999"))
                .when(documentService).uploadDocuments(eq("b-999"), any(), any());

        mockMvc.perform(post("/api/v1/bookings/b-999/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // PATCH /api/v1/bookings/{bookingId}/documents/verify  — verify document (Sprint 2)
    // -----------------------------------------------------------------------

    @Test
    void verifyDocument_approve_returns200() throws Exception {
        TravelAgentBookingDetailDTO detail = TravelAgentBookingDetailDTO.builder()
                .bookingId("b-001").state("BOOKED").build();
        when(bookingService.verifyDocument(eq("b-001"), any(), eq("Bearer valid-token")))
                .thenReturn(detail);

        String body = "{\"documentId\":\"doc-1\",\"action\":\"APPROVE\"}";

        mockMvc.perform(patch("/api/v1/bookings/b-001/documents/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value("b-001"));
    }

    @Test
    void verifyDocument_noToken_returns401() throws Exception {
        when(bookingService.verifyDocument(eq("b-001"), any(), isNull()))
                .thenThrow(new UnauthorizedException("Unauthorized"));

        String body = "{\"documentId\":\"doc-1\",\"action\":\"APPROVE\"}";

        mockMvc.perform(patch("/api/v1/bookings/b-001/documents/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // POST /api/v1/bookings/{bookingId}/confirm  — confirm v2 (Sprint 2)
    // -----------------------------------------------------------------------

    @Test
    void confirmBookingV2_returns200() throws Exception {
        TravelAgentBookingDetailDTO detail = TravelAgentBookingDetailDTO.builder()
                .bookingId("b-001").state("CONFIRMED").build();
        when(bookingService.confirmBookingV2(eq("b-001"), any(), eq("Bearer valid-token")))
                .thenReturn(detail);

        mockMvc.perform(post("/api/v1/bookings/b-001/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CONFIRMED"));
    }

    @Test
    void confirmBookingV2_noToken_returns401() throws Exception {
        when(bookingService.confirmBookingV2(eq("b-001"), any(), isNull()))
                .thenThrow(new UnauthorizedException("Unauthorized"));

        mockMvc.perform(post("/api/v1/bookings/b-001/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void confirmBookingV2_bookingNotFound_returns404() throws Exception {
        when(bookingService.confirmBookingV2(eq("b-999"), any(), any()))
                .thenThrow(new BookingNotFoundException("b-999"));

        mockMvc.perform(post("/api/v1/bookings/b-999/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // DELETE /api/v1/bookings/{bookingId}/cancel  — cancel v2 (Sprint 2)
    // -----------------------------------------------------------------------

    @Test
    void cancelBookingV2_returns200() throws Exception {
        TravelAgentBookingDetailDTO detail = TravelAgentBookingDetailDTO.builder()
                .bookingId("b-001").state("CANCELED").build();
        when(bookingService.cancelBookingV2(eq("b-001"), any(), eq("Bearer valid-token")))
                .thenReturn(detail);

        String body = "{\"reason\":\"CUSTOMERS_EMERGENCY\",\"reasonNote\":\"Medical\"}";

        mockMvc.perform(delete("/api/v1/bookings/b-001/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CANCELED"));
    }

    @Test
    void cancelBookingV2_noToken_returns401() throws Exception {
        when(bookingService.cancelBookingV2(eq("b-001"), any(), isNull()))
                .thenThrow(new UnauthorizedException("Unauthorized"));

        String body = "{\"reason\":\"CUSTOMERS_EMERGENCY\"}";

        mockMvc.perform(delete("/api/v1/bookings/b-001/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancelBookingV2_bookingNotFound_returns404() throws Exception {
        when(bookingService.cancelBookingV2(eq("b-999"), any(), any()))
                .thenThrow(new BookingNotFoundException("b-999"));

        String body = "{\"reason\":\"OTHER\"}";

        mockMvc.perform(delete("/api/v1/bookings/b-999/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // PATCH /api/v1/bookings/{bookingId}/edit  — edit booking (Sprint 2)
    // -----------------------------------------------------------------------

    @Test
    void editBooking_validRequest_returns200() throws Exception {
        TravelAgentBookingDetailDTO detail = TravelAgentBookingDetailDTO.builder()
                .bookingId("b-001").state("BOOKED").build();
        when(bookingService.editBooking(eq("b-001"), any(), eq("Bearer valid-token")))
                .thenReturn(detail);

        String body = "{\"adults\":3,\"children\":1,\"mealPlan\":\"HB\",\"duration\":10}";

        mockMvc.perform(patch("/api/v1/bookings/b-001/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value("b-001"));
    }

    @Test
    void editBooking_noToken_returns401() throws Exception {
        when(bookingService.editBooking(eq("b-001"), any(), isNull()))
                .thenThrow(new UnauthorizedException("Unauthorized"));

        mockMvc.perform(patch("/api/v1/bookings/b-001/edit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"adults\":2}"))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // PATCH /api/v1/bookings/{bookingId}/customer-approval  — record approval (Sprint 2)
    // -----------------------------------------------------------------------

    @Test
    void recordCustomerApproval_validRequest_returns200() throws Exception {
        TravelAgentBookingDetailDTO detail = TravelAgentBookingDetailDTO.builder()
                .bookingId("b-001").state("BOOKED").build();
        when(bookingService.recordCustomerApproval(eq("b-001"), any(), eq("Bearer valid-token")))
                .thenReturn(detail);

        String body = "{\"approvalMode\":\"OFFLINE\",\"approvalGiven\":true,\"approvalNote\":\"Approved\"}";

        mockMvc.perform(patch("/api/v1/bookings/b-001/customer-approval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value("b-001"));
    }

    @Test
    void recordCustomerApproval_noToken_returns401() throws Exception {
        when(bookingService.recordCustomerApproval(eq("b-001"), any(), isNull()))
                .thenThrow(new UnauthorizedException("Unauthorized"));

        String body = "{\"approvalMode\":\"OFFLINE\",\"approvalGiven\":true}";

        mockMvc.perform(patch("/api/v1/bookings/b-001/customer-approval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }
}
