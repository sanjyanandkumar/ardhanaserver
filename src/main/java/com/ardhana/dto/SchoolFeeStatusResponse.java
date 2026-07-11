package com.ardhana.dto;

import java.math.BigDecimal;
import java.util.List;

/** The signed-in school's own fee status — what they see on their payment page. */
public class SchoolFeeStatusResponse {
    private String schoolId;
    private String schoolName;
    private BigDecimal feeAmount; // null = nothing currently due
    private String currency;
    private List<SchoolPaymentResponse> paymentHistory;

    public SchoolFeeStatusResponse(String schoolId, String schoolName, BigDecimal feeAmount,
                                    String currency, List<SchoolPaymentResponse> paymentHistory) {
        this.schoolId = schoolId;
        this.schoolName = schoolName;
        this.feeAmount = feeAmount;
        this.currency = currency;
        this.paymentHistory = paymentHistory;
    }

    public String getSchoolId() { return schoolId; }
    public String getSchoolName() { return schoolName; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public String getCurrency() { return currency; }
    public List<SchoolPaymentResponse> getPaymentHistory() { return paymentHistory; }
}
