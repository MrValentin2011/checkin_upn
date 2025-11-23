package ui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

/**
 * Form genérico para crear/editar pasajeros.
 * Exponer campos como propiedades públicas para usarlos al cerrar el diálogo.
 */
public class PassengerForm extends JDialog {

    // Datos para precargar en modo edición
    public static class Data {
        public Integer id;
        public String firstName, lastName, docType, docNumber, email, phone;
        public LocalDate dob;
        public Integer frequentCounter;

        public Data(Integer id, String fn, String ln, String dt, String dn,
                    LocalDate dob, String em, String ph, Integer fc) {
            this.id = id; this.firstName = fn; this.lastName = ln;
            this.docType = dt; this.docNumber = dn; this.dob = dob;
            this.email = em; this.phone = ph; this.frequentCounter = fc;
        }
    }

    // Campos de UI
    private final JTextField txtFirst = new JTextField(16);
    private final JTextField txtLast  = new JTextField(16);
    private final JComboBox<String> cbType = new JComboBox<>(new String[]{"DNI","Passport"});
    private final JTextField txtDoc   = new JTextField(16);
    private final JTextField txtDob   = new JTextField(10); // yyyy-MM-dd (opcional)
    private final JTextField txtEmail = new JTextField(18);
    private final JTextField txtPhone = new JTextField(14);
    private final JSpinner spFreq     = new JSpinner(new SpinnerNumberModel(0, 0, 1_000_000, 1));

    private boolean accepted = false;

    // Valores resultantes (leídos desde el panel al pulsar Guardar)
    public String firstName, lastName, docType, docNumber, email, phone;
    public LocalDate dob;
    public int frequentCounter = 0;

    public PassengerForm(Data data) {
        super((Frame) null, true);
        setTitle(data == null ? "Nuevo Pasajero" : "Editar Pasajero");
        setSize(420, 400);
        setLocationRelativeTo(null);
        buildUI();
        preload(data);
    }

    private void buildUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,8,6,8);
        c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;

        int r=0;
        c.gridx=0;c.gridy=r; add(new JLabel("Nombre(s):"), c);
        c.gridx=1;          add(txtFirst, c); r++;

        c.gridx=0;c.gridy=r; add(new JLabel("Apellido(s):"), c);
        c.gridx=1;          add(txtLast, c); r++;

        c.gridx=0;c.gridy=r; add(new JLabel("Tipo Doc:"), c);
        c.gridx=1;          add(cbType, c); r++;

        c.gridx=0;c.gridy=r; add(new JLabel("N° Doc:"), c);
        c.gridx=1;          add(txtDoc, c); r++;

        c.gridx=0;c.gridy=r; add(new JLabel("F.Nacimiento (yyyy-MM-dd):"), c);
        c.gridx=1;          add(txtDob, c); r++;

        c.gridx=0;c.gridy=r; add(new JLabel("Email:"), c);
        c.gridx=1;          add(txtEmail, c); r++;

        c.gridx=0;c.gridy=r; add(new JLabel("Teléfono:"), c);
        c.gridx=1;          add(txtPhone, c); r++;

        c.gridx=0;c.gridy=r; add(new JLabel("Frecuente:"), c);
        c.gridx=1;          add(spFreq, c); r++;

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnOk = new JButton("Guardar");
        JButton btnCancel = new JButton("Cancelar");
        btns.add(btnOk); btns.add(btnCancel);
        c.gridx=0;c.gridy=r; c.gridwidth=2; add(btns, c);

        btnOk.addActionListener(e -> onAccept());
        btnCancel.addActionListener(e -> dispose());
    }

    private void preload(Data d) {
        if (d == null) return;
        txtFirst.setText(d.firstName != null ? d.firstName : "");
        txtLast.setText(d.lastName != null ? d.lastName : "");
        cbType.setSelectedItem(d.docType != null ? d.docType : "DNI");
        txtDoc.setText(d.docNumber != null ? d.docNumber : "");
        txtDob.setText(d.dob != null ? d.dob.toString() : "");
        txtEmail.setText(d.email != null ? d.email : "");
        txtPhone.setText(d.phone != null ? d.phone : "");
        spFreq.setValue(d.frequentCounter != null ? d.frequentCounter : 0);
    }

    private void onAccept() {
        firstName = txtFirst.getText().trim();
        lastName  = txtLast.getText().trim();
        docType   = (String) cbType.getSelectedItem();
        docNumber = txtDoc.getText().trim();
        email     = txtEmail.getText().trim();
        phone     = txtPhone.getText().trim();
        frequentCounter = (Integer) spFreq.getValue();

        if (firstName.isEmpty() || lastName.isEmpty() || docNumber.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nombre, Apellido y N° Doc son obligatorios");
            return;
        }
        String dobStr = txtDob.getText().trim();
        if (!dobStr.isEmpty()) {
            try { dob = LocalDate.parse(dobStr); }
            catch (Exception ex) { JOptionPane.showMessageDialog(this, "Fecha inválida (use yyyy-MM-dd)"); return; }
        } else {
            dob = null;
        }
        accepted = true;
        dispose();
    }

    /** Muestra el diálogo y devuelve true si se pulsó Guardar. */
    public boolean showDialog(Component parent) {
        setLocationRelativeTo(parent);
        setVisible(true);
        return accepted;
    }
}
