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
	private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Clave","Valor"}, 0) {
		@Override public boolean isCellEditable(int row, int col) { return col == 1; }
	};

	public ConfigPanel() {
		setLayout(new BorderLayout());
		JTable table = new JTable(model);
		loadParameters();

		add(new JScrollPane(table), BorderLayout.CENTER);

		JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton btnSave = new JButton("Guardar cambios");
		btnSave.addActionListener(e -> {
			int row = table.getSelectedRow();
			if (row < 0) {
				JOptionPane.showMessageDialog(this, "Seleccione una fila para guardar");
				return;
			}
			String key = table.getValueAt(row, 0).toString();
			String val = table.getValueAt(row, 1).toString();
			boolean ok = cfg.actualizarParametro(key, val);
			JOptionPane.showMessageDialog(this, ok ? "Parámetro actualizado" : "Fallo al actualizar");
			loadParameters();
		});

		bottom.add(btnSave);
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
