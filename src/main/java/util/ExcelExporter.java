/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import model.ReportData;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author USER
 */
public class ExcelExporter {
    /**
     * Exporta una lista de ReportData a un archivo Excel (.xlsx)
     * @param data Lista de reportes a exportar
     * @param filePath Ruta donde se guardará el archivo Excel
     */
    public static void exportReport(List<ReportData> data, String filePath) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Reporte Diario");

        // === Estilos ===
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        CellStyle numericStyle = workbook.createCellStyle();
        numericStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

        // === Encabezados ===
        Row header = sheet.createRow(0);
        String[] columns = {"Código Vuelo", "Aerolínea", "Destino", "Pasajeros Check-In", "Equipajes", "Peso Total (kg)", "Último Check-In"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        // === Contenido ===
        int rowNum = 1;
        for (ReportData r : data) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(r.getFlightCode());
            row.createCell(1).setCellValue(r.getAirline());
            row.createCell(2).setCellValue(r.getDestination());
            row.createCell(3).setCellValue(r.getPassengers());
            row.createCell(4).setCellValue(r.getBaggageCount());
            Cell pesoCell = row.createCell(5);
            pesoCell.setCellValue(r.getTotalWeight());
            pesoCell.setCellStyle(numericStyle);
            row.createCell(6).setCellValue(
                r.getLastCheckIn() != null ? r.getLastCheckIn().toString() : "-"
            );
        }

        // === Ajustar ancho de columnas ===
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // === Guardar archivo ===
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
            System.out.println("✅ Reporte exportado correctamente a: " + filePath);
        } catch (IOException e) {
            System.err.println("❌ Error al exportar Excel: " + e.getMessage());
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar workbook: " + e.getMessage());
            }
        }
    }
}
