/*
 * Servicio de métricas de desempeño
 */
package service.impl;

import dao.impl.PerformanceMetricsDao;
import dao.impl.CheckInDao;
import model.PerformanceMetrics;
import model.CheckIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Servicio para cálculo y gestión de métricas de desempeño.
 * Recopila datos de check-in, calcula estadísticas y proporciona análisis.
 */
public class PerformanceMetricsService {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMetricsService.class);
    private static PerformanceMetricsService instance;
    private final PerformanceMetricsDao dao;
    private final CheckInDao checkInDao;

    private PerformanceMetricsService() {
        this.dao = new PerformanceMetricsDao();
        this.checkInDao = new CheckInDao();
        PerformanceMetricsDao.createTableIfNotExists();
    }

    /**
     * Obtiene la instancia singleton
     */
    public static synchronized PerformanceMetricsService getInstance() {
        if (instance == null) {
            instance = new PerformanceMetricsService();
        }
        return instance;
    }

    /**
     * Calcula métricas de un agente para hoy
     */
    public PerformanceMetrics calculateDailyAgentMetrics(int userId) {
        return calculateAgentMetricsForPeriod(userId, "DAILY", -1, 0);
    }

    /**
     * Calcula métricas de un agente para esta semana
     */
    public PerformanceMetrics calculateWeeklyAgentMetrics(int userId) {
        return calculateAgentMetricsForPeriod(userId, "WEEKLY", -7, 0);
    }

    /**
     * Calcula métricas de un agente para este mes
     */
    public PerformanceMetrics calculateMonthlyAgentMetrics(int userId) {
        return calculateAgentMetricsForPeriod(userId, "MONTHLY", -30, 0);
    }

    /**
     * Calcula métricas para un período personalizado
     */
    private PerformanceMetrics calculateAgentMetricsForPeriod(int userId, String period, int daysBack, int hoursBack) {
        LocalDateTime from;
        if (daysBack != 0) {
            from = LocalDateTime.now().plus(daysBack, ChronoUnit.DAYS);
        } else {
            from = LocalDateTime.now().plus(hoursBack, ChronoUnit.HOURS);
        }
        LocalDateTime to = LocalDateTime.now();

        // Obtener todos los check-ins del agente en el período
        // (Esto es una simplificación - en producción usarías un query específico)
        List<Double> checkInTimes = new ArrayList<>();
        int totalPassengers = 0;
        int totalBaggage = 0;
        int securityIssues = 0;

        // Simular recopilación de datos (en producción, consultar base de datos)
        // Este es un ejemplo básico
        double avgTime = 45.0; // segundos
        double minTime = 30.0;
        double maxTime = 120.0;
        int totalCheckIns = 50;

        PerformanceMetrics metrics = new PerformanceMetrics(userId, null, period);
        metrics.setTotalCheckIns(totalCheckIns);
        metrics.setAverageCheckInTime(avgTime);
        metrics.setMinCheckInTime(minTime);
        metrics.setMaxCheckInTime(maxTime);
        metrics.setTotalPassengersProcessed(totalPassengers);
        metrics.setTotalBaggageItems(totalBaggage);
        metrics.setSecurityIssues(securityIssues);

        if (dao.insert(metrics)) {
            logger.info("Métricas calculadas para agente {} en período {}", userId, period);
            return metrics;
        }
        return null;
    }

    /**
     * Calcula métricas de un vuelo
     */
    public PerformanceMetrics calculateFlightMetrics(int flightId) {
        PerformanceMetrics metrics = new PerformanceMetrics(null, flightId, "FLIGHT");
        
        // Aquí irían los cálculos reales basados en check-ins del vuelo
        metrics.setTotalCheckIns(100);
        metrics.setAverageCheckInTime(50.0);
        metrics.setMinCheckInTime(20.0);
        metrics.setMaxCheckInTime(150.0);
        metrics.setTotalPassengersProcessed(100);
        metrics.setTotalBaggageItems(150);
        metrics.setSecurityIssues(2);

        if (dao.insert(metrics)) {
            logger.info("Métricas calculadas para vuelo {}", flightId);
            return metrics;
        }
        return null;
    }

    /**
     * Obtiene métricas de un agente
     */
    public List<PerformanceMetrics> getAgentMetrics(int userId, String period) {
        return dao.getAgentMetrics(userId, period);
    }

    /**
     * Obtiene métricas de un vuelo
     */
    public List<PerformanceMetrics> getFlightMetrics(int flightId) {
        return dao.getFlightMetrics(flightId);
    }

    /**
     * Obtiene los mejores agentes
     */
    public List<PerformanceMetrics> getTopPerformers(int limit) {
        return dao.getTopAgents(limit);
    }

    /**
     * Obtiene métricas agregadas
     */
    public Map<String, Object> getAggregatedMetrics(String period) {
        return dao.getAggregatedMetrics(period);
    }

    /**
     * Compara dos períodos
     */
    public Map<String, Object> comparePeriods(String period1, String period2) {
        return dao.comparePeriods(period1, period2);
    }

    /**
     * Obtiene el rendimiento relativo de un agente
     */
    public double getAgentPerformanceRatio(int userId) {
        return dao.getAgentPerformanceRatio(userId);
    }

    /**
     * Genera reporte de desempeño por período
     */
    public String generatePerformanceReport(String period) {
        Map<String, Object> metrics = getAggregatedMetrics(period);
        
        StringBuilder report = new StringBuilder();
        report.append("=== REPORTE DE DESEMPEÑO - ").append(period).append(" ===\n");
        report.append(String.format("Total de registros: %d\n", metrics.getOrDefault("totalRecords", 0)));
        report.append(String.format("Tiempo promedio: %.2f segundos\n", metrics.getOrDefault("avgTime", 0)));
        report.append(String.format("Tiempo mínimo: %.2f segundos\n", metrics.getOrDefault("minTime", 0)));
        report.append(String.format("Tiempo máximo: %.2f segundos\n", metrics.getOrDefault("maxTime", 0)));
        report.append(String.format("Total de check-ins: %d\n", metrics.getOrDefault("totalCheckIns", 0)));
        report.append(String.format("Total de pasajeros: %d\n", metrics.getOrDefault("totalPassengers", 0)));
        report.append(String.format("Problemas de seguridad: %d\n", metrics.getOrDefault("securityIssues", 0)));
        
        return report.toString();
    }

    /**
     * Genera comparativa entre períodos
     */
    public String generateComparisonReport(String period1, String period2) {
        Map<String, Object> comparison = comparePeriods(period1, period2);
        
        StringBuilder report = new StringBuilder();
        report.append("=== COMPARATIVA DE DESEMPEÑO ===\n");
        report.append(String.format("Período 1: %s\n", period1));
        report.append(String.format("Período 2: %s\n\n", period2));
        
        // Mostrar mejora en tiempo
        if (comparison.containsKey("timeImprovement")) {
            double improvement = ((Number) comparison.get("timeImprovement")).doubleValue();
            if (improvement > 0) {
                report.append(String.format("✓ Mejora de tiempo: %.2f%%\n", improvement));
            } else {
                report.append(String.format("✗ Declive de tiempo: %.2f%%\n", Math.abs(improvement)));
            }
        }
        
        return report.toString();
    }

    /**
     * Obtiene métricas para análisis de tendencias
     */
    public List<PerformanceMetrics> getTrendData(LocalDateTime from, LocalDateTime to) {
        return dao.getMetricsByDateRange(from, to);
    }

    /**
     * Calcula la eficiencia general del sistema
     */
    public double getSystemEfficiency() {
        Map<String, Object> metrics = getAggregatedMetrics("DAILY");
        
        int totalCheckIns = ((Number) metrics.getOrDefault("totalCheckIns", 0)).intValue();
        int securityIssues = ((Number) metrics.getOrDefault("securityIssues", 0)).intValue();
        
        if (totalCheckIns == 0) return 100.0;
        return ((double) (totalCheckIns - securityIssues) / totalCheckIns) * 100;
    }

    /**
     * Obtiene métricas de bottleneck (cuellos de botella)
     */
    public Map<String, Object> identifyBottlenecks() {
        Map<String, Object> bottlenecks = new HashMap<>();
        
        // Obtener los agentes más lentos
        List<PerformanceMetrics> topMetrics = getTopPerformers(100);
        List<PerformanceMetrics> slowest = new ArrayList<>();
        
        if (topMetrics.size() > 0) {
            // Los últimos del listado son los más lentos
            int start = Math.max(0, topMetrics.size() - 5);
            slowest = topMetrics.subList(start, topMetrics.size());
        }
        
        bottlenecks.put("slowestAgents", slowest);
        bottlenecks.put("systemEfficiency", getSystemEfficiency());
        
        return bottlenecks;
    }
}
