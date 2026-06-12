package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.SalesReportRow;
import com.epam.edp.demo.entity.TourStatDocument;
import com.epam.edp.demo.repository.TourStatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesStatisticsService {

    private static final double ZERO_EPSILON = 1.0e-9;

    private final TourStatRepository tourStatRepository;

    public List<SalesReportRow> buildReport(Instant from, Instant to, String location) {
        long periodSeconds = to.getEpochSecond() - from.getEpochSecond();
        Instant prevFrom = from.minusSeconds(periodSeconds);
        Instant prevTo   = from;

        List<TourStatDocument> current  = fetchDocs(from, to, location);
        List<TourStatDocument> previous = fetchDocs(prevFrom, prevTo, location); // fetchDocs already filters by CONFIRMED

        Map<String, List<TourStatDocument>> currentByTour  = groupByTour(current);
        Map<String, List<TourStatDocument>> previousByTour = groupByTour(previous);

        List<SalesReportRow> rows = new ArrayList<>();

        for (Map.Entry<String, List<TourStatDocument>> entry : currentByTour.entrySet()) {
            String tourId   = entry.getKey();
            List<TourStatDocument> curDocs  = entry.getValue();
            List<TourStatDocument> prevDocs = previousByTour.getOrDefault(tourId, List.of());

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

            OptionalDouble prevAvgFeedback = prevDocs.stream()
                    .filter(d -> d.getFeedbackScore() != null)
                    .mapToDouble(TourStatDocument::getFeedbackScore).average();

            rows.add(SalesReportRow.builder()
                    .tourName(sample.getTourName())
                    .country(sample.getCountry())
                    .city(sample.getCity())
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

        log.info("sales.report.built rows={} period={} to {} location={}", rows.size(), from, to, location);
        return rows;
    }

    private List<TourStatDocument> fetchDocs(Instant from, Instant to, String location) {
        if (location == null || location.isBlank()) {
            return tourStatRepository.findByEventTimestampBetween(from, to).stream()
                    .filter(d -> "CONFIRMED".equals(d.getBookingStatus()))
                    .toList();
        }
        // Try city first, fall back to country
        List<TourStatDocument> byCity = tourStatRepository.findByCityAndEventTimestampBetween(location, from, to).stream()
                .filter(d -> "CONFIRMED".equals(d.getBookingStatus()))
                .toList();
        if (!byCity.isEmpty()) return byCity;
        return tourStatRepository.findByCountryAndEventTimestampBetween(location, from, to).stream()
                .filter(d -> "CONFIRMED".equals(d.getBookingStatus()))
                .toList();
    }

    private Map<String, List<TourStatDocument>> groupByTour(List<TourStatDocument> docs) {
        return docs.stream().collect(Collectors.groupingBy(TourStatDocument::getTourId));
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
