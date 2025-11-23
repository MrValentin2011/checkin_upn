package ui.dialogs;

import config.db.DBConnection;
import model.Reservation;
import service.impl.ReservationService;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReservationCreateDialog extends JDialog {

    private final JTextField txtPNR = new JTextField(18);
    private final JComboBox<String> cbPassenger = new JComboBox<>();
    private final JComboBox<String> cbFlight = new JComboBox<>();
    private final JComboBox<String> cbSeatPref = new JComboBox<>(new String[]{"-", "Window", "Aisle", "Middle"});

    private final Map<String, Integer> passengerMap = new LinkedHashMap<>();
    private final Map<String, Integer> flightMap = new LinkedHashMap<>();

    private final ReservationService reservationService = new ReservationService();
    private final Integer preselectedPassengerId;

    public interface OnCreated {
        void done(String pnr);
    }
    private final OnCreated callback;

    /**
     * @param parent frame padre
     * @param passengerId pasajero preseleccionado (puede ser null)
     * @param callback callback opcional al crear la reserva
     */
    public ReservationCreateDialog(Frame parent, Integer passengerId, OnCreated callback) {
        super(parent, true);
        this.callback = callback;
        this.preselectedPassengerId = passengerId;

        setTitle("Nueva Reserva");
        setSize(500, 260);
        setLocationRelativeTo(parent);
        setLayout(new GridBagLayout());

        buildForm();
        loadPassengers();
        loadFlights();

        if (preselectedPassengerId != null) {
            // Preselecciona el pasajero si viene del PassengerPanel
            for (var entry : passengerMap.entrySet()) {
                if (entry.getValue().equals(preselectedPassengerId)) {
                    cbPassenger.setSelectedItem(entry.getKey());
                    break;
                }
            }
        }

        // Genera un PNR automático simple
        txtPNR.setText("PNR" + System.currentTimeMillis());
    }

    private void buildForm() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        int r = 0;
        c.gridx = 0; c.gridy = r; add(new JLabel("PNR:"), c);
        c.gridx = 1; add(txtPNR, c); r++;

        c.gridx = 0; c.gridy = r; add(new JLabel("Pasajero:"), c);
        c.gridx = 1; add(cbPassenger, c); r++;

        c.gridx = 0; c.gridy = r; add(new JLabel("Vuelo:"), c);
        c.gridx = 1; add(cbFlight, c); r++;

        c.gridx = 0; c.gridy = r; add(new JLabel("Preferencia Asiento:"), c);
        c.gridx = 1; add(cbSeatPref, c); r++;

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("💾 Guardar");
        JButton btnCancel = new JButton("❌ Cancelar");
        actions.add(btnSave);
        actions.add(btnCancel);

        c.gridx = 0; c.gridy = r; c.gridwidth = 2; add(actions, c);

        // UX improvements: tooltips, mnemonics, default button and ESC to cancel
        txtPNR.setToolTipText("Código PNR de 6 caracteres (ej: ABC123)");
        cbPassenger.setToolTipText("Seleccione el pasajero para la reserva");
        cbFlight.setToolTipText("Seleccione el vuelo disponible (no muestra vuelos Closed/Cancelled)");

        btnSave.setMnemonic('G');
        btnCancel.setMnemonic('C');

        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> saveReservation());

        // Make Enter trigger Save and ESC close the dialog
        getRootPane().setDefaultButton(btnSave);
        getRootPane().registerKeyboardAction(e -> dispose(),
            KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void saveReservation() {
        try {
            String pnr = txtPNR.getText().trim();
            if (pnr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Ingrese PNR");
                return;
            }
            if (cbPassenger.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Seleccione pasajero");
                return;
            }
            if (cbFlight.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Seleccione vuelo");
                return;
            }

            int passengerId = passengerMap.get(cbPassenger.getSelectedItem().toString());
            int flightId = flightMap.get(cbFlight.getSelectedItem().toString());
            String seatPref = cbSeatPref.getSelectedItem().toString();
            if ("-".equals(seatPref)) seatPref = null;

            Reservation r = new Reservation();
            r.setPnr(pnr);
            r.setPassengerId(passengerId);
            r.setFlightId(flightId);
            r.setStatus("Booked");
            r.setSeatPreference(seatPref);
            r.setCreatedAt(LocalDateTime.now());

            boolean ok = reservationService.crearReserva(r);
            if (!ok) {
                JOptionPane.showMessageDialog(this, "Error al guardar la reserva ❌");
                return;
            }

            JOptionPane.showMessageDialog(this, "Reserva creada correctamente ✅\nPNR: " + pnr);
            if (callback != null) callback.done(pnr);
            dispose();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void loadPassengers() {
        cbPassenger.removeAllItems();
        passengerMap.clear();
        String sql = """
            SELECT passenger_id, first_name, last_name, doc_type, doc_number
            FROM Passengers
            ORDER BY last_name, first_name
        """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("passenger_id");
                String label = String.format("%s %s (%s %s)",
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("doc_type"),
                        rs.getString("doc_number"));
                cbPassenger.addItem(label);
                passengerMap.put(label, id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error cargando pasajeros: " + e.getMessage());
        }
    }

    private void loadFlights() {
        cbFlight.removeAllItems();
        flightMap.clear();
        String sql = """
            SELECT f.flight_id, f.flight_number, a.name AS airline,
                   o.city AS origin_city, d.city AS dest_city,
                   f.departure_time, f.status
            FROM Flights f
            JOIN Airlines a ON a.airline_id = f.airline_id
            JOIN Destinations o ON o.destination_id = f.origin_id
            JOIN Destinations d ON d.destination_id = f.destination_id
            WHERE f.status NOT IN ('Closed','Canceled')
            ORDER BY f.departure_time DESC
        """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("flight_id");
                Timestamp dep = rs.getTimestamp("departure_time");
                String label = String.format("%s | %s %s→%s | %s",
                        rs.getString("flight_number"),
                        rs.getString("airline"),
                        rs.getString("origin_city"),
                        rs.getString("dest_city"),
                        dep != null ? dep.toLocalDateTime().toString().replace('T', ' ') : "-");
                cbFlight.addItem(label);
                flightMap.put(label, id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error cargando vuelos: " + e.getMessage());
        }
    }
}
