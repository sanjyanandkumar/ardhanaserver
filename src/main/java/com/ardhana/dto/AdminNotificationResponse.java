package com.ardhana.dto;

import java.time.Instant;

import com.ardhana.model.AdminNotificationType;

public class AdminNotificationResponse {

    private String id;
    private AdminNotificationType type;
    private String message;
    private String relatedClassScheduleId;
    private boolean read;
    private Instant createdAt;

    public AdminNotificationResponse(String id, AdminNotificationType type, String message,
                                      String relatedClassScheduleId, boolean read, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.message = message;
        this.relatedClassScheduleId = relatedClassScheduleId;
        this.read = read;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public AdminNotificationType getType() { return type; }
    public void setType(AdminNotificationType type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getRelatedClassScheduleId() { return relatedClassScheduleId; }
    public void setRelatedClassScheduleId(String relatedClassScheduleId) { this.relatedClassScheduleId = relatedClassScheduleId; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
