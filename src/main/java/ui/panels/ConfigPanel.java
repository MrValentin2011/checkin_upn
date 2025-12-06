/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ui.panels;

import service.impl.ConfigService;
import model.ConfigParameter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Panel para administración de parámetros de configuración.
 */
public class ConfigPanel extends JPanel {

	private final ConfigService cfg = ConfigService.getInstance();
	private final java.util.Set<String> EDITABLE_KEYS = java.util.Set.of("baggage.price.perKg", "baggage.price.perPiece");

	private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Clave","Valor"}, 0) {
		@Override public boolean isCellEditable(int row, int col) {
			if (col != 1) return false;
			Object key = getValueAt(row, 0);
			if (key == null) return false;
			String k = key.toString();
			return EDITABLE_KEYS.contains(k);
		}
	};

	public ConfigPanel() {
		setLayout(new BorderLayout());
		JTable table = new JTable(model);
		loadParameters();

		// Nota: se eliminó el editor rápido; la edición de tarifas se realiza directamente en la tabla.

		add(new JScrollPane(table), BorderLayout.CENTER);

		// Persistir cambios inmediatamente al editar celdas editables
		model.addTableModelListener(evt -> {
			try {
				if (evt.getType() != javax.swing.event.TableModelEvent.UPDATE) return;
				int col = evt.getColumn();
				int row = evt.getFirstRow();
				if (col != 1 || row < 0) return;
				Object keyObj = model.getValueAt(row, 0);
				if (keyObj == null) return;
				String key = keyObj.toString();
				if (!EDITABLE_KEYS.contains(key)) {
					JOptionPane.showMessageDialog(this, "Esta clave no es editable desde aquí.", "Parámetro Protegido", JOptionPane.WARNING_MESSAGE);
					loadParameters();
					return;
				}
				String newVal = String.valueOf(model.getValueAt(row, 1));
				// validación numérica
				double dv;
				try {
					dv = Double.parseDouble(newVal);
					if (dv < 0) throw new NumberFormatException("negativo");
				} catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(this, "Valor inválido para " + key + ": debe ser número no negativo", "Valor Inválido", JOptionPane.ERROR_MESSAGE);
					loadParameters();
					return;
				}
				boolean ok = cfg.actualizarParametro(key, newVal);
				if (ok) {
					JOptionPane.showMessageDialog(this, "Parámetro '" + key + "' actualizado: " + newVal, "Actualización Exitosa", JOptionPane.INFORMATION_MESSAGE);
				} else {
					JOptionPane.showMessageDialog(this, "Error al guardar '" + key + "' en la base de datos", "Error al Guardar", JOptionPane.ERROR_MESSAGE);
					loadParameters();
				}
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(this, "Error al procesar la edición: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				loadParameters();
			}
		});

		JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		// Notas: solo se permite actualizar valores existentes desde la tabla.
		JButton btnSaveAll = new JButton("Guardar todo");
		JButton btnRefresh = new JButton("Refrescar");

		btnSaveAll.addActionListener(e -> {
			int rows = model.getRowCount();
			int success = 0;
			for (int r = 0; r < rows; r++) {
				String key = model.getValueAt(r, 0).toString();
				String val = String.valueOf(model.getValueAt(r, 1));
				// Only allow updating the configured editable keys
				if (!EDITABLE_KEYS.contains(key)) continue;
				// validate numeric keys
				try {
					double dv = Double.parseDouble(val);
					if (dv < 0) throw new NumberFormatException("negativo");
				} catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(this, "Valor inválido para " + key + ": debe ser número no negativo", "Valor Inválido", JOptionPane.ERROR_MESSAGE);
					continue;
				}
				if (cfg.actualizarParametro(key, val)) success++; 
			}
			JOptionPane.showMessageDialog(this, "Parámetros actualizados: " + success, "Actualización Completada", JOptionPane.INFORMATION_MESSAGE);
			loadParameters();
		});

		btnRefresh.addActionListener(e -> loadParameters());

		bottom.add(btnSaveAll);
		bottom.add(btnRefresh);
		add(bottom, BorderLayout.SOUTH);
	}

	private void loadParameters() {
		model.setRowCount(0);
		List<ConfigParameter> params = cfg.listarParametros();
		for (ConfigParameter p : params) {
			model.addRow(new Object[]{p.getName(), p.getValue()});
		}
	}
}
