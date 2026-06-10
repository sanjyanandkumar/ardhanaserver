package com.ardhana.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lesson_attachments",
    uniqueConstraints = @UniqueConstraint(columnNames = {"lesson_id", "student_id"}))
public class LessonAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Lob
    @Column(name = "file_data", nullable = false)
    private byte[] fileData;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @PrePersist
    public void prePersist() {
        if (uploadedAt == null) uploadedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getLessonId() { return lessonId; }
    public void setLessonId(UUID lessonId) { this.lessonId = lessonId; }
    public UUID getStudentId() { return studentId; }
    public void setStudentId(UUID studentId) { this.studentId = studentId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
}