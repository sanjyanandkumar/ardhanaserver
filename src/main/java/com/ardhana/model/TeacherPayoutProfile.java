package com.ardhana.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Per-teacher payout settings: the agreed rate (admin-editable) and the
 * teacher's own bank account (teacher-editable only — admin never sees or
 * sets the actual account number/IFSC, just whether it's on file, to keep
 * banking-detail access limited to the account owner).
 */
@Entity
@Table(name = "teacher_payout_profiles")
public class TeacherPayoutProfile {

    @Id
    @Column(name = "user_id", length = 36)
    private String userId;

    // Rupees paid per completed class — set by an admin. Null means the
    // teacher isn't payable yet (no agreed rate on file).
    @Column(name = "rate_per_session")
    private BigDecimal ratePerSession;

    @Column(name = "account_holder_name")
    private String accountHolderName;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "ifsc")
    private String ifsc;

    // UPI VPA, e.g. "name@bank" — an alternative to bank_account details.
    // A teacher may have either, both, or neither on file.
    @Column(name = "upi_vpa")
    private String upiVpa;

    // Which method to use when BOTH bank and UPI details are on file. Null
    // is fine when only one method is set — resolvePayoutMethod() falls
    // back sensibly. Set explicitly by the teacher via the earnings page.
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_payout_method", length = 20)
    private PayoutMethod preferredPayoutMethod;

    // Cached from Razorpay's response after a successful payout, for
    // reference/troubleshooting only — never required as input.
    @Column(name = "razorpay_contact_id")
    private String razorpayContactId;

    @Column(name = "razorpay_fund_account_id")
    private String razorpayFundAccountId;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    public void onSave() { updatedAt = Instant.now(); }

    public boolean hasBankDetails() {
        return accountHolderName != null && !accountHolderName.isBlank()
                && accountNumber != null && !accountNumber.isBlank()
                && ifsc != null && !ifsc.isBlank();
    }

    public boolean hasUpiDetails() {
        return upiVpa != null && !upiVpa.isBlank();
    }

    public boolean hasAnyPayoutMethod() {
        return hasBankDetails() || hasUpiDetails();
    }

    /**
     * Which method a payout should actually use: the teacher's explicit
     * preference if it's still valid (details for it are still on file),
     * otherwise bank account if present, otherwise UPI if present,
     * otherwise null (nothing payable).
     */
    public PayoutMethod resolvePayoutMethod() {
        if (preferredPayoutMethod == PayoutMethod.UPI && hasUpiDetails()) return PayoutMethod.UPI;
        if (preferredPayoutMethod == PayoutMethod.BANK_ACCOUNT && hasBankDetails()) return PayoutMethod.BANK_ACCOUNT;
        if (hasBankDetails()) return PayoutMethod.BANK_ACCOUNT;
        if (hasUpiDetails()) return PayoutMethod.UPI;
        return null;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public BigDecimal getRatePerSession() { return ratePerSession; }
    public void setRatePerSession(BigDecimal ratePerSession) { this.ratePerSession = ratePerSession; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getIfsc() { return ifsc; }
    public void setIfsc(String ifsc) { this.ifsc = ifsc; }
    public String getUpiVpa() { return upiVpa; }
    public void setUpiVpa(String upiVpa) { this.upiVpa = upiVpa; }
    public PayoutMethod getPreferredPayoutMethod() { return preferredPayoutMethod; }
    public void setPreferredPayoutMethod(PayoutMethod preferredPayoutMethod) { this.preferredPayoutMethod = preferredPayoutMethod; }
    public String getRazorpayContactId() { return razorpayContactId; }
    public void setRazorpayContactId(String razorpayContactId) { this.razorpayContactId = razorpayContactId; }
    public String getRazorpayFundAccountId() { return razorpayFundAccountId; }
    public void setRazorpayFundAccountId(String razorpayFundAccountId) { this.razorpayFundAccountId = razorpayFundAccountId; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
