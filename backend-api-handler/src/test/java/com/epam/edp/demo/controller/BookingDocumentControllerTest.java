package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.booking.document.*;
import com.epam.edp.demo.exception.BookingNotFoundException;
import com.epam.edp.demo.exception.DocumentNotFoundException;
import com.epam.edp.demo.exception.GlobalExceptionHandler;
import com.epam.edp.demo.exception.UnauthorizedException;
import com.epam.edp.demo.security.JwtAuthenticationFilter;
import com.epam.edp.demo.security.JwtTokenProvider;
import com.epam.edp.demo.security.RateLimitFilter;
import com.epam.edp.demo.service.BookingDocumentService;
import com.epam.edp.demo.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = BookingController.class, excludeAutoConfiguration = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BookingDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    // -----------------------------------------------------------------------
    // POST /api/v1/bookings/{bookingId}/documents  — upload
    // -----------------------------------------------------------------------

    @Test
    void uploadDocuments_validRequest_returns201() throws Exception {
        doNothing().when(documentService).uploadDocuments(eq("b-001"), any(), eq("Bearer valid-token"));

        UploadDocumentsRequestDTO request = new UploadDocumentsRequestDTO();
        request.setPayments(List.of(
                new DocumentUploadDTO("Payment receipt 1.pdf", "pdf", "SGVsbG8gV29ybGQ=")
        ));
        request.setGuestDocuments(List.of(
                new GuestDocumentsUploadDTO("John Doe", List.of(
                        new DocumentUploadDTO("Passport.pdf", "pdf", "SGVsbG8gV29ybGQ=")
                ))
        ));

        mockMvc.perform(post("/api/v1/bookings/b-001/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Documents uploaded successfully."));
    }

    @Test
    void uploadDocuments_noToken_returns401() throws Exception {
        doThrow(new UnauthorizedException("Unauthorized"))
                .when(documentService).uploadDocuments(eq("b-001"), any(), isNull());

        UploadDocumentsRequestDTO request = new UploadDocumentsRequestDTO();
        request.setPayments(Collections.emptyList());
        request.setGuestDocuments(Collections.emptyList());

        mockMvc.perform(post("/api/v1/bookings/b-001/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadDocuments_bookingNotFound_returns404() throws Exception {
        doThrow(new BookingNotFoundException("b-999"))
                .when(documentService).uploadDocuments(eq("b-999"), any(), any());

        UploadDocumentsRequestDTO request = new UploadDocumentsRequestDTO();
        request.setPayments(Collections.emptyList());
        request.setGuestDocuments(Collections.emptyList());

        mockMvc.perform(post("/api/v1/bookings/b-999/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer valid-token")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/bookings/{bookingId}/documents  — retrieve
    // -----------------------------------------------------------------------

    @Test
    void getDocuments_validToken_returns200() throws Exception {
        RetrieveDocumentsResponseDTO response = new RetrieveDocumentsResponseDTO(
                List.of(new DocumentResponseDTO("doc-1", "Payment receipt 1.pdf", null, null)),
                List.of(new GuestDocumentsResponseDTO("John Doe",
                        List.of(new DocumentResponseDTO("doc-2", "Passport.pdf", null, null))))
        );
        when(documentService.getDocuments(eq("b-001"), eq("Bearer valid-token"))).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings/b-001/documents")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payments").isArray())
                .andExpect(jsonPath("$.payments[0].id").value("doc-1"))
                .andExpect(jsonPath("$.payments[0].fileName").value("Payment receipt 1.pdf"))
                .andExpect(jsonPath("$.guestDocuments").isArray())
                .andExpect(jsonPath("$.guestDocuments[0].userName").value("John Doe"))
                .andExpect(jsonPath("$.guestDocuments[0].documents[0].fileName").value("Passport.pdf"));
    }

    @Test
    void getDocuments_noToken_returns401() throws Exception {
        when(documentService.getDocuments(eq("b-001"), isNull()))
                .thenThrow(new UnauthorizedException("Unauthorized"));

        mockMvc.perform(get("/api/v1/bookings/b-001/documents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getDocuments_bookingNotFound_returns404() throws Exception {
        when(documentService.getDocuments(eq("b-999"), any()))
                .thenThrow(new BookingNotFoundException("b-999"));

        mockMvc.perform(get("/api/v1/bookings/b-999/documents")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound());
    }

    // -----------------------------------------------------------------------
    // DELETE /api/v1/bookings/{bookingId}/documents/{documentId}  — delete
    // -----------------------------------------------------------------------

    @Test
    void deleteDocument_validToken_returns200() throws Exception {
        doNothing().when(documentService).deleteDocument(eq("b-001"), eq("doc-1"), eq("Bearer valid-token"));

        mockMvc.perform(delete("/api/v1/bookings/b-001/documents/doc-1")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Document deleted successfully."));
    }

    @Test
    void deleteDocument_noToken_returns401() throws Exception {
        doThrow(new UnauthorizedException("Unauthorized"))
                .when(documentService).deleteDocument(eq("b-001"), eq("doc-1"), isNull());

        mockMvc.perform(delete("/api/v1/bookings/b-001/documents/doc-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteDocument_bookingNotFound_returns404() throws Exception {
        doThrow(new BookingNotFoundException("b-999"))
                .when(documentService).deleteDocument(eq("b-999"), eq("doc-1"), any());

        mockMvc.perform(delete("/api/v1/bookings/b-999/documents/doc-1")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDocument_documentNotFound_returns404() throws Exception {
        doThrow(new DocumentNotFoundException("b-001", "doc-999"))
                .when(documentService).deleteDocument(eq("b-001"), eq("doc-999"), any());

        mockMvc.perform(delete("/api/v1/bookings/b-001/documents/doc-999")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound());
    }
}

