package com.epam.edp.demo.service;

import com.epam.edp.demo.dto.SalesReportRow;
import com.epam.edp.demo.dto.TravelAgentReportRow;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.function.BiConsumer;

@Slf4j
@Service
public class PdfReportGenerator {

    private static final Color HEADER_BG  = new Color(0x02, 0x7E, 0xAC);
    private static final Color ROW_ALT_BG = new Color(0xF5, 0xF8, 0xFA);
    private static final Color BORDER     = new Color(0xD3, 0xE1, 0xED);
    private static final Color GREEN      = new Color(0x11, 0x88, 0x19);
    private static final Color RED        = new Color(0xB7, 0x0B, 0x0B);

    @FunctionalInterface
    private interface TableRowWriter<T> extends BiConsumer<PdfPTable, T> {
    }

    public byte[] generateTravelAgentReport(List<TravelAgentReportRow> rows) {
        return generateReport(
                rows,
                "Travel Agent Performance Report",
                new String[] {
                    "Travel Agent", "Email", "Period From", "Period To",
                    "Tours Sold", "Delta Tours", "Avg Feedback", "Min Feedback",
                    "Delta Feedback", "Revenue (USD)", "Delta Revenue"
                },
                new float[] { 12f, 16f, 9f, 9f, 8f, 9f, 10f, 10f, 11f, 11f, 10f },
                (table, r) -> {
                    boolean alt = isAlternateRow(table);
                    addCell(table, r.getAgentName(), alt, false);
                    addCell(table, r.getAgentEmail(), alt, false);
                    addCell(table, r.getReportPeriodStart(), alt, true);
                    addCell(table, r.getReportPeriodEnd(), alt, true);
                    addCell(table, String.valueOf(r.getToursSold()), alt, true);
                    addDeltaCell(table, r.getDeltaToursSoldPct(), alt);
                    addCell(table, fmt1(r.getAvgFeedbackRate()), alt, true);
                    addCell(table, fmt1(r.getMinFeedbackRate()), alt, true);
                    addDeltaCell(table, r.getDeltaAvgFeedbackPct(), alt);
                    addCell(table, "$" + fmtMoney(r.getRevenueUsd()), alt, true);
                    addDeltaCell(table, r.getDeltaRevenuePct(), alt);
                }
        );
    }

    public byte[] generateSalesReport(List<SalesReportRow> rows) {
        return generateReport(
                rows,
                "Sales Statistics Report",
                new String[] {
                    "Tour Name", "Country", "City", "Period From", "Period To",
                    "Tours Sold", "Delta Tours", "Avg Feedback", "Min Feedback",
                    "Delta Feedback", "Revenue (USD)", "Delta Revenue"
                },
                new float[] { 14f, 8f, 8f, 8f, 8f, 7f, 8f, 9f, 9f, 10f, 10f, 9f },
                (table, r) -> {
                    boolean alt = isAlternateRow(table);
                    addCell(table, r.getTourName(), alt, false);
                    addCell(table, r.getCountry(), alt, false);
                    addCell(table, r.getCity(), alt, false);
                    addCell(table, r.getReportPeriodStart(), alt, true);
                    addCell(table, r.getReportPeriodEnd(), alt, true);
                    addCell(table, String.valueOf(r.getToursSold()), alt, true);
                    addDeltaCell(table, r.getDeltaToursSoldPct(), alt);
                    addCell(table, fmt1(r.getAvgFeedbackRate()), alt, true);
                    addCell(table, fmt1(r.getMinFeedbackRate()), alt, true);
                    addDeltaCell(table, r.getDeltaAvgFeedbackPct(), alt);
                    addCell(table, "$" + fmtMoney(r.getRevenueUsd()), alt, true);
                    addDeltaCell(table, r.getDeltaRevenuePct(), alt);
                }
        );
    }

    private <T> byte[] generateReport(List<T> rows,
                                      String title,
                                      String[] headers,
                                      float[] widths,
                                      TableRowWriter<T> rowWriter) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A3.rotate(), 30, 30, 30, 30);
            PdfWriter.getInstance(doc, out);
            doc.open();

            addTitle(doc, title);
            if (!rows.isEmpty()) {
                addSubtitle(doc, extractPeriodStart(rows.get(0)), extractPeriodEnd(rows.get(0)));
            }

            PdfPTable table = buildTable(headers, widths);

            for (T row : rows) {
                rowWriter.accept(table, row);
            }

            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("PDF generation failed", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private String extractPeriodStart(Object row) {
        if (row instanceof TravelAgentReportRow travelAgentRow) {
            return travelAgentRow.getReportPeriodStart();
        }
        return ((SalesReportRow) row).getReportPeriodStart();
    }

    private String extractPeriodEnd(Object row) {
        if (row instanceof TravelAgentReportRow travelAgentRow) {
            return travelAgentRow.getReportPeriodEnd();
        }
        return ((SalesReportRow) row).getReportPeriodEnd();
    }

    private boolean isAlternateRow(PdfPTable table) {
        return (table.size() / table.getNumberOfColumns()) % 2 == 0;
    }

    private void addTitle(Document doc, String text) throws DocumentException {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(0x0B, 0x38, 0x57));
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(6);
        doc.add(p);
    }

    private void addSubtitle(Document doc, String from, String to) throws DocumentException {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 11, new Color(0x67, 0x78, 0x83));
        Paragraph p = new Paragraph("Period: " + from + "  –  " + to, font);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(14);
        doc.add(p);
    }

    private PdfPTable buildTable(String[] headers, float[] widths) throws DocumentException {
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setWidths(widths);
        table.setSpacingBefore(4);

        Font hFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, hFont));
            cell.setBackgroundColor(HEADER_BG);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(5);
            cell.setBorderColor(BORDER);
            table.addCell(cell);
        }
        return table;
    }

    private void addCell(PdfPTable table, String text, boolean alt, boolean center) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(0x0B, 0x38, 0x57));
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBackgroundColor(alt ? ROW_ALT_BG : Color.WHITE);
        cell.setHorizontalAlignment(center ? Element.ALIGN_CENTER : Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        cell.setBorderColor(BORDER);
        table.addCell(cell);
    }

    private void addDeltaCell(PdfPTable table, String delta, boolean alt) {
        boolean positive = delta != null && delta.startsWith("+");
        boolean negative = delta != null && delta.startsWith("-");
        Color color = positive ? GREEN : (negative ? RED : new Color(0x67, 0x78, 0x83));
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, color);
        PdfPCell cell = new PdfPCell(new Phrase(delta != null ? delta : "—", font));
        cell.setBackgroundColor(alt ? ROW_ALT_BG : Color.WHITE);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        cell.setBorderColor(BORDER);
        table.addCell(cell);
    }

    private String fmt1(double v) {
        return String.format("%.1f", v);
    }

    private String fmtMoney(double v) {
        return String.format("%,.0f", v);
    }
}
