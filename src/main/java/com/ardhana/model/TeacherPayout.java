package com.ardhana.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One actual money transfer to a teacher, covering some number of their
 * completed-but-unpaid classes at the time it was made. See
 * ClassSchedule.teacherPayoutId, which links each covered session back here.
 */
@Entity
@Table(name = "teacher_payouts")
public class TeacherPayout {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "teacher_id", nullable = false)
    private String teacherId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TeacherPayoutStatus status = TeacherPayoutStatus.PENDING;

    @Column(name = "session_count", nullable = false)
    private int sessionCount;

    @Column(name = "razorpay_payout_id")
    private String razorpayPayoutId;

    @Column(name = "razorpay_fund_account_id")
    private String razorpayFundAccountId;

    @Column(name = "razorpay_contact_id")
    private String razorpayContactId;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public TeacherPayoutStatus getStatus() { return status; }
    public void setStatus(TeacherPayoutStatus status) { this.status = status; }
    public int getSessionCount() { return sessionCount; }
    public void setSessionCount(int sessionCount) { this.sessionCount = sessionCount; }
    public String getRazorpayPayoutId() { return razorpayPayoutId; }
    public void setRazorpayPayoutId(String razorpayPayoutId) { this.razorpayPayoutId = razorpayPayoutId; }
    public String getRazorpayFundAccountId() { return razorpayFundAccountId; }
    public void setRazorpayFundAccountId(String razorpayFundAccountId) { this.razorpayFundAccountId = razorpayFundAccountId; }
    public String getRazorpayContactId() { return razorpayContactId; }
    public void setRazorpayContactId(String razorpayContactId) { this.razorpayContactId = razorpayContactId; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
