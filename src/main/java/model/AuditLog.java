package model;

import java.time.LocalDateTime;

/**
 *
 * @author USER
 */
public class AuditLog {
    private int id;
    private int userId;
    private String action;
    private LocalDateTime dateTime;
    private String description;

    public AuditLog() {
    }

    public AuditLog(int id, int userId, String action, LocalDateTime dateTime, String description) {
        this.id = id;
        this.userId = userId;
        this.action = action;
        this.dateTime = dateTime;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    
}
