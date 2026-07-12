package com.ardhana.dto;

import java.time.Instant;

/**
 * Public metadata about the current app build — lets the frontend show
 * "no build uploaded yet" instead of a broken download link/button, and
 * display file size without fetching the whole APK.
 */
public class AppDownloadFileInfoResponse {

    private boolean available;
    private String fileName;
    private long fileSize;
    private Instant uploadedAt;

    public AppDownloadFileInfoResponse() {}

    public AppDownloadFileInfoResponse(boolean available, String fileName, long fileSize, Instant uploadedAt) {
        this.available = available;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.uploadedAt = uploadedAt;
    }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
}
