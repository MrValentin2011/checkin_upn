// ui.panels.DashboardPanel
package ui.panels;

import java.awt.*;
import javax.swing.*;
import ui.frames.MainFrame;

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

        JPanel sidePanel = new JPanel(new GridLayout(7, 1, 10, 10)); // ← 7 filas (sumamos Pasajeros)
        sidePanel.setBackground(new Color(33, 45, 62));
        sidePanel.setPreferredSize(new Dimension(200, getHeight()));

        JButton btnCheckIn = createMenuButton("✈️ Check-In");
        JButton btnFlights = createMenuButton("🛫 Vuelos");
        JButton btnPassengers = createMenuButton("🧑‍✈️ Pasajeros");   // ← NUEVO
        JButton btnReports = createMenuButton("📊 Reportes");
        JButton btnConfig = createMenuButton("⚙️ Configuración");
        JButton btnLogout = createMenuButton("🚪 Cerrar sesión");

        sidePanel.add(btnCheckIn);
        sidePanel.add(btnFlights);
        sidePanel.add(btnPassengers); // ← NUEVO
        sidePanel.add(btnReports);
        sidePanel.add(btnConfig);
        sidePanel.add(btnLogout);

        JLabel lblWelcome = new JLabel("Bienvenido al sistema de AEROCHECK", SwingConstants.CENTER);
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 18));
        contentPanel.add(lblWelcome, BorderLayout.CENTER);

        btnCheckIn.addActionListener(e -> showPanel(new CheckInPanel()));
        btnFlights.addActionListener(e -> showPanel(new FlightPanel()));
        btnPassengers.addActionListener(e -> showPanel(new PassengerPanel())); // ← NUEVO
        // Reemplazamos el acceso directo a Baggage por el panel de reportes
        //btnBaggage.addActionListener(e -> showPanel(new BaggagePanel()));
        btnReports.addActionListener(e -> showPanel(new ReportPanel()));
        btnConfig.addActionListener(e -> showPanel(new ConfigPanel()));
        
        btnLogout.addActionListener(e -> {
            int opt = JOptionPane.showConfirmDialog(this, "¿Desea cerrar sesión?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (opt == JOptionPane.YES_OPTION) {
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
