package com.epam.edp.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Snapshot of booking fields taken before a travel agent edit.
 * Used to revert changes if the customer declines the proposed edit.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingEditSnapshot {
    private String mealPlan;
    private GuestCount guests;
    private List<PersonDetail> personalDetails;
    private Double totalPrice;
}
