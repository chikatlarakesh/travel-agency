package com.epam.edp.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tour_stats")
public class TourStatDocument {

    @Id
    private String id;

    @Indexed
    private String bookingId;

    // Travel agent fields
    @Indexed
    private String travelAgentId;
    private String agentName;
    private String agentEmail;

    // Tour fields
    @Indexed
    private String tourId;
    private String tourName;
    private String country;
    private String city;

    // Booking fields
    private String bookingStatus;
    private int touristCount;
    private double revenue;

    // Feedback (updated when review submitted)
    private Double feedbackScore;

    @Indexed
    private Instant eventTimestamp;
}
