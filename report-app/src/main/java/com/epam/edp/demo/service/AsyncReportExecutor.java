package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.SalesReportRow;
import com.epam.edp.demo.dto.TravelAgentReportRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Executes report generation and email dispatch on the async executor thread pool,
 * keeping the scheduler thread free for other scheduled tasks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncReportExecutor {

    private final TravelAgentPerformanceService taService;
    private final SalesStatisticsService        salesService;
    private final ExcelReportGenerator          excelGenerator;
    private final CsvReportGenerator            csvGenerator;
    private final PdfReportGenerator            pdfGenerator;
    private final EmailService                  emailService;

    @Async
    public void generateAndSendReports(Instant from, Instant to) {
        log.info("async.report.start from={} to={}", from, to);
        try {
            List<TravelAgentReportRow> taRows    = taService.buildReport(from, to);
            List<SalesReportRow>       salesRows = salesService.buildReport(from, to, null);

            byte[] taExcel    = excelGenerator.generateTravelAgentReport(taRows);
            byte[] taCsv      = csvGenerator.generateTravelAgentReport(taRows);
            byte[] taPdf      = pdfGenerator.generateTravelAgentReport(taRows);
            byte[] salesExcel = excelGenerator.generateSalesReport(salesRows);
            byte[] salesCsv   = csvGenerator.generateSalesReport(salesRows);
            byte[] salesPdf   = pdfGenerator.generateSalesReport(salesRows);

            String period = from + " to " + to;

            emailService.sendReportEmail(
                    "Weekly Travel Agent Performance Report — " + period,
                    "Please find attached the weekly Travel Agent Performance report for " + period + ".",
                    taExcel, "travel_agent_performance.xlsx",
                    taCsv,   "travel_agent_performance.csv",
                    taPdf,   "travel_agent_performance.pdf"
            );

            emailService.sendReportEmail(
                    "Weekly Sales Statistics Report — " + period,
                    "Please find attached the weekly Sales Statistics report for " + period + ".",
                    salesExcel, "sales_statistics.xlsx",
                    salesCsv,   "sales_statistics.csv",
                    salesPdf,   "sales_statistics.pdf"
            );

            log.info("async.report.done taRows={} salesRows={}", taRows.size(), salesRows.size());
        } catch (Exception e) {
            log.error("async.report.error reason={}", e.getMessage(), e);
        }
    }
}
