package com.project.prenatalVision.application.report;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.DottedLineSeparator;
import com.project.prenatalVision.domain.scan.ScanRecord;
import com.project.prenatalVision.infrastructure.azure.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final StorageService storageService;

    private static final Map<String, String> FULL_NAMES = Map.ofEntries(
        Map.entry("H", "Head"),
        Map.entry("CRL", "Crown-Rump Length"),
        Map.entry("NT", "Nuchal Translucency"),
        Map.entry("NB", "Nasal Bone"),
        Map.entry("AB", "Abdomen"),
        Map.entry("B", "Buttocks"),
        Map.entry("G", "Gut"),
        Map.entry("C", "Chest cavity (Thorax)"),
        Map.entry("LV", "Lateral Ventricle"),
        Map.entry("MX", "Maxilla"),
        Map.entry("MDS", "Mandible"),
        Map.entry("MLS", "Mandible"),
        Map.entry("RBP", "Rhombencephalon"),
        Map.entry("DP", "Diencephalon"),
        Map.entry("NTAPS", "Nasal Tip and Pre-nasal Skin")
    );

    private String getFullName(String abbrev) {
        return FULL_NAMES.getOrDefault(abbrev.toUpperCase(), abbrev);
    }

    public byte[] generateScanReport(ScanRecord scan) {
        int dCount = scan.getDetections() != null ? scan.getDetections().size() : 0;
        log.info("Generating PDF report for scan: {}. Detections count: {}", scan.getId(), dCount);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.BLUE);
            Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);

            // 1. Header
            Paragraph title = new Paragraph("PrenatalVision Report", headerFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(0f);
            document.add(title);

            // 2. Scan Info Table
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(20f); // Increased space between title and info

            addInfoCell(infoTable, "Email:", scan.getUserEmail(), boldFont, normalFont);
            addInfoCell(infoTable, "Scan ID:", scan.getId(), boldFont, normalFont);
            addInfoCell(infoTable, "Scan Type:", scan.getScanType().toUpperCase(), boldFont, normalFont);
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault());
            addInfoCell(infoTable, "Report Date:", formatter.format(scan.getCreatedAt()), boldFont, normalFont);

            document.add(infoTable);
            document.add(Chunk.NEWLINE);

            // 3. Analysis Findings
            document.add(new Paragraph("Clinical Findings", subHeaderFont));
            int detectionCount = scan.getDetections() != null ? scan.getDetections().size() : 0;
            
            StringBuilder structuresText = new StringBuilder();
            if (scan.getDetections() != null && !scan.getDetections().isEmpty()) {
                scan.getDetections().forEach(det -> {
                    if (structuresText.length() > 0) structuresText.append(", ");
                    structuresText.append(getFullName(det.getClassName()));
                });
            }
            
            document.add(new Paragraph("A total of " + detectionCount + " anatomical structures were successfully identified: " + structuresText.toString() + ".", normalFont));
            document.add(Chunk.NEWLINE);

            // 4. Biometric Measurements Table
            document.add(new Paragraph("Biometric Measurements", subHeaderFont));
            PdfPTable measurementsTable = new PdfPTable(2);
            measurementsTable.setWidthPercentage(100);
            measurementsTable.setSpacingBefore(10f);
            measurementsTable.setSpacingAfter(10f);

            // Table Header
            PdfPCell h1 = new PdfPCell(new Phrase("Structure / Measurement", boldFont));
            h1.setBackgroundColor(new Color(230, 230, 250));
            h1.setPadding(8f);
            measurementsTable.addCell(h1);

            PdfPCell h2 = new PdfPCell(new Phrase("Value (mm)", boldFont));
            h2.setBackgroundColor(new Color(230, 230, 250));
            h2.setPadding(8f);
            measurementsTable.addCell(h2);

            Map<String, Object> measurements = scan.getMeasurements();
            if (measurements != null && !measurements.isEmpty()) {
                measurements.forEach((key, val) -> {
                    if (val instanceof Map) {
                        Map<String, Object> m = (Map<String, Object>) val;
                        String displayKey = getFullName(key);
                        
                        // Robust key matching (handling both full names and abbreviations)
                        boolean isHead = key.equalsIgnoreCase("Head") || key.equalsIgnoreCase("H");
                        boolean isAbdomen = key.equalsIgnoreCase("Abdomen") || key.equalsIgnoreCase("AB");

                        if (isHead) {
                            if (m.get("BPD_mm") != null) addMeasurementRow(measurementsTable, "Biparietal Diameter (BPD)", m.get("BPD_mm") + " mm", normalFont);
                            if (m.get("HC_mm") != null) addMeasurementRow(measurementsTable, "Head Circumference (HC)", m.get("HC_mm") + " mm", normalFont);
                        } else if (isAbdomen) {
                            if (m.get("circumference_mm") != null) addMeasurementRow(measurementsTable, "Abdominal Circumference (AC)", m.get("circumference_mm") + " mm", normalFont);
                        } else {
                            // General extraction with unit handling
                            Object measureVal = null;
                            String unit = " mm";
                            
                            if (m.get("thickness_mm") != null) measureVal = m.get("thickness_mm");
                            else if (m.get("length_mm") != null) measureVal = m.get("length_mm");
                            else if (m.get("length_cm") != null) { measureVal = m.get("length_cm"); unit = " cm"; }
                            else if (m.get("dimension_mm") != null) measureVal = m.get("dimension_mm");

                            if (measureVal != null) {
                                String label = key.toUpperCase().equals("NT") ? displayKey + " (NT)" : displayKey;
                                addMeasurementRow(measurementsTable, label, measureVal + unit, normalFont);
                            }
                        }
                    }
                });
            } else {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No measurements recorded", normalFont));
                emptyCell.setColspan(2);
                emptyCell.setPadding(10f);
                measurementsTable.addCell(emptyCell);
            }

            document.add(measurementsTable);
            document.add(Chunk.NEWLINE);

            // 6. Disclaimer and Signature
            document.add(new Paragraph("Medical Disclaimer", boldFont));
            document.add(new Paragraph("This report is generated using an automated AI evaluation portal. It is intended for clinical assistance and research purposes only. All measurements and detections must be validated by a certified radiologist or healthcare professional.", smallFont));
            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);

            // Signature Sections (Side by Side)
            PdfPTable sigTable = new PdfPTable(2);
            sigTable.setWidthPercentage(100);
            sigTable.setSpacingBefore(40f);
            sigTable.getDefaultCell().setBorder(com.lowagie.text.Rectangle.NO_BORDER);

            // Left: Physician
            PdfPCell physicianCell = new PdfPCell();
            physicianCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            physicianCell.addElement(new Paragraph("__________________________", normalFont));
            physicianCell.addElement(new Paragraph("Attending Physician's Signature", normalFont));
            sigTable.addCell(physicianCell);

            // Right: Date/Time with dots
            PdfPCell dateCell = new PdfPCell();
            dateCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph datePara = new Paragraph("..............................", normalFont);
            datePara.setAlignment(Element.ALIGN_RIGHT);
            dateCell.addElement(datePara);
            Paragraph dateLabel = new Paragraph("Date / Time", normalFont);
            dateLabel.setAlignment(Element.ALIGN_RIGHT);
            dateCell.addElement(dateLabel);
            sigTable.addCell(dateCell);

            document.add(sigTable);

            // 7. Images Section
            document.newPage();
            document.add(new Paragraph("Ultrasound Visualizations - Individual Findings", subHeaderFont));
            document.add(Chunk.NEWLINE);

            int detectionCountTotal = (scan.getDetections() != null) ? scan.getDetections().size() : 0;
            int cols = Math.min(Math.max(detectionCountTotal, 1), 2); 
            PdfPTable imageTable = new PdfPTable(cols);
            imageTable.setWidthPercentage(100);
            
            if (scan.getDetections() != null && !scan.getDetections().isEmpty()) {
                scan.getDetections().forEach(det -> {
                   addDetectionImageToTable(imageTable, scan.getEnhancedImageUrl(), det, smallFont);
                });
            } else {
                addImageToTable(imageTable, scan.getAnnotatedImageUrl(), "Analyzed Result", smallFont);
            }
            
            imageTable.completeRow();
            document.add(imageTable);

            document.close();
        } catch (Exception e) {
            log.error("Error creating PDF report", e);
            throw new RuntimeException("Failed to generate PDF report", e);
        }

        return out.toByteArray();
    }

    private void addInfoCell(PdfPTable table, String label, String value, Font labelFont, Font valFont) {
        PdfPCell cell1 = new PdfPCell(new Phrase(label, labelFont));
        cell1.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        cell1.setPadding(2f);
        table.addCell(cell1);

        PdfPCell cell2 = new PdfPCell(new Phrase(value, valFont));
        cell2.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        cell2.setPadding(2f);
        table.addCell(cell2);
    }

    private void addMeasurementRow(PdfPTable table, String label, Object value, Font font) {
        PdfPCell cell1 = new PdfPCell(new Phrase(label, font));
        cell1.setPadding(6f);
        table.addCell(cell1);

        String valStr = (value != null) ? String.valueOf(value) : "N/A";
        PdfPCell cell2 = new PdfPCell(new Phrase(valStr, font));
        cell2.setPadding(6f);
        table.addCell(cell2);
    }

    private void addImageToTable(PdfPTable table, String imageUrl, String caption, Font font) {
        try {
            byte[] bytes = storageService.downloadBlob(imageUrl);
            Image img = Image.getInstance(bytes);
            img.scaleToFit(240f, 240f);
            
            PdfPCell cell = new PdfPCell();
            cell.addElement(img);
            cell.addElement(new Paragraph(caption, font));
            cell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            cell.setPadding(5f);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            table.addCell(cell);
        } catch (Exception e) {
            log.error("Could not load image for PDF: {}", imageUrl, e);
            table.addCell(new Paragraph("Image not available", font));
        }
    }

    private void addDetectionImageToTable(PdfPTable table, String imageUrl, com.project.prenatalVision.domain.scan.DetectionResult det, Font font) {
        try {
            byte[] originalBytes = storageService.downloadBlob(imageUrl);
            byte[] markedBytes = drawBoxOnImage(originalBytes, det);
            
            Image img = Image.getInstance(markedBytes);
            img.scaleToFit(240f, 240f);

            PdfPCell cell = new PdfPCell();
            cell.addElement(img);
            cell.addElement(new Paragraph(getFullName(det.getClassName()), font));
            cell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            cell.setPadding(8f);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);

            table.addCell(cell);
        } catch (Exception e) {
            log.error("Could not load image for detection PDF: {}", imageUrl, e);
            table.addCell(new Paragraph("Visualization failed", font));
        }
    }

    private byte[] drawBoxOnImage(byte[] imageBytes, com.project.prenatalVision.domain.scan.DetectionResult det) throws Exception {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (img == null) return imageBytes;

        Graphics2D g2d = img.createGraphics();

        Map<String, Color> colors = Map.of(
            "NT", Color.RED,
            "NB", Color.BLUE,
            "H", Color.GREEN,
            "CRL", Color.RED
        );
        
        Color color = colors.getOrDefault(det.getClassName(), Color.GREEN);
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(3.0f)); // Thick box for visibility

        List<Double> bbox = det.getBbox();
        if (bbox != null && bbox.size() >= 4) {
            int x1 = bbox.get(0).intValue();
            int y1 = bbox.get(1).intValue();
            int x2 = bbox.get(2).intValue();
            int y2 = bbox.get(3).intValue();
            
            int width = x2 - x1;
            int height = y2 - y1;
            
            g2d.drawRect(x1, y1, width, height);

            // Label background
            String label = det.getClassName().toUpperCase();
            g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
            java.awt.FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(label);
            int textHeight = fm.getHeight();
            
            g2d.fillRect(x1, y1 - textHeight, textWidth + 10, textHeight);
            
            // Text color
            g2d.setColor(Color.WHITE);
            g2d.drawString(label, x1 + 5, y1 - fm.getDescent());
        }

        g2d.dispose();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }
}
