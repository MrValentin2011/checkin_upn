/*
 * DAO para gestión de notificaciones persistentes
 */
package dao.impl;

import config.db.DBConnection;
import model.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Acceso a datos para notificaciones.
 * Gestiona CRUD, búsqueda, filtrado y archivado de notificaciones.
 */
public class NotificationDao {
    private static final Logger logger = LoggerFactory.getLogger(NotificationDao.class);

    /**
     * Crea la tabla de notificaciones si no existe
     */
    public static void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS NOTIFICATIONS (
                notification_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                title TEXT NOT NULL,
                message TEXT NOT NULL,
                priority TEXT NOT NULL,
                category TEXT NOT NULL,
                status TEXT DEFAULT 'UNREAD',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                read_at TIMESTAMP,
                action_url TEXT,
                icon TEXT,
                FOREIGN KEY (user_id) REFERENCES Users(user_id)
            )
        """;
        
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            // Crear índices
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_user_status ON NOTIFICATIONS(user_id, status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_created_at ON NOTIFICATIONS(created_at DESC)");
            logger.info("Tabla NOTIFICATIONS creada/verificada");
        } catch (SQLException e) {
            logger.error("Error creando tabla de notificaciones", e);
        }
    }

    /**
     * Inserta una nueva notificación
     */
    public boolean insert(Notification notification) {
        String sql = """
            INSERT INTO NOTIFICATIONS 
            (user_id, title, message, priority, category, status, action_url, icon)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setObject(1, notification.getUserId() == 0 ? null : notification.getUserId());
            ps.setString(2, notification.getTitle());
            ps.setString(3, notification.getMessage());
            ps.setString(4, notification.getPriority().name());
            ps.setString(5, notification.getCategory().name());
            ps.setString(6, notification.getStatus().name());
            ps.setString(7, notification.getActionUrl());
            ps.setString(8, notification.getIcon());
            
            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        notification.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error insertando notificación", e);
        }
        return false;
    }

    /**
     * Obtiene todas las notificaciones no leídas de un usuario
     */
    public List<Notification> getUnread(int userId) {
        return getByStatus(userId, Notification.Status.UNREAD);
    }

    /**
     * Obtiene notificaciones por estado
     */
    public List<Notification> getByStatus(int userId, Notification.Status status) {
        String sql = """
            SELECT * FROM NOTIFICATIONS 
            WHERE user_id = ? AND status = ? 
            ORDER BY created_at DESC
            """;
        
        List<Notification> notifications = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.setString(2, status.name());
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error obteniendo notificaciones por estado", e);
        }
        return notifications;
    }

    /**
     * Obtiene notificaciones por categoría
     */
    public List<Notification> getByCategory(int userId, Notification.Category category) {
        String sql = """
            SELECT * FROM NOTIFICATIONS 
            WHERE user_id = ? AND category = ? AND status != 'ARCHIVED'
            ORDER BY priority DESC, created_at DESC
            """;
        
        List<Notification> notifications = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.setString(2, category.name());
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error obteniendo notificaciones por categoría", e);
        }
        return notifications;
    }

    /**
     * Obtiene notificaciones por prioridad
     */
    public List<Notification> getByPriority(int userId, Notification.Priority priority) {
        String sql = """
            SELECT * FROM NOTIFICATIONS 
            WHERE user_id = ? AND priority = ? AND status != 'ARCHIVED'
            ORDER BY created_at DESC
            """;
        
        List<Notification> notifications = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.setString(2, priority.name());
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error obteniendo notificaciones por prioridad", e);
        }
        return notifications;
    }

    /**
     * Obtiene historial completo (todas excepto archivadas)
     */
    public List<Notification> getHistory(int userId, int limit) {
        String sql = """
            SELECT * FROM NOTIFICATIONS 
            WHERE user_id = ? AND status != 'ARCHIVED'
            ORDER BY created_at DESC
            LIMIT ?
            """;
        
        List<Notification> notifications = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error obteniendo historial de notificaciones", e);
        }
        return notifications;
    }

    /**
     * Marca una notificación como leída
     */
    public boolean markAsRead(int notificationId) {
        String sql = "UPDATE NOTIFICATIONS SET status = 'READ', read_at = CURRENT_TIMESTAMP WHERE notification_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, notificationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error marcando notificación como leída", e);
        }
        return false;
    }

    /**
     * Marca todas las notificaciones de un usuario como leídas
     */
    public boolean markAllAsRead(int userId) {
        String sql = "UPDATE NOTIFICATIONS SET status = 'READ', read_at = CURRENT_TIMESTAMP WHERE user_id = ? AND status = 'UNREAD'";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.error("Error marcando todas las notificaciones como leídas", e);
        }
        return false;
    }

    /**
     * Archiva una notificación
     */
    public boolean archive(int notificationId) {
        String sql = "UPDATE NOTIFICATIONS SET status = 'ARCHIVED' WHERE notification_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, notificationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error archivando notificación", e);
        }
        return false;
    }

    /**
     * Archiva todas las notificaciones de un usuario
     */
    public boolean archiveAll(int userId) {
        String sql = "UPDATE NOTIFICATIONS SET status = 'ARCHIVED' WHERE user_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.error("Error archivando todas las notificaciones", e);
        }
        return false;
    }

    /**
     * Cuenta notificaciones no leídas
     */
    public int countUnread(int userId) {
        String sql = "SELECT COUNT(*) FROM NOTIFICATIONS WHERE user_id = ? AND status = 'UNREAD'";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error contando notificaciones no leídas", e);
        }
        return 0;
    }

    /**
     * Mapea ResultSet a Notification
     */
    private Notification mapResultSet(ResultSet rs) throws SQLException {
        Notification notif = new Notification();
        notif.setId(rs.getInt("notification_id"));
        
        Object userId = rs.getObject("user_id");
        if (userId != null) {
            notif.setUserId(((Number) userId).intValue());
        }
        
        notif.setTitle(rs.getString("title"));
        notif.setMessage(rs.getString("message"));
        notif.setPriority(Notification.Priority.valueOf(rs.getString("priority")));
        notif.setCategory(Notification.Category.valueOf(rs.getString("category")));
        notif.setStatus(Notification.Status.valueOf(rs.getString("status")));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            notif.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp readAt = rs.getTimestamp("read_at");
        if (readAt != null) {
            notif.setReadAt(readAt.toLocalDateTime());
        }
        
        notif.setActionUrl(rs.getString("action_url"));
        notif.setIcon(rs.getString("icon"));
        
        return notif;
    }
}
