package com.epam.edp.demo.dto.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class UpdateBookingRequestDTO {

    @NotNull(message = "guests is required")
    @Valid
    private GuestsDTO guests;

    @NotEmpty(message = "personalDetails is required")
    @Valid
    private List<PersonDetailDTO> personalDetails;

    @Pattern(regexp = "^(BB|HB|FB|AI)$", message = "mealPlan must be one of: BB, HB, FB, AI")
    private String mealPlan;

    private Double totalPrice;
}
