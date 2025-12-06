package ui.panels;

import javax.swing.*;
import java.awt.*; // Importar AWT para Font, GridBagLayout, etc.
import service.impl.AuthService;
import ui.frames.MainFrame;
import ui.dialogs.PasswordRecoveryDialog;
import ui.dialogs.PasswordResetDialog;
import model.User;
import config.app.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginPanel extends JPanel {
    private JTextField txtUser;
    private JPasswordField txtPass;
    private JButton btnLogin;
    private final AuthService authService;
    private final MainFrame mainFrame;
    private static final Logger logger = LoggerFactory.getLogger(LoginPanel.class);

    public LoginPanel(MainFrame frame) {
        this.mainFrame = frame;
        this.authService = service.impl.AuthService.getInstance();
        initUI(); // 👈 Este es el método que mejoramos
    }

    /**
     * Inicializa la UI con un formulario centrado usando GridBagLayout.
     */
    private void initUI() {
        // 1. El layout principal usará GridBagLayout para centrar el formulario
        setLayout(new GridBagLayout());

        // 2. Creamos un panel interno para el formulario
        JPanel formPanel = new JPanel(new GridBagLayout());
        // Añadimos un borde con padding
        formPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Padding entre componentes

        // --- 0. Título ---
        JLabel lblTitle = new JLabel("Iniciar Sesión");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 22));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2; // Ocupa 2 columnas
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 20, 0); // Padding inferior para el título
        formPanel.add(lblTitle, gbc);

        // --- 1. Etiqueta Usuario ---
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1; // Reseteamos a 1 columna
        gbc.anchor = GridBagConstraints.LINE_END; // Alinear a la derecha
        gbc.insets = new Insets(5, 5, 5, 5); // Resetear insets
        formPanel.add(new JLabel("Usuario:"), gbc);

        // --- 2. Campo Usuario ---
        txtUser = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_START; // Alinear a la izquierda
        gbc.fill = GridBagConstraints.HORIZONTAL;   // Rellenar horizontalmente
        gbc.weightx = 1.0;                          // Permitir que crezca
        formPanel.add(txtUser, gbc);

        // --- 3. Etiqueta Contraseña ---
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        formPanel.add(new JLabel("Contraseña:"), gbc);

        // --- 4. Campo Contraseña ---
        txtPass = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(txtPass, gbc);
        
        // --- 5. Botón Iniciar Sesión ---
        btnLogin = new JButton("Iniciar Sesión");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(15, 0, 0, 0); // Padding superior
        formPanel.add(btnLogin, gbc);

        // --- 6. Link de recuperación de contraseña ---
        JButton btnForgotPassword = new JButton("¿Olvidaste tu contraseña?");
        btnForgotPassword.setContentAreaFilled(false);
        btnForgotPassword.setBorderPainted(false);
        btnForgotPassword.setForeground(new Color(52, 152, 219));
        btnForgotPassword.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnForgotPassword.setFont(new Font("Arial", Font.PLAIN, 11));
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(10, 0, 0, 0);
        formPanel.add(btnForgotPassword, gbc);

        // --- 7. Link para cambiar contraseña con token ---
        JButton btnResetWithToken = new JButton("Cambiar contraseña con token");
        btnResetWithToken.setContentAreaFilled(false);
        btnResetWithToken.setBorderPainted(false);
        btnResetWithToken.setForeground(new Color(40, 167, 69));
        btnResetWithToken.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnResetWithToken.setFont(new Font("Arial", Font.PLAIN, 11));
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(5, 0, 0, 0);
        formPanel.add(btnResetWithToken, gbc);

        // --- ACCIONES (Listeners) ---
        btnLogin.addActionListener(e -> login());
        btnForgotPassword.addActionListener(e -> openPasswordRecovery());
        btnResetWithToken.addActionListener(e -> openPasswordReset());
        
        // BONUS: Permitir login con "Enter" en el campo de contraseña
        txtPass.addActionListener(e -> login());

        // 3. Añadimos el formPanel al LoginPanel principal (que lo centrará)
        add(formPanel, new GridBagConstraints());
    }


    private void login() {
        try {
            String username = txtUser.getText();
            String password = new String(txtPass.getPassword());
            
            if (username.isBlank() || password.isBlank()) {
                JOptionPane.showMessageDialog(this, "Usuario y contraseña no pueden estar vacíos.", "Campos Vacíos", JOptionPane.WARNING_MESSAGE);
                return;
            }

            User u = authService.login(username, password);
            SessionManager.getInstance().setCurrentUser(u);  // Guardar usuario en sesión
            JOptionPane.showMessageDialog(this, "Bienvenido " + u.getUsername(), "Inicio de Sesión", JOptionPane.INFORMATION_MESSAGE);
            mainFrame.loadDashboard();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error de Autenticación", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Abre el diálogo para solicitar recuperación de contraseña
     * El usuario ingresa su email y recibe un token para cambiar su contraseña
     */
    private void openPasswordRecovery() {
        try {
            JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            PasswordRecoveryDialog dialog = new PasswordRecoveryDialog(parentFrame);
            dialog.setVisible(true);
            
            if (dialog.isTokenSent()) {
                int option = JOptionPane.showConfirmDialog(this,
                    "Token enviado a tu email.\n¿Deseas cambiar tu contraseña ahora?",
                    "Token Enviado",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE);
                
                if (option == JOptionPane.YES_OPTION) {
                    openPasswordReset();
                }
            }
        } catch (Exception ex) {
            logger.error("Error abriendo diálogo de recuperación", ex);
            JOptionPane.showMessageDialog(this,
                "Error al abrir el diálogo de recuperación: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Abre el diálogo para cambiar contraseña usando un token
     * El usuario pega el token recibido por email e ingresa su nueva contraseña
     */
    private void openPasswordReset() {
        try {
            JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            PasswordResetDialog dialog = new PasswordResetDialog(parentFrame);
            dialog.setVisible(true);
            
            if (dialog.isPasswordReset()) {
                JOptionPane.showMessageDialog(this,
                    "Contraseña actualizada exitosamente.\nPuedes iniciar sesión con tu nueva contraseña.",
                    "Contraseña Actualizada",
                    JOptionPane.INFORMATION_MESSAGE);
                // Limpiar campos de login
                txtUser.setText("");
                txtPass.setText("");
                txtUser.requestFocus();
            }
        } catch (Exception ex) {
            logger.error("Error abriendo diálogo de reset de contraseña", ex);
            JOptionPane.showMessageDialog(this,
                "Error al abrir el diálogo de reset: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
}