package util;

import dao.impl.UserDao;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.impl.AuditService;
import java.util.Scanner;

/**
 * Utilidad de consola para pruebas de recuperación de contraseña.
 * Permite generar tokens (que se muestran en consola por la simulación de EmailService)
 * y usar esos tokens para actualizar la contraseña directamente desde la consola.
 */
public class PasswordResetConsole {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetConsole.class);

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        PasswordResetTokenManager tokenManager = PasswordResetTokenManager.getInstance();
        UserDao userDao = new UserDao();
        AuditService audit = AuditService.getInstance();

        System.out.println("=== Consola de recuperación de contraseña (modo prueba) ===");
        while (true) {
            System.out.println("Elija una opción:\n1) Generar token por email\n2) Usar token para cambiar contraseña\n3) Salir");
            System.out.print("Opción: ");
            String opt = sc.nextLine().trim();
            if (opt.equals("1")) {
                System.out.print("Ingrese email del usuario: ");
                String email = sc.nextLine().trim();
                if (email.isEmpty()) {
                    System.out.println("Email vacío. Intente de nuevo.");
                    continue;
                }
                User u = userDao.findByEmail(email);
                if (u == null) {
                    System.out.println("No existe usuario con ese email.");
                    continue;
                }
                String token = tokenManager.generateToken(u.getId(), email);
                System.out.println("Token generado y (simulado) enviado. Revise la consola donde se imprime la simulación.");
                System.out.println("(Para pruebas puede recuperar el token en esta app con: PasswordResetTokenManager.getInstance().getTokenForEmail(\""+email+"\"))");

            } else if (opt.equals("2")) {
                System.out.print("Ingrese token: ");
                String token = sc.nextLine().trim();
                if (!tokenManager.isValidToken(token)) {
                    System.out.println("Token inválido o expirado.");
                    continue;
                }
                int userId = tokenManager.getUserIdFromToken(token);
                if (userId < 0) {
                    System.out.println("No se encontró usuario para ese token.");
                    continue;
                }

                System.out.print("Ingrese nueva contraseña: ");
                String newPass = sc.nextLine();
                if (newPass.length() < 6) {
                    System.out.println("La contraseña debe tener al menos 6 caracteres.");
                    continue;
                }

                String hashed = util.PasswordUtil.hashPassword(newPass);
                boolean updated = userDao.updatePassword(userId, hashed);
                if (updated) {
                    tokenManager.consumeToken(token);
                    User u = userDao.findById(userId);
                    String username = u != null ? u.getUsername() : String.valueOf(userId);
                    audit.logPasswordReset(userId, username, true);
                    System.out.println("Contraseña actualizada correctamente para el usuario: " + username);
                } else {
                    audit.logPasswordReset(userId, "?", false);
                    System.out.println("No se pudo actualizar la contraseña (revisar logs).");
                }

            } else if (opt.equals("3") || opt.equalsIgnoreCase("salir")) {
                System.out.println("Saliendo...");
                break;
            } else {
                System.out.println("Opción inválida.");
            }
        }

        sc.close();
    }
}
