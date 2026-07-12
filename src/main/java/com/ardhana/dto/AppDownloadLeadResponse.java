package com.ardhana.dto;

import java.time.Instant;

/** Admin-facing view of a captured lead. */
public class AppDownloadLeadResponse {

    private String id;
    private String fullName;
    private String mobile;
    private String platform;
    private Instant createdAt;

    public AppDownloadLeadResponse(String id, String fullName, String mobile, String platform, Instant createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.mobile = mobile;
        this.platform = platform;
        this.createdAt = createdAt;
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
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
