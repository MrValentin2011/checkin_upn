/*
 * Servicio de negocio para notificaciones
 */
package service.impl;

import dao.impl.NotificationDao;
import model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Servicio para gestión de notificaciones.
 * Maneja lógica de negocio, filtrado y búsqueda de notificaciones.
 */
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static NotificationService instance;
    private final NotificationDao dao;
    private final List<NotificationListener> listeners = Collections.synchronizedList(new ArrayList<>());

    /**
     * Interface para listeners de notificaciones
     */
    public interface NotificationListener {
        void onNotificationReceived(Notification notification);
        void onNotificationRead(int notificationId);
        void onNotificationArchived(int notificationId);
    }

    private NotificationService() {
        this.dao = new NotificationDao();
        NotificationDao.createTableIfNotExists();
    }

    /**
     * Obtiene la instancia singleton
     */
    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    /**
     * Crea y persiste una notificación para un usuario
     */
    public Notification createNotification(int userId, String title, String message,
                                          Notification.Priority priority,
                                          Notification.Category category) {
        return createNotificationInternal(userId, title, message, priority, category, null, null);
    }

    /**
     * Crea y persiste una notificación con URL de acción
     */
    public Notification createNotificationWithAction(int userId, String title, String message,
                                                    Notification.Priority priority,
                                                    Notification.Category category,
                                                    String actionUrl, String icon) {
        return createNotificationInternal(userId, title, message, priority, category, actionUrl, icon);
    }

    /**
     * Crea una notificación global (sin usuario específico)
     */
    public Notification broadcastNotification(String title, String message,
                                             Notification.Priority priority,
                                             Notification.Category category) {
        return createNotificationInternal(0, title, message, priority, category, null, null);
    }

    /**
     * Crea notificación interna
     */
    private Notification createNotificationInternal(int userId, String title, String message,
                                                   Notification.Priority priority,
                                                   Notification.Category category,
                                                   String actionUrl, String icon) {
        Notification notif = new Notification();
        if (userId > 0) {
            notif.setUserId(userId);
        }
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setPriority(priority);
        notif.setCategory(category);
        notif.setStatus(Notification.Status.UNREAD);
        notif.setCreatedAt(LocalDateTime.now());
        notif.setActionUrl(actionUrl);
        notif.setIcon(icon);

        if (dao.insert(notif)) {
            logger.info("Notificación creada: {} para usuario {}", notif.getId(), userId);
            notifyListeners(notif);
            return notif;
        }
        return null;
    }

    /**
     * Obtiene notificaciones no leídas
     */
    public List<Notification> getUnreadNotifications(int userId) {
        return dao.getUnread(userId);
    }

    /**
     * Obtiene todas las notificaciones (excepto archivadas)
     */
    public List<Notification> getAllNotifications(int userId) {
        return dao.getHistory(userId, 1000);
    }

    /**
     * Filtra notificaciones por categoría
     */
    public List<Notification> getNotificationsByCategory(int userId, Notification.Category category) {
        return dao.getByCategory(userId, category);
    }

    /**
     * Filtra notificaciones por prioridad
     */
    public List<Notification> getNotificationsByPriority(int userId, Notification.Priority priority) {
        return dao.getByPriority(userId, priority);
    }

    /**
     * Obtiene solo notificaciones críticas
     */
    public List<Notification> getCriticalNotifications(int userId) {
        List<Notification> critical = new ArrayList<>();
        for (Notification n : getAllNotifications(userId)) {
            if (n.getPriority() == Notification.Priority.CRITICAL ||
                n.getPriority() == Notification.Priority.HIGH) {
                critical.add(n);
            }
        }
        return critical;
    }

    /**
     * Cuenta notificaciones no leídas
     */
    public int getUnreadCount(int userId) {
        return dao.countUnread(userId);
    }

    /**
     * Marca como leída
     */
    public void markAsRead(int notificationId) {
        if (dao.markAsRead(notificationId)) {
            notifyListenersRead(notificationId);
        }
    }

    /**
     * Marca todas como leídas
     */
    public void markAllAsRead(int userId) {
        dao.markAllAsRead(userId);
    }

    /**
     * Archiva notificación
     */
    public void archive(int notificationId) {
        if (dao.archive(notificationId)) {
            notifyListenersArchived(notificationId);
        }
    }

    /**
     * Archiva todas las notificaciones de un usuario
     */
    public void archiveAll(int userId) {
        dao.archiveAll(userId);
    }

    /**
     * Busca notificaciones por texto
     */
    public List<Notification> search(int userId, String query) {
        List<Notification> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (Notification n : getAllNotifications(userId)) {
            if (n.getTitle().toLowerCase().contains(lowerQuery) ||
                n.getMessage().toLowerCase().contains(lowerQuery)) {
                results.add(n);
            }
        }
        return results;
    }

    /**
     * Obtiene notificaciones de un período
     */
    public List<Notification> getNotificationsPeriod(int userId, LocalDateTime from, LocalDateTime to) {
        List<Notification> filtered = new ArrayList<>();
        for (Notification n : getAllNotifications(userId)) {
            if (!n.getCreatedAt().isBefore(from) && !n.getCreatedAt().isAfter(to)) {
                filtered.add(n);
            }
        }
        return filtered;
    }

    /**
     * Registra un listener de notificaciones
     */
    public void addListener(NotificationListener listener) {
        listeners.add(listener);
    }

    /**
     * Desregistra un listener
     */
    public void removeListener(NotificationListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifica a listeners sobre nueva notificación
     */
    private void notifyListeners(Notification notification) {
        for (NotificationListener listener : listeners) {
            try {
                listener.onNotificationReceived(notification);
            } catch (Exception e) {
                logger.error("Error en listener de notificación", e);
            }
        }
    }

    /**
     * Notifica a listeners sobre lectura
     */
    private void notifyListenersRead(int notificationId) {
        for (NotificationListener listener : listeners) {
            try {
                listener.onNotificationRead(notificationId);
            } catch (Exception e) {
                logger.error("Error en listener de lectura", e);
            }
        }
    }

    /**
     * Notifica a listeners sobre archivado
     */
    private void notifyListenersArchived(int notificationId) {
        for (NotificationListener listener : listeners) {
            try {
                listener.onNotificationArchived(notificationId);
            } catch (Exception e) {
                logger.error("Error en listener de archivado", e);
            }
        }
    }

    /**
     * Ejemplo de casos de uso
     */
    public void sendCheckInNotification(int userId, String passengerName, String flightNumber) {
        createNotificationWithAction(
            userId,
            "Check-in completado",
            "Check-in de " + passengerName + " en vuelo " + flightNumber,
            Notification.Priority.MEDIUM,
            Notification.Category.CHECKIN,
            "checkin:" + userId,
            "✓"
        );
    }

    public void sendFlightNotification(int userId, String flightNumber, String message) {
        createNotification(
            userId,
            "Notificación de vuelo: " + flightNumber,
            message,
            Notification.Priority.HIGH,
            Notification.Category.FLIGHT
        );
    }

    public void sendSecurityAlert(int userId, String message) {
        createNotificationWithAction(
            userId,
            "Alerta de seguridad",
            message,
            Notification.Priority.CRITICAL,
            Notification.Category.SECURITY,
            "security:review",
            "⚠"
        );
    }

    public void sendSystemNotification(String message) {
        broadcastNotification(
            "Notificación del sistema",
            message,
            Notification.Priority.MEDIUM,
            Notification.Category.SYSTEM
        );
    }
}
