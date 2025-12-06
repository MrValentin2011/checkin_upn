package ui.panels;

import dao.impl.PassengerDao;
import model.Passenger;
import service.impl.PassengerService;
import ui.dialogs.PassengerForm;
import ui.dialogs.ReservationCreateDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

public class PassengerPanel extends JPanel {

    private final PassengerService passengerService = new PassengerService();
    private final PassengerDao passengerDao = new PassengerDao(); // Para el cálculo de frecuente

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
        System.out.println("[PassengerPanel] loadData() - Búsqueda: '" + (q == null ? "" : q) + "'");
        List<Passenger> list = (q == null || q.isBlank())
                ? passengerService.listarPasajeros()
                : passengerService.buscar(q);
        System.out.println("[PassengerPanel] loadData() - Total de pasajeros encontrados: " + list.size());
        for (Passenger p : list) {
            int frequentCount = passengerDao.computeFrequentCount(p.getId());
            model.addRow(new Object[]{
                p.getId(),
                p.getFirstName(),
                p.getLastName(),
                p.getDocumentType(),
                p.getDocumentNumber(),
                p.getDateOfBirth() != null ? p.getDateOfBirth().toString() : "",
                p.getEmail(),
                p.getPhone(),
                frequentCount
            });
        }
    }

    private void createPassenger() {
        System.out.println("[PassengerPanel] createPassenger() - Abriendo formulario de nuevo pasajero");
        PassengerForm form = new PassengerForm(null);
        if (!form.showDialog(this)) {
            System.out.println("[PassengerPanel] createPassenger() - Formulario cancelado");
            return;
        }

        System.out.println("[PassengerPanel] createPassenger() - Datos del formulario:");
        System.out.println("  - Nombre: " + form.firstName);
        System.out.println("  - Apellido: " + form.lastName);
        System.out.println("  - Tipo Doc: " + form.docType);
        System.out.println("  - N° Doc: " + form.docNumber);
        System.out.println("  - Email: " + form.email);
        System.out.println("  - Teléfono: " + form.phone);

        // Validación de duplicado de documento
        if (passengerService.existeDocumento(form.docNumber, null)) {
            System.out.println("[PassengerPanel] createPassenger() - ERROR: Documento duplicado: " + form.docNumber);
            JOptionPane.showMessageDialog(this, "El documento ya existe. Verifique.", "Documento Duplicado", JOptionPane.WARNING_MESSAGE);
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
        // Nuevo pasajero: contador frecuente inicia en 0 (se calcula a partir de check-ins)
        p.setFrequentCounter(0);

        System.out.println("[PassengerPanel] createPassenger() - Intentando guardar pasajero...");
        boolean ok = passengerService.crear(p);
        if (!ok) {
            System.out.println("[PassengerPanel] createPassenger() - ERROR al insertar pasajero en BD");
            JOptionPane.showMessageDialog(this, "No se pudo crear el pasajero.", "Error al Crear", JOptionPane.ERROR_MESSAGE);
            return;
        }
        System.out.println("[PassengerPanel] createPassenger() - Pasajero creado exitosamente con ID: " + p.getId());
        loadData(txtBuscar.getText().trim());
    }

    private void editPassenger() {
        int row = table.getSelectedRow();
        if (row < 0) { 
            System.out.println("[PassengerPanel] editPassenger() - ERROR: No hay pasajero seleccionado");
            JOptionPane.showMessageDialog(this, "Seleccione un pasajero", "Selección Requerida", JOptionPane.INFORMATION_MESSAGE); 
            return; 
        }

        // Leer datos actuales de la tabla (incluyendo el ID)
        Integer passengerId = (Integer) model.getValueAt(row, 0);
        String firstName = (String) model.getValueAt(row, 1);
        String lastName = (String) model.getValueAt(row, 2);
        String docType = (String) model.getValueAt(row, 3);
        String docNumber = (String) model.getValueAt(row, 4);
        LocalDate dob = strToLocalDate((String) model.getValueAt(row, 5));
        String email = (String) model.getValueAt(row, 6);
        String phone = (String) model.getValueAt(row, 7);
        // NO pasar el valor "Frecuente" desde la tabla, siempre calcularlo desde BD
        int frequentCountFromDb = passengerDao.computeFrequentCount(passengerId);

        System.out.println("[PassengerPanel] editPassenger() - Editando pasajero ID: " + passengerId);
        System.out.println("  - Nombre actual: " + firstName + " " + lastName);

        PassengerForm.Data d = new PassengerForm.Data(
                passengerId, firstName, lastName, docType, docNumber,
                dob, email, phone, frequentCountFromDb
        );

        PassengerForm form = new PassengerForm(d);
        if (!form.showDialog(this)) {
            System.out.println("[PassengerPanel] editPassenger() - Formulario de edición cancelado");
            return;
        }

        System.out.println("[PassengerPanel] editPassenger() - Datos modificados:");
        System.out.println("  - Nombre: " + form.firstName);
        System.out.println("  - Apellido: " + form.lastName);
        System.out.println("  - Tipo Doc: " + form.docType);
        System.out.println("  - N° Doc: " + form.docNumber);
        System.out.println("  - Email: " + form.email);
        System.out.println("  - Teléfono: " + form.phone);

        // Validación de duplicado de documento (excluye el propio ID)
        if (passengerService.existeDocumento(form.docNumber, passengerId)) {
            System.out.println("[PassengerPanel] editPassenger() - ERROR: Documento duplicado: " + form.docNumber);
            JOptionPane.showMessageDialog(this, "El documento ya existe en otro pasajero.", "Documento Duplicado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Passenger p = new Passenger();
        p.setId(passengerId);
        p.setFirstName(form.firstName);
        p.setLastName(form.lastName);
        p.setDocumentType(form.docType);
        p.setDocumentNumber(form.docNumber);
        p.setDateOfBirth(form.dob);
        p.setEmail(form.email);
        p.setPhone(form.phone);
        // El contador frecuente se recalcula SIEMPRE desde la BD después de actualizar
        p.setFrequentCounter(passengerDao.computeFrequentCount(passengerId));

        System.out.println("[PassengerPanel] editPassenger() - Intentando actualizar pasajero ID: " + passengerId);
        boolean ok = passengerService.actualizar(p);
        if (!ok) {
            System.out.println("[PassengerPanel] editPassenger() - ERROR al actualizar pasajero en BD");
            JOptionPane.showMessageDialog(this, "No se pudo actualizar el pasajero.", "Error al Actualizar", JOptionPane.ERROR_MESSAGE);
            return;
        }
        System.out.println("[PassengerPanel] editPassenger() - Pasajero actualizado exitosamente");
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
                JOptionPane.showMessageDialog(this, "Reserva creada con PNR: " + pnr, "Reserva Exitosa", JOptionPane.INFORMATION_MESSAGE)
            );
        dlg.setVisible(true);
    }

    private LocalDate strToLocalDate(String s) {
        try { return (s == null || s.isBlank()) ? null : LocalDate.parse(s); }
        catch (Exception e) { return null; }
    }
}
