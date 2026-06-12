package com.epam.edp.demo.dto.booking;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class GuestsDTO {

    @Min(value = 1, message = "At least 1 adult is required")
    private int adult;

    @Min(value = 0, message = "Children count cannot be negative")
    private int children;
}
