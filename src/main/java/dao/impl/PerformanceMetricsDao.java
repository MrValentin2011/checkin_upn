/*
 * DAO para métricas de desempeño
 */
package dao.impl;

import config.db.DBConnection;
import model.PerformanceMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Acceso a datos para métricas de desempeño.
 * Gestiona CRUD, búsqueda y cálculo de estadísticas.
 */
public class PerformanceMetricsDao {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMetricsDao.class);

    /**
     * Crea la tabla de métricas si no existe
     */
    public static void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS PERFORMANCE_METRICS (
                metrics_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                flight_id INTEGER,
                total_check_ins INTEGER DEFAULT 0,
                average_check_in_time REAL DEFAULT 0,
                min_check_in_time REAL,
                max_check_in_time REAL,
                total_passengers INTEGER DEFAULT 0,
                total_baggage INTEGER DEFAULT 0,
                security_issues INTEGER DEFAULT 0,
                record_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                period TEXT,
                FOREIGN KEY (user_id) REFERENCES Users(user_id),
                FOREIGN KEY (flight_id) REFERENCES Flights(flight_id)
            )
        """;
        
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_user_period ON PERFORMANCE_METRICS(user_id, period)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_flight_date ON PERFORMANCE_METRICS(flight_id, record_date)");
            logger.info("Tabla PERFORMANCE_METRICS creada/verificada");
        } catch (SQLException e) {
            logger.error("Error creando tabla de métricas", e);
        }
    }

    /**
     * Inserta nuevas métricas
     */
    public boolean insert(PerformanceMetrics metrics) {
        String sql = """
            INSERT INTO PERFORMANCE_METRICS 
            (user_id, flight_id, total_check_ins, average_check_in_time, 
             min_check_in_time, max_check_in_time, total_passengers, total_baggage, 
             security_issues, period)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setObject(1, metrics.getUserId());
            ps.setObject(2, metrics.getFlightId());
            ps.setInt(3, metrics.getTotalCheckIns());
            ps.setDouble(4, metrics.getAverageCheckInTime());
            ps.setObject(5, metrics.getMinCheckInTime() > 0 ? metrics.getMinCheckInTime() : null);
            ps.setObject(6, metrics.getMaxCheckInTime() > 0 ? metrics.getMaxCheckInTime() : null);
            ps.setInt(7, metrics.getTotalPassengersProcessed());
            ps.setInt(8, metrics.getTotalBaggageItems());
            ps.setInt(9, metrics.getSecurityIssues());
            ps.setString(10, metrics.getPeriod());
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error insertando métricas", e);
        }
        return false;
    }

    /**
     * Obtiene métricas de un agente en un período
     */
    public List<PerformanceMetrics> getAgentMetrics(int userId, String period) {
        String sql = """
            SELECT * FROM PERFORMANCE_METRICS 
            WHERE user_id = ? AND period = ?
            ORDER BY record_date DESC
            """;
        
        return executeQuery(sql, stmt -> {
            stmt.setInt(1, userId);
            stmt.setString(2, period);
        });
    }

    /**
     * Obtiene métricas de un vuelo
     */
    public List<PerformanceMetrics> getFlightMetrics(int flightId) {
        String sql = """
            SELECT * FROM PERFORMANCE_METRICS 
            WHERE flight_id = ? 
            ORDER BY record_date DESC
            """;
        
        return executeQuery(sql, stmt -> stmt.setInt(1, flightId));
    }

    /**
     * Obtiene métricas globales (sin usuario/vuelo específico)
     */
    public List<PerformanceMetrics> getGlobalMetrics(String period) {
        String sql = """
            SELECT * FROM PERFORMANCE_METRICS 
            WHERE user_id IS NULL AND flight_id IS NULL AND period = ?
            ORDER BY record_date DESC
            """;
        
        return executeQuery(sql, stmt -> stmt.setString(1, period));
    }

    /**
     * Obtiene las mejores métricas de agentes
     */
    public List<PerformanceMetrics> getTopAgents(int limit) {
        String sql = """
            SELECT * FROM PERFORMANCE_METRICS 
            WHERE user_id IS NOT NULL
            ORDER BY average_check_in_time ASC, total_check_ins DESC
            LIMIT ?
            """;
        
        return executeQuery(sql, stmt -> stmt.setInt(1, limit));
    }

    /**
     * Obtiene métricas para un rango de fechas
     */
    public List<PerformanceMetrics> getMetricsByDateRange(LocalDateTime from, LocalDateTime to) {
        String sql = """
            SELECT * FROM PERFORMANCE_METRICS 
            WHERE record_date BETWEEN ? AND ?
            ORDER BY record_date DESC
            """;
        
        return executeQuery(sql, stmt -> {
            stmt.setTimestamp(1, Timestamp.valueOf(from));
            stmt.setTimestamp(2, Timestamp.valueOf(to));
        });
    }

    /**
     * Calcula métricas agregadas para un período
     */
    public Map<String, Object> getAggregatedMetrics(String period) {
        String sql = """
            SELECT 
                COUNT(*) as totalRecords,
                AVG(average_check_in_time) as avgTime,
                MIN(min_check_in_time) as minTime,
                MAX(max_check_in_time) as maxTime,
                SUM(total_check_ins) as totalCheckIns,
                SUM(total_passengers) as totalPassengers,
                SUM(security_issues) as securityIssues
            FROM PERFORMANCE_METRICS 
            WHERE period = ?
            """;
        
        Map<String, Object> result = new HashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, period);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result.put("totalRecords", rs.getInt("totalRecords"));
                    result.put("avgTime", rs.getDouble("avgTime"));
                    result.put("minTime", rs.getDouble("minTime"));
                    result.put("maxTime", rs.getDouble("maxTime"));
                    result.put("totalCheckIns", rs.getInt("totalCheckIns"));
                    result.put("totalPassengers", rs.getInt("totalPassengers"));
                    result.put("securityIssues", rs.getInt("securityIssues"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error obteniendo métricas agregadas", e);
        }
        return result;
    }

    /**
     * Compara métricas entre dos períodos
     */
    public Map<String, Object> comparePeriods(String period1, String period2) {
        Map<String, Object> comparison = new HashMap<>();
        comparison.put("period1", getAggregatedMetrics(period1));
        comparison.put("period2", getAggregatedMetrics(period2));
        
        // Calcular mejoras/declives
        Map<String, Object> metrics1 = (Map<String, Object>) comparison.get("period1");
        Map<String, Object> metrics2 = (Map<String, Object>) comparison.get("period2");
        
        if (!metrics1.isEmpty() && !metrics2.isEmpty()) {
            double avgTime1 = ((Number) metrics1.getOrDefault("avgTime", 0)).doubleValue();
            double avgTime2 = ((Number) metrics2.getOrDefault("avgTime", 0)).doubleValue();
            
            if (avgTime1 > 0) {
                double improvement = ((avgTime1 - avgTime2) / avgTime1) * 100;
                comparison.put("timeImprovement", improvement);
            }
        }
        
        return comparison;
    }

    /**
     * Obtiene el rendimiento de un agente respecto al promedio
     */
    public double getAgentPerformanceRatio(int userId) {
        String sql = """
            SELECT 
                (SELECT AVG(average_check_in_time) FROM PERFORMANCE_METRICS WHERE user_id = ?) /
                (SELECT AVG(average_check_in_time) FROM PERFORMANCE_METRICS WHERE user_id IS NOT NULL) as ratio
            """;
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("ratio");
                }
            }
        } catch (SQLException e) {
            logger.error("Error calculando ratio de rendimiento", e);
        }
        return 1.0;
    }

    /**
     * Helper para ejecutar queries y mapear resultados
     */
    private List<PerformanceMetrics> executeQuery(String sql, StatementSetter setter) {
        List<PerformanceMetrics> results = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error ejecutando query de métricas", e);
        }
        return results;
    }

    /**
     * Mapea ResultSet a PerformanceMetrics
     */
    private PerformanceMetrics mapResultSet(ResultSet rs) throws SQLException {
        PerformanceMetrics metrics = new PerformanceMetrics();
        metrics.setMetricsId(rs.getInt("metrics_id"));
        
        Object userId = rs.getObject("user_id");
        if (userId != null) {
            metrics.setUserId(((Number) userId).intValue());
        }
        
        Object flightId = rs.getObject("flight_id");
        if (flightId != null) {
            metrics.setFlightId(((Number) flightId).intValue());
        }
        
        metrics.setTotalCheckIns(rs.getInt("total_check_ins"));
        metrics.setAverageCheckInTime(rs.getDouble("average_check_in_time"));
        
        Object minTime = rs.getObject("min_check_in_time");
        if (minTime != null) {
            metrics.setMinCheckInTime(((Number) minTime).doubleValue());
        }
        
        Object maxTime = rs.getObject("max_check_in_time");
        if (maxTime != null) {
            metrics.setMaxCheckInTime(((Number) maxTime).doubleValue());
        }
        
        metrics.setTotalPassengersProcessed(rs.getInt("total_passengers"));
        metrics.setTotalBaggageItems(rs.getInt("total_baggage"));
        metrics.setSecurityIssues(rs.getInt("security_issues"));
        
        Timestamp recordDate = rs.getTimestamp("record_date");
        if (recordDate != null) {
            metrics.setRecordDate(recordDate.toLocalDateTime());
        }
        
        metrics.setPeriod(rs.getString("period"));
        
        return metrics;
    }

    @FunctionalInterface
    private interface StatementSetter {
        void set(PreparedStatement ps) throws SQLException;
    }
}
