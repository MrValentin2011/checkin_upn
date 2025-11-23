/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import model.ReportData;
import model.Reservation;
import dao.impl.PassengerDao;
import dao.impl.FlightDao;
import model.Passenger;
import model.Flight;

/**
 *
 * @author USER
 */
public class PDFGenerator {

    // ============================================================
    // 1️⃣  MÉTODO EXISTENTE: genera un Boarding Pass con QR
    // ============================================================
        public static String generateBoardingPass(Reservation reservation, File qrFile, String seatCode)
            throws FileNotFoundException, IOException {
        String outputDir = "boarding_pass/";
        File dir = new File(outputDir);
        if (!dir.exists()) dir.mkdirs();

        String pdfPath = outputDir + "boarding_" + reservation.getPnr() + ".pdf";

        try (FileOutputStream fos = new FileOutputStream(pdfPath)) {
            Document document = new Document(PageSize.A6.rotate());
            PdfWriter.getInstance(document, fos);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, Color.BLACK);
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);

            Paragraph title = new Paragraph("BOARDING PASS - " + reservation.getPnr(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);

            // Intentar mostrar nombres y referencias legibles en lugar de solo IDs
            String passengerLabel = String.valueOf(reservation.getPassengerId());
            if (reservation.getPassenger() != null) {
                Passenger p = reservation.getPassenger();
                passengerLabel = (p.getFirstName() == null ? "" : p.getFirstName()) + " " + (p.getLastName() == null ? "" : p.getLastName());
            } else {
                PassengerDao pd = new PassengerDao();
                Passenger p = pd.findById(reservation.getPassengerId());
                if (p != null) passengerLabel = (p.getFirstName() == null ? "" : p.getFirstName()) + " " + (p.getLastName() == null ? "" : p.getLastName());
            }

            String flightLabel = String.valueOf(reservation.getFlightId());
            FlightDao fd = new FlightDao();
            Flight f = fd.findById(reservation.getFlightId());
            if (f != null) {
                flightLabel = f.getCode() + " (" + f.getOrigin() + " → " + f.getDestination() + ")";
            }

            table.addCell(getCell("Pasajero:", normalFont));
            table.addCell(getCell(passengerLabel, normalFont));
            table.addCell(getCell("Vuelo:", normalFont));
            table.addCell(getCell(flightLabel, normalFont));
            if (seatCode != null && !seatCode.isBlank()) {
                table.addCell(getCell("Asiento:", normalFont));
                table.addCell(getCell(seatCode, normalFont));
            }

            table.addCell(getCell("Estado:", normalFont));
            table.addCell(getCell(reservation.getStatus(), normalFont));
            table.addCell(getCell("Emitido:", normalFont));
            table.addCell(getCell(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .format(LocalDateTime.now()), normalFont));

            document.add(table);
            document.add(new Paragraph(" "));

            if (qrFile != null && qrFile.exists()) {
                Image qrImage = Image.getInstance(qrFile.getAbsolutePath());
                qrImage.scaleAbsolute(100, 100);
                qrImage.setAlignment(Element.ALIGN_CENTER);
                document.add(qrImage);
            }

            document.add(new Paragraph("\n¡Gracias por volar con nosotros!", normalFont));
            document.close();

        } catch (DocumentException e) {
            throw new IOException("Error generando PDF: " + e.getMessage(), e);
        }

        return pdfPath;
    }

    private static PdfPCell getCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    // ============================================================
    // 2️⃣  NUEVO MÉTODO: genera un reporte PDF (operativo / estadístico)
    // ============================================================
    public static void generateReportPDF(List<ReportData> data, String outputPath)
            throws FileNotFoundException, IOException {

        File outFile = new File(outputPath);
        File parent = outFile.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, fos);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, Color.BLACK);
            Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD, Color.BLACK);
            Font normalFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);

            Paragraph title = new Paragraph("Reporte Operativo - Check-In", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            Paragraph meta = new Paragraph("Generado: " +
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(LocalDateTime.now()), normalFont);
            meta.setAlignment(Element.ALIGN_RIGHT);
            document.add(meta);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{2f, 2f, 2f, 1f, 1f, 1.5f});
            table.setWidthPercentage(100);

            table.addCell(createCell("Código Vuelo", headerFont, Element.ALIGN_CENTER));
            table.addCell(createCell("Aerolínea", headerFont, Element.ALIGN_CENTER));
            table.addCell(createCell("Destino", headerFont, Element.ALIGN_CENTER));
            table.addCell(createCell("Pasajeros", headerFont, Element.ALIGN_CENTER));
            table.addCell(createCell("Equipajes", headerFont, Element.ALIGN_CENTER));
            table.addCell(createCell("Peso Total (kg)", headerFont, Element.ALIGN_CENTER));

            if (data == null || data.isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("No hay datos para mostrar", normalFont));
                empty.setColspan(6);
                empty.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(empty);
            } else {
                for (ReportData r : data) {
                    table.addCell(createCell(nullSafe(r.getFlightCode()), normalFont, Element.ALIGN_LEFT));
                    table.addCell(createCell(nullSafe(r.getAirline()), normalFont, Element.ALIGN_LEFT));
                    table.addCell(createCell(nullSafe(r.getDestination()), normalFont, Element.ALIGN_LEFT));
                    table.addCell(createCell(String.valueOf(r.getPassengers()), normalFont, Element.ALIGN_CENTER));
                    table.addCell(createCell(String.valueOf(r.getBaggageCount()), normalFont, Element.ALIGN_CENTER));
                    table.addCell(createCell(String.format("%.2f", r.getTotalWeight()), normalFont, Element.ALIGN_RIGHT));
                }
            }

            document.add(table);
            document.close();
        } catch (DocumentException de) {
            throw new IOException("Error generando PDF: " + de.getMessage(), de);
        }
    }

    private static PdfPCell createCell(String text, Font font, int hAlign) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setHorizontalAlignment(hAlign);
        cell.setPadding(5f);
        return cell;
    }

    private static String nullSafe(String s) {
        return s == null ? "-" : s;
    }
}
