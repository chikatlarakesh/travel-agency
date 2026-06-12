package com.epam.edp.demo.entity;

import com.epam.edp.demo.enums.MealPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Embedded pricing option inside a TourInstance.
 *
 * Captures the base price per person for a given duration, plus optional
 * per-day meal supplements. Using BigDecimal avoids floating-point issues
 * when storing and comparing monetary values.
 *
 * Example document fragment:
 * {
 *   "duration": 7,
 *   "pricePerPerson": 1400.00,
 *   "mealSupplementsPerDay": { "HB": 30.00, "FB": 50.00, "AI": 80.00 }
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingOption {

    /** Duration in days (e.g. 7, 10, 12). */
    private int duration;

    /** Base price per person for this duration (BB meal plan included). */
    private BigDecimal pricePerPerson;

    /**
     * Additional cost per person per day for each non-base meal plan.
     * BB is the baseline (supplement = 0 / absent).
     */
    private Map<MealPlan, BigDecimal> mealSupplementsPerDay;
}
