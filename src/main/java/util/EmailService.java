/*
 * Servicio híbrido: Simula el envío y opcionalmente intenta enviarlo por Gmail.
 * Ideal para proyectos escolares.
 */
package util;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import config.app.ConfigManager;

public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final ConfigManager configManager = ConfigManager.getInstance();

    /**
     * Envía email de recuperación con token:
     * 1. Siempre simula el envío.
     * 2. Si hay SMTP configurado → intenta enviar el email real.
     */
    public boolean sendPasswordResetEmail(String recipientEmail, String token, String username) {
        try {
            // Validar email
            if (!isValidEmail(recipientEmail)) {
                logger.error("Email inválido: {}", recipientEmail);
                return false;
            }

            // =====================================================
            //    1. SIMULACIÓN (siempre funciona)
            // =====================================================
            System.out.println("\n=============================");
            System.out.println("   SIMULACIÓN DE ENVÍO DE EMAIL");
            System.out.println("=============================");
            System.out.println("Para: " + recipientEmail);
            System.out.println("Usuario: " + username);
            System.out.println("TOKEN DE RECUPERACIÓN: " + token);
            System.out.println("=============================\n");

            logger.info("Simulación completada.");

            // =====================================================
            //    2. INTENTO REAL (solo si config está completa)
            // =====================================================
            String senderEmail = configManager.getProperty("smtp.sender.email", "");
            String senderPass = configManager.getProperty("smtp.sender.password", "");

            if (senderEmail.isEmpty() || senderPass.isEmpty()) {
                logger.warn("SMTP no configurado. Solo se hizo simulación.");
                return true; // simulación funciona
            }

            enviarCorreoReal(recipientEmail, token, username);

            return true;

        } catch (Exception e) {
            logger.error("Error general en envío híbrido", e);
            return false;
        }
    }

    /**
     * Envío real vía Gmail SMTP.
     */
    private void enviarCorreoReal(String recipientEmail, String token, String username) {
        try {
            String host = configManager.getProperty("smtp.host", "smtp.gmail.com");
            String port = configManager.getProperty("smtp.port", "587");
            String senderEmail = configManager.getProperty("smtp.sender.email", "");
            String senderPassword = configManager.getProperty("smtp.sender.password", "");

            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(senderEmail, senderPassword);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Recuperación de Contraseña - Token");
            message.setText("Hola " + username + ", tu token es: " + token);

            Transport.send(message);

            logger.info("Correo REAL enviado correctamente a: {}", recipientEmail);

        } catch (Exception e) {
            logger.error("Fallo al enviar correo REAL, pero la simulación sí funcionó.", e);
        }
    }

    private boolean isValidEmail(String email) {
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email != null && email.matches(emailPattern);
    }

    /**
     * Email de confirmación (simulado + opcional real).
     */
    public boolean sendPasswordChangedEmail(String recipientEmail, String username) {
        try {
            System.out.println("\n=============================");
            System.out.println(" SIMULACIÓN EMAIL CONFIRMACIÓN");
            System.out.println("=============================");
            System.out.println("Para: " + recipientEmail);
            System.out.println("Usuario: " + username);
            System.out.println("Tu contraseña fue cambiada.");
            System.out.println("=============================\n");

            logger.info("Simulación completada (cambio de contraseña).");

            // intento real opcional
            String senderEmail = configManager.getProperty("smtp.sender.email", "");
            String senderPass = configManager.getProperty("smtp.sender.password", "");

            if (!senderEmail.isEmpty() && !senderPass.isEmpty()) {
                enviarCorreoConfirmacionReal(recipientEmail, username);
            }

            return true;

        } catch (Exception e) {
            logger.error("Error en simulación + intento real de confirmación", e);
            return false;
        }
    }

    private void enviarCorreoConfirmacionReal(String recipientEmail, String username) {
        try {
            String host = configManager.getProperty("smtp.host", "smtp.gmail.com");
            String port = configManager.getProperty("smtp.port", "587");
            String senderEmail = configManager.getProperty("smtp.sender.email", "");
            String senderPassword = configManager.getProperty("smtp.sender.password", "");

            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(senderEmail, senderPassword);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Contraseña Actualizada");
            message.setText("Hola " + username + ", tu contraseña fue actualizada.");

            Transport.send(message);

            logger.info("Correo REAL de confirmación enviado.");

        } catch (Exception e) {
            logger.error("Fallo al enviar correo REAL de confirmación.", e);
        }
    }
}
