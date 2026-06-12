package com.epam.edp.demo.dto.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class CreateBookingRequestDTO {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotBlank(message = "tourId is required")
    private String tourId;

    @NotBlank(message = "date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "date must be in YYYY-MM-DD format")
    private String date;

    @NotBlank(message = "duration is required")
    private String duration;

    @NotBlank(message = "mealPlan is required")
    @Pattern(regexp = "^(BB|HB|FB|AI)$", message = "mealPlan must be one of: BB, HB, FB, AI")
    private String mealPlan;

    @NotNull(message = "guests is required")
    @Valid
    private GuestsDTO guests;

    @NotEmpty(message = "personalDetails is required")
    @Valid
    private List<PersonDetailDTO> personalDetails;

    private Double totalPrice;
}
