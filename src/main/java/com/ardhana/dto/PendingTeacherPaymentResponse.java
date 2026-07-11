package com.ardhana.dto;

import java.math.BigDecimal;

/** One row in the admin's "pending payments" view — one per teacher with unpaid completed sessions. */
public class PendingTeacherPaymentResponse {
    private String teacherId;
    private String teacherName;
    private BigDecimal ratePerSession; // null if no rate set yet — payout is blocked until it is
    private int pendingSessionCount;
    private BigDecimal pendingAmount;
    private boolean hasBankDetails;
    private boolean hasUpiDetails;
    // What a payout would actually use right now: "BANK_ACCOUNT", "UPI", or null if neither is on file.
    private String payoutMethod;

    public PendingTeacherPaymentResponse(String teacherId, String teacherName, BigDecimal ratePerSession,
                                          int pendingSessionCount, BigDecimal pendingAmount, boolean hasBankDetails,
                                          boolean hasUpiDetails, String payoutMethod) {
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.ratePerSession = ratePerSession;
        this.pendingSessionCount = pendingSessionCount;
        this.pendingAmount = pendingAmount;
        this.hasBankDetails = hasBankDetails;
        this.hasUpiDetails = hasUpiDetails;
        this.payoutMethod = payoutMethod;
    }

    public String getTeacherId() { return teacherId; }
    public String getTeacherName() { return teacherName; }
    public BigDecimal getRatePerSession() { return ratePerSession; }
    public int getPendingSessionCount() { return pendingSessionCount; }
    public BigDecimal getPendingAmount() { return pendingAmount; }
    public boolean isHasBankDetails() { return hasBankDetails; }
    public boolean isHasUpiDetails() { return hasUpiDetails; }
    public String getPayoutMethod() { return payoutMethod; }
}
