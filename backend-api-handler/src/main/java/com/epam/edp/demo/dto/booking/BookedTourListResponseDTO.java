package com.epam.edp.demo.dto.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Paginated list of booked tours")
public class BookedTourListResponseDTO {
    private List<BookedTourDTO> bookings;
}
