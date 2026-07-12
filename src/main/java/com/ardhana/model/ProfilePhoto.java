package com.ardhana.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Stores a user's profile photo bytes, keyed by user id (1:1 with
 * UserProfile). Kept in its own table — like LessonAttachment — so the
 * (potentially large) BLOB isn't loaded every time UserProfile is fetched
 * for routine things like /api/auth/profile.
 */
@Entity
@Table(name = "profile_photos")
public class ProfilePhoto {

    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    // columnDefinition is required here: without it, Hibernate infers the
    // BLOB subtype from JPA's default @Column length (255) and decides this
    // should be TINYBLOB — which doesn't match the LONGBLOB column schema.sql
    // actually creates. On profiles using ddl-auto=update (local MySQL dev),
    // that mismatch makes Hibernate try to shrink the column on every
    // startup and fail with "Data too long" once any real photo is stored.
    @Lob
    @Column(name = "file_data", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] fileData;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    public void onSave() {
        updatedAt = Instant.now();
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
