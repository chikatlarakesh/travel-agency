package com.epam.edp.demo.util;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Formats meal plan codes into human-readable display strings.
 */
@Component
public class MealPlanFormatter {

    private static final Map<String, String> MEAL_PLAN_LABELS = Map.of(
            "BB", "Breakfast (BB)",
            "HB", "Half-board (HB)",
            "FB", "Full-board (FB)",
            "AI", "All inclusive (AI)"
    );

    public String format(String code) {
        return MEAL_PLAN_LABELS.getOrDefault(code, code);
    }

    public List<String> formatAll(List<String> codes) {
        return codes.stream()
                .map(this::format)
                .toList();
    }
}
