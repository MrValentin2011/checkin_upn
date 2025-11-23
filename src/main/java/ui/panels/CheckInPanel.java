package ui.panels;

import model.CheckInResult;
import model.Reservation;
import model.Seat;
import service.impl.CheckInService;
import util.exception.CheckInException;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.ArrayList;
import model.Baggage;

/**
 * Panel completo del proceso de Check-In.
 */
public class CheckInPanel extends JPanel {

    private final CheckInService checkInService = new CheckInService();

    private final JTextField txtPnr = new JTextField(12);
    private final JTextField txtDoc = new JTextField(12);
    private final JTextArea txtInfo = new JTextArea(8, 40);

    private Reservation currentReservation;

    // Botones de acción (instancia)
    private final JButton btnValidateDoc;
    private final JButton btnAutoSeat;
    private final JButton btnManualSeat;
    private final JButton btnComplete;

    public CheckInPanel() {

        setLayout(new BorderLayout());

        // -------------------------------------------
        // PANEL SUPERIOR DE BÚSQUEDA
        // -------------------------------------------
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("PNR:"));
        top.add(txtPnr);

        JButton btnSearchPnr = new JButton("Buscar PNR");
        btnSearchPnr.setToolTipText("Buscar reserva usando el código PNR");
        top.add(btnSearchPnr);

        top.add(new JLabel(" Documento:"));
        top.add(txtDoc);

        JButton btnSearchDoc = new JButton("Buscar Documento");
        btnSearchDoc.setToolTipText("Buscar reserva usando un número de documento");
        top.add(btnSearchDoc);

        add(top, BorderLayout.NORTH);

        // -------------------------------------------
        // VISOR DE INFORMACIÓN
        // -------------------------------------------
        txtInfo.setEditable(false);
        txtInfo.setFont(new Font("Consolas", Font.PLAIN, 13));
        add(new JScrollPane(txtInfo), BorderLayout.CENTER);

        // -------------------------------------------
        // PANEL DE ACCIONES DE CHECK-IN
        // -------------------------------------------
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

        btnValidateDoc = new JButton("Validar Documento");
        btnValidateDoc.setToolTipText("Comprueba si el documento del pasajero coincide con la reserva");

        btnAutoSeat = new JButton("Asignación Automática");
        btnAutoSeat.setToolTipText("El sistema asignará automáticamente un asiento disponible");

        btnManualSeat = new JButton("Asignación Manual");
        btnManualSeat.setToolTipText("Permite elegir manualmente el asiento del pasajero");

        btnComplete = new JButton("Completar Check-In");
        btnComplete.setToolTipText("Finaliza el check-in, genera el boarding pass y el PDF");

        // Solo mostramos el botón para completar el check-in; las demás funciones
        // se integran en el flujo al presionar este botón.
        actions.add(btnComplete);

        add(actions, BorderLayout.SOUTH);

        // -------------------------------------------
        // EVENTOS
        // -------------------------------------------
        btnSearchPnr.addActionListener(e -> buscarPorPNR());
        btnSearchDoc.addActionListener(e -> buscarPorDoc());

        btnComplete.addActionListener(e -> completarCheckIn());

        // deshabilitar acciones inicialmente
        setAccionesCheckInHabilitadas(false);
    }

    // =====================================================
    //  MÉTODOS PRINCIPALES
    // =====================================================

    private void buscarPorPNR() {
        String pnr = txtPnr.getText().trim();

        if (pnr.isBlank()) {
            JOptionPane.showMessageDialog(this, "Ingrese PNR");
            return;
        }

        try {
            currentReservation = checkInService.buscarReservaPorPNR(pnr);
            mostrarReserva();
        } catch (CheckInException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            currentReservation = null;
            mostrarReserva();
        }
    }

    private void buscarPorDoc() {
        String doc = txtDoc.getText().trim();

        if (doc.isBlank()) {
            JOptionPane.showMessageDialog(this, "Ingrese documento");
            return;
        }

        try {
            currentReservation = checkInService.buscarReservaPorDoc(doc);
            mostrarReserva();
        } catch (CheckInException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            currentReservation = null;
            mostrarReserva();
        }
    }

    private void mostrarReserva() {

        if (currentReservation == null) {
            txtInfo.setText("Reserva no encontrada.");
            setAccionesCheckInHabilitadas(false);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("PNR: ").append(currentReservation.getPnr()).append("\n");
        sb.append("Vuelo ID: ").append(currentReservation.getFlightId()).append("\n");
        sb.append("Estado reserva: ").append(currentReservation.getStatus()).append("\n");

        if (currentReservation.getPassenger() != null) {
            sb.append("Pasajero: ").append(currentReservation.getPassenger().getFirstName()).append("\n");
            sb.append("Documento: ").append(
                    currentReservation.getPassenger().getDocumentType() + " " +
                    currentReservation.getPassenger().getDocumentNumber()
            ).append("\n");

            sb.append("Email: ").append(currentReservation.getPassenger().getEmail()).append("\n");
        }

        boolean yaCheckeado = checkInService.yaCheckeado(currentReservation.getId());
        sb.append("Ya check-in: ").append(yaCheckeado ? "SI" : "NO").append("\n");

        txtInfo.setText(sb.toString());

        setAccionesCheckInHabilitadas(!yaCheckeado);
    }

    private void setAccionesCheckInHabilitadas(boolean enabled) {
        btnValidateDoc.setEnabled(enabled);
        btnAutoSeat.setEnabled(enabled);
        btnManualSeat.setEnabled(enabled);
        btnComplete.setEnabled(enabled);
    }
    /*
    // =====================================================
    // VALIDAR DOCUMENTO
    // =====================================================
    private void validarDoc() {
        if (currentReservation == null) {
            JOptionPane.showMessageDialog(this, "Debe buscar una reserva.");
            return;
        }

        boolean ok = checkInService.validarDocumento(currentReservation);

        JOptionPane.showMessageDialog(
                this,
                ok ? "Documento válido ✔" : "El documento no coincide con la reserva ❌"
        );
    }

    // =====================================================
    // ASIENTO AUTOMÁTICO
    // =====================================================
    private void asignarAutomatico() {
        if (currentReservation == null) return;

        try {
            Integer seatId = checkInService.asignarAsientoAutomatico(
                    currentReservation.getFlightId(),
                    currentReservation.getId()
            );

            if (seatId == null) {
                JOptionPane.showMessageDialog(this, "No hay asientos disponibles.");
                return;
            }

            String code = fetchSeatCode(seatId);

            JOptionPane.showMessageDialog(this,
                    "Asiento asignado automáticamente: " + code);

            mostrarReserva();
        } catch (CheckInException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =====================================================
    // ASIENTO MANUAL
    // =====================================================
    private void asignarManual() {

        if (currentReservation == null) return;

        List<Seat> seats = checkInService.listarAsientosDisponibles(currentReservation.getFlightId());
        if (seats.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay asientos disponibles");
            return;
        }

        Object[] opciones = seats.stream().map(Seat::getSeatCode).toArray();
        String seleccion = (String) JOptionPane.showInputDialog(
                this,
                "Seleccione un asiento disponible:",
                "Asignación Manual",
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[0]
        );

        if (seleccion == null) return;

        Integer seatId = seats.stream()
                .filter(s -> seleccion.equals(s.getSeatCode()))
                .map(Seat::getId)
                .findFirst()
                .orElse(null);

        if (seatId == null) {
            JOptionPane.showMessageDialog(this, "Error: asiento no válido.");
            return;
        }

        try {
            boolean ok = checkInService.asignarAsientoManual(seatId, currentReservation.getId());

            JOptionPane.showMessageDialog(
                    this,
                    ok ? "Asiento asignado: " + seleccion : "No se pudo asignar (ocupado por otro agente)."
            );

            mostrarReserva();
        } catch (CheckInException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }*/

    // =====================================================
    // COMPLETAR CHECK-IN
    // =====================================================
    private void completarCheckIn() {

        if (currentReservation == null) return;
        String agentStr = JOptionPane.showInputDialog(
                this,
                "Ingrese su user_id (agente):"
        );

        if (agentStr == null || agentStr.isBlank()) return;

        int agentId;
        try {
            agentId = Integer.parseInt(agentStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "ID inválido");
            return;
        }

        try {
            // 1) Pedir número de documento y validar que coincida con la reserva
            if (currentReservation.getPassenger() == null) {
                JOptionPane.showMessageDialog(this, "La reserva no tiene pasajero asociado.");
                return;
            }

            String inputDoc = JOptionPane.showInputDialog(this, "Ingrese número de documento del pasajero para validación:");
            if (inputDoc == null) return; // cancel
            inputDoc = inputDoc.trim();
            String expected = currentReservation.getPassenger().getDocumentNumber();
            if (expected == null || expected.isBlank() || !expected.equalsIgnoreCase(inputDoc)) {
                JOptionPane.showMessageDialog(this, "Documento no coincide. Se cancela el proceso.", "Validación", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 2) Selección manual de asiento mediante diálogo visual
            java.util.concurrent.atomic.AtomicInteger selectedSeat = new java.util.concurrent.atomic.AtomicInteger(-1);
            // Vamos a mostrar un mapa visual completo: filas x columnas.
            java.util.List<Seat> seats = checkInService.listarTodosAsientosPorVuelo(currentReservation.getFlightId());
            if (seats.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No hay asientos definidos para este vuelo.");
                return;
            }

            // Determinar filas y letras (columnas)
            java.util.TreeMap<Integer, java.util.Map<Character, Seat>> grid = new java.util.TreeMap<>();
            java.util.LinkedHashSet<Character> lettersOrdered = new java.util.LinkedHashSet<>();
            int maxRow = 0;
            for (Seat s : seats) {
                String code = s.getSeatCode();
                if (code == null) continue;
                int idx = 0;
                while (idx < code.length() && Character.isDigit(code.charAt(idx))) idx++;
                if (idx == 0 || idx >= code.length()) continue;
                String rowStr = code.substring(0, idx);
                String letters = code.substring(idx);
                int rowNum = 1;
                try { rowNum = Integer.parseInt(rowStr); } catch (NumberFormatException ex) { continue; }
                char letter = letters.charAt(0);
                lettersOrdered.add(letter);
                grid.computeIfAbsent(rowNum, k -> new java.util.HashMap<>()).put(letter, s);
                if (rowNum > maxRow) maxRow = rowNum;
            }

            int cols = Math.max(1, lettersOrdered.size());
            int rows = Math.max(1, maxRow);

            // Dialogo
            JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this), "Seleccionar Asiento", Dialog.ModalityType.APPLICATION_MODAL);
            JPanel panel = new JPanel(new BorderLayout());

            // Header with letters
            JPanel header = new JPanel(new GridLayout(1, cols, 6, 6));
            for (char letter : lettersOrdered) {
                JLabel lbl = new JLabel(String.valueOf(letter), SwingConstants.CENTER);
                lbl.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                header.add(lbl);
            }

            JPanel gridPanel = new JPanel(new GridLayout(rows, cols, 6, 6));

            // Column with row numbers (left side)
            JPanel numbersPanel = new JPanel(new GridLayout(rows, 1, 6, 6));
            for (int r = 1; r <= rows; r++) {
                JLabel lblRow = new JLabel(String.valueOf(r), SwingConstants.CENTER);
                lblRow.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                numbersPanel.add(lblRow);
            }

            // Fill rows (1..rows) building the grid and colors/tooltips
            for (int r = 1; r <= rows; r++) {
                java.util.Map<Character, Seat> rowMap = grid.getOrDefault(r, java.util.Map.of());
                for (char letter : lettersOrdered) {
                    Seat s = rowMap.get(letter);
                    if (s != null) {
                        JButton btn = new JButton(s.getSeatCode());
                        btn.setMargin(new Insets(4,4,4,4));
                        // color by class
                        String cls = s.getSeatClass() == null ? "" : s.getSeatClass().toLowerCase();
                        if (s.isOccupied()) {
                            btn.setBackground(Color.DARK_GRAY);
                            btn.setForeground(Color.WHITE);
                            btn.setEnabled(false);
                            btn.setToolTipText("Ocupado (reservation=" + s.getReservationId() + ")");
                        } else {
                            if (cls.contains("business")) {
                                btn.setBackground(new Color(255, 200, 100));
                            } else if (cls.contains("first")) {
                                btn.setBackground(new Color(180, 220, 255));
                            } else {
                                btn.setBackground(new Color(200, 255, 200));
                            }
                            btn.setOpaque(true);
                            btn.setToolTipText("Clase: " + s.getSeatClass());
                            btn.addActionListener(ae -> {
                                try {
                                    boolean ok = checkInService.asignarAsientoManual(s.getId(), currentReservation.getId());
                                    if (ok) {
                                        selectedSeat.set(s.getId());
                                        dlg.dispose();
                                    } else {
                                        JOptionPane.showMessageDialog(dlg, "No se pudo asignar el asiento (ocupado).", "Asignación", JOptionPane.WARNING_MESSAGE);
                                        btn.setEnabled(false);
                                    }
                                } catch (CheckInException ex) {
                                    JOptionPane.showMessageDialog(dlg, "Error al asignar asiento: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                                }
                            });
                        }
                        gridPanel.add(btn);
                    } else {
                        JPanel empty = new JPanel();
                        empty.setBackground(Color.WHITE);
                        gridPanel.add(empty);
                    }
                }
            }

            // Center wrapper: numbers at left, grid in center
            JPanel centerWrapper = new JPanel(new BorderLayout());
            centerWrapper.add(numbersPanel, BorderLayout.WEST);
            centerWrapper.add(gridPanel, BorderLayout.CENTER);

            panel.add(new JLabel("Seleccione un asiento (click)", SwingConstants.CENTER), BorderLayout.NORTH);
            panel.add(header, BorderLayout.NORTH);
            panel.add(centerWrapper, BorderLayout.CENTER);

            // Legend panel
            JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
            legend.add(createLegendItem(new Color(200,255,200), "Economy"));
            legend.add(createLegendItem(new Color(255,200,100), "Business"));
            legend.add(createLegendItem(new Color(180,220,255), "First"));
            legend.add(createLegendItem(Color.DARK_GRAY, "Ocupado"));
            panel.add(legend, BorderLayout.SOUTH);
            dlg.getContentPane().add(panel);
            dlg.setSize(600, 400);
            dlg.setLocationRelativeTo(this);
            dlg.setVisible(true);

            // Si el dialog fue cerrado sin seleccionar (selectedSeatId null), cancelar
            if (selectedSeat.get() == -1) {
                JOptionPane.showMessageDialog(this, "No se seleccionó ningún asiento. Proceso cancelado.");
                return;
            }

            // 3) Registrar equipaje (si aplica)
            List<Baggage> bagList = new ArrayList<>();
            int registerBaggage = JOptionPane.showConfirmDialog(this, "¿Registrar equipaje para este pasajero?", "Equipaje", JOptionPane.YES_NO_OPTION);
            if (registerBaggage == JOptionPane.YES_OPTION) {
                String cntStr = JOptionPane.showInputDialog(this, "¿Cuántas piezas de equipaje desea registrar? (ingrese número)", "1");
                if (cntStr != null && !cntStr.isBlank()) {
                    int cnt = 1;
                    try { cnt = Integer.parseInt(cntStr); } catch (NumberFormatException ex) { cnt = 1; }
                    for (int i = 0; i < Math.max(1, cnt); i++) {
                        String weightStr = JOptionPane.showInputDialog(this, "Peso (kg) del equipaje #" + (i+1) + ":", "0.0");
                        String piecesStr = JOptionPane.showInputDialog(this, "Número de piezas (si aplica) del equipaje #" + (i+1) + ":", "1");
                        String type = JOptionPane.showInputDialog(this, "Tipo (ej. checked/cabin) del equipaje #" + (i+1) + ":", "checked");
                        double weight = 0.0; int pieces = 1;
                        try { weight = Double.parseDouble(weightStr); } catch (Exception ex) { weight = 0.0; }
                        try { pieces = Integer.parseInt(piecesStr); } catch (Exception ex) { pieces = 1; }
                        Baggage b = new Baggage();
                        b.setWeight(weight);
                        b.setPieces(pieces);
                        b.setType(type == null ? "checked" : type);
                        b.setTagCode("TAG-" + System.currentTimeMillis() + "-" + (i+1));
                        bagList.add(b);
                    }
                }
            }

            // 4) Llamar al servicio para completar el check-in
                CheckInResult resultado = checkInService.realizarCheckIn(
                    currentReservation,
                    agentId,
                    selectedSeat.get(),
                    bagList
            );

            if (resultado == null) {
                JOptionPane.showMessageDialog(this, "Error: No se pudo completar el check-in.");
                return;
            }

            // Mostrar mensaje de éxito
            String msg = "Check-in completado exitosamente\n\n" +
                    "Código BP: " + resultado.getBoardingCode() + "\n" +
                    "Asiento: " + resultado.getSeatCode();
            if (resultado.getBaggageCharge() > 0) {
                msg += "\nCargos por equipaje: S/ " + String.format("%.2f", resultado.getBaggageCharge());
            }
            JOptionPane.showMessageDialog(this, msg);

            // Intentar guardar el PDF
            String pdfPath = resultado.getPdfPath();
            if (pdfPath != null && !pdfPath.isEmpty()) {
                File generatedPdf = new File(pdfPath);
                if (generatedPdf.exists()) {
                    // Preguntar al usuario dónde guardar el PDF (Save dialog)
                    JFileChooser chooser = new JFileChooser();
                    chooser.setDialogTitle("Guardar Boarding Pass (PDF)");
                    chooser.setCurrentDirectory(new File(System.getProperty("user.home"), "Downloads"));
                    chooser.setSelectedFile(new File(generatedPdf.getName()));

                    int option = chooser.showSaveDialog(this);
                    if (option == JFileChooser.APPROVE_OPTION) {
                        File dest = chooser.getSelectedFile();
                        try {
                            Files.copy(generatedPdf.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            JOptionPane.showMessageDialog(this, "PDF guardado en:\n" + dest.getAbsolutePath());
                            try { Desktop.getDesktop().open(dest); } catch (Exception openEx) { /* ignore */ }
                        } catch (Exception copyEx) {
                            copyEx.printStackTrace();
                            JOptionPane.showMessageDialog(this, "Error al guardar PDF: " + copyEx.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            try { Desktop.getDesktop().open(generatedPdf); } catch (Exception openEx) { /* ignore */ }
                        }
                    } else {
                        // Si cancela, intentar abrir desde ubicación por defecto
                        try { Desktop.getDesktop().open(generatedPdf); } catch (Exception openEx) { /* ignore */ }
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Advertencia: PDF no encontrado en:\n" + pdfPath, "Aviso", JOptionPane.WARNING_MESSAGE);
                }
            }

            mostrarReserva();
        } catch (CheckInException e) {
            JOptionPane.showMessageDialog(this, "Error en check-in: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error inesperado: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // =====================================================
    // OBTENER CÓDIGO DE ASIENTO
    // =====================================================
    private String fetchSeatCode(int seatId) {
        try (var conn = config.db.DBConnection.getConnection();
             var ps = conn.prepareStatement("SELECT seat_code FROM Seats WHERE seat_id = ?")) {

            ps.setInt(1, seatId);
            var rs = ps.executeQuery();
            if (rs.next()) return rs.getString("seat_code");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Helper para crear item de leyenda (color + texto)
    private JComponent createLegendItem(Color color, String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JPanel box = new JPanel();
        box.setBackground(color);
        box.setPreferredSize(new Dimension(16, 16));
        box.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JLabel lbl = new JLabel(text);
        lbl.setBorder(BorderFactory.createEmptyBorder(0,4,0,8));
        p.add(box);
        p.add(lbl);
        return p;
    }
}
