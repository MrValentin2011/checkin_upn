// ui.panels.DashboardPanel
package ui.panels;

import java.awt.*;
import javax.swing.*;
import ui.frames.MainFrame;
import config.app.SessionManager;
import model.User;

public class DashboardPanel extends JPanel {

    private final MainFrame mainFrame;
    private final JPanel contentPanel;

    public DashboardPanel(MainFrame frame) {
        this.mainFrame = frame;
        this.contentPanel = new JPanel(new BorderLayout());
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JPanel sidePanel = new JPanel(new GridLayout(0, 1, 10, 10)); // Auto filas
        sidePanel.setBackground(new Color(33, 45, 62));
        sidePanel.setPreferredSize(new Dimension(200, getHeight()));

        // Crear botones (TODOS)
        JButton btnCheckIn = createMenuButton("✈️ Check-In");
        JButton btnFlights = createMenuButton("🛫 Vuelos");
        JButton btnPassengers = createMenuButton("🧑‍✈️ Pasajeros");
        //JButton btnNotifications = createMenuButton("🔔 Notificaciones");
        //JButton btnStatistics = createMenuButton("📊 Estadísticas");
        JButton btnReports = createMenuButton("📋 Reportes");
        JButton btnHelp = createMenuButton("❓ Ayuda");
        JButton btnConfig = createMenuButton("⚙️ Configuración");
        JButton btnLogout = createMenuButton("🚪 Cerrar sesión");

        // --- CONTROL DE ACCESO ---
        User currentUser = SessionManager.getInstance().getCurrentUser();
        int roleId = currentUser.getId_role();

        switch (roleId) {

            case 1: // AGENTE
                sidePanel.add(btnCheckIn);
                sidePanel.add(btnHelp);
                sidePanel.add(btnLogout);
                break;

            case 2: // SUPERVISOR
                sidePanel.add(btnCheckIn);
                //sidePanel.add(btnStatistics);
                sidePanel.add(btnReports);
                sidePanel.add(btnHelp);
                sidePanel.add(btnLogout);
                break;

            case 3: // ADMINISTRADOR
                sidePanel.add(btnCheckIn);
                sidePanel.add(btnFlights);
                sidePanel.add(btnPassengers);
                //sidePanel.add(btnNotifications);
                //sidePanel.add(btnStatistics);
                sidePanel.add(btnReports);
                sidePanel.add(btnConfig);
                sidePanel.add(btnHelp);
                sidePanel.add(btnLogout);
                break;
        }

        // --- PANEL CENTRAL ---
        JLabel lblWelcome = new JLabel("Bienvenido al sistema de AEROCHECK", SwingConstants.CENTER);
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 18));
        contentPanel.add(lblWelcome, BorderLayout.CENTER);

        // --- LISTENERS ---
        btnCheckIn.addActionListener(e -> showPanel(new CheckInPanel()));
        btnFlights.addActionListener(e -> showPanel(new FlightPanel()));
        btnPassengers.addActionListener(e -> showPanel(new PassengerPanel()));
        //btnNotifications.addActionListener(
        //        e -> showPanel(new NotificationPanel(SessionManager.getInstance().getCurrentUserId())));
        //btnStatistics.addActionListener(e -> showPanel(new StatisticsPanel()));
        btnReports.addActionListener(e -> showPanel(new ReportPanel()));
        btnHelp.addActionListener(e -> showPanel(new HelpPanel()));
        btnConfig.addActionListener(e -> showPanel(new ConfigPanel()));

        btnLogout.addActionListener(e -> {
            int opt = JOptionPane.showConfirmDialog(this, "¿Desea cerrar sesión?", "Confirmar",
                    JOptionPane.YES_NO_OPTION);

            if (opt == JOptionPane.YES_OPTION) {
                SessionManager.getInstance().clearSession();

                // ❗ CREAR UN NUEVO LOGIN PANEL VACÍO
                mainFrame.replaceLoginPanel();

                mainFrame.showPanel("login");
            }
        });

        add(sidePanel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);
    }

    private JButton createMenuButton(String text) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setBackground(new Color(52, 73, 94));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMargin(new Insets(5, 20, 5, 5));
        return btn;
    }

    private void showPanel(JPanel panel) {
        contentPanel.removeAll();
        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

}
