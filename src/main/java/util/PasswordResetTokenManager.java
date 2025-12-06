/*
 * Gestor de tokens para recuperación de contraseña
 * Implementa tokens seguros con expiración de 24 horas
 */
package util;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gestor de tokens para recuperación de contraseña.
 * Genera tokens únicos con expiración automática de 24 horas.
 */
public class PasswordResetTokenManager {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetTokenManager.class);
    private static final int TOKEN_LENGTH = 32;
    private static final long EXPIRATION_HOURS = 24;
    private static PasswordResetTokenManager instance;
    
    // Estructura: token -> {userId, email, expirationTime}
    private final Map<String, TokenData> tokens = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    // Servicio de email (simulación + intento real si está configurado)
    private final EmailService emailService = new EmailService();

    private PasswordResetTokenManager() {
    }

    public static synchronized PasswordResetTokenManager getInstance() {
        if (instance == null) {
            instance = new PasswordResetTokenManager();
        }
        return instance;
    }

    /**
     * Genera un token único para recuperación de contraseña.
     *
     * @param userId ID del usuario
     * @param email Email del usuario
     * @return Token generado (32 caracteres alfanuméricos)
     */
    public String generateToken(int userId, String email) {
        // Limpiar tokens expirados
        cleanExpiredTokens();
        
        String token = generateSecureToken();
        LocalDateTime expirationTime = LocalDateTime.now().plus(EXPIRATION_HOURS, ChronoUnit.HOURS);
        
        tokens.put(token, new TokenData(userId, email, expirationTime));
        logger.info("Token generado para usuario {} - Expira: {}", userId, expirationTime);
        
        // Enviar correo de recuperación (simulado). Usamos el email como nombre de usuario
        // para no romper la API existente; es suficiente para la simulación de proyecto.
        try {
            boolean emailOk = emailService.sendPasswordResetEmail(email, token, email);
            if (emailOk) {
                logger.info("Correo de recuperación enviado (simulación/real) a: {}", email);
            } else {
                logger.warn("Fallo al intentar enviar correo de recuperación a: {}", email);
            }
        } catch (Exception e) {
            logger.error("Error al invocar EmailService para token: {}", token.substring(0, 8) + "...", e);
        }

        // Devolvemos el token (la app además lo guarda en memoria para pruebas)
        return token;
    }

    /**
     * Recupera (para pruebas) un token asociado a un usuario.
     * Devuelve el primer token válido encontrado o null si no existe.
     * Nota: esto facilita probar la recuperación en la propia aplicación.
     */
    public String getTokenForUser(int userId) {
        cleanExpiredTokens();
        for (Map.Entry<String, TokenData> e : tokens.entrySet()) {
            if (e.getValue().userId == userId) {
                return e.getKey();
            }
        }
        return null;
    }

    /**
     * Recupera (para pruebas) un token asociado a un email.
     */
    public String getTokenForEmail(String email) {
        cleanExpiredTokens();
        for (Map.Entry<String, TokenData> e : tokens.entrySet()) {
            if (e.getValue().email != null && e.getValue().email.equalsIgnoreCase(email)) {
                return e.getKey();
            }
        }
        return null;
    }

    /**
     * Valida un token de recuperación.
     *
     * @param token Token a validar
     * @return true si el token es válido y no ha expirado
     */
    public boolean isValidToken(String token) {
        if (!tokens.containsKey(token)) {
            logger.warn("Intento de uso de token inválido");
            return false;
        }

        TokenData data = tokens.get(token);
        if (LocalDateTime.now().isAfter(data.expirationTime)) {
            tokens.remove(token);
            logger.warn("Token expirado: {}", token.substring(0, 8) + "...");
            return false;
        }

        return true;
    }

    /**
     * Obtiene el ID de usuario asociado a un token.
     *
     * @param token Token válido
     * @return ID del usuario o -1 si no existe
     */
    public int getUserIdFromToken(String token) {
        if (!isValidToken(token)) {
            return -1;
        }
        return tokens.get(token).userId;
    }

    /**
     * Obtiene el email asociado a un token.
     *
     * @param token Token válido
     * @return Email del usuario o null si no existe
     */
    public String getEmailFromToken(String token) {
        if (!isValidToken(token)) {
            return null;
        }
        return tokens.get(token).email;
    }

    /**
     * Consume (invalida) un token de recuperación.
     *
     * @param token Token a consumir
     */
    public void consumeToken(String token) {
        tokens.remove(token);
        logger.info("Token consumido: {}", token.substring(0, 8) + "...");
    }

    /**
     * Genera un token seguro usando SecureRandom.
     *
     * @return Token de 32 caracteres alfanuméricos
     */
    private String generateSecureToken() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz";
        StringBuilder token = new StringBuilder();
        
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            token.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return token.toString();
    }

    /**
     * Limpia tokens expirados de la memoria.
     */
    private void cleanExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        tokens.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expirationTime));
    }

    /**
     * Estructura interna para almacenar datos del token.
     */
    private static class TokenData {
        int userId;
        String email;
        LocalDateTime expirationTime;

        TokenData(int userId, String email, LocalDateTime expirationTime) {
            this.userId = userId;
            this.email = email;
            this.expirationTime = expirationTime;
        }
    }
}
