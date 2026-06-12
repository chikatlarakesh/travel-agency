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
public class ReviewStatsEvent {
    private String tourId;
    private String tourName;
    private String travelAgentId;
    private String userId;
    private int rating;
    private Instant eventTimestamp;
}
