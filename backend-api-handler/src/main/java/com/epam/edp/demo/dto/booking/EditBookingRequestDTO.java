package com.epam.edp.demo.dto.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for a travel agent to edit a booking (requires prior customer approval)")
public class EditBookingRequestDTO {

    @Schema(description = "Updated number of adult guests")
    @Min(value = 1, message = "At least 1 adult guest is required")
    private Integer adults;

    @Schema(description = "Updated number of child guests")
    @Min(value = 0, message = "Children count cannot be negative")
    private Integer children;

    @Schema(description = "Updated meal plan", example = "HB")
    private String mealPlan;

    @Schema(description = "Updated tour duration in days", example = "7")
    @Min(value = 1, message = "Duration must be at least 1 day")
    private Integer duration;
}
