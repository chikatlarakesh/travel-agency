package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.SalesReportRow;
import com.epam.edp.demo.dto.TravelAgentReportRow;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelReportGeneratorTest {

    private final ExcelReportGenerator generator = new ExcelReportGenerator();

    private TravelAgentReportRow taRow() {
        return TravelAgentReportRow.builder()
                .agentName("Alice").agentEmail("alice@test.com")
                .reportPeriodStart("2024-01-01").reportPeriodEnd("2024-01-31")
                .toursSold(10).deltaToursSoldPct("+20%")
                .avgFeedbackRate(4.2).minFeedbackRate(3.0).deltaAvgFeedbackPct("+5%")
                .revenueUsd(10000.0).deltaRevenuePct("+15%")
                .build();
    }

    private SalesReportRow salesRow() {
        return SalesReportRow.builder()
                .tourName("Paris Tour").country("France").city("Paris")
                .reportPeriodStart("2024-01-01").reportPeriodEnd("2024-01-31")
                .toursSold(5).deltaToursSoldPct("+10%")
                .avgFeedbackRate(4.5).minFeedbackRate(4.0).deltaAvgFeedbackPct("+2%")
                .revenueUsd(5000.0).deltaRevenuePct("+10%")
                .build();
    }

    @Test
    void generateTravelAgentReport_producesValidWorkbook() throws Exception {
        byte[] bytes = generator.generateTravelAgentReport(List.of(taRow()));

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("Travel Agent Performance");

            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Agent Name");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("Agent Email");

            Row data = sheet.getRow(1);
            assertThat(data.getCell(0).getStringCellValue()).isEqualTo("Alice");
            assertThat(data.getCell(4).getStringCellValue()).isEqualTo("10");
        }
    }

    @Test
    void generateSalesReport_producesValidWorkbook() throws Exception {
        byte[] bytes = generator.generateSalesReport(List.of(salesRow()));

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("Sales Statistics");

            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Tour Name");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("Country");

            Row data = sheet.getRow(1);
            assertThat(data.getCell(0).getStringCellValue()).isEqualTo("Paris Tour");
            assertThat(data.getCell(1).getStringCellValue()).isEqualTo("France");
        }
    }

    @Test
    void generateTravelAgentReport_emptyList_onlyHeaderRow() throws Exception {
        byte[] bytes = generator.generateTravelAgentReport(List.of());
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(0);
        }
    }
}
