/*
 * Panel de estadísticas y métricas de desempeño
 */
package ui.panels;

import model.PerformanceMetrics;
import service.impl.PerformanceMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Panel para visualizar estadísticas y métricas de desempeño del sistema.
 * Incluye dashboard con indicadores clave, comparativas de períodos y análisis de agentes.
 */
public class StatisticsPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsPanel.class);
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PerformanceMetricsService metricsService;
    private JTabbedPane tabbedPane;
    private JLabel lblSystemEfficiency;
    private JLabel lblAvgTime;
    private JLabel lblTotalCheckIns;
    private JLabel lblSecurityIssues;
    private JTable topAgentsTable;
    private JTable comparisonTable;
    private JTextArea reportArea;

    public StatisticsPanel() {
        this.metricsService = PerformanceMetricsService.getInstance();
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Crear tabs principales
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("📊 Dashboard", createDashboardPanel());
        tabbedPane.addTab("🥇 Mejores Agentes", createTopAgentsPanel());
        tabbedPane.addTab("📈 Comparativa de Períodos", createComparisonPanel());
        tabbedPane.addTab("📋 Reportes", createReportPanel());

        add(tabbedPane, BorderLayout.CENTER);
        refreshData();
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.BOTH;

        // Título
        JLabel titleLabel = new JLabel("DASHBOARD DE DESEMPEÑO");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        panel.add(titleLabel, gbc);

        // KPI: Eficiencia del sistema
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(createKPIPanel("Eficiencia del Sistema", "%", "#2ecc71"), gbc);

        lblSystemEfficiency = new JLabel("0%");
        lblSystemEfficiency.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblSystemEfficiency.setForeground(new Color(46, 204, 113));
        gbc.gridx = 1;
        panel.add(lblSystemEfficiency, gbc);

        // KPI: Tiempo promedio
        gbc.gridx = 2;
        panel.add(createKPIPanel("Tiempo Promedio", "seg", "#3498db"), gbc);

        lblAvgTime = new JLabel("0s");
        lblAvgTime.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblAvgTime.setForeground(new Color(52, 152, 219));
        gbc.gridx = 3;
        panel.add(lblAvgTime, gbc);

        // KPI: Total de check-ins
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(createKPIPanel("Total Check-ins", "hoy", "#9b59b6"), gbc);

        lblTotalCheckIns = new JLabel("0");
        lblTotalCheckIns.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblTotalCheckIns.setForeground(new Color(155, 89, 182));
        gbc.gridx = 1;
        panel.add(lblTotalCheckIns, gbc);

        // KPI: Problemas de seguridad
        gbc.gridx = 2;
        panel.add(createKPIPanel("Problemas Seguridad", "", "#e74c3c"), gbc);

        lblSecurityIssues = new JLabel("0");
        lblSecurityIssues.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblSecurityIssues.setForeground(new Color(231, 76, 60));
        gbc.gridx = 3;
        panel.add(lblSecurityIssues, gbc);

        // Botones de acción
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 4;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        JButton btnRefresh = new JButton("🔄 Refrescar");
        btnRefresh.addActionListener(e -> refreshData());
        buttonPanel.add(btnRefresh);
        
        JButton btnExport = new JButton("📥 Exportar");
        btnExport.addActionListener(e -> exportStatistics());
        buttonPanel.add(btnExport);
        
        JButton btnAnalyze = new JButton("🔍 Analizar");
        btnAnalyze.addActionListener(e -> analyzeBottlenecks());
        buttonPanel.add(btnAnalyze);
        
        panel.add(buttonPanel, gbc);

        return panel;
    }

    private JPanel createKPIPanel(String label, String unit, String color) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        panel.setBackground(Color.WHITE);

        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblLabel.setForeground(Color.GRAY);
        panel.add(lblLabel, BorderLayout.NORTH);

        JLabel lblUnit = new JLabel(unit);
        lblUnit.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblUnit.setForeground(Color.LIGHT_GRAY);
        panel.add(lblUnit, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createTopAgentsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Tabla de mejores agentes
        String[] columns = {"Agente ID", "Check-ins", "Tiempo Promedio", "Eficiencia", "Pasajeros"};
        DefaultTableModel model = new DefaultTableModel(null, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        topAgentsTable = new JTable(model);
        topAgentsTable.setRowHeight(25);
        JScrollPane scrollPane = new JScrollPane(topAgentsTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createComparisonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Selector de períodos
        JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectorPanel.add(new JLabel("Período 1:"));
        JComboBox<String> combo1 = new JComboBox<>(new String[]{"DAILY", "WEEKLY", "MONTHLY"});
        selectorPanel.add(combo1);

        selectorPanel.add(new JLabel("Período 2:"));
        JComboBox<String> combo2 = new JComboBox<>(new String[]{"DAILY", "WEEKLY", "MONTHLY"});
        combo2.setSelectedIndex(1);
        selectorPanel.add(combo2);

        JButton btnCompare = new JButton("Comparar");
        btnCompare.addActionListener(e -> comparePeriodsAction((String) combo1.getSelectedItem(), (String) combo2.getSelectedItem()));
        selectorPanel.add(btnCompare);

        panel.add(selectorPanel, BorderLayout.NORTH);

        // Tabla de comparativa
        String[] columns = {"Métrica", "Período 1", "Período 2", "Variación"};
        DefaultTableModel model = new DefaultTableModel(null, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        comparisonTable = new JTable(model);
        comparisonTable.setRowHeight(25);
        JScrollPane scrollPane = new JScrollPane(comparisonTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createReportPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Selector de período
        JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectorPanel.add(new JLabel("Período:"));
        JComboBox<String> periodCombo = new JComboBox<>(new String[]{"DAILY", "WEEKLY", "MONTHLY"});
        selectorPanel.add(periodCombo);

        JButton btnGenerate = new JButton("Generar Reporte");
        btnGenerate.addActionListener(e -> generateReportAction((String) periodCombo.getSelectedItem()));
        selectorPanel.add(btnGenerate);

        panel.add(selectorPanel, BorderLayout.NORTH);

        // Área de reporte
        reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setFont(new Font("Courier New", Font.PLAIN, 11));
        reportArea.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane scrollPane = new JScrollPane(reportArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void refreshData() {
        SwingUtilities.invokeLater(() -> {
            try {
                // Actualizar dashboard
                double efficiency = metricsService.getSystemEfficiency();
                lblSystemEfficiency.setText(String.format("%.1f%%", efficiency));

                Map<String, Object> dailyMetrics = metricsService.getAggregatedMetrics("DAILY");
                double avgTime = ((Number) dailyMetrics.getOrDefault("avgTime", 0)).doubleValue();
                lblAvgTime.setText(String.format("%.1f s", avgTime));

                int totalCheckIns = ((Number) dailyMetrics.getOrDefault("totalCheckIns", 0)).intValue();
                lblTotalCheckIns.setText(String.valueOf(totalCheckIns));

                int securityIssues = ((Number) dailyMetrics.getOrDefault("securityIssues", 0)).intValue();
                lblSecurityIssues.setText(String.valueOf(securityIssues));

                // Actualizar tabla de mejores agentes
                updateTopAgentsTable();

                logger.info("Estadísticas actualizadas");
            } catch (Exception e) {
                logger.error("Error actualizando estadísticas", e);
            }
        });
    }

    private void updateTopAgentsTable() {
        DefaultTableModel model = (DefaultTableModel) topAgentsTable.getModel();
        model.setRowCount(0);

        List<PerformanceMetrics> topAgents = metricsService.getTopPerformers(10);
        for (PerformanceMetrics metric : topAgents) {
            if (metric.getUserId() != null) {
                Object[] row = {
                    metric.getUserId(),
                    metric.getTotalCheckIns(),
                    String.format("%.1f s", metric.getAverageCheckInTime()),
                    String.format("%.1f%%", metric.getEfficiencyRate()),
                    metric.getTotalPassengersProcessed()
                };
                model.addRow(row);
            }
        }
    }

    private void comparePeriodsAction(String period1, String period2) {
        Map<String, Object> comparison = metricsService.comparePeriods(period1, period2);
        
        DefaultTableModel model = (DefaultTableModel) comparisonTable.getModel();
        model.setRowCount(0);

        Map<String, Object> m1 = (Map<String, Object>) comparison.get("period1");
        Map<String, Object> m2 = (Map<String, Object>) comparison.get("period2");

        if (!m1.isEmpty() && !m2.isEmpty()) {
            double avgTime1 = ((Number) m1.getOrDefault("avgTime", 0)).doubleValue();
            double avgTime2 = ((Number) m2.getOrDefault("avgTime", 0)).doubleValue();

            Object[] row1 = {
                "Tiempo Promedio",
                String.format("%.1f s", avgTime1),
                String.format("%.1f s", avgTime2),
                String.format("%.1f%%", ((avgTime1 - avgTime2) / avgTime1) * 100)
            };
            model.addRow(row1);

            int checkIns1 = ((Number) m1.getOrDefault("totalCheckIns", 0)).intValue();
            int checkIns2 = ((Number) m2.getOrDefault("totalCheckIns", 0)).intValue();

            Object[] row2 = {
                "Total Check-ins",
                String.valueOf(checkIns1),
                String.valueOf(checkIns2),
                String.format("%+d", checkIns2 - checkIns1)
            };
            model.addRow(row2);
        }
    }

    private void generateReportAction(String period) {
        String report = metricsService.generatePerformanceReport(period);
        reportArea.setText(report);
    }

    private void exportStatistics() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar estadísticas");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos de texto", "txt"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String content = generateExportContent();
                java.nio.file.Files.write(chooser.getSelectedFile().toPath(), content.getBytes());
                JOptionPane.showMessageDialog(this, "Estadísticas exportadas exitosamente", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                logger.error("Error exportando estadísticas", e);
                JOptionPane.showMessageDialog(this, "Error al exportar", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void analyzeBottlenecks() {
        Map<String, Object> bottlenecks = metricsService.identifyBottlenecks();
        
        StringBuilder analysis = new StringBuilder();
        analysis.append("=== ANÁLISIS DE CUELLOS DE BOTELLA ===\n\n");
        
        double efficiency = ((Number) bottlenecks.get("systemEfficiency")).doubleValue();
        analysis.append(String.format("Eficiencia del sistema: %.1f%%\n\n", efficiency));

        @SuppressWarnings("unchecked")
        List<PerformanceMetrics> slowest = (List<PerformanceMetrics>) bottlenecks.get("slowestAgents");
        if (!slowest.isEmpty()) {
            analysis.append("Agentes con mayor tiempo de procesamiento:\n");
            for (PerformanceMetrics m : slowest) {
                analysis.append(String.format("  • Agente %d: %.1f seg promedio\n",
                    m.getUserId(), m.getAverageCheckInTime()));
            }
        }

        reportArea.setText(analysis.toString());
    }

    private String generateExportContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("REPORTE DE ESTADÍSTICAS - ").append(LocalDateTime.now().format(timeFormatter)).append("\n");
        sb.append("=".repeat(60)).append("\n\n");

        sb.append("METRICAS DIARIAS:\n");
        sb.append(metricsService.generatePerformanceReport("DAILY")).append("\n");

        sb.append("\nMETRICAS SEMANALES:\n");
        sb.append(metricsService.generatePerformanceReport("WEEKLY")).append("\n");

        sb.append("\nMETRICAS MENSUALES:\n");
        sb.append(metricsService.generatePerformanceReport("MONTHLY")).append("\n");

        return sb.toString();
    }
}
