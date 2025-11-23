/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service.impl;

import dao.impl.ReportDao;
import java.io.IOException;
import java.util.List;
import model.ReportData;
import util.ExcelExporter;
import util.PDFGenerator;

/**
 *
 * @author USER
 */
public class ReportService {
    private final ReportDao dao = new ReportDao();

    public List<ReportData> generarReporteDiario() {
        return dao.listDailyReport();
    }

    public List<ReportData> generarResumenReservaciones() {
        return dao.listReservationsSummary();
    }

    public List<ReportData> generarResumenReservaciones(java.sql.Date startDate, java.sql.Date endDate) {
        return dao.listReservationsSummary(startDate, endDate);
    }

    public List<ReportData> generarResumenCheckins() {
        return dao.listCheckinsSummary();
    }

    public List<ReportData> generarResumenCheckins(java.sql.Date startDate, java.sql.Date endDate) {
        return dao.listCheckinsSummary(startDate, endDate);
    }

    public java.util.List<model.LogEntry> generarLogsRecientes(int limit) {
        return dao.listRecentLogs(limit);
    }

    public java.util.List<model.LogEntry> generarLogsRecientes(int limit, java.sql.Date startDate, java.sql.Date endDate) {
        return dao.listRecentLogs(limit, startDate, endDate);
    }

    public void exportarExcel(List<ReportData> data, String path) {
        ExcelExporter.exportReport(data, path);
    }

    public void exportarPDF(List<ReportData> data, String path) throws IOException {
        PDFGenerator.generateReportPDF(data, path);
    }
}
