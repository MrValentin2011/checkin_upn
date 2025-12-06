/*
 * Modelo para métricas de desempeño
 */
package model;

import java.time.LocalDateTime;

/**
 * Representa métricas de desempeño de un agente o vuelo.
 * Incluye tiempos de check-in, volúmenes y estadísticas de procesamiento.
 */
public class PerformanceMetrics {
    private int metricsId;
    private Integer userId;           // ID del agente (null = general/vuelo)
    private Integer flightId;         // ID del vuelo (null = agente general)
    private int totalCheckIns;        // Total de check-ins procesados
    private double averageCheckInTime; // Tiempo promedio en segundos
    private double minCheckInTime;    // Tiempo mínimo en segundos
    private double maxCheckInTime;    // Tiempo máximo en segundos
    private int totalPassengersProcessed;
    private int totalBaggageItems;
    private int securityIssues;       // Problemas detectados
    private LocalDateTime recordDate;
    private String period;            // DAILY, WEEKLY, MONTHLY

    // Constructores
    public PerformanceMetrics() {
    }

    public PerformanceMetrics(Integer userId, Integer flightId, String period) {
        this.userId = userId;
        this.flightId = flightId;
        this.period = period;
        this.recordDate = LocalDateTime.now();
    }

    // Getters y Setters
    public int getMetricsId() { return metricsId; }
    public void setMetricsId(int metricsId) { this.metricsId = metricsId; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getFlightId() { return flightId; }
    public void setFlightId(Integer flightId) { this.flightId = flightId; }

    public int getTotalCheckIns() { return totalCheckIns; }
    public void setTotalCheckIns(int totalCheckIns) { this.totalCheckIns = totalCheckIns; }

    public double getAverageCheckInTime() { return averageCheckInTime; }
    public void setAverageCheckInTime(double averageCheckInTime) { this.averageCheckInTime = averageCheckInTime; }

    public double getMinCheckInTime() { return minCheckInTime; }
    public void setMinCheckInTime(double minCheckInTime) { this.minCheckInTime = minCheckInTime; }

    public double getMaxCheckInTime() { return maxCheckInTime; }
    public void setMaxCheckInTime(double maxCheckInTime) { this.maxCheckInTime = maxCheckInTime; }

    public int getTotalPassengersProcessed() { return totalPassengersProcessed; }
    public void setTotalPassengersProcessed(int totalPassengersProcessed) { this.totalPassengersProcessed = totalPassengersProcessed; }

    public int getTotalBaggageItems() { return totalBaggageItems; }
    public void setTotalBaggageItems(int totalBaggageItems) { this.totalBaggageItems = totalBaggageItems; }

    public int getSecurityIssues() { return securityIssues; }
    public void setSecurityIssues(int securityIssues) { this.securityIssues = securityIssues; }

    public LocalDateTime getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDateTime recordDate) { this.recordDate = recordDate; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    /**
     * Calcula el promedio de tiempo de procesamiento por pasajero
     */
    public double getTimePerPassenger() {
        if (totalPassengersProcessed == 0) return 0;
        return (averageCheckInTime * totalCheckIns) / totalPassengersProcessed;
    }

    /**
     * Calcula la tasa de eficiencia (porcentaje sin problemas de seguridad)
     */
    public double getEfficiencyRate() {
        if (totalCheckIns == 0) return 0;
        return ((double) (totalCheckIns - securityIssues) / totalCheckIns) * 100;
    }

    @Override
    public String toString() {
        return String.format("Métricas(usuario:%d, vuelo:%d, checkIns:%d, promedio:%.1fs, eficiencia:%.1f%%)",
            userId != null ? userId : 0,
            flightId != null ? flightId : 0,
            totalCheckIns,
            averageCheckInTime,
            getEfficiencyRate()
        );
    }
}
