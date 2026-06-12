package com.epam.edp.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyReportScheduler {

    private final AsyncReportExecutor asyncReportExecutor;

    // Every Monday at 08:00 UTC — delegates immediately to async executor
    @Scheduled(cron = "0 0 8 * * MON", zone = "UTC")
    public void sendWeeklyReports() {
        Instant to   = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant from = to.minus(7, ChronoUnit.DAYS);
        log.info("scheduler.weekly.trigger from={} to={}", from, to);
        asyncReportExecutor.generateAndSendReports(from, to);
    }
}
