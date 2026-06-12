package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.SalesReportRow;
import com.epam.edp.demo.dto.TravelAgentReportRow;
import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class CsvReportGenerator {

    private static final String[] TA_HEADERS = {
        "Agent Name", "Agent Email", "Period Start", "Period End",
        "Tours Sold", "Delta Tours Sold %", "Avg Feedback", "Min Feedback",
        "Delta Avg Feedback %", "Revenue (USD)", "Delta Revenue %"
    };

    private static final String[] SALES_HEADERS = {
        "Tour Name", "Country", "City", "Period Start", "Period End",
        "Tours Sold", "Delta Tours Sold %", "Avg Feedback", "Min Feedback",
        "Delta Avg Feedback %", "Revenue (USD)", "Delta Revenue %"
    };

    public byte[] generateTravelAgentReport(List<TravelAgentReportRow> rows) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVWriter writer = new CSVWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {

            writer.writeNext(TA_HEADERS);
            for (TravelAgentReportRow r : rows) {
                writer.writeNext(new String[]{
                    r.getAgentName(),
                    r.getAgentEmail(),
                    r.getReportPeriodStart(),
                    r.getReportPeriodEnd(),
                    String.valueOf(r.getToursSold()),
                    r.getDeltaToursSoldPct(),
                    String.format("%.2f", r.getAvgFeedbackRate()),
                    String.format("%.2f", r.getMinFeedbackRate()),
                    r.getDeltaAvgFeedbackPct(),
                    String.format("%.2f", r.getRevenueUsd()),
                    r.getDeltaRevenuePct()
                });
            }
            writer.flush();
            return out.toByteArray();
        } catch (IOException e) {
            log.error("csv.ta.error reason={}", e.getMessage());
            throw new RuntimeException("Failed to generate travel agent CSV report", e);
        }
    }

    public byte[] generateSalesReport(List<SalesReportRow> rows) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVWriter writer = new CSVWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {

            writer.writeNext(SALES_HEADERS);
            for (SalesReportRow r : rows) {
                writer.writeNext(new String[]{
                    r.getTourName(),
                    r.getCountry(),
                    r.getCity(),
                    r.getReportPeriodStart(),
                    r.getReportPeriodEnd(),
                    String.valueOf(r.getToursSold()),
                    r.getDeltaToursSoldPct(),
                    String.format("%.2f", r.getAvgFeedbackRate()),
                    String.format("%.2f", r.getMinFeedbackRate()),
                    r.getDeltaAvgFeedbackPct(),
                    String.format("%.2f", r.getRevenueUsd()),
                    r.getDeltaRevenuePct()
                });
            }
            writer.flush();
            return out.toByteArray();
        } catch (IOException e) {
            log.error("csv.sales.error reason={}", e.getMessage());
            throw new RuntimeException("Failed to generate sales CSV report", e);
        }
    }
}
