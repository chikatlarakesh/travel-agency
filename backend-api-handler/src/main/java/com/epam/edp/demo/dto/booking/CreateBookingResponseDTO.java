package com.epam.edp.demo.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateBookingResponseDTO {
    private String freeCancelation;
    private String details;
}
