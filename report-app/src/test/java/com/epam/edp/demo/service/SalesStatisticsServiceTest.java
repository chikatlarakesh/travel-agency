package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.SalesReportRow;
import com.epam.edp.demo.entity.TourStatDocument;
import com.epam.edp.demo.repository.TourStatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesStatisticsServiceTest {

    @Mock  TourStatRepository tourStatRepository;
    @InjectMocks SalesStatisticsService service;

    private final Instant FROM = Instant.parse("2024-02-01T00:00:00Z");
    private final Instant TO   = Instant.parse("2024-03-01T00:00:00Z");

    private TourStatDocument doc(String tourId, String tourName, String country, String city,
                                  String status, int tourists, double revenue, Double feedback) {
        return TourStatDocument.builder()
                .bookingId("bk-" + Math.random())
                .travelAgentId("agent-1")
                .agentName("Agent One")
                .agentEmail("agent@test.com")
                .tourId(tourId)
                .tourName(tourName)
                .country(country)
                .city(city)
                .bookingStatus(status)
                .touristCount(tourists)
                .revenue(revenue)
                .feedbackScore(feedback)
                .eventTimestamp(FROM.plusSeconds(500))
                .build();
    }

    @Test
    void buildReport_groupsByTourId() {
        List<TourStatDocument> curr = List.of(
                doc("tour-1", "Paris Tour", "France", "Paris", "CONFIRMED", 4, 4000.0, 4.5),
                doc("tour-1", "Paris Tour", "France", "Paris", "CONFIRMED", 3, 3000.0, 4.0),
                doc("tour-2", "Rome Tour",  "Italy",  "Rome",  "CONFIRMED", 2, 2000.0, 3.5)
        );

        when(tourStatRepository.findByEventTimestampBetween(any(), any()))
                .thenReturn(curr)
                .thenReturn(List.of());

        List<SalesReportRow> rows = service.buildReport(FROM, TO, null);

        assertThat(rows).hasSize(2);
        SalesReportRow paris = rows.stream().filter(r -> "tour-1".equals(r.getTourName()) || "Paris Tour".equals(r.getTourName())).findFirst().orElseThrow();
        assertThat(paris.getToursSold()).isEqualTo(7);  // 4 + 3
        assertThat(paris.getRevenueUsd()).isEqualTo(7000.0);
    }

    @Test
    void buildReport_excludesCancelledBookings() {
        List<TourStatDocument> curr = List.of(
                doc("tour-1", "Paris Tour", "France", "Paris", "CONFIRMED",  3, 3000.0, 4.0),
                doc("tour-1", "Paris Tour", "France", "Paris", "CANCELLED", 5, 5000.0, 2.0)
        );
        when(tourStatRepository.findByEventTimestampBetween(any(), any()))
                .thenReturn(curr)
                .thenReturn(List.of());

        List<SalesReportRow> rows = service.buildReport(FROM, TO, null);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getToursSold()).isEqualTo(3);
    }

    @Test
    void buildReport_deltaCalculation() {
        List<TourStatDocument> curr = List.of(
                doc("tour-1", "Paris Tour", "France", "Paris", "CONFIRMED", 6, 6000.0, 4.0)
        );
        List<TourStatDocument> prev = List.of(
                doc("tour-1", "Paris Tour", "France", "Paris", "CONFIRMED", 3, 3000.0, 4.0)
        );
        when(tourStatRepository.findByEventTimestampBetween(any(), any()))
                .thenReturn(curr)
                .thenReturn(prev);

        List<SalesReportRow> rows = service.buildReport(FROM, TO, null);
        assertThat(rows.get(0).getDeltaToursSoldPct()).isEqualTo("+100%");
        assertThat(rows.get(0).getDeltaRevenuePct()).isEqualTo("+100%");
    }

    @Test
    void buildReport_withLocationFilter_usesCity() {
        List<TourStatDocument> cityDocs = List.of(
                doc("tour-1", "Paris Tour", "France", "Paris", "CONFIRMED", 3, 3000.0, 4.0)
        );
        when(tourStatRepository.findByCityAndEventTimestampBetween(any(), any(), any()))
                .thenReturn(cityDocs)
                .thenReturn(List.of());

        List<SalesReportRow> rows = service.buildReport(FROM, TO, "Paris");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getCity()).isEqualTo("Paris");
    }
}
