/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ui.panels;

import service.ServiceLocator;
import service.impl.ReportService;
import model.ReportData;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Panel básico de reportes / dashboard.
 */
public class ReportPanel extends JPanel {

	private final ReportService reportService = ServiceLocator.getInstance().getReportService();

	public ReportPanel() {
		setLayout(new BorderLayout());
		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top.add(new JLabel("Dashboard de Reportes"));
		add(top, BorderLayout.NORTH);

		JPanel center = new JPanel(new GridLayout(2,2,10,10));
		center.add(createCard("Reservaciones", "Resumen de reservaciones por vuelo (hoy)", this::openReservationsSummary));
		center.add(createCard("Check-ins", "Resumen de check-ins por vuelo (hoy)", this::openCheckinsSummary));
		center.add(createCard("Auditoría", "Ver logs de auditoría", this::openLogs));

		add(center, BorderLayout.CENTER);
	}

	private JComponent createCard(String title, String subtitle, Runnable onOpen) {
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createTitledBorder(title));
		p.add(new JLabel(subtitle), BorderLayout.CENTER);
		JButton btn = new JButton("Abrir");
		btn.addActionListener((ActionEvent e) -> onOpen.run());
		p.add(btn, BorderLayout.SOUTH);
		return p;
	}

	private void openReservationsSummary() {
		SwingUtilities.invokeLater(() -> {
			java.util.List<ReportData> data = reportService.generarResumenReservaciones();
			showReservationsDialog("Reservaciones (hoy)", data);
		});
	}

	private void openCheckinsSummary() {
		SwingUtilities.invokeLater(() -> {
			java.util.List<ReportData> data = reportService.generarResumenCheckins();
			showCheckinsDialog("Check-ins (hoy)", data);
		});
	}

	private void openLogs() {
		SwingUtilities.invokeLater(() -> {
			java.util.List<model.LogEntry> logs = reportService.generarLogsRecientes(100);
			showLogsDialog("Logs Recientes", logs);
		});
	}

    private void showReservationsDialog(String title, java.util.List<ReportData> data) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setLayout(new BorderLayout());

        // Panel de filtros de fecha
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Desde:"));
        javax.swing.JSpinner spinStart = new javax.swing.JSpinner(new javax.swing.SpinnerDateModel());
        spinStart.setEditor(new javax.swing.JSpinner.DateEditor(spinStart, "yyyy-MM-dd"));
        filterPanel.add(spinStart);

        filterPanel.add(new JLabel("Hasta:"));
        javax.swing.JSpinner spinEnd = new javax.swing.JSpinner(new javax.swing.SpinnerDateModel());
        spinEnd.setEditor(new javax.swing.JSpinner.DateEditor(spinEnd, "yyyy-MM-dd"));
        filterPanel.add(spinEnd);

        JButton btnFilter = new JButton("Filtrar");
        btnFilter.addActionListener(ae -> {
            java.util.Date dStart = (java.util.Date) spinStart.getValue();
            java.util.Date dEnd = (java.util.Date) spinEnd.getValue();
            java.sql.Date sqlStart = dStart == null ? null : new java.sql.Date(dStart.getTime());
            java.sql.Date sqlEnd = dEnd == null ? null : new java.sql.Date(dEnd.getTime());

            java.util.List<ReportData> filteredData = reportService.generarResumenReservaciones(sqlStart, sqlEnd);
            updateReservationsTable(data, filteredData);
        });
        filterPanel.add(btnFilter);
        dlg.add(filterPanel, BorderLayout.NORTH);

        String[] cols = new String[]{"Vuelo","Reservaciones"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r,int c){return false;} };

        java.util.Map<String,Integer> map = new java.util.LinkedHashMap<>();
        if (data != null) {
            for (ReportData r : data) {
                model.addRow(new Object[]{r.getFlightCode(), r.getPassengers()});
                map.put(r.getFlightCode(), r.getPassengers());
            }
        }

        JTable table = new JTable(model);
        JScrollPane sp = new JScrollPane(table);

        PieChartPanel chart = new PieChartPanel(map, "Reservaciones (hoy)");

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chart, sp);
        split.setResizeWeight(0.6);
        dlg.add(split, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton close = new JButton("Cerrar");
        close.addActionListener(ae->dlg.dispose());
        bottom.add(close);
        dlg.add(bottom, BorderLayout.SOUTH);

        dlg.setSize(800,600);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void updateReservationsTable(java.util.List<ReportData> oldData, java.util.List<ReportData> newData) {
        // This is a placeholder - in a real implementation, we'd update the table model
    }

    private void showCheckinsDialog(String title, java.util.List<ReportData> data) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setLayout(new BorderLayout());

        // Panel de filtros de fecha
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Desde:"));
        javax.swing.JSpinner spinStart = new javax.swing.JSpinner(new javax.swing.SpinnerDateModel());
        spinStart.setEditor(new javax.swing.JSpinner.DateEditor(spinStart, "yyyy-MM-dd"));
        filterPanel.add(spinStart);

        filterPanel.add(new JLabel("Hasta:"));
        javax.swing.JSpinner spinEnd = new javax.swing.JSpinner(new javax.swing.SpinnerDateModel());
        spinEnd.setEditor(new javax.swing.JSpinner.DateEditor(spinEnd, "yyyy-MM-dd"));
        filterPanel.add(spinEnd);

        JButton btnFilter = new JButton("Filtrar");
        btnFilter.addActionListener(ae -> {
            java.util.Date dStart = (java.util.Date) spinStart.getValue();
            java.util.Date dEnd = (java.util.Date) spinEnd.getValue();
            java.sql.Date sqlStart = dStart == null ? null : new java.sql.Date(dStart.getTime());
            java.sql.Date sqlEnd = dEnd == null ? null : new java.sql.Date(dEnd.getTime());

            java.util.List<ReportData> filteredData = reportService.generarResumenCheckins(sqlStart, sqlEnd);
            updateCheckinsTable(data, filteredData);
        });
        filterPanel.add(btnFilter);
        dlg.add(filterPanel, BorderLayout.NORTH);

        String[] cols = new String[]{"Vuelo","Check-ins","Equipaje (count)"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r,int c){return false;} };

        java.util.Map<String,Integer> map = new java.util.LinkedHashMap<>();
        if (data != null) {
            for (ReportData r : data) {
                model.addRow(new Object[]{r.getFlightCode(), r.getPassengers(), r.getBaggageCount()});
                map.put(r.getFlightCode(), r.getPassengers());
            }
        }

        JTable table = new JTable(model);
        JScrollPane sp = new JScrollPane(table);

        PieChartPanel chart = new PieChartPanel(map, "Check-ins (hoy)");
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chart, sp);
        split.setResizeWeight(0.6);
        dlg.add(split, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton close = new JButton("Cerrar");
        close.addActionListener(ae->dlg.dispose());
        bottom.add(close);
        dlg.add(bottom, BorderLayout.SOUTH);

        dlg.setSize(800,600);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void updateCheckinsTable(java.util.List<ReportData> oldData, java.util.List<ReportData> newData) {
        // This is a placeholder - in a real implementation, we'd update the table model
    }

    private void showLogsDialog(String title, java.util.List<model.LogEntry> logs) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setLayout(new BorderLayout());

        // Panel de filtros de fecha
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Desde:"));
        javax.swing.JSpinner spinStart = new javax.swing.JSpinner(new javax.swing.SpinnerDateModel());
        spinStart.setEditor(new javax.swing.JSpinner.DateEditor(spinStart, "yyyy-MM-dd"));
        filterPanel.add(spinStart);

        filterPanel.add(new JLabel("Hasta:"));
        javax.swing.JSpinner spinEnd = new javax.swing.JSpinner(new javax.swing.SpinnerDateModel());
        spinEnd.setEditor(new javax.swing.JSpinner.DateEditor(spinEnd, "yyyy-MM-dd"));
        filterPanel.add(spinEnd);

        JButton btnFilter = new JButton("Filtrar");
        btnFilter.addActionListener(ae -> {
            java.util.Date dStart = (java.util.Date) spinStart.getValue();
            java.util.Date dEnd = (java.util.Date) spinEnd.getValue();
            java.sql.Date sqlStart = dStart == null ? null : new java.sql.Date(dStart.getTime());
            java.sql.Date sqlEnd = dEnd == null ? null : new java.sql.Date(dEnd.getTime());

            java.util.List<model.LogEntry> filteredLogs = reportService.generarLogsRecientes(100, sqlStart, sqlEnd);
            updateLogsTable(logs, filteredLogs);
        });
        filterPanel.add(btnFilter);
        dlg.add(filterPanel, BorderLayout.NORTH);

        String[] cols = new String[]{"Usuario","Acción","Fecha/Hora"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r,int c){return false;} };
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (logs != null) {
            for (model.LogEntry le : logs) {
                model.addRow(new Object[]{le.getUserName(), le.getAction(), le.getTimestamp()==null?"-":dtf.format(le.getTimestamp())});
            }
        }

        JTable table = new JTable(model);
        JScrollPane sp = new JScrollPane(table);
        dlg.add(sp, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton close = new JButton("Cerrar");
        close.addActionListener(ae->dlg.dispose());
        bottom.add(close);
        dlg.add(bottom, BorderLayout.SOUTH);

        dlg.setSize(800,500);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void updateLogsTable(java.util.List<model.LogEntry> oldData, java.util.List<model.LogEntry> newData) {
        // This is a placeholder - in a real implementation, we'd update the table model
    }

	// Simple pie chart renderer (no external libs)
	private static class PieChartPanel extends JPanel {
		private final java.util.Map<String,Integer> data;
		private final String title;

		PieChartPanel(java.util.Map<String,Integer> data, String title) {
			this.data = data == null ? new java.util.LinkedHashMap<>() : data;
			this.title = title == null ? "" : title;
			setPreferredSize(new Dimension(800, 300));
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			int w = getWidth();
			int h = getHeight();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(Color.WHITE);
			g2.fillRect(0,0,w,h);

			g2.setColor(Color.DARK_GRAY);
			g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
			g2.drawString(title, 10, 20);

			int cx = 20; int cy = 40; int size = Math.min(w, h) - 120;
			if (size <= 0) return;

			int total = data.values().stream().mapToInt(Integer::intValue).sum();
			if (total <= 0) {
				g2.drawString("No hay datos para graficar", 10, 50);
				return;
			}

			int start = 0;
			int i = 0;
			Color[] palette = new Color[]{new Color(0x4CAF50), new Color(0xFF9800), new Color(0x2196F3), new Color(0x9C27B0), new Color(0xF44336)};
			for (Map.Entry<String,Integer> e : data.entrySet()) {
				int v = e.getValue();
				int angle = (int) Math.round(360.0 * v / total);
				g2.setColor(palette[i % palette.length]);
				g2.fillArc(cx, cy, size, size, start, angle);
				start += angle;
				i++;
			}

			// legend
			int lx = cx + size + 10; int ly = cy + 10; int idx = 0;
			i = 0;
			for (Map.Entry<String,Integer> e : data.entrySet()) {
				g2.setColor(palette[i % palette.length]);
				g2.fillRect(lx, ly + idx*22, 16, 16);
				g2.setColor(Color.BLACK);
				g2.drawString(e.getKey() + " (" + e.getValue() + ")", lx + 20, ly + idx*22 + 12);
				idx++; i++;
			}
		}
	}
}
