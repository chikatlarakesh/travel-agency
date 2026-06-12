package com.epam.edp.demo.dto.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "List of bookings assigned to a travel agent")
public class AgentBookingListResponseDTO {
    private List<TravelAgentBookingDetailDTO> bookings;
    private int total;
}
