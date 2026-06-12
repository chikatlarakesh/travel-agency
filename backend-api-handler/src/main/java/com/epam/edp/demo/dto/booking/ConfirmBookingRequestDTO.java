package com.epam.edp.demo.dto.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to confirm a booking (travel agent action)")
public class ConfirmBookingRequestDTO {

    @Schema(description = "Optional notes from the travel agent", example = "All documents verified and approved.")
    private String notes;
}
