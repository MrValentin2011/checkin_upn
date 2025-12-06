/*
 * Modelo de notificación persistente
 */
package model;

import java.time.LocalDateTime;

/**
 * Representa una notificación persistente en el sistema.
 * Se almacena en BD y puede ser filtrada/buscada.
 */
public class Notification {
    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum Category {
        SYSTEM, CHECKIN, FLIGHT, BAGGAGE, PAYMENT, SECURITY, OTHER
    }

    public enum Status {
        UNREAD, READ, ARCHIVED
    }

    private int id;
    private int userId; // null = notificación global
    private String title;
    private String message;
    private Priority priority;
    private Category category;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private String actionUrl; // Link para tomar acción
    private String icon; // Emoji o icon name

    public Notification() {}

    public Notification(String title, String message, Priority priority, Category category) {
        this.title = title;
        this.message = message;
        this.priority = priority;
        this.category = category;
        this.status = Status.UNREAD;
        this.createdAt = LocalDateTime.now();
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s", priority, title, message);
    }
}
