package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.TravelAgentReportRow;
import com.epam.edp.demo.entity.TourStatDocument;
import com.epam.edp.demo.repository.TourStatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelAgentPerformanceService {

    private static final double ZERO_EPSILON = 1.0e-9;

    private final TourStatRepository tourStatRepository;

    public List<TravelAgentReportRow> buildReport(Instant from, Instant to) {
        long periodSeconds = to.getEpochSecond() - from.getEpochSecond();
        Instant prevFrom = from.minusSeconds(periodSeconds);
        Instant prevTo   = from;

        List<TourStatDocument> current  = tourStatRepository.findByEventTimestampBetween(from, to);
        List<TourStatDocument> previous = tourStatRepository.findByEventTimestampBetween(prevFrom, prevTo);

        // Count CONFIRMED bookings as "sold" (event is fired at confirmation time)
        Map<String, List<TourStatDocument>> currentByAgent  = groupByAgent(current,  "CONFIRMED");
        Map<String, List<TourStatDocument>> previousByAgent = groupByAgent(previous, "CONFIRMED");

        List<TravelAgentReportRow> rows = new ArrayList<>();

        for (Map.Entry<String, List<TourStatDocument>> entry : currentByAgent.entrySet()) {
            String agentId = entry.getKey();
            List<TourStatDocument> curDocs = entry.getValue();
            List<TourStatDocument> prevDocs = previousByAgent.getOrDefault(agentId, List.of());

            TourStatDocument sample = curDocs.get(0);

            int curSold  = curDocs.stream().mapToInt(TourStatDocument::getTouristCount).sum();
            int prevSold = prevDocs.stream().mapToInt(TourStatDocument::getTouristCount).sum();

            double curRevenue  = curDocs.stream().mapToDouble(TourStatDocument::getRevenue).sum();
            double prevRevenue = prevDocs.stream().mapToDouble(TourStatDocument::getRevenue).sum();

            OptionalDouble avgFeedback = curDocs.stream()
                    .filter(d -> d.getFeedbackScore() != null)
                    .mapToDouble(TourStatDocument::getFeedbackScore).average();
            OptionalDouble minFeedback = curDocs.stream()
                    .filter(d -> d.getFeedbackScore() != null)
                    .mapToDouble(TourStatDocument::getFeedbackScore).min();

            List<TourStatDocument> prevWithFeedback = prevDocs.stream()
                    .filter(d -> d.getFeedbackScore() != null).toList();
            OptionalDouble prevAvgFeedback = prevWithFeedback.stream()
                    .mapToDouble(TourStatDocument::getFeedbackScore).average();

            rows.add(TravelAgentReportRow.builder()
                    .agentName(sample.getAgentName())
                    .agentEmail(sample.getAgentEmail())
                    .reportPeriodStart(formatInstant(from))
                    .reportPeriodEnd(formatInstant(to))
                    .toursSold(curSold)
                    .deltaToursSoldPct(delta(prevSold, curSold))
                    .avgFeedbackRate(avgFeedback.orElse(0.0))
                    .minFeedbackRate(minFeedback.orElse(0.0))
                    .deltaAvgFeedbackPct(delta(prevAvgFeedback.orElse(0.0), avgFeedback.orElse(0.0)))
                    .revenueUsd(curRevenue)
                    .deltaRevenuePct(delta(prevRevenue, curRevenue))
                    .build());
        }

        log.info("ta.report.built rows={} period={} to {}", rows.size(), from, to);
        return rows;
    }

    private Map<String, List<TourStatDocument>> groupByAgent(List<TourStatDocument> docs, String status) {
        return docs.stream()
                .filter(d -> status.equals(d.getBookingStatus()))
                .collect(Collectors.groupingBy(TourStatDocument::getTravelAgentId));
    }

    private String delta(double prev, double current) {
        if (Math.abs(prev) < ZERO_EPSILON) return current > 0 ? "+100%" : "0%";
        double pct = ((current - prev) / prev) * 100.0;
        return (pct >= 0 ? "+" : "") + Math.round(pct) + "%";
    }

    private String formatInstant(Instant instant) {
        return java.time.LocalDate.ofInstant(instant, java.time.ZoneOffset.UTC).toString();
    }
}
