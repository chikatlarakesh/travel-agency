package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.SalesReportRow;
import com.epam.edp.demo.dto.TravelAgentReportRow;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;

@Slf4j
@Service
public class ExcelReportGenerator {

    @FunctionalInterface
    private interface RowWriter<T> extends BiConsumer<Row, T> {
    }

    public byte[] generateTravelAgentReport(List<TravelAgentReportRow> rows) {
        return generateReport(
                rows,
                "Travel Agent Performance",
                new String[] {
                    "Agent Name", "Agent Email", "Period Start", "Period End",
                    "Tours Sold", "Delta Tours Sold %", "Avg Feedback", "Min Feedback",
                    "Delta Avg Feedback %", "Revenue (USD)", "Delta Revenue %"
                },
                (row, r) -> {
                    CellStyle dataStyle = row.getSheet().getWorkbook().getCellStyleAt((short) 1);
                    setCell(row, 0, r.getAgentName(), dataStyle);
                    setCell(row, 1, r.getAgentEmail(), dataStyle);
                    setCell(row, 2, r.getReportPeriodStart(), dataStyle);
                    setCell(row, 3, r.getReportPeriodEnd(), dataStyle);
                    setCell(row, 4, String.valueOf(r.getToursSold()), dataStyle);
                    setCell(row, 5, r.getDeltaToursSoldPct(), dataStyle);
                    setCell(row, 6, formatDecimal(r.getAvgFeedbackRate()), dataStyle);
                    setCell(row, 7, formatDecimal(r.getMinFeedbackRate()), dataStyle);
                    setCell(row, 8, r.getDeltaAvgFeedbackPct(), dataStyle);
                    setCell(row, 9, formatDecimal(r.getRevenueUsd()), dataStyle);
                    setCell(row, 10, r.getDeltaRevenuePct(), dataStyle);
                },
                "excel.ta.error",
                "Failed to generate travel agent Excel report"
        );
    }

    public byte[] generateSalesReport(List<SalesReportRow> rows) {
        return generateReport(
                rows,
                "Sales Statistics",
                new String[] {
                    "Tour Name", "Country", "City", "Period Start", "Period End",
                    "Tours Sold", "Delta Tours Sold %", "Avg Feedback", "Min Feedback",
                    "Delta Avg Feedback %", "Revenue (USD)", "Delta Revenue %"
                },
                (row, r) -> {
                    CellStyle dataStyle = row.getSheet().getWorkbook().getCellStyleAt((short) 1);
                    setCell(row, 0, r.getTourName(), dataStyle);
                    setCell(row, 1, r.getCountry(), dataStyle);
                    setCell(row, 2, r.getCity(), dataStyle);
                    setCell(row, 3, r.getReportPeriodStart(), dataStyle);
                    setCell(row, 4, r.getReportPeriodEnd(), dataStyle);
                    setCell(row, 5, String.valueOf(r.getToursSold()), dataStyle);
                    setCell(row, 6, r.getDeltaToursSoldPct(), dataStyle);
                    setCell(row, 7, formatDecimal(r.getAvgFeedbackRate()), dataStyle);
                    setCell(row, 8, formatDecimal(r.getMinFeedbackRate()), dataStyle);
                    setCell(row, 9, r.getDeltaAvgFeedbackPct(), dataStyle);
                    setCell(row, 10, formatDecimal(r.getRevenueUsd()), dataStyle);
                    setCell(row, 11, r.getDeltaRevenuePct(), dataStyle);
                },
                "excel.sales.error",
                "Failed to generate sales Excel report"
        );
    }

    private <T> byte[] generateReport(List<T> rows,
                                      String sheetName,
                                      String[] headers,
                                      RowWriter<T> rowWriter,
                                      String logKey,
                                      String errorMessage) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(sheetName);

            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle dataStyle   = createDataStyle(wb);

            createHeaderRow(sheet, headers, headerStyle);

            int rowIdx = 1;
            for (T item : rows) {
                Row row = sheet.createRow(rowIdx++);
                rowWriter.accept(row, item);
            }
            autoSizeColumns(sheet, headers.length);

            return toBytes(wb);
        } catch (IOException e) {
            log.error("{} reason={}", logKey, e.getMessage());
            throw new RuntimeException(errorMessage, e);
        }
    }

    private String formatDecimal(double value) {
        return String.format("%.2f", value);
    }

    private void createHeaderRow(Sheet sheet, String[] headers, CellStyle style) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void setCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void autoSizeColumns(Sheet sheet, int count) {
        for (int i = 0; i < count; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private byte[] toBytes(XSSFWorkbook wb) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.write(out);
            return out.toByteArray();
        }
    }
}
