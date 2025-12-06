/*
 * Panel de interfaz de usuario para notificaciones
 */
package ui.panels;

import model.Notification;
import service.impl.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.Timer;

/**
 * Panel para visualizar y gestionar notificaciones persistentes.
 * Incluye filtrado por categoría, prioridad, búsqueda y acciones.
 */
public class NotificationPanel extends JPanel implements NotificationService.NotificationListener {
    private static final Logger logger = LoggerFactory.getLogger(NotificationPanel.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private final int userId;
    private final NotificationService service;
    private JTable notificationTable;
    private DefaultTableModel tableModel;
    private JComboBox<Notification.Category> categoryFilter;
    private JCheckBox priorityFilter;
    private JCheckBox unreadFilter;
    private JTextField searchField;
    private JLabel unreadCountLabel;
    private Timer refreshTimer;

    // Color constants
    private static final Color COLOR_CRITICAL = new Color(200, 50, 50);
    private static final Color COLOR_HIGH = new Color(255, 140, 0);
    private static final Color COLOR_MEDIUM = new Color(255, 200, 50);
    private static final Color COLOR_LOW = new Color(200, 200, 200);

    public NotificationPanel(int userId) {
        this.userId = userId;
        this.service = NotificationService.getInstance();
        initializeUI();
        setupAutoRefresh();
        service.addListener(this);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Panel de filtros y búsqueda
        add(createFilterPanel(), BorderLayout.NORTH);

        // Tabla de notificaciones
        add(createTablePanel(), BorderLayout.CENTER);

        // Panel de botones
        add(createButtonPanel(), BorderLayout.SOUTH);

        refreshNotifications();
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Categoría
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Categoría:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.2;
        categoryFilter = new JComboBox<>();
        categoryFilter.addItem(null); // Todas
        for (Notification.Category cat : Notification.Category.values()) {
            categoryFilter.addItem(cat);
        }
        categoryFilter.addActionListener(e -> refreshNotifications());
        panel.add(categoryFilter, gbc);

        // Prioridad alta/crítica
        gbc.gridx = 2;
        gbc.weightx = 0;
        priorityFilter = new JCheckBox("Solo críticas/altas");
        priorityFilter.addActionListener(e -> refreshNotifications());
        panel.add(priorityFilter, gbc);

        // Solo no leídas
        gbc.gridx = 3;
        gbc.weightx = 0;
        unreadFilter = new JCheckBox("Solo no leídas");
        unreadFilter.addActionListener(e -> refreshNotifications());
        panel.add(unreadFilter, gbc);

        // Contador de no leídas
        gbc.gridx = 4;
        gbc.weightx = 0;
        unreadCountLabel = new JLabel();
        updateUnreadCount();
        panel.add(unreadCountLabel, gbc);

        // Búsqueda
        gbc.gridx = 5;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Buscar:"), gbc);

        gbc.gridx = 6;
        gbc.weightx = 0.3;
        searchField = new JTextField();
        searchField.addActionListener(e -> refreshNotifications());
        panel.add(searchField, gbc);

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"Fecha", "Categoría", "Prioridad", "Título", "Mensaje"};
        tableModel = new DefaultTableModel(null, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        notificationTable = new JTable(tableModel);
        notificationTable.setRowHeight(25);
        notificationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        notificationTable.setDefaultRenderer(Object.class, new NotificationCellRenderer());

        // Configurar ancho de columnas
        notificationTable.getColumnModel().getColumn(0).setPreferredWidth(130); // Fecha
        notificationTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Categoría
        notificationTable.getColumnModel().getColumn(2).setPreferredWidth(80);  // Prioridad
        notificationTable.getColumnModel().getColumn(3).setPreferredWidth(150); // Título
        notificationTable.getColumnModel().getColumn(4).setPreferredWidth(300); // Mensaje

        JScrollPane scrollPane = new JScrollPane(notificationTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        // Marcar como leída
        JButton readButton = new JButton("Marcar como leída");
        readButton.addActionListener(e -> markSelectedAsRead());
        panel.add(readButton);

        // Marcar todas como leídas
        JButton readAllButton = new JButton("Marcar todas como leídas");
        readAllButton.addActionListener(e -> markAllAsRead());
        panel.add(readAllButton);

        // Archivar
        JButton archiveButton = new JButton("Archivar");
        archiveButton.addActionListener(e -> archiveSelected());
        panel.add(archiveButton);

        // Archivar todas
        JButton archiveAllButton = new JButton("Archivar todas");
        archiveAllButton.addActionListener(e -> archiveAll());
        panel.add(archiveAllButton);

        // Refrescar
        JButton refreshButton = new JButton("Refrescar");
        refreshButton.addActionListener(e -> refreshNotifications());
        panel.add(refreshButton);

        return panel;
    }

    private void refreshNotifications() {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);

            List<Notification> notifications = getAllNotifications();

            for (Notification notif : notifications) {
                Object[] row = {
                    dateFormat.format(java.sql.Timestamp.valueOf(notif.getCreatedAt())),
                    notif.getCategory().name(),
                    notif.getPriority().name(),
                    notif.getTitle(),
                    notif.getMessage(),
                    notif // Almacenar el objeto completo para fácil acceso
                };
                tableModel.addRow(row);
            }

            updateUnreadCount();
        });
    }

    private List<Notification> getAllNotifications() {
        List<Notification> notifications = new ArrayList<>();

        // Aplicar filtro de búsqueda
        String searchText = searchField.getText().trim();
        if (!searchText.isEmpty()) {
            notifications = service.search(userId, searchText);
        } else {
            notifications = service.getAllNotifications(userId);
        }

        // Aplicar filtro de categoría
        Notification.Category selectedCategory = (Notification.Category) categoryFilter.getSelectedItem();
        if (selectedCategory != null) {
            notifications = notifications.stream()
                .filter(n -> n.getCategory() == selectedCategory)
                .toList();
        }

        // Aplicar filtro de prioridad
        if (priorityFilter.isSelected()) {
            notifications = notifications.stream()
                .filter(n -> n.getPriority() == Notification.Priority.CRITICAL ||
                           n.getPriority() == Notification.Priority.HIGH)
                .toList();
        }

        // Aplicar filtro de no leídas
        if (unreadFilter.isSelected()) {
            notifications = notifications.stream()
                .filter(n -> n.getStatus() == Notification.Status.UNREAD)
                .toList();
        }

        // Ordenar por fecha descendente
        notifications.sort((n1, n2) -> n2.getCreatedAt().compareTo(n1.getCreatedAt()));

        return notifications;
    }

    private void markSelectedAsRead() {
        int selectedRow = notificationTable.getSelectedRow();
        if (selectedRow >= 0) {
            // Obtener la notificación del modelo
            List<Notification> notifications = getAllNotifications();
            if (selectedRow < notifications.size()) {
                Notification selected = notifications.get(selectedRow);
                service.markAsRead(selected.getId());
                refreshNotifications();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Selecciona una notificación", "Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void markAllAsRead() {
        if (JOptionPane.showConfirmDialog(this, "¿Marcar todas como leídas?", "Confirmar",
            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            service.markAllAsRead(userId);
            refreshNotifications();
        }
    }

    private void archiveSelected() {
        int selectedRow = notificationTable.getSelectedRow();
        if (selectedRow >= 0) {
            List<Notification> notifications = getAllNotifications();
            if (selectedRow < notifications.size()) {
                Notification selected = notifications.get(selectedRow);
                service.archive(selected.getId());
                refreshNotifications();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Selecciona una notificación", "Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void archiveAll() {
        if (JOptionPane.showConfirmDialog(this, "¿Archivar todas las notificaciones?", "Confirmar",
            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            service.archiveAll(userId);
            refreshNotifications();
        }
    }

    private void updateUnreadCount() {
        int count = service.getUnreadCount(userId);
        unreadCountLabel.setText("No leídas: " + count);
        if (count > 0) {
            unreadCountLabel.setFont(unreadCountLabel.getFont().deriveFont(Font.BOLD));
            unreadCountLabel.setForeground(Color.RED);
        } else {
            unreadCountLabel.setFont(unreadCountLabel.getFont().deriveFont(Font.PLAIN));
            unreadCountLabel.setForeground(Color.BLACK);
        }
    }

    private void setupAutoRefresh() {
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                refreshNotifications();
            }
        }, 10000, 10000); // Refrescar cada 10 segundos
    }

    @Override
    public void onNotificationReceived(Notification notification) {
        if (notification.getUserId() == 0 || notification.getUserId() == userId) {
            refreshNotifications();
        }
    }

    @Override
    public void onNotificationRead(int notificationId) {
        refreshNotifications();
    }

    @Override
    public void onNotificationArchived(int notificationId) {
        refreshNotifications();
    }

    public void dispose() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
        service.removeListener(this);
    }

    /**
     * Custom cell renderer para colorear por prioridad
     */
    private static class NotificationCellRenderer extends JLabel implements TableCellRenderer {
        public NotificationCellRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                      boolean isSelected, boolean hasFocus,
                                                      int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(Color.WHITE);
                setForeground(Color.BLACK);

                // Colorear según prioridad (columna 2 = Prioridad)
                if (column == 2 && value != null) {
                    String priority = value.toString();
                    switch (priority) {
                        case "CRITICAL" -> {
                            setBackground(COLOR_CRITICAL);
                            setForeground(Color.WHITE);
                        }
                        case "HIGH" -> {
                            setBackground(COLOR_HIGH);
                            setForeground(Color.WHITE);
                        }
                        case "MEDIUM" -> setBackground(COLOR_MEDIUM);
                        case "LOW" -> setBackground(COLOR_LOW);
                    }
                }
            }

            setText(value != null ? value.toString() : "");
            return this;
        }
    }
}
