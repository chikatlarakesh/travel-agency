package com.epam.edp.demo.controller;

import com.epam.edp.demo.dto.SalesReportRow;
import com.epam.edp.demo.dto.TravelAgentReportRow;
import com.epam.edp.demo.service.AsyncReportExecutor;
import com.epam.edp.demo.service.CsvReportGenerator;
import com.epam.edp.demo.service.ExcelReportGenerator;
import com.epam.edp.demo.service.PdfReportGenerator;
import com.epam.edp.demo.service.SalesStatisticsService;
import com.epam.edp.demo.service.TravelAgentPerformanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final TravelAgentPerformanceService taService;
    private final SalesStatisticsService        salesService;
    private final ExcelReportGenerator          excelGenerator;
    private final CsvReportGenerator            csvGenerator;
    private final PdfReportGenerator            pdfGenerator;
    private final AsyncReportExecutor           asyncReportExecutor;

    /**
     * GET /api/reports?type=staff&fromDate=2024-01-01&toDate=2024-01-31&location=Paris
     * Returns JSON rows for the admin frontend table.
     */
    @GetMapping
    public ResponseEntity<?> getReport(
            @RequestParam String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String location) {

        Instant from = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to   = toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        if ("staff".equalsIgnoreCase(type) || "travel_agent".equalsIgnoreCase(type)) {
            List<TravelAgentReportRow> rows = taService.buildReport(from, to);
            return ResponseEntity.ok(rows);
        } else if ("sales".equalsIgnoreCase(type)) {
            List<SalesReportRow> rows = salesService.buildReport(from, to, location);
            return ResponseEntity.ok(rows);
        }

        return ResponseEntity.badRequest().body("Unknown report type: " + type + ". Use 'staff' or 'sales'.");
    }

    /**
     * GET /api/reports/download?type=staff&fromDate=2024-01-01&toDate=2024-01-31&format=excel&location=Paris
     */
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadReport(
            @RequestParam String type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(required = false) String location) {

        Instant from = fromDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to   = toDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        boolean isExcel = "excel".equalsIgnoreCase(format);
        boolean isPdf   = "pdf".equalsIgnoreCase(format);
        boolean isStaff = "staff".equalsIgnoreCase(type) || "travel_agent".equalsIgnoreCase(type);

        byte[] content;
        String filename;
        MediaType mediaType;

        if (isStaff) {
            List<TravelAgentReportRow> rows = taService.buildReport(from, to);
            if (isPdf) {
                content  = pdfGenerator.generateTravelAgentReport(rows);
                filename = "travel_agent_performance.pdf";
            } else if (isExcel) {
                content  = excelGenerator.generateTravelAgentReport(rows);
                filename = "travel_agent_performance.xlsx";
            } else {
                content  = csvGenerator.generateTravelAgentReport(rows);
                filename = "travel_agent_performance.csv";
            }
        } else {
            List<SalesReportRow> rows = salesService.buildReport(from, to, location);
            if (isPdf) {
                content  = pdfGenerator.generateSalesReport(rows);
                filename = "sales_statistics.pdf";
            } else if (isExcel) {
                content  = excelGenerator.generateSalesReport(rows);
                filename = "sales_statistics.xlsx";
            } else {
                content  = csvGenerator.generateSalesReport(rows);
                filename = "sales_statistics.csv";
            }
        }

        mediaType = isPdf   ? MediaType.APPLICATION_PDF
                : isExcel   ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                            : MediaType.parseMediaType("text/csv");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(content);
    }

    /**
     * POST /api/reports/send-now
     * Manually triggers the weekly report email — useful for testing without waiting for Monday's cron.
     * Protected: requires ADMIN role (same as all /api/reports/** endpoints).
     */
    @PostMapping("/send-now")
    public ResponseEntity<String> sendNow() {
        Instant to   = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant from = to.minus(7, ChronoUnit.DAYS);
        asyncReportExecutor.generateAndSendReports(from, to);
        log.info("manual.report.trigger from={} to={}", from, to);
        return ResponseEntity.accepted().body("Report generation triggered. Check your inbox in ~30 seconds.");
    }
}
