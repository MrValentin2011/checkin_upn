package ui.panels;

import dao.impl.PassengerDao;
import model.Passenger;
import ui.dialogs.PassengerForm;
import ui.dialogs.ReservationCreateDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class PassengerPanel extends JPanel {

    private final PassengerDao passengerDao = new PassengerDao();

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Nombre", "Apellido", "Tipo Doc", "N° Doc", "F.Nac.", "Email", "Teléfono", "Frecuente"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextField txtBuscar = new JTextField(18);

    public PassengerPanel() {
        setLayout(new BorderLayout(8, 8));

        // Top: filtros
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        top.add(new JLabel("Buscar (nombre/apellido/doc):"));
        top.add(txtBuscar);
        JButton btnFiltrar = new JButton("Filtrar");
        JButton btnLimpiar = new JButton("Limpiar");
        top.add(btnFiltrar);
        top.add(btnLimpiar);

        // Center: tabla
        table.setRowHeight(22);
        JScrollPane scroll = new JScrollPane(table);

        // Bottom: acciones
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnNuevo   = new JButton("➕ Nuevo");
        JButton btnEditar  = new JButton("✏️ Editar");
        JButton btnReserva = new JButton("🎫 Nueva Reserva");
        actions.add(btnReserva);
        actions.add(btnNuevo);
        actions.add(btnEditar);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);

        // Eventos
        btnFiltrar.addActionListener(e -> loadData(txtBuscar.getText().trim()));
        btnLimpiar.addActionListener(e -> { txtBuscar.setText(""); loadData(""); });
        btnNuevo.addActionListener(e -> createPassenger());
        btnEditar.addActionListener(e -> editPassenger());
        btnReserva.addActionListener(e -> openReservationDialog());

        // Carga inicial
        loadData("");
    }

    private void loadData(String q) {
        model.setRowCount(0);
        List<Passenger> list = (q == null || q.isBlank())
                ? passengerDao.listAll()
                : passengerDao.search(q);
        for (Passenger p : list) {
            model.addRow(new Object[]{
                p.getId(),
                p.getFirstName(),
                p.getLastName(),
                p.getDocumentType(),
                p.getDocumentNumber(),
                p.getDateOfBirth() != null ? p.getDateOfBirth().toString() : "",
                p.getEmail(),
                p.getPhone(),
                p.getFrequentCounter()
            });
        }
    }

    private void createPassenger() {
        PassengerForm form = new PassengerForm(null);
        if (!form.showDialog(this)) return;

        // Validación de duplicado de documento
        if (passengerDao.existsByDocument(form.docNumber, null)) {
            JOptionPane.showMessageDialog(this, "El documento ya existe. Verifique.");
            return;
        }

        Passenger p = new Passenger();
        p.setFirstName(form.firstName);
        p.setLastName(form.lastName);
        p.setDocumentType(form.docType);
        p.setDocumentNumber(form.docNumber);
        p.setDateOfBirth(form.dob);
        p.setEmail(form.email);
        p.setPhone(form.phone);
        p.setFrequentCounter(form.frequentCounter);

        boolean ok = passengerDao.insert(p);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "No se pudo crear el pasajero.");
            return;
        }
        loadData(txtBuscar.getText().trim());
    }

    private void editPassenger() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Seleccione un pasajero"); return; }

        PassengerForm.Data d = new PassengerForm.Data(
                (Integer) model.getValueAt(row, 0),
                (String)  model.getValueAt(row, 1),
                (String)  model.getValueAt(row, 2),
                (String)  model.getValueAt(row, 3),
                (String)  model.getValueAt(row, 4),
                strToLocalDate((String) model.getValueAt(row, 5)),
                (String)  model.getValueAt(row, 6),
                (String)  model.getValueAt(row, 7),
                (Integer) model.getValueAt(row, 8)
        );

        PassengerForm form = new PassengerForm(d);
        if (!form.showDialog(this)) return;

        // Validación de duplicado de documento (excluye el propio ID)
        if (passengerDao.existsByDocument(form.docNumber, d.id)) {
            JOptionPane.showMessageDialog(this, "El documento ya existe en otro pasajero.");
            return;
        }

        Passenger p = new Passenger();
        p.setId(d.id);
        p.setFirstName(form.firstName);
        p.setLastName(form.lastName);
        p.setDocumentType(form.docType);
        p.setDocumentNumber(form.docNumber);
        p.setDateOfBirth(form.dob);
        p.setEmail(form.email);
        p.setPhone(form.phone);
        p.setFrequentCounter(form.frequentCounter);

        boolean ok = passengerDao.update(p);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "No se pudo actualizar el pasajero.");
            return;
        }
        loadData(txtBuscar.getText().trim());
    }

    // Elimina pasajero seleccionado
    /* 
    private void deletePassenger() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Seleccione un pasajero"); return; }
        int id = (Integer) model.getValueAt(row, 0);

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Eliminar pasajero ID " + id + "?\nSi tiene reservas vinculadas, no se podrá eliminar.",
                "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        boolean ok = passengerDao.delete(id);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "No se pudo eliminar (posibles reservas vinculadas).");
            return;
        }
        loadData(txtBuscar.getText().trim());
    }*/

    private void openReservationDialog() {
        int row = table.getSelectedRow();
        Integer passengerId = (row >= 0) ? (Integer) model.getValueAt(row, 0) : null;

        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        ReservationCreateDialog dlg =
            new ReservationCreateDialog(owner, passengerId, pnr ->
                JOptionPane.showMessageDialog(this, "Reserva creada con PNR: " + pnr)
            );
        dlg.setVisible(true);
    }

    private LocalDate strToLocalDate(String s) {
        try { return (s == null || s.isBlank()) ? null : LocalDate.parse(s); }
        catch (Exception e) { return null; }
    }
}
