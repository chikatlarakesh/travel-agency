package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.TravelAgentReportRow;
import com.epam.edp.demo.entity.TourStatDocument;
import com.epam.edp.demo.repository.TourStatRepository;
import org.junit.jupiter.api.BeforeEach;
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
class TravelAgentPerformanceServiceTest {

    @Mock  TourStatRepository tourStatRepository;
    @InjectMocks TravelAgentPerformanceService service;

    private final Instant FROM = Instant.parse("2024-02-01T00:00:00Z");
    private final Instant TO   = Instant.parse("2024-03-01T00:00:00Z");

    private TourStatDocument doc(String agentId, String status, int tourists, double revenue, Double feedback) {
        return TourStatDocument.builder()
                .bookingId("bk-" + agentId + "-" + Math.random())
                .travelAgentId(agentId)
                .agentName("Agent " + agentId)
                .agentEmail(agentId + "@test.com")
                .tourId("tour-1")
                .tourName("Tour One")
                .country("France")
                .city("Paris")
                .bookingStatus(status)
                .touristCount(tourists)
                .revenue(revenue)
                .feedbackScore(feedback)
                .eventTimestamp(FROM.plusSeconds(1000))
                .build();
    }

    @BeforeEach
    void setUp() {
        // Previous period docs: agent-A had 2 CONFIRMED bookings with 3 tourists each = 6
        List<TourStatDocument> prev = List.of(
                doc("agent-A", "CONFIRMED", 3, 3000.0, 4.0),
                doc("agent-A", "CONFIRMED", 3, 3000.0, 4.0)
        );
        // Current period docs: agent-A has 3 CONFIRMED bookings with 4 tourists each = 12
        List<TourStatDocument> curr = List.of(
                doc("agent-A", "CONFIRMED", 4, 4000.0, 4.5),
                doc("agent-A", "CONFIRMED", 4, 4000.0, 4.0),
                doc("agent-A", "CONFIRMED", 4, 4000.0, null),
                doc("agent-A", "CANCELLED", 5, 5000.0, 3.0)  // should be excluded
        );

        when(tourStatRepository.findByEventTimestampBetween(any(), any()))
                .thenReturn(curr)   // first call = current
                .thenReturn(prev);  // second call = previous
    }

    @Test
    void buildReport_aggregatesToursSoldFromFinishedOnly() {
        List<TravelAgentReportRow> rows = service.buildReport(FROM, TO);

        assertThat(rows).hasSize(1);
        TravelAgentReportRow row = rows.get(0);
        assertThat(row.getToursSold()).isEqualTo(12);  // 3 × 4 tourists, CANCELLED excluded
    }

    @Test
    void buildReport_computesDeltaToursSold() {
        List<TravelAgentReportRow> rows = service.buildReport(FROM, TO);
        // prev=6, curr=12 → +100%
        assertThat(rows.get(0).getDeltaToursSoldPct()).isEqualTo("+100%");
    }

    @Test
    void buildReport_computesAvgFeedbackIgnoringNulls() {
        List<TravelAgentReportRow> rows = service.buildReport(FROM, TO);
        // feedback 4.5 and 4.0 (null excluded) → avg = 4.25
        assertThat(rows.get(0).getAvgFeedbackRate()).isEqualTo(4.25);
    }

    @Test
    void buildReport_computesMinFeedback() {
        List<TravelAgentReportRow> rows = service.buildReport(FROM, TO);
        assertThat(rows.get(0).getMinFeedbackRate()).isEqualTo(4.0);
    }

    @Test
    void buildReport_revenueSum() {
        List<TravelAgentReportRow> rows = service.buildReport(FROM, TO);
        // 3 finished × $4000 = $12000
        assertThat(rows.get(0).getRevenueUsd()).isEqualTo(12000.0);
    }

    @Test
    void buildReport_deltaRevenue() {
        List<TravelAgentReportRow> rows = service.buildReport(FROM, TO);
        // prev=$6000, curr=$12000 → +100%
        assertThat(rows.get(0).getDeltaRevenuePct()).isEqualTo("+100%");
    }

    @Test
    void buildReport_noPreviousData_deltaPlusHundred() {
        when(tourStatRepository.findByEventTimestampBetween(any(), any()))
                .thenReturn(List.of(doc("agent-B", "CONFIRMED", 5, 5000.0, 4.0)))
                .thenReturn(List.of());

        List<TravelAgentReportRow> rows = service.buildReport(FROM, TO);
        assertThat(rows.get(0).getDeltaToursSoldPct()).isEqualTo("+100%");
    }
}
