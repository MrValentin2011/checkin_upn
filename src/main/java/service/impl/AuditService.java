/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service.impl;

import dao.impl.AuditLogDao;
import java.time.LocalDateTime;
import model.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servicio centralizado de auditoría.
 * Registra todas las operaciones críticas en BD y logs.
 */
public class AuditService {
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("audit");
    private final AuditLogDao dao = new AuditLogDao();
    private static AuditService instance;

    private AuditService() {
    }

    public static AuditService getInstance() {
        if (instance == null) {
            synchronized (AuditService.class) {
                if (instance == null) {
                    instance = new AuditService();
                }
            }
        }
        return instance;
    }

    /**
     * Registra una acción crítica en la auditoría.
     * Se almacena en BD y en archivo de log.
     */
    public void registrarAccion(int userId, String action, String description) {
        try {
            AuditLog log = new AuditLog(0, userId, action, LocalDateTime.now(), description);
            dao.insert(log);
            auditLogger.info("USER_ID={} | ACTION={} | DETAILS={}", userId, action, description);
        } catch (Exception e) {
            logger.error("Error al registrar auditoría", e);
        }
    }

    /**
     * Registra operación de login.
     */
    public void logLogin(Integer userId, String username, boolean success) {
        String action = "LOGIN";
        String details = "Username=" + username + " | Success=" + success;
        if (userId != null) {
            registrarAccion(userId, action, details);
        } else {
            auditLogger.warn("ANONYMOUS | ACTION={} | DETAILS={}", action, details);
        }
    }

    /**
     * Registra operación de logout.
     */
    public void logLogout(Integer userId, String username) {
        String action = "LOGOUT";
        String details = "Username=" + username;
        registrarAccion(userId, action, details);
    }

    /**
     * Registra check-in completado.
     */
    public void logCheckIn(Integer userId, Integer reservationId, String pnr, Integer seatId) {
        String action = "CHECKIN_COMPLETED";
        String details = "ReservationID=" + reservationId + " | PNR=" + pnr + " | SeatID=" + seatId;
        registrarAccion(userId, action, details);
    }

    /**
     * Registra cancelación de check-in.
     */
    public void logCheckInCancellation(Integer userId, Integer reservationId, String reason) {
        String action = "CHECKIN_CANCELLED";
        String details = "ReservationID=" + reservationId + " | Reason=" + reason;
        registrarAccion(userId, action, details);
    }

    /**
     * Registra cambio de asiento.
     */
    public void logSeatChange(Integer userId, Integer reservationId, Integer oldSeatId, Integer newSeatId) {
        String action = "SEAT_CHANGED";
        String details = "ReservationID=" + reservationId + " | OldSeat=" + oldSeatId + " | NewSeat=" + newSeatId;
        registrarAccion(userId, action, details);
    }

    /**
     * Registra error crítico.
     */
    public void logError(Integer userId, String operation, String errorMessage) {
        String action = "ERROR_" + operation;
        String details = errorMessage;
        registrarAccion(userId, action, details);
    }

    /**
     * Registra acceso denegado.
     */
    public void logAccessDenied(Integer userId, String operation, String reason) {
        String action = "ACCESS_DENIED";
        String details = "Operation=" + operation + " | Reason=" + reason;
        registrarAccion(userId, action, details);
    }

    /**
     * Registra cambio de contraseña por recuperación.
     */
    public void logPasswordReset(Integer userId, String username, boolean success) {
        String action = "PASSWORD_RESET";
        String details = "Username=" + username + " | Success=" + success;
        registrarAccion(userId, action, details);
    }

    /**
     * Registra cacelación de check-in por agente.
     */
    public void logCheckInCancellation(Integer userId, Integer reservationId) {
        String action = "CHECKIN_CANCELLED";
        String details = "ReservationID=" + reservationId + " | Check-in cancelado por el agente.";
        registrarAccion(userId, action, details);
    }

}
