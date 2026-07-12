package com.ardhana.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** A name + mobile number captured from someone downloading the app — visible to admins. */
@Entity
@Table(name = "app_download_leads")
public class AppDownloadLead {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "mobile", nullable = false, length = 20)
    private String mobile;

    @Column(name = "platform", nullable = false, length = 20)
    private String platform = "android";

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public Instant getCreatedAt() { return createdAt; }
}
