package com.ardhana.dto;

import java.math.BigDecimal;
import java.util.List;

/** The signed-in teacher's own earnings summary — everything they need to see about their pay. */
public class TeacherEarningsResponse {
    private BigDecimal ratePerSession; // null if admin hasn't set one yet
    private int pendingSessionCount;
    private BigDecimal pendingAmount;
    private BigDecimal totalPaidAmount;
    private boolean hasBankDetails;
    private String accountHolderName; // shown as-is (not sensitive on its own)
    private String maskedAccountNumber; // e.g. "•••• 4321", or null if not set
    private String maskedIfsc;
    private boolean hasUpiDetails;
    private String upiVpa; // shown in full to the owning teacher — a VPA is a payment handle, not a secret
    // Which method a payout would actually use right now: "BANK_ACCOUNT", "UPI", or null.
    private String effectivePayoutMethod;
    // The teacher's explicit choice, if they've set one (may differ from effectivePayoutMethod
    // if that preference's details were since removed). Null if never set.
    private String preferredPayoutMethod;
    private List<TeacherPayoutResponse> payoutHistory;

    public TeacherEarningsResponse(BigDecimal ratePerSession, int pendingSessionCount, BigDecimal pendingAmount,
                                    BigDecimal totalPaidAmount, boolean hasBankDetails, String accountHolderName,
                                    String maskedAccountNumber, String maskedIfsc, boolean hasUpiDetails,
                                    String upiVpa, String effectivePayoutMethod, String preferredPayoutMethod,
                                    List<TeacherPayoutResponse> payoutHistory) {
        this.ratePerSession = ratePerSession;
        this.pendingSessionCount = pendingSessionCount;
        this.pendingAmount = pendingAmount;
        this.totalPaidAmount = totalPaidAmount;
        this.hasBankDetails = hasBankDetails;
        this.accountHolderName = accountHolderName;
        this.maskedAccountNumber = maskedAccountNumber;
        this.maskedIfsc = maskedIfsc;
        this.hasUpiDetails = hasUpiDetails;
        this.upiVpa = upiVpa;
        this.effectivePayoutMethod = effectivePayoutMethod;
        this.preferredPayoutMethod = preferredPayoutMethod;
        this.payoutHistory = payoutHistory;
    }

    public BigDecimal getRatePerSession() { return ratePerSession; }
    public int getPendingSessionCount() { return pendingSessionCount; }
    public BigDecimal getPendingAmount() { return pendingAmount; }
    public BigDecimal getTotalPaidAmount() { return totalPaidAmount; }
    public boolean isHasBankDetails() { return hasBankDetails; }
    public String getAccountHolderName() { return accountHolderName; }
    public String getMaskedAccountNumber() { return maskedAccountNumber; }
    public String getMaskedIfsc() { return maskedIfsc; }
    public boolean isHasUpiDetails() { return hasUpiDetails; }
    public String getUpiVpa() { return upiVpa; }
    public String getEffectivePayoutMethod() { return effectivePayoutMethod; }
    public String getPreferredPayoutMethod() { return preferredPayoutMethod; }
    public List<TeacherPayoutResponse> getPayoutHistory() { return payoutHistory; }
}
