package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.booking.AgentBookingListResponseDTO;
import com.epam.edp.demo.dto.booking.TravelAgentBookingDetailDTO;
import com.epam.edp.demo.exception.BadRequestException;
import com.epam.edp.demo.exception.BookingNotFoundException;
import com.epam.edp.demo.exception.GlobalExceptionHandler;
import com.epam.edp.demo.exception.UnauthorizedException;
import com.epam.edp.demo.security.JwtAuthenticationFilter;
import com.epam.edp.demo.security.JwtTokenProvider;
import com.epam.edp.demo.security.RateLimitFilter;
import com.epam.edp.demo.service.BookingService;
import com.epam.edp.demo.service.BookingDocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = TravelAgentController.class, excludeAutoConfiguration = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TravelAgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private BookingDocumentService bookingDocumentService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitFilter rateLimitFilter;

    // -----------------------------------------------------------------------
    // GET /api/v1/travel-agent/bookings
    // -----------------------------------------------------------------------

    @Test
    void getAgentBookings_noFilter_returns200() throws Exception {
        TravelAgentBookingDetailDTO detail = TravelAgentBookingDetailDTO.builder()
                .bookingId("b-001").state("BOOKED").tourName("Rome Tour").build();
        AgentBookingListResponseDTO response = new AgentBookingListResponseDTO(List.of(detail), 1);
        when(bookingService.getAgentBookings(isNull(), eq("Bearer valid-token"))).thenReturn(response);

        mockMvc.perform(get("/api/v1/travel-agent/bookings")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.bookings[0].bookingId").value("b-001"));
    }

    @Test
    void getAgentBookings_withStatusFilter_returns200() throws Exception {
        TravelAgentBookingDetailDTO detail = TravelAgentBookingDetailDTO.builder()
                .bookingId("b-002").state("CONFIRMED").build();
        AgentBookingListResponseDTO response = new AgentBookingListResponseDTO(List.of(detail), 1);
        when(bookingService.getAgentBookings(eq("CONFIRMED"), eq("Bearer valid-token"))).thenReturn(response);

        mockMvc.perform(get("/api/v1/travel-agent/bookings")
                        .param("status", "CONFIRMED")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.bookings[0].state").value("CONFIRMED"));
    }

    @Test
    void getAgentBookings_emptyList_returns200WithEmptyArray() throws Exception {
        AgentBookingListResponseDTO response = new AgentBookingListResponseDTO(Collections.emptyList(), 0);
        when(bookingService.getAgentBookings(isNull(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/travel-agent/bookings")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.bookings").isArray());
    }

    @Test
    void getAgentBookings_noToken_returns401() throws Exception {
        when(bookingService.getAgentBookings(isNull(), isNull()))
                .thenThrow(new UnauthorizedException("Unauthorized"));

        mockMvc.perform(get("/api/v1/travel-agent/bookings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAgentBookings_invalidStatusFilter_returns400() throws Exception {
        when(bookingService.getAgentBookings(eq("INVALID"), any()))
                .thenThrow(new BadRequestException("Invalid status filter: INVALID"));

        mockMvc.perform(get("/api/v1/travel-agent/bookings")
                        .param("status", "INVALID")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/travel-agent/bookings/{bookingId}
    // -----------------------------------------------------------------------

    @Test
    void getAgentBookingById_returns200() throws Exception {
        TravelAgentBookingDetailDTO detail = TravelAgentBookingDetailDTO.builder()
                .bookingId("b-001").state("BOOKED").tourName("Paris Tour").build();
        when(bookingService.getAgentBookingById(eq("b-001"), eq("Bearer valid-token")))
                .thenReturn(detail);

        mockMvc.perform(get("/api/v1/travel-agent/bookings/b-001")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value("b-001"))
                .andExpect(jsonPath("$.tourName").value("Paris Tour"));
    }

    @Test
    void getAgentBookingById_noToken_returns401() throws Exception {
        when(bookingService.getAgentBookingById(eq("b-001"), isNull()))
                .thenThrow(new UnauthorizedException("Unauthorized"));

        mockMvc.perform(get("/api/v1/travel-agent/bookings/b-001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAgentBookingById_notFound_returns404() throws Exception {
        when(bookingService.getAgentBookingById(eq("b-999"), any()))
                .thenThrow(new BookingNotFoundException("b-999"));

        mockMvc.perform(get("/api/v1/travel-agent/bookings/b-999")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAgentBookingById_notAssignedAgent_returns401() throws Exception {
        when(bookingService.getAgentBookingById(eq("b-001"), any()))
                .thenThrow(new UnauthorizedException("You are not the assigned agent for this booking"));

        mockMvc.perform(get("/api/v1/travel-agent/bookings/b-001")
                        .header("Authorization", "Bearer other-token"))
                .andExpect(status().isUnauthorized());
    }
}
