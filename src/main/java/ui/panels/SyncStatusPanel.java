/*
 * Panel para mostrar estado de sincronización y operaciones pendientes
 */
package ui.panels;

import offline.sync.SyncManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Panel para monitorear sincronización offline/online.
 * Muestra:
 * - Estado de conexión (online/offline)
 * - Progreso de sincronización
 * - Operaciones pendientes
 * - Botón para sincronización manual
 */
public class SyncStatusPanel extends JPanel implements SyncManager.SyncListener {
    private static final Logger logger = LoggerFactory.getLogger(SyncStatusPanel.class);
    
    private final SyncManager syncManager;
    private JLabel connectionStatusLabel;
    private JLabel pendingCountLabel;
    private JProgressBar syncProgressBar;
    private JLabel syncMessageLabel;
    private JButton syncNowButton;
    private JLabel lastSyncLabel;

    public SyncStatusPanel() {
        this.syncManager = SyncManager.getInstance();
        initUI();
        syncManager.addSyncListener(this);
        updateUI();
    }

    private void initUI() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(new Color(240, 240, 240));

        // Panel superior: Estado de conexión
        JPanel statusPanel = createStatusPanel();
        add(statusPanel);
        add(Box.createVerticalStrut(10));

        // Panel de progreso
        JPanel progressPanel = createProgressPanel();
        add(progressPanel);
        add(Box.createVerticalStrut(10));

        // Panel de acciones
        JPanel actionPanel = createActionPanel();
        add(actionPanel);
        add(Box.createVerticalGlue());
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder("Estado de Conexión"));

        // Conexión
        connectionStatusLabel = new JLabel();
        connectionStatusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        updateConnectionStatus();

        JPanel connectionContainer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        connectionContainer.setOpaque(false);
        connectionContainer.add(connectionStatusLabel);

        // Operaciones pendientes
        pendingCountLabel = new JLabel();
        pendingCountLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        JPanel pendingContainer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pendingContainer.setOpaque(false);
        pendingContainer.add(pendingCountLabel);

        panel.add(connectionContainer, BorderLayout.NORTH);
        panel.add(pendingContainer, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createProgressPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder("Sincronización"));

        // Progress bar
        syncProgressBar = new JProgressBar(0, 100);
        syncProgressBar.setValue(0);
        syncProgressBar.setStringPainted(true);
        syncProgressBar.setPreferredSize(new Dimension(300, 25));
        syncProgressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

        panel.add(syncProgressBar);
        panel.add(Box.createVerticalStrut(5));

        // Mensaje
        syncMessageLabel = new JLabel("Estado: Listo");
        syncMessageLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        syncMessageLabel.setForeground(new Color(100, 100, 100));

        panel.add(syncMessageLabel);

        return panel;
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setOpaque(false);

        // Botón sincronizar ahora
        syncNowButton = new JButton("Sincronizar Ahora");
        syncNowButton.setFont(new Font("Arial", Font.PLAIN, 11));
        syncNowButton.addActionListener(e -> performSync());

        // Último sincronizado
        lastSyncLabel = new JLabel("Última sincronización: Nunca");
        lastSyncLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        lastSyncLabel.setForeground(new Color(150, 150, 150));

        panel.add(syncNowButton);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(lastSyncLabel);

        return panel;
    }

    private void performSync() {
        syncNowButton.setEnabled(false);
        syncMessageLabel.setText("Sincronizando...");
        
        new Thread(() -> {
            try {
                SyncManager.SyncResult result = syncManager.synchronizeSync();
                SwingUtilities.invokeLater(() -> {
                    String message = result.success
                        ? "✅ Sincronización completada: " + result.message
                        : "⚠️ Sincronización parcial: " + result.message;
                    syncMessageLabel.setText(message);
                    lastSyncLabel.setText("Última sincronización: ahora");
                    refreshUIState();
                });
            } catch (Exception e) {
                logger.error("Error durante sincronización manual", e);
                SwingUtilities.invokeLater(() -> {
                    syncMessageLabel.setText("❌ Error: " + e.getMessage());
                });
            } finally {
                SwingUtilities.invokeLater(() -> syncNowButton.setEnabled(true));
            }
        }).start();
    }

    private void refreshUIState() {
        updateConnectionStatus();
        updatePendingCount();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        refreshUIState();
    }

    private void updateConnectionStatus() {
        SyncManager.SyncStatus status = syncManager.getStatus();
        
        if (status.isOnline) {
            connectionStatusLabel.setText("🟢 EN LÍNEA");
            connectionStatusLabel.setForeground(new Color(34, 177, 76)); // Verde
            syncNowButton.setEnabled(true);
        } else {
            connectionStatusLabel.setText("🔴 SIN CONEXIÓN");
            connectionStatusLabel.setForeground(new Color(221, 75, 57)); // Rojo
            syncNowButton.setEnabled(false);
        }
    }

    private void updatePendingCount() {
        SyncManager.SyncStatus status = syncManager.getStatus();
        int pending = status.pendingOperations;
        
        if (pending > 0) {
            pendingCountLabel.setText(String.format("⏳ %d operación(es) pendiente(s)", pending));
            pendingCountLabel.setForeground(new Color(255, 152, 0)); // Naranja
        } else {
            pendingCountLabel.setText("✅ 0 operaciones pendientes");
            pendingCountLabel.setForeground(new Color(34, 177, 76)); // Verde
        }
    }

    @Override
    public void onConnectionStatusChanged(boolean isOnline) {
        SwingUtilities.invokeLater(() -> {
            updateConnectionStatus();
            String message = isOnline ? "Conectado al servidor" : "Desconectado del servidor (modo offline)";
            syncMessageLabel.setText(message);
        });
    }

    @Override
    public void onSyncProgress(int progress, String message) {
        SwingUtilities.invokeLater(() -> {
            syncProgressBar.setValue(progress);
            syncMessageLabel.setText(message);
        });
    }

    @Override
    public void onOperationQueued(String operationType) {
        SwingUtilities.invokeLater(this::updatePendingCount);
    }

    /**
     * Limpia recursos
     */
    public void cleanup() {
        syncManager.removeSyncListener(this);
    }
}
