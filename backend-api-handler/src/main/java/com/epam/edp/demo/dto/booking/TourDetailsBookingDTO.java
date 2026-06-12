package com.epam.edp.demo.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TourDetailsBookingDTO {
    private String date;
    private String mealPlan;
    private String guests;
    private String totalPrice;
    private String documents;
}
