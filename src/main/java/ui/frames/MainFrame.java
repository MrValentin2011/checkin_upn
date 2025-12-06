package ui.frames;

import java.awt.*;
import java.net.URL;
import javax.swing.*;
import ui.panels.DashboardPanel;
import ui.panels.LoginPanel;
import config.app.SessionManager;

public class MainFrame extends javax.swing.JFrame {

    private CardLayout cardLayout;
    private JPanel mainPanel;

    public MainFrame() {
        initComponents();
        prepararVentana();
    }

    /**
     * Muestra un panel por nombre
     */
    public void showPanel(String name) {
        cardLayout.show(mainPanel, name);
    }

    /**
     * Crea el dashboard DESPUÉS del login
     */
    public void loadDashboard() {

        // Seguridad: impedir cargar Dashboard sin usuario
        if (SessionManager.getInstance().getCurrentUser() == null) {
            JOptionPane.showMessageDialog(this,
                    "No hay usuario en sesión, regresando al login.",
                    "Sesión inválida",
                    JOptionPane.WARNING_MESSAGE);

            showPanel("login");
            return;
        }

        // Crear nuevo DashboardPanel con el usuario ya cargado
        DashboardPanel dashboard = new DashboardPanel(this);

        // Reemplazar panel si ya existe
        mainPanel.add(dashboard, "dashboard");

        // Mostrarlo
        showPanel("dashboard");
    }

    private void prepararVentana() {
        setTitle("Sistema de Check-In Aeroportuario - AEROCHECK");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        setIconoAplicacion("/img/icono_app.png");

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // ❗ SOLO se carga el login al inicio
        mainPanel.add(new LoginPanel(this), "login");

        add(mainPanel, BorderLayout.CENTER);

        // Vista inicial
        cardLayout.show(mainPanel, "login");
    }

    public void replaceLoginPanel() {
        mainPanel.remove(mainPanel.getComponent(0)); // Elimina panel actual "login"
        mainPanel.add(new LoginPanel(this), "login"); // Agrega uno nuevo limpio
    }

    private void setIconoAplicacion(String path) {
        try {
            URL iconURL = getClass().getResource(path);

            if (iconURL != null) {
                Image icon = new ImageIcon(iconURL).getImage();
                this.setIconImage(icon);
            } else {
                System.err.println("Advertencia: No se encontró icono en: " + path);
            }
        } catch (Exception e) {
            System.err.println("Error cargando ícono: ");
            e.printStackTrace();
        }
    }

    // Código generado por NetBeans
    @SuppressWarnings("unchecked")
    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 400, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 300, Short.MAX_VALUE));

        pack();
    }

    public static void main(String args[]) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
        }

        EventQueue.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}
