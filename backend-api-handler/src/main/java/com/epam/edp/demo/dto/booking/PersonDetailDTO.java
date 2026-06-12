package com.epam.edp.demo.dto.booking;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PersonDetailDTO {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;
}
