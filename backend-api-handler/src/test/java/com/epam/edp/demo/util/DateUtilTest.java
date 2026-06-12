package com.epam.edp.demo.util;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DateUtilTest {

    private DateUtil dateUtil;

    @Before
    public void setUp() {
        dateUtil = new DateUtil();
    }

    @Test
    public void computeFreeCancellationDate_subtractsDays() {
        assertEquals("2025-01-05", dateUtil.computeFreeCancellationDate("2025-01-15", 10));
    }

    @Test
    public void computeFreeCancellationDate_zeroDays_returnsSameDate() {
        assertEquals("2025-06-01", dateUtil.computeFreeCancellationDate("2025-06-01", 0));
    }

    @Test
    public void computeFreeCancellationDate_crossMonthBoundary() {
        assertEquals("2025-01-28", dateUtil.computeFreeCancellationDate("2025-02-07", 10));
    }
}
