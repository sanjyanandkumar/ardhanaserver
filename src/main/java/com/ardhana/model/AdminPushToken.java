package com.ardhana.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * An FCM device token registered by an admin's app install, so
 * PushNotificationService knows where to send alerts (e.g. a teacher marking
 * a class unavailable). Keyed by the token itself — re-registering the same
 * device (e.g. after a token refresh) upserts rather than duplicating.
 */
@Entity
@Table(name = "admin_push_tokens")
public class AdminPushToken {

    @Id
    @Column(name = "token", length = 255)
    private String token;

    // Who registered it — kept for reference/debugging only; delivery itself
    // broadcasts to every stored token, not scoped per admin.
    @Column(name = "admin_user_id", nullable = false)
    private String adminUserId;

    @Column(name = "platform", nullable = false, length = 20)
    private String platform;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getAdminUserId() { return adminUserId; }
    public void setAdminUserId(String adminUserId) { this.adminUserId = adminUserId; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public Instant getCreatedAt() { return createdAt; }
}
