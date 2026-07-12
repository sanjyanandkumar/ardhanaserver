package com.ardhana.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * The current Android app build (APK), stored as a single row keyed by
 * platform — same pattern as ProfilePhoto: kept out of any other table so
 * the (large) BLOB is only ever loaded when actually serving a download.
 * Uploading a new file overwrites the existing row for that platform.
 */
@Entity
@Table(name = "app_download_files")
public class AppDownloadFile {

    // Fixed value ("android") for now — a single current build per
    // platform. Room to add "ios" etc. later without a schema change.
    @Id
    @Column(name = "platform", length = 20)
    private String platform;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    // columnDefinition is required — see ProfilePhoto.fileData for why
    // (without it Hibernate infers TINYBLOB instead of LONGBLOB).
    @Lob
    @Column(name = "file_data", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] fileData;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @PrePersist
    @PreUpdate
    public void onSave() {
        uploadedAt = Instant.now();
    }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
}
