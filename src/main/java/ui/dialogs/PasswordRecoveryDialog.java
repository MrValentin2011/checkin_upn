/*
 * Diálogo para solicitar recuperación de contraseña
 */
package ui.dialogs;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.EmailService;
import util.PasswordResetTokenManager;
import dao.impl.UserDao;
import model.User;

/**
 * Diálogo para solicitar recuperación de contraseña.
 * Valida el email y envía un token de recuperación.
 */
public class PasswordRecoveryDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(PasswordRecoveryDialog.class);
    private JTextField txtEmail;
    private JButton btnSendToken;
    private JButton btnCancel;
    private boolean success = false;

    public PasswordRecoveryDialog(JFrame parent) {
        super(parent, "Recuperar Contraseña", true);
        initComponents();
        setLocationRelativeTo(parent);
        setSize(500, 300);
        setResizable(false);
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Panel superior
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("🔐 Recuperar Contraseña", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        topPanel.add(titleLabel, BorderLayout.NORTH);

        JLabel descLabel = new JLabel(
                "<html><center>Ingresa tu email registrado y te enviaremos un token<br>" +
                "para recuperar tu contraseña.</center></html>",
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

        // Etiqueta y campo de email
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.3;
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        centerPanel.add(emailLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        txtEmail = new JTextField(20);
        txtEmail.setFont(new Font("Arial", Font.PLAIN, 12));
        txtEmail.setToolTipText("Ingresa tu email registrado");
        centerPanel.add(txtEmail, gbc);

        // Nota informativa
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        JLabel noteLabel = new JLabel(
                "<html><font size=\"-1\">⚠️ El token será válido por 24 horas</font></html>");
        noteLabel.setForeground(new Color(150, 100, 0));
        centerPanel.add(noteLabel, gbc);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Panel inferior (botones)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        btnSendToken = new JButton("📧 Enviar Token");
        btnSendToken.setFont(new Font("Arial", Font.PLAIN, 12));
        btnSendToken.setBackground(new Color(52, 152, 219));
        btnSendToken.setForeground(Color.WHITE);
        btnSendToken.setFocusPainted(false);
        btnSendToken.addActionListener(e -> sendToken());
        buttonPanel.add(btnSendToken);

        btnCancel = new JButton("Cancelar");
        btnCancel.setFont(new Font("Arial", Font.PLAIN, 12));
        btnCancel.addActionListener(e -> dispose());
        buttonPanel.add(btnCancel);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void sendToken() {
        String email = txtEmail.getText().trim();

        // Validaciones
        if (email.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Por favor ingresa tu email",
                    "Campo requerido",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!isValidEmail(email)) {
            JOptionPane.showMessageDialog(this,
                    "Email inválido. Por favor revisa el formato.",
                    "Email Inválido",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Buscar usuario por email
        UserDao userDao = new UserDao();
        User user = userDao.findByEmail(email);

        if (user == null) {
            JOptionPane.showMessageDialog(this,
                    "No encontramos una cuenta con ese email.\n" +
                    "Por favor verifica y intenta nuevamente.",
                    "Email no encontrado",
                    JOptionPane.INFORMATION_MESSAGE);
            logger.warn("Intento de recuperación con email no registrado: {}", email);
            return;
        }

        if (!user.isActive()) {
            JOptionPane.showMessageDialog(this,
                    "Tu cuenta está desactivada.\n" +
                    "Contacta al administrador.",
                    "Cuenta Desactivada",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Generar token
        PasswordResetTokenManager tokenManager = PasswordResetTokenManager.getInstance();
        String token = tokenManager.generateToken(user.getId(), email);

        // Enviar email
        EmailService emailService = new EmailService();
        boolean emailSent = emailService.sendPasswordResetEmail(email, token, user.getUsername());

        if (emailSent) {
            JOptionPane.showMessageDialog(this,
                    "✅ Email enviado exitosamente!\n\n" +
                    "Te hemos enviado un token a: " + email + "\n" +
                    "El token será válido por 24 horas.\n\n" +
                    "Revisa tu bandeja de entrada (o spam).",
                    "Token Enviado",
                    JOptionPane.INFORMATION_MESSAGE);
            success = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                    "❌ No pudimos enviar el email.\n\n" +
                    "Por favor verifica:\n" +
                    "1. Tu conexión a internet\n" +
                    "2. El email esté correcto\n" +
                    "3. Contacta al administrador si el problema persiste",
                    "Error al enviar email",
                    JOptionPane.ERROR_MESSAGE);
            logger.error("Fallo al enviar email de recuperación a: {}", email);
        }
    }

    private boolean isValidEmail(String email) {
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(emailPattern);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isTokenSent() {
        return success;
    }
}
