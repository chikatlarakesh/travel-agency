package com.epam.edp.demo.dto.booking;

import com.epam.edp.demo.enums.CancellationReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to cancel a booking (travel agent action)")
public class CancelBookingRequestDTO {

    @NotNull(message = "Cancellation reason is required")
    @Schema(description = "Structured cancellation reason", example = "CUSTOMERS_EMERGENCY", required = true)
    private CancellationReason reason;

    @Schema(description = "Additional detail about the cancellation reason")
    private String reasonNote;
}
