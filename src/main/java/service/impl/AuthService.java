/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service.impl;

import dao.impl.UserDao;
import java.time.LocalTime;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.PasswordUtil;

/**
 * Servicio de autenticación con Singleton.
 */
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserDao userDao = new UserDao();
    private final AuditService auditService = AuditService.getInstance();
    private static AuthService instance;

    private AuthService() {
    }

    public static AuthService getInstance() {
        if (instance == null) {
            synchronized (AuthService.class) {
                if (instance == null) {
                    instance = new AuthService();
                }
            }
        }
        return instance;
    }

    public User login(String username, String password) throws Exception {
        try {
            User user = userDao.findByUsername(username);
            if (user == null) {
                auditService.logLogin(null, username, false);
                logger.warn("Intento de login con usuario inexistente: {}", username);
                throw new Exception("Usuario no encontrado");
            }

            if (!user.isActive()) {
                auditService.logLogin(user.getId(), username, false);
                logger.warn("Intento de login con usuario inactivo: {}", username);
                throw new Exception("Usuario inactivo");
            }

            // Validar horario de acceso (configurable, deshabilitado por defecto para desarrollo)
            boolean enforceWorkHours = Boolean.parseBoolean(
                config.app.ConfigManager.getInstance().getProperty("enforce.work.hours", "false"));
            if (enforceWorkHours) {
                LocalTime now = LocalTime.now();
                LocalTime end = LocalTime.of(22, 0);
                if (now.isAfter(end)) {
                    auditService.logAccessDenied(user.getId(), "LOGIN", "Fuera de horario");
                    logger.warn("Intento de login fuera de horario para usuario: {}", username);
                    throw new Exception("Acceso fuera del horario laboral (0:00 - 22:00)");
                }
            }

            if (!PasswordUtil.verifyPassword(password, user.getPassword())) {
                auditService.logLogin(user.getId(), username, false);
                logger.warn("Intento de login con contraseña incorrecta para usuario: {}", username);
                throw new Exception("Contraseña incorrecta");
            }

            auditService.logLogin(user.getId(), username, true);
            logger.info("Login exitoso para usuario: {}", username);
            return user;
        } catch (Exception e) {
            logger.error("Error en autenticación", e);
            throw e;
        }
    }

    public void logout(User user) {
        if (user != null) {
            auditService.logLogout(user.getId(), user.getUsername());
            logger.info("Logout para usuario: {}", user.getUsername());
        }
    }
}
