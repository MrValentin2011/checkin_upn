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
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Panel de reportes / dashboard con filtros de fecha y opciones de exportación.
 */
public class ReportPanel extends JPanel {

	private final ReportService reportService = ServiceLocator.getInstance().getReportService();

	public ReportPanel() {
		setLayout(new BorderLayout());
		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top.add(new JLabel("📊 Dashboard de Reportes"));
		add(top, BorderLayout.NORTH);

		JPanel center = new JPanel(new GridLayout(2, 2, 10, 10));
		center.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		center.add(createCard("📋 Reservaciones", "Resumen de reservaciones por vuelo", this::openReservationsSummary));
		center.add(createCard("✈️ Check-ins", "Resumen de check-ins completados por vuelo", this::openCheckinsSummary));
		center.add(createCard("📝 Auditoría", "Ver logs de auditoría del sistema", this::openLogs));

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
		System.out.println("[ReportPanel] openReservationsSummary() - Abriendo diálogo de reservaciones");
		SwingUtilities.invokeLater(() -> {
			java.util.List<ReportData> data = reportService.generarResumenReservaciones();
			System.out.println("[ReportPanel] openReservationsSummary() - Datos iniciales: " + (data == null ? 0 : data.size()) + " registros");
			showReservationsDialog("Reservaciones (Hoy)", data);
		});
	}

	private void openCheckinsSummary() {
		System.out.println("[ReportPanel] openCheckinsSummary() - Abriendo diálogo de check-ins");
		SwingUtilities.invokeLater(() -> {
			java.util.List<ReportData> data = reportService.generarResumenCheckins();
			System.out.println("[ReportPanel] openCheckinsSummary() - Datos iniciales: " + (data == null ? 0 : data.size()) + " registros");
			showCheckinsDialog("Check-ins (Hoy)", data);
		});
	}

	private void openLogs() {
		System.out.println("[ReportPanel] openLogs() - Abriendo diálogo de logs");
		SwingUtilities.invokeLater(() -> {
			java.util.List<model.LogEntry> logs = reportService.generarLogsRecientes(100);
			System.out.println("[ReportPanel] openLogs() - Datos iniciales: " + (logs == null ? 0 : logs.size()) + " logs");
			showLogsDialog("Logs Recientes", logs);
		});
	}

    private void showReservationsDialog(String title, java.util.List<ReportData> initialData) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setLayout(new BorderLayout());

        // Panel de filtros de fecha
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filtros por Fecha"));
        
        JLabel lblFrom = new JLabel("Desde:");
        JSpinner spinStart = new JSpinner(new SpinnerDateModel());
        spinStart.setEditor(new JSpinner.DateEditor(spinStart, "yyyy-MM-dd"));
        spinStart.setValue(new Date());
        
        JLabel lblTo = new JLabel("Hasta:");
        JSpinner spinEnd = new JSpinner(new SpinnerDateModel());
        spinEnd.setEditor(new JSpinner.DateEditor(spinEnd, "yyyy-MM-dd"));
        spinEnd.setValue(new Date());

        // Definir los componentes de la tabla y gráfico al inicio
        String[] cols = new String[]{"Vuelo", "Reservaciones"};
        DefaultTableModel tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel);
        table.setRowHeight(22);
        JScrollPane sp = new JScrollPane(table);

        PieChartPanel chart = new PieChartPanel(new LinkedHashMap<>(), "Reservaciones (Hoy)");

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chart, sp);
        split.setResizeWeight(0.6);

        // Cargar datos iniciales
        if (initialData != null && !initialData.isEmpty()) {
            updateReservationsTable(tableModel, chart, initialData);
        } else {
            System.out.println("[ReportPanel] showReservationsDialog() - Sin datos iniciales");
        }

        JButton btnFilter = new JButton("🔍 Filtrar");
        btnFilter.addActionListener(ae -> {
            System.out.println("[ReportPanel] showReservationsDialog() - Filtrando por fechas");
            Date dStart = (Date) spinStart.getValue();
            Date dEnd = (Date) spinEnd.getValue();
            java.sql.Date sqlStart = new java.sql.Date(dStart.getTime());
            java.sql.Date sqlEnd = new java.sql.Date(dEnd.getTime());

            System.out.println("[ReportPanel] showReservationsDialog() - Rango: " + sqlStart + " a " + sqlEnd);
            java.util.List<ReportData> filteredData = reportService.generarResumenReservaciones(sqlStart, sqlEnd);
            System.out.println("[ReportPanel] showReservationsDialog() - Datos filtrados: " + (filteredData == null ? 0 : filteredData.size()) + " registros");
            
            if (filteredData != null && !filteredData.isEmpty()) {
                updateReservationsTable(tableModel, chart, filteredData);
            } else {
                tableModel.setRowCount(0);
                chart.updateData(new LinkedHashMap<>(), "Reservaciones (Sin datos)");
                JOptionPane.showMessageDialog(dlg, "No hay datos para el rango de fechas seleccionado", "Sin Resultados", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        filterPanel.add(lblFrom);
        filterPanel.add(spinStart);
        filterPanel.add(lblTo);
        filterPanel.add(spinEnd);
        filterPanel.add(btnFilter);
        
        dlg.add(filterPanel, BorderLayout.NORTH);
        dlg.add(split, BorderLayout.CENTER);

        // Panel inferior con botones de exportación y cierre
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        JButton btnClose = new JButton("Cerrar");
        btnClose.addActionListener(ae -> dlg.dispose());
        bottom.add(btnClose);
        
        dlg.add(bottom, BorderLayout.SOUTH);

        dlg.setSize(900, 700);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void updateReservationsTable(DefaultTableModel tableModel, PieChartPanel chart, java.util.List<ReportData> data) {
        tableModel.setRowCount(0);
        Map<String, Integer> map = new LinkedHashMap<>();
        
        if (data != null) {
            for (ReportData r : data) {
                tableModel.addRow(new Object[]{r.getFlightCode(), r.getPassengers()});
                map.put(r.getFlightCode(), r.getPassengers());
            }
        }
        
        chart.updateData(map, "Reservaciones");
    }

    private void showCheckinsDialog(String title, java.util.List<ReportData> initialData) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setLayout(new BorderLayout());

        // Panel de filtros de fecha
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filtros por Fecha"));
        
        JLabel lblFrom = new JLabel("Desde:");
        JSpinner spinStart = new JSpinner(new SpinnerDateModel());
        spinStart.setEditor(new JSpinner.DateEditor(spinStart, "yyyy-MM-dd"));
        spinStart.setValue(new Date());
        
        JLabel lblTo = new JLabel("Hasta:");
        JSpinner spinEnd = new JSpinner(new SpinnerDateModel());
        spinEnd.setEditor(new JSpinner.DateEditor(spinEnd, "yyyy-MM-dd"));
        spinEnd.setValue(new Date());

        String[] cols = new String[]{"Vuelo", "Check-ins", "Equipaje"};
        DefaultTableModel tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel);
        table.setRowHeight(22);
        JScrollPane sp = new JScrollPane(table);

        PieChartPanel chart = new PieChartPanel(new LinkedHashMap<>(), "Check-ins (Hoy)");

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chart, sp);
        split.setResizeWeight(0.6);

        // Cargar datos iniciales
        if (initialData != null && !initialData.isEmpty()) {
            updateCheckinsTable(tableModel, chart, initialData);
        } else {
            System.out.println("[ReportPanel] showCheckinsDialog() - Sin datos iniciales");
        }

        JButton btnFilter = new JButton("🔍 Filtrar");
        btnFilter.addActionListener(ae -> {
            System.out.println("[ReportPanel] showCheckinsDialog() - Filtrando por fechas");
            Date dStart = (Date) spinStart.getValue();
            Date dEnd = (Date) spinEnd.getValue();
            java.sql.Date sqlStart = new java.sql.Date(dStart.getTime());
            java.sql.Date sqlEnd = new java.sql.Date(dEnd.getTime());

            System.out.println("[ReportPanel] showCheckinsDialog() - Rango: " + sqlStart + " a " + sqlEnd);
            java.util.List<ReportData> filteredData = reportService.generarResumenCheckins(sqlStart, sqlEnd);
            System.out.println("[ReportPanel] showCheckinsDialog() - Datos filtrados: " + (filteredData == null ? 0 : filteredData.size()) + " registros");
            
            if (filteredData != null && !filteredData.isEmpty()) {
                updateCheckinsTable(tableModel, chart, filteredData);
            } else {
                tableModel.setRowCount(0);
                chart.updateData(new LinkedHashMap<>(), "Check-ins (Sin datos)");
                JOptionPane.showMessageDialog(dlg, "No hay datos para el rango de fechas seleccionado", "Sin Resultados", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        filterPanel.add(lblFrom);
        filterPanel.add(spinStart);
        filterPanel.add(lblTo);
        filterPanel.add(spinEnd);
        filterPanel.add(btnFilter);
        
        dlg.add(filterPanel, BorderLayout.NORTH);
        dlg.add(split, BorderLayout.CENTER);

        // Panel inferior con botones de exportación y cierre
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton btnExportExcel = new JButton("📊 Exportar Excel");
        btnExportExcel.addActionListener(ae -> exportToExcel(initialData, dlg));
        bottom.add(btnExportExcel);

        JButton btnExportPDF = new JButton("📄 Exportar PDF");
        btnExportPDF.addActionListener(ae -> exportToPDF(initialData, dlg));
        bottom.add(btnExportPDF);

        JButton btnClose = new JButton("Cerrar");
        btnClose.addActionListener(ae -> dlg.dispose());
        bottom.add(btnClose);
        
        dlg.add(bottom, BorderLayout.SOUTH);

        dlg.setSize(900, 700);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void updateCheckinsTable(DefaultTableModel tableModel, PieChartPanel chart, java.util.List<ReportData> data) {
        tableModel.setRowCount(0);
        Map<String, Integer> map = new LinkedHashMap<>();
        
        if (data != null) {
            for (ReportData r : data) {
                tableModel.addRow(new Object[]{r.getFlightCode(), r.getPassengers(), r.getBaggageCount()});
                map.put(r.getFlightCode(), r.getPassengers());
            }
        }
        
        chart.updateData(map, "Check-ins");
    }

    private void showLogsDialog(String title, java.util.List<model.LogEntry> initialData) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), title, Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setLayout(new BorderLayout());

        // Panel de filtros de fecha
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filtros por Fecha"));
        
        JLabel lblFrom = new JLabel("Desde:");
        JSpinner spinStart = new JSpinner(new SpinnerDateModel());
        spinStart.setEditor(new JSpinner.DateEditor(spinStart, "yyyy-MM-dd"));
        spinStart.setValue(new Date());
        
        JLabel lblTo = new JLabel("Hasta:");
        JSpinner spinEnd = new JSpinner(new SpinnerDateModel());
        spinEnd.setEditor(new JSpinner.DateEditor(spinEnd, "yyyy-MM-dd"));
        spinEnd.setValue(new Date());

        String[] cols = new String[]{"Usuario", "Acción", "Fecha/Hora"};
        DefaultTableModel tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        if (initialData != null && !initialData.isEmpty()) {
            for (model.LogEntry le : initialData) {
                tableModel.addRow(new Object[]{
                    le.getUserName(),
                    le.getAction(),
                    le.getTimestamp() == null ? "-" : dtf.format(le.getTimestamp())
                });
            }
        }

        JTable table = new JTable(tableModel);
        table.setRowHeight(22);
        JScrollPane sp = new JScrollPane(table);

        JButton btnFilter = new JButton("🔍 Filtrar");
        btnFilter.addActionListener(ae -> {
            System.out.println("[ReportPanel] showLogsDialog() - Filtrando por fechas");
            Date dStart = (Date) spinStart.getValue();
            Date dEnd = (Date) spinEnd.getValue();
            java.sql.Date sqlStart = new java.sql.Date(dStart.getTime());
            java.sql.Date sqlEnd = new java.sql.Date(dEnd.getTime());

            System.out.println("[ReportPanel] showLogsDialog() - Rango: " + sqlStart + " a " + sqlEnd);
            java.util.List<model.LogEntry> filteredLogs = reportService.generarLogsRecientes(100, sqlStart, sqlEnd);
            System.out.println("[ReportPanel] showLogsDialog() - Logs filtrados: " + (filteredLogs == null ? 0 : filteredLogs.size()));
            
            tableModel.setRowCount(0);
            if (filteredLogs != null && !filteredLogs.isEmpty()) {
                for (model.LogEntry le : filteredLogs) {
                    tableModel.addRow(new Object[]{
                        le.getUserName(),
                        le.getAction(),
                        le.getTimestamp() == null ? "-" : dtf.format(le.getTimestamp())
                    });
                }
            } else {
                JOptionPane.showMessageDialog(dlg, "No hay logs para el rango de fechas seleccionado", "Sin Resultados", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        filterPanel.add(lblFrom);
        filterPanel.add(spinStart);
        filterPanel.add(lblTo);
        filterPanel.add(spinEnd);
        filterPanel.add(btnFilter);
        
        dlg.add(filterPanel, BorderLayout.NORTH);
        dlg.add(sp, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton btnClose = new JButton("Cerrar");
        btnClose.addActionListener(ae -> dlg.dispose());
        bottom.add(btnClose);
        
        dlg.add(bottom, BorderLayout.SOUTH);

        dlg.setSize(900, 600);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void exportToExcel(java.util.List<ReportData> data, JDialog parentDialog) {
        if (data == null || data.isEmpty()) {
            JOptionPane.showMessageDialog(parentDialog, "No hay datos para exportar", "Sin Datos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar como Excel");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos Excel (*.xlsx)", "xlsx"));
        
        if (chooser.showSaveDialog(parentDialog) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String path = file.getAbsolutePath();
            if (!path.endsWith(".xlsx")) {
                path += ".xlsx";
            }
            
            try {
                System.out.println("[ReportPanel] exportToExcel() - Exportando a: " + path);
                reportService.exportarExcel(data, path);
                JOptionPane.showMessageDialog(parentDialog, "Archivo exportado exitosamente a:\n" + path, "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);
                System.out.println("[ReportPanel] exportToExcel() - Exportación completada");
            } catch (Exception e) {
                System.err.println("[ReportPanel] exportToExcel() - ERROR: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(parentDialog, "Error al exportar: " + e.getMessage(), "Error de Exportación", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportToPDF(java.util.List<ReportData> data, JDialog parentDialog) {
        if (data == null || data.isEmpty()) {
            JOptionPane.showMessageDialog(parentDialog, "No hay datos para exportar", "Sin Datos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Guardar como PDF");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos PDF (*.pdf)", "pdf"));
        
        if (chooser.showSaveDialog(parentDialog) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String path = file.getAbsolutePath();
            if (!path.endsWith(".pdf")) {
                path += ".pdf";
            }
            
            try {
                System.out.println("[ReportPanel] exportToPDF() - Exportando a: " + path);
                reportService.exportarPDF(data, path);
                JOptionPane.showMessageDialog(parentDialog, "Archivo exportado exitosamente a:\n" + path, "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);
                System.out.println("[ReportPanel] exportToPDF() - Exportación completada");
            } catch (Exception e) {
                System.err.println("[ReportPanel] exportToPDF() - ERROR: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(parentDialog, "Error al exportar: " + e.getMessage(), "Error de Exportación", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

	// Pie chart renderer mejorado
	private static class PieChartPanel extends JPanel {
		private Map<String, Integer> data;
		private String title;

		PieChartPanel(Map<String, Integer> data, String title) {
			this.data = data == null ? new LinkedHashMap<>() : data;
			this.title = title == null ? "" : title;
			setPreferredSize(new Dimension(800, 350));
		}

		void updateData(Map<String, Integer> newData, String newTitle) {
			this.data = newData == null ? new LinkedHashMap<>() : newData;
			this.title = newTitle == null ? "" : newTitle;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;
			int w = getWidth();
			int h = getHeight();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(Color.WHITE);
			g2.fillRect(0, 0, w, h);

			g2.setColor(Color.DARK_GRAY);
			g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
			g2.drawString(title, 20, 30);

			int cx = 40;
			int cy = 50;
			int size = Math.min(w - 200, h - 100);
			if (size <= 0) {
				g2.drawString("Espacio insuficiente para graficar", 20, 60);
				return;
			}

			int total = data.values().stream().mapToInt(Integer::intValue).sum();
			if (total <= 0) {
				g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
				g2.drawString("No hay datos para graficar", 20, 60);
				return;
			}

			int start = 0;
			int i = 0;
			Color[] palette = new Color[]{
				new Color(0x4CAF50), new Color(0xFF9800), new Color(0x2196F3),
				new Color(0x9C27B0), new Color(0xF44336), new Color(0x00BCD4),
				new Color(0x8BC34A), new Color(0xE91E63), new Color(0x009688),
				new Color(0xFFEB3B)
			};

			for (Map.Entry<String, Integer> e : data.entrySet()) {
				int v = e.getValue();
				int angle = (int) Math.round(360.0 * v / total);
				g2.setColor(palette[i % palette.length]);
				g2.fillArc(cx, cy, size, size, start, angle);
				g2.setColor(Color.WHITE);
				g2.setStroke(new BasicStroke(2));
				g2.drawArc(cx, cy, size, size, start, angle);
				start += angle;
				i++;
			}

			// Leyenda
			int lx = cx + size + 30;
			int ly = cy + 10;
			int idx = 0;
			i = 0;
			g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 11f));
			for (Map.Entry<String, Integer> e : data.entrySet()) {
				g2.setColor(palette[i % palette.length]);
				g2.fillRect(lx, ly + idx * 22, 14, 14);
				g2.setColor(Color.BLACK);
				g2.drawRect(lx, ly + idx * 22, 14, 14);
				g2.drawString(e.getKey() + " (" + e.getValue() + ")", lx + 20, ly + idx * 22 + 12);
				idx++;
				i++;
			}
		}
	}
}
