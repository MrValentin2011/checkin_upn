// ui.dialogs.FlightFormDialog (solo muestro la versión con los nuevos campos y guardar)
package ui.dialogs;

import dao.impl.AirlineDao;
import dao.impl.DestinationDao;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;
import model.Flight;
import service.impl.FlightService;

public class FlightFormDialog extends JDialog {

    private final JTextField txtNumero = new JTextField(10);
    private final JComboBox<String> cbAirline = new JComboBox<>();
    private final JComboBox<String> cbOrigin = new JComboBox<>();
    private final JComboBox<String> cbDestination = new JComboBox<>();
    private final JTextField txtDeparture = new JTextField(16);
    private final JTextField txtArrival = new JTextField(16);
    private final JComboBox<String> cbStatus = new JComboBox<>(new String[]{"Scheduled", "Delayed", "Cancelled", "Closed"});
    private final JSpinner spCapacity = new JSpinner(new SpinnerNumberModel(150, 1, 600, 1));

    // === NUEVOS CAMPOS ===
    private final JSpinner spFirst = new JSpinner(new SpinnerNumberModel(0, 0, 600, 1));
    private final JSpinner spBusiness = new JSpinner(new SpinnerNumberModel(0, 0, 600, 1));
    private final JSpinner spEconomy = new JSpinner(new SpinnerNumberModel(150, 0, 600, 1)); // read-only, se autocalcula
    private final JComboBox<Integer> cbSeatsPerRow = new JComboBox<>(new Integer[]{4, 6, 8});

    private final FlightService service = new FlightService();
    private Map<Integer, String> airlinesMap;
    private Map<Integer, String> destMap;

    private Integer airlineIdSel;
    private Integer originIdSel;
    private Integer destIdSel;

    private Flight flight; // modo edición

    public FlightFormDialog(Frame parent, Flight flight) {
        super(parent, true);
        this.flight = flight;
        setTitle(flight == null ? "Nuevo Vuelo" : "Editar Vuelo");
        setSize(480, 540);
        setLocationRelativeTo(parent);
        setLayout(new GridLayout(14, 2, 5, 5));
        initForm();
    }

    private void initForm() {
        add(new JLabel("Código Vuelo:"));
        add(txtNumero);

        add(new JLabel("Aerolínea:"));
        add(cbAirline);

        add(new JLabel("Origen:"));
        add(cbOrigin);

        add(new JLabel("Destino:"));
        add(cbDestination);

        add(new JLabel("Salida (yyyy-MM-dd HH:mm):"));
        add(txtDeparture);

        add(new JLabel("Llegada (yyyy-MM-dd HH:mm):"));
        add(txtArrival);

        add(new JLabel("Estado:"));
        add(cbStatus);

        add(new JLabel("Capacidad total:"));
        add(spCapacity);

        // === NUEVOS CONTROLES DE DISTRIBUCIÓN ===
        add(new JLabel("Asientos por fila:"));
        add(cbSeatsPerRow);

        add(new JLabel("First:"));
        add(spFirst);

        add(new JLabel("Business:"));
        add(spBusiness);

        add(new JLabel("Economy (auto):"));
        add(spEconomy);
        spEconomy.setEnabled(false); // solo lectura

        JButton btnSave = new JButton("💾 Guardar");
        JButton btnCancel = new JButton("❌ Cancelar");
        add(btnSave);
        add(btnCancel);

        cargarCombos();
        cbSeatsPerRow.setSelectedItem(6); // por defecto 6 (ABCDEF)

        // Si es edición, cargar datos básicos (OJO: no reconfiguramos distribución en edición)
        if (flight != null) {
            txtNumero.setText(flight.getCode());
            cbAirline.setSelectedItem(flight.getAirline());
            cbOrigin.setSelectedItem(flight.getOrigin());
            cbDestination.setSelectedItem(flight.getDestination());
            if (flight.getDepartureTime() != null) {
                txtDeparture.setText(flight.getDepartureTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
            if (flight.getArrivalTime() != null) {
                txtArrival.setText(flight.getArrivalTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
            cbStatus.setSelectedItem(flight.getStatus());
            spCapacity.setValue(flight.getCapacity());

            // En edición real de asientos, lo ideal es bloquear cambio de distribución:
            spFirst.setEnabled(false);
            spBusiness.setEnabled(false);
            cbSeatsPerRow.setEnabled(false);

            spCapacity.setValue(flight.getCapacity());

            // === NUEVO: leer distribución de Seats ===
            dao.impl.SeatDao sdao = new dao.impl.SeatDao();
            java.util.Map<String, Integer> dist = sdao.getClassCountsByFlight(flight.getId());

            int first = dist.getOrDefault("First", 0);
            int business = dist.getOrDefault("Business", 0);
            int economy = dist.getOrDefault("Economy", 0);

            spFirst.setValue(first);
            spBusiness.setValue(business);
            spEconomy.setValue(economy); // es “auto”, queda deshabilitado

            // (opcional) inferir asientos por fila
            Integer spr = sdao.inferSeatsPerRow(flight.getId());
            if (spr != null) {
                cbSeatsPerRow.setSelectedItem(spr);
            }

            // Si no quieres permitir cambiar distribución en edición, puedes mantenerlos deshabilitados.
            spCapacity.setEnabled(false);
            spFirst.setEnabled(false);
            spBusiness.setEnabled(false);
            cbSeatsPerRow.setEnabled(false);
        }

        // Recalcular Economy cuando cambien First/Business/Capacity
        var recalcEconomy = (Runnable) () -> {
            int cap = (Integer) spCapacity.getValue();
            int f = (Integer) spFirst.getValue();
            int b = (Integer) spBusiness.getValue();
            int eco = cap - f - b;
            if (eco < 0) {
                eco = 0;
            }
            spEconomy.setValue(eco);
        };
        spCapacity.addChangeListener(e -> recalcEconomy.run());
        spFirst.addChangeListener(e -> recalcEconomy.run());
        spBusiness.addChangeListener(e -> recalcEconomy.run());
        recalcEconomy.run();

        btnSave.addActionListener(e -> guardar());
        btnCancel.addActionListener(e -> dispose());
    }

    private void cargarCombos() {
        AirlineDao adao = new AirlineDao();
        DestinationDao ddao = new DestinationDao();

        airlinesMap = adao.listAll();
        destMap = ddao.listAll();

        for (String name : airlinesMap.values()) {
            cbAirline.addItem(name);
        }
        for (String name : destMap.values()) {
            cbOrigin.addItem(name);
            cbDestination.addItem(name);
        }
    }

    private void guardar() {
        try {
            Flight f = (flight != null) ? flight : new Flight();
            f.setCode(txtNumero.getText().trim());
            f.setAirline((String) cbAirline.getSelectedItem());
            f.setOrigin((String) cbOrigin.getSelectedItem());
            f.setDestination((String) cbDestination.getSelectedItem());
            f.setDepartureTime(LocalDateTime.parse(txtDeparture.getText().trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            if (!txtArrival.getText().isBlank()) {
                f.setArrivalTime(LocalDateTime.parse(txtArrival.getText().trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
            f.setStatus((String) cbStatus.getSelectedItem());
            f.setCapacity((Integer) spCapacity.getValue());

            // IDs seleccionados
            airlineIdSel = airlinesMap.entrySet().stream()
                    .filter(e -> e.getValue().equals(cbAirline.getSelectedItem()))
                    .map(Map.Entry::getKey).findFirst().orElse(null);
            originIdSel = destMap.entrySet().stream()
                    .filter(e -> e.getValue().equals(cbOrigin.getSelectedItem()))
                    .map(Map.Entry::getKey).findFirst().orElse(null);
            destIdSel = destMap.entrySet().stream()
                    .filter(e -> e.getValue().equals(cbDestination.getSelectedItem()))
                    .map(Map.Entry::getKey).findFirst().orElse(null);

            // Validaciones básicas
            if (f.getCode().isBlank()) {
                JOptionPane.showMessageDialog(this, "Ingrese código de vuelo");
                return;
            }
            if (airlineIdSel == null || originIdSel == null || destIdSel == null) {
                JOptionPane.showMessageDialog(this, "Seleccione aerolínea/origen/destino");
                return;
            }
            if (f.getArrivalTime() != null && !f.getArrivalTime().isAfter(f.getDepartureTime())) {
                JOptionPane.showMessageDialog(this, "La llegada debe ser posterior a la salida");
                return;
            }

            // Distribución por clase (solo se valida/usa cuando creamos un vuelo nuevo)
            boolean ok;
            if (flight == null) {
                int cap = f.getCapacity();
                int first = (Integer) spFirst.getValue();
                int business = (Integer) spBusiness.getValue();
                int economy = (Integer) spEconomy.getValue();
                int sum = first + business + economy;

                if (sum != cap) {
                    JOptionPane.showMessageDialog(this, "La suma First+Business+Economy debe ser igual a la capacidad (" + cap + ")");
                    return;
                }

                LinkedHashMap<String, Integer> dist = new LinkedHashMap<>();
                if (first > 0) {
                    dist.put("First", first);
                }
                if (business > 0) {
                    dist.put("Business", business);
                }
                if (economy > 0) {
                    dist.put("Economy", economy);
                }

                int seatsPerRow = (Integer) cbSeatsPerRow.getSelectedItem();

                // Crear vuelo + asientos en la misma transacción
                ok = service.registrarVueloConAsientos(f, airlineIdSel, originIdSel, destIdSel, dist, seatsPerRow);
            } else {
                // Edición de datos del vuelo (sin tocar asientos ni validar distribución)
                ok = service.actualizarVuelo(f, airlineIdSel, originIdSel, destIdSel);
            }

            if (ok) {
                JOptionPane.showMessageDialog(this, "Vuelo guardado correctamente ✅");
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Error al guardar vuelo ❌");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}
