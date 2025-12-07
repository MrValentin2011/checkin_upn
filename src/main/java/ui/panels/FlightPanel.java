package ui.panels;

import ui.dialogs.FlightFormDialog;
import model.Flight;
import service.impl.FlightService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FlightPanel extends JPanel {

    private final FlightService service = new FlightService();
    private final DefaultTableModel model;
    private final JTable table;
    private final JTextField txtFiltro;
    private final JButton btnEditar;
    private final JButton btnEliminar;

    public FlightPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JLabel title = new JLabel("🛫 Gestión de Vuelos", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        add(title, BorderLayout.NORTH);

        // 🔍 Barra superior
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Buscar: "));
        txtFiltro = new JTextField(25);
        JButton btnBuscar = new JButton("Filtrar");
        JButton btnLimpiar = new JButton("Limpiar");
        JButton btnNuevo = new JButton("➕ Nuevo Vuelo");
        btnEditar = new JButton("✏️ Editar");
        btnEliminar = new JButton("🗑 Eliminar");

        btnEditar.setEnabled(false);
        btnEliminar.setEnabled(false);

        topPanel.add(txtFiltro);
        topPanel.add(btnBuscar);
        topPanel.add(btnLimpiar);
        topPanel.add(btnNuevo);
        topPanel.add(btnEditar);
        add(topPanel, BorderLayout.NORTH);

        // 🧾 Tabla
        model = new DefaultTableModel(new Object[] {
                "ID", "Código", "Aerolínea", "Origen", "Destino", "Salida", "Llegada", "Estado", "Capacidad"
        }, 0);
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // 🔄 Panel inferior
        JButton btnRefresh = new JButton("🔄 Actualizar");
        JPanel bottom = new JPanel();
        bottom.add(btnRefresh);
        add(bottom, BorderLayout.SOUTH);

        // 🎯 Eventos
        btnBuscar.addActionListener(e -> cargarVuelos(txtFiltro.getText()));
        btnLimpiar.addActionListener(e -> {
            txtFiltro.setText("");
            cargarVuelos("");
        });
        btnRefresh.addActionListener(e -> cargarVuelos(""));

        btnNuevo.addActionListener(e -> {
            FlightFormDialog dialog = new FlightFormDialog((Frame) SwingUtilities.getWindowAncestor(this), null);
            dialog.setVisible(true);
            cargarVuelos("");
        });

        btnEditar.addActionListener(e -> editarSeleccionado());
        btnEliminar.addActionListener(e -> eliminarSeleccionado());

        // 🧭 Activar botones solo si hay selección
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                btnEditar.setEnabled(false);
                btnEliminar.setEnabled(false);
                return;
            }

            String estado = (String) model.getValueAt(row, 7); // columna 'Estado'

            // Si el vuelo está CLOSED, desactivar edición
            if ("CLOSED".equalsIgnoreCase(estado)) {
                btnEditar.setEnabled(false);
            } else {
                btnEditar.setEnabled(true);
            }

            // Eliminar sí puede estar habilitado si tú lo decides; lo dejo habilitado
            btnEliminar.setEnabled(true);
        });

        cargarVuelos("");
    }

    private void cargarVuelos(String filtro) {
        model.setRowCount(0);
        List<Flight> vuelos = service.listarVuelos(filtro);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (Flight f : vuelos) {
            model.addRow(new Object[] {
                    f.getId(),
                    f.getCode(),
                    f.getAirline(),
                    f.getOrigin(),
                    f.getDestination(),
                    f.getDepartureTime() != null ? f.getDepartureTime().format(formatter) : "-",
                    f.getArrivalTime() != null ? f.getArrivalTime().format(formatter) : "-",
                    f.getStatus(),
                    f.getCapacity()
            });
        }
    }

    private void editarSeleccionado() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un vuelo para editar", "Selección Requerida",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            Flight f = new Flight();
            f.setId((int) model.getValueAt(row, 0));
            f.setCode((String) model.getValueAt(row, 1));
            f.setAirline((String) model.getValueAt(row, 2));
            f.setOrigin((String) model.getValueAt(row, 3));
            f.setDestination((String) model.getValueAt(row, 4));

            String depStr = (String) model.getValueAt(row, 5);
            String arrStr = (String) model.getValueAt(row, 6);
            if (depStr != null && !depStr.equals("-"))
                f.setDepartureTime(LocalDateTime.parse(depStr, formatter));
            if (arrStr != null && !arrStr.equals("-"))
                f.setArrivalTime(LocalDateTime.parse(arrStr, formatter));

            f.setStatus((String) model.getValueAt(row, 7));
            f.setCapacity((int) model.getValueAt(row, 8));

            FlightFormDialog dialog = new FlightFormDialog((Frame) SwingUtilities.getWindowAncestor(this), f);
            dialog.setVisible(true);
            cargarVuelos("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al abrir formulario: " + ex.getMessage(), "Error al Editar",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void eliminarSeleccionado() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione un vuelo para eliminar");
            return;
        }

        int id = (int) model.getValueAt(row, 0);
        String code = (String) model.getValueAt(row, 1);

        int opt = JOptionPane.showConfirmDialog(this,
                "¿Está seguro que desea eliminar el vuelo " + code + "?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION);

        if (opt == JOptionPane.YES_OPTION) {
            boolean ok = service.eliminarVuelo(id);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Vuelo eliminado correctamente ✅");
                cargarVuelos("");
            } else {
                JOptionPane.showMessageDialog(this,
                        "No se pudo eliminar el vuelo.\nPosiblemente tiene reservas o check-ins asociados.",
                        "Error", JOptionPane.WARNING_MESSAGE);
            }
        }
    }
}
