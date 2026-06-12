package com.epam.edp.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourStatsEvent {
    private String bookingId;
    private String travelAgentId;
    private String agentName;
    private String agentEmail;
    private String tourId;
    private String tourName;
    private String country;
    private String city;
    private String bookingStatus;
    private int touristCount;
    private double revenue;
    private Instant eventTimestamp;
}
