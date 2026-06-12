package com.epam.edp.demo.util;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MealPlanFormatterTest {

    private MealPlanFormatter formatter;

    @Before
    public void setUp() {
        formatter = new MealPlanFormatter();
    }

    @Test
    public void format_BB_returnsBreakfast() {
        assertEquals("Breakfast (BB)", formatter.format("BB"));
    }

    @Test
    public void format_HB_returnsHalfBoard() {
        assertEquals("Half-board (HB)", formatter.format("HB"));
    }

    @Test
    public void format_FB_returnsFullBoard() {
        assertEquals("Full-board (FB)", formatter.format("FB"));
    }

    @Test
    public void format_AI_returnsAllInclusive() {
        assertEquals("All inclusive (AI)", formatter.format("AI"));
    }

    @Test
    public void format_unknown_returnsCodeAsIs() {
        assertEquals("RO", formatter.format("RO"));
    }

    @Test
    public void formatAll_returnsAllFormatted() {
        List<String> result = formatter.formatAll(Arrays.asList("BB", "AI", "HB"));
        assertEquals(Arrays.asList("Breakfast (BB)", "All inclusive (AI)", "Half-board (HB)"), result);
    }
}
