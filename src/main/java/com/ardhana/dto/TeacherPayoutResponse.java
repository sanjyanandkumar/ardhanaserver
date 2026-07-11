package com.ardhana.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class TeacherPayoutResponse {
    private String id;
    private String teacherId;
    private String teacherName;
    private BigDecimal amount;
    private String currency;
    private String status;
    private int sessionCount;
    private String razorpayPayoutId;
    private String failureReason;
    private Instant createdAt;
    private Instant processedAt;

    public TeacherPayoutResponse(String id, String teacherId, String teacherName, BigDecimal amount,
                                  String currency, String status, int sessionCount, String razorpayPayoutId,
                                  String failureReason, Instant createdAt, Instant processedAt) {
        this.id = id;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.sessionCount = sessionCount;
        this.razorpayPayoutId = razorpayPayoutId;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
    }

    public String getId() { return id; }
    public String getTeacherId() { return teacherId; }
    public String getTeacherName() { return teacherName; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public int getSessionCount() { return sessionCount; }
    public String getRazorpayPayoutId() { return razorpayPayoutId; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getProcessedAt() { return processedAt; }
}
