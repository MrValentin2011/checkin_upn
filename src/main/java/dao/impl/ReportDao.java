/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao.impl;

import config.db.DBConnection;
import java.sql.*;
import java.util.*;
import model.ReportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data Access Object para reportes.
 * Genera reportes operativos con información de vuelos, pasajeros y equipaje.
 */
public class ReportDao {
    private static final Logger logger = LoggerFactory.getLogger(ReportDao.class);

    /**
     * Genera un reporte operativo diario con información de vuelos, pasajeros y
     * equipaje. Este reporte se usa en RF22 (reportes diarios) y RF38
     * (estadísticas de rendimiento).
     */
    public List<ReportData> listDailyReport() {
        List<ReportData> list = new ArrayList<>();

        String sql = """
            SELECT 
                f.flight_number,
                a.name AS airline,
                d.city AS destination,
                COUNT(DISTINCT c.checkin_id) AS passengers_checked_in,
                COUNT(DISTINCT b.baggage_id) AS baggage_count,
                ISNULL(SUM(b.weight), 0) AS total_weight,
                MAX(c.checkin_time) AS last_checkin_time
            FROM Flights f
            INNER JOIN Airlines a ON f.airline_id = a.airline_id
            INNER JOIN Destinations d ON f.destination_id = d.destination_id
            LEFT JOIN Reservations r ON r.flight_id = f.flight_id
            LEFT JOIN CheckIns c ON c.reservation_id = r.reservation_id
            LEFT JOIN Baggage b ON b.checkin_id = c.checkin_id
            WHERE CAST(f.departure_time AS DATE) = CAST(GETDATE() AS DATE)
            GROUP BY f.flight_number, a.name, d.city
            ORDER BY d.city
        """;

        try (Connection conn = DBConnection.getConnection(); 
             Statement stmt = conn.createStatement(); 
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ReportData r = new ReportData();
                r.setFlightCode(rs.getString("flight_number"));
                r.setAirline(rs.getString("airline"));
                r.setDestination(rs.getString("destination"));
                r.setPassengers(rs.getInt("passengers_checked_in"));
                r.setBaggageCount(rs.getInt("baggage_count"));
                r.setTotalWeight(rs.getDouble("total_weight"));
                r.setLastCheckIn(rs.getTimestamp("last_checkin_time") != null
                        ? rs.getTimestamp("last_checkin_time").toLocalDateTime()
                        : null);
                list.add(r);
            }
            conn.commit();

        } catch (SQLException e) {
            logger.error("Error al generar reporte diario", e);
        }
        return list;
    }

    /**
     * Genera un reporte de rendimiento por agente (promedio de tiempo y volumen
     * de check-ins).
     */
    public List<ReportData> listPerformanceReport() {
        List<ReportData> list = new ArrayList<>();

        String sql = """
            SELECT 
                u.full_name AS agent_name,
                COUNT(DISTINCT c.checkin_id) AS total_checkins,
                AVG(DATEDIFF(SECOND, c.checkin_time, GETDATE())) AS avg_seconds
            FROM Users u
            LEFT JOIN AuditLog al ON al.user_id = u.user_id
            LEFT JOIN CheckIns c ON al.action LIKE '%checkin%' AND CAST(c.checkin_time AS DATE) = CAST(GETDATE() AS DATE)
            WHERE CAST(c.checkin_time AS DATE) = CAST(GETDATE() AS DATE)
            GROUP BY u.full_name
            ORDER BY total_checkins DESC
        """;

        try (Connection conn = DBConnection.getConnection(); 
             Statement stmt = conn.createStatement(); 
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ReportData r = new ReportData();
                r.setAgentName(rs.getString("agent_name"));
                r.setPassengers(rs.getInt("total_checkins"));
                r.setAverageSeconds(rs.getDouble("avg_seconds"));
                list.add(r);
            }
            conn.commit();

        } catch (SQLException e) {
            logger.error("Error al generar reporte de rendimiento", e);
        }
        return list;
    }

    /**
     * Exporta los datos a Excel o CSV (se usa desde ReportService).
     */
    public List<ReportData> listAllForExport() {
        List<ReportData> list = new ArrayList<>();

        String sql = """
            SELECT 
                f.flight_number,
                a.name AS airline,
                d.city AS destination,
                COUNT(DISTINCT c.checkin_id) AS passengers_checked_in,
                COUNT(DISTINCT b.baggage_id) AS baggage_count,
                ISNULL(SUM(b.weight), 0) AS total_weight
            FROM Flights f
            INNER JOIN Airlines a ON f.airline_id = a.airline_id
            INNER JOIN Destinations d ON f.destination_id = d.destination_id
            LEFT JOIN Reservations r ON r.flight_id = f.flight_id
            LEFT JOIN CheckIns c ON c.reservation_id = r.reservation_id
            LEFT JOIN Baggage b ON b.checkin_id = c.checkin_id
            GROUP BY f.flight_number, a.name, d.city
        """;

        try (Connection conn = DBConnection.getConnection(); 
             Statement stmt = conn.createStatement(); 
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ReportData r = new ReportData();
                r.setFlightCode(rs.getString("flight_number"));
                r.setAirline(rs.getString("airline"));
                r.setDestination(rs.getString("destination"));
                r.setPassengers(rs.getInt("passengers_checked_in"));
                r.setBaggageCount(rs.getInt("baggage_count"));
                r.setTotalWeight(rs.getDouble("total_weight"));
                list.add(r);
            }
            conn.commit();

        } catch (SQLException e) {
            logger.error("Error al exportar datos", e);
        }
        return list;
    }

    /**
     * Resumen de reservaciones por vuelo (hoy).
     */
    public List<ReportData> listReservationsSummary() {
        return listReservationsSummary(null, null);
    }

    /**
     * Resumen de reservaciones por vuelo con filtro de fechas.
     */
    public List<ReportData> listReservationsSummary(java.sql.Date startDate, java.sql.Date endDate) {
        List<ReportData> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT f.flight_number, COUNT(r.reservation_id) AS reservations
            FROM Flights f
            LEFT JOIN Reservations r ON r.flight_id = f.flight_id
            GROUP BY f.flight_number
            ORDER BY f.flight_number
        """);

        if (startDate != null || endDate != null) {
            sql.insert(sql.indexOf("GROUP BY"), "WHERE (1=1 "
                    + (startDate != null ? "AND CAST(r.created_at AS DATE) >= '" + startDate + "'" : "")
                    + (endDate != null ? " AND CAST(r.created_at AS DATE) <= '" + endDate + "'" : "")
                    + ") ");
        } else {
            sql.insert(sql.indexOf("GROUP BY"), "WHERE CAST(f.departure_time AS DATE) = CAST(GETDATE() AS DATE) ");
        }

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {

            while (rs.next()) {
                ReportData r = new ReportData();
                r.setFlightCode(rs.getString("flight_number"));
                r.setPassengers(rs.getInt("reservations"));
                list.add(r);
            }
            conn.commit();

        } catch (SQLException e) {
            logger.error("Error al generar resumen de reservaciones", e);
        }
        return list;
    }

    /**
     * Resumen de check-ins por vuelo (hoy) e incluye conteo de equipaje.
     */
    public List<ReportData> listCheckinsSummary() {
        return listCheckinsSummary(null, null);
    }

    /**
     * Resumen de check-ins por vuelo con filtro de fechas.
     */
    public List<ReportData> listCheckinsSummary(java.sql.Date startDate, java.sql.Date endDate) {
        List<ReportData> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT f.flight_number,
                   COUNT(DISTINCT c.checkin_id) AS checkins,
                   ISNULL(COUNT(b.baggage_id),0) AS baggage_count
            FROM Flights f
            LEFT JOIN Reservations r ON r.flight_id = f.flight_id
            LEFT JOIN CheckIns c ON c.reservation_id = r.reservation_id
            LEFT JOIN Baggage b ON b.checkin_id = c.checkin_id
            GROUP BY f.flight_number
            ORDER BY f.flight_number
        """);

        if (startDate != null || endDate != null) {
            sql.insert(sql.indexOf("GROUP BY"), "WHERE (1=1 "
                    + (startDate != null ? "AND CAST(c.checkin_time AS DATE) >= '" + startDate + "'" : "")
                    + (endDate != null ? " AND CAST(c.checkin_time AS DATE) <= '" + endDate + "'" : "")
                    + ") ");
        } else {
            sql.insert(sql.indexOf("GROUP BY"), "WHERE CAST(f.departure_time AS DATE) = CAST(GETDATE() AS DATE) ");
        }


        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {

            while (rs.next()) {
                ReportData r = new ReportData();
                r.setFlightCode(rs.getString("flight_number"));
                r.setPassengers(rs.getInt("checkins"));
                r.setBaggageCount(rs.getInt("baggage_count"));
                list.add(r);
            }
            conn.commit();

        } catch (SQLException e) {
            logger.error("Error al generar resumen de checkins", e);
        }
        return list;
    }

    /**
     * Lista de logs recientes (audit) para mostrar en el panel.
     */
    public List<model.LogEntry> listRecentLogs(int limit) {
        return listRecentLogs(limit, null, null);
    }

    /**
     * Lista de logs recientes con filtro de fechas.
     */
    public List<model.LogEntry> listRecentLogs(int limit, java.sql.Date startDate, java.sql.Date endDate) {
        List<model.LogEntry> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT TOP (" + limit + ") al.user_id, u.full_name, al.action, al.created_at "
                + "FROM AuditLog al LEFT JOIN Users u ON al.user_id = u.user_id ");

        if (startDate != null || endDate != null) {
            sql.append("WHERE (1=1 ");
            if (startDate != null) sql.append("AND CAST(al.created_at AS DATE) >= '").append(startDate).append("' ");
            if (endDate != null) sql.append("AND CAST(al.created_at AS DATE) <= '").append(endDate).append("' ");
            sql.append(") ");
        }

        sql.append("ORDER BY al.created_at DESC");


        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {

            while (rs.next()) {
                model.LogEntry le = new model.LogEntry();
                le.setUserId(rs.getInt(1));
                le.setUserName(rs.getString(2));
                le.setAction(rs.getString(3));
                Timestamp ts = rs.getTimestamp(4);
                if (ts != null) le.setTimestamp(ts.toLocalDateTime());
                list.add(le);
            }
            conn.commit();

        } catch (SQLException e) {
            logger.error("Error al obtener logs recientes", e);
        }
        return list;
    }
}
