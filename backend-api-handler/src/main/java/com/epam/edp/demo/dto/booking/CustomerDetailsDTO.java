package com.epam.edp.demo.dto.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer contact info visible to assigned travel agent")
public class CustomerDetailsDTO {
    private String firstName;
    private String lastName;
    private String email;
    private int guestCount;
    private int adults;
    private int children;
}
