/*
 * Diálogo para cambiar contraseña usando token
 */
package ui.dialogs;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.EmailService;
import util.PasswordResetTokenManager;
import util.PasswordUtil;
import dao.impl.UserDao;
import model.User;
import service.impl.AuditService;

/**
 * Diálogo para cambiar contraseña usando token de recuperación.
 */
public class PasswordResetDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetDialog.class);
    private JTextField txtToken;
    private JPasswordField txtPassword;
    private JPasswordField txtPasswordConfirm;
    private JButton btnReset;
    private JButton btnCancel;
    private boolean success = false;

    public PasswordResetDialog(JFrame parent) {
        super(parent, "Cambiar Contraseña", true);
        initComponents();
        setLocationRelativeTo(parent);
        setSize(550, 450);
        setResizable(false);
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Panel superior
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("🔐 Cambiar Contraseña", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        topPanel.add(titleLabel, BorderLayout.NORTH);

        JLabel descLabel = new JLabel(
                "<html><center>Ingresa el token que recibiste por email<br>" +
                "y define tu nueva contraseña.</center></html>",
                SwingConstants.CENTER);
        descLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        descLabel.setForeground(new Color(100, 100, 100));
        descLabel.setBorder(new EmptyBorder(10, 0, 20, 0));
        topPanel.add(descLabel, BorderLayout.SOUTH);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Panel central
        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Token
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        JLabel tokenLabel = new JLabel("Token:");
        tokenLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        centerPanel.add(tokenLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        txtToken = new JTextField(30);
        txtToken.setFont(new Font("Arial", Font.PLAIN, 12));
        txtToken.setToolTipText("Copia el token que recibiste por email");
        centerPanel.add(txtToken, gbc);

        // Nueva contraseña
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        JLabel passLabel = new JLabel("Nueva Contraseña:");
        passLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        centerPanel.add(passLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        txtPassword = new JPasswordField(30);
        txtPassword.setFont(new Font("Arial", Font.PLAIN, 12));
        txtPassword.setToolTipText("Mínimo 8 caracteres");
        centerPanel.add(txtPassword, gbc);

        // Confirmación de contraseña
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.3;
        JLabel confirmLabel = new JLabel("Confirmar Contraseña:");
        confirmLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        centerPanel.add(confirmLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        txtPasswordConfirm = new JPasswordField(30);
        txtPasswordConfirm.setFont(new Font("Arial", Font.PLAIN, 12));
        txtPasswordConfirm.setToolTipText("Repite tu nueva contraseña");
        centerPanel.add(txtPasswordConfirm, gbc);

        // Requisitos de contraseña
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JLabel reqLabel = new JLabel(
                "<html><font size=\"-1\">📋 Requisitos:<br>" +
                "• Mínimo 8 caracteres<br>" +
                "• Letras mayúsculas y minúsculas<br>" +
                "• Números o símbolos<br>" +
                "• Diferente a la anterior</font></html>");
        reqLabel.setForeground(new Color(100, 100, 100));
        reqLabel.setBorder(new EmptyBorder(15, 0, 0, 0));
        centerPanel.add(reqLabel, gbc);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Panel inferior (botones)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        btnReset = new JButton("✅ Cambiar Contraseña");
        btnReset.setFont(new Font("Arial", Font.PLAIN, 12));
        btnReset.setBackground(new Color(40, 167, 69));
        btnReset.setForeground(Color.WHITE);
        btnReset.setFocusPainted(false);
        btnReset.addActionListener(e -> resetPassword());
        buttonPanel.add(btnReset);

        btnCancel = new JButton("Cancelar");
        btnCancel.setFont(new Font("Arial", Font.PLAIN, 12));
        btnCancel.addActionListener(e -> dispose());
        buttonPanel.add(btnCancel);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void resetPassword() {
        String token = txtToken.getText().trim();
        String password = new String(txtPassword.getPassword());
        String passwordConfirm = new String(txtPasswordConfirm.getPassword());

        // Validaciones
        if (token.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Por favor ingresa el token",
                    "Token requerido",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Por favor ingresa la nueva contraseña",
                    "Contraseña requerida",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!password.equals(passwordConfirm)) {
            JOptionPane.showMessageDialog(this,
                    "Las contraseñas no coinciden",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (password.length() < 8) {
            JOptionPane.showMessageDialog(this,
                    "La contraseña debe tener mínimo 8 caracteres",
                    "Contraseña débil",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Validar token
        PasswordResetTokenManager tokenManager = PasswordResetTokenManager.getInstance();
        if (!tokenManager.isValidToken(token)) {
            JOptionPane.showMessageDialog(this,
                    "❌ Token inválido o expirado.\n\n" +
                    "Por favor solicita un nuevo token.",
                    "Token no válido",
                    JOptionPane.ERROR_MESSAGE);
            logger.warn("Intento de reset con token inválido");
            return;
        }

        int userId = tokenManager.getUserIdFromToken(token);
        String email = tokenManager.getEmailFromToken(token);

        // Obtener usuario
        UserDao userDao = new UserDao();
        User user = userDao.findById(userId);

        if (user == null) {
            JOptionPane.showMessageDialog(this,
                    "Usuario no encontrado",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Hash de la nueva contraseña
        String hashedPassword = PasswordUtil.hashPassword(password);

        // Actualizar contraseña
        boolean updated = userDao.updatePassword(userId, hashedPassword);

        if (updated) {
            // Consumir token
            tokenManager.consumeToken(token);

            // Auditoría
            AuditService auditService = AuditService.getInstance();
            auditService.logPasswordReset(userId, user.getUsername(), true);

            // Enviar email de confirmación
            EmailService emailService = new EmailService();
            emailService.sendPasswordChangedEmail(email, user.getUsername());

            JOptionPane.showMessageDialog(this,
                    "✅ Contraseña actualizada exitosamente!\n\n" +
                    "Ya puedes entrar con tu nueva contraseña.",
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE);

            logger.info("Contraseña reset exitosa para usuario: {}", user.getUsername());
            success = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                    "❌ Error al actualizar la contraseña.\n" +
                    "Por favor intenta nuevamente.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            logger.error("Fallo al actualizar contraseña para usuario: {}", userId);
        }
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isPasswordReset() {
        return success;
    }
}
