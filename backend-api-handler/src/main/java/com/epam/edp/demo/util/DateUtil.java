package com.epam.edp.demo.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DateUtil {

    public String computeFreeCancellationDate(String startDate, int daysBefore) {
        return LocalDate.parse(startDate).minusDays(daysBefore).toString();
    }
}
