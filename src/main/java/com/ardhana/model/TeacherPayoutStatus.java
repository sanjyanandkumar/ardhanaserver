package com.ardhana.model;

/** Mirrors the RazorpayX payout status vocabulary (see Razorpay Payout Status Details docs). */
public enum TeacherPayoutStatus {
    PENDING,     // created locally, Razorpay call not made/finished yet
    QUEUED,
    PROCESSING,
    PROCESSED,
    REVERSED,
    CANCELLED,
    REJECTED,
    FAILED
}
