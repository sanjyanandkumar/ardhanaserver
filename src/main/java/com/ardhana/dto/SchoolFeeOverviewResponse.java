package com.ardhana.dto;

import java.math.BigDecimal;

/** One row in the admin's school-fees overview — one per school. */
public class SchoolFeeOverviewResponse {
    private String schoolId;
    private String schoolName;
    private BigDecimal feeAmount; // null = nothing currently due
    private String currency;
    private boolean hasLogin;

    public SchoolFeeOverviewResponse(String schoolId, String schoolName, BigDecimal feeAmount,
                                      String currency, boolean hasLogin) {
        this.schoolId = schoolId;
        this.schoolName = schoolName;
        this.feeAmount = feeAmount;
        this.currency = currency;
        this.hasLogin = hasLogin;
    }

    public String getSchoolId() { return schoolId; }
    public String getSchoolName() { return schoolName; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public String getCurrency() { return currency; }
    public boolean isHasLogin() { return hasLogin; }
}
