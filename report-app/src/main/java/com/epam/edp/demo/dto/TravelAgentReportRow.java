package com.epam.edp.demo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TravelAgentReportRow {
    private String agentName;
    private String agentEmail;
    private String reportPeriodStart;
    private String reportPeriodEnd;
    private int toursSold;
    private String deltaToursSoldPct;
    private double avgFeedbackRate;
    private double minFeedbackRate;
    private String deltaAvgFeedbackPct;
    private double revenueUsd;
    private String deltaRevenuePct;
}
