package com.ardhana.dto;

import java.math.BigDecimal;

/**
 * Returned from POST /api/school-payments/checkout — mirrors
 * CheckoutInitResponse (the student course-purchase equivalent). The
 * frontend uses this to open the Razorpay Checkout widget.
 */
public class SchoolPaymentCheckoutResponse {
    private String schoolPaymentId;
    private String razorpayOrderId;
    private BigDecimal amount;
    private String currency;
    private String keyId;
    private boolean testMode;

    public SchoolPaymentCheckoutResponse(String schoolPaymentId, String razorpayOrderId, BigDecimal amount,
                                          String currency, String keyId, boolean testMode) {
        this.schoolPaymentId = schoolPaymentId;
        this.razorpayOrderId = razorpayOrderId;
        this.amount = amount;
        this.currency = currency;
        this.keyId = keyId;
        this.testMode = testMode;
    }

    public String getSchoolPaymentId() { return schoolPaymentId; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getKeyId() { return keyId; }
    public boolean isTestMode() { return testMode; }
}
