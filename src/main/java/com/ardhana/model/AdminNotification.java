package com.ardhana.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * A shared, admin-wide alert (not per-recipient) — any user with the admin
 * role sees the same list and the same read/unread state. Simpler than
 * per-admin delivery tracking, and matches how every other admin view in
 * this app already works (one shared view, not per-admin-user scoping).
 */
@Entity
@Table(name = "admin_notifications")
public class AdminNotification {

    @Id
    @Column(name = "id")
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private AdminNotificationType type;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    // Optional — lets the frontend link straight to the relevant class
    // schedule. Null for future notification types that aren't tied to one.
    @Column(name = "related_class_schedule_id")
    private String relatedClassScheduleId;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = Instant.now();
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
}
