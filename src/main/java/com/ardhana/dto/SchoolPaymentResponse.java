package com.ardhana.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class SchoolPaymentResponse {
    private String id;
    private String schoolId;
    private String schoolName;
    private BigDecimal amount;
    private String currency;
    private String status;
    private Instant createdAt;
    private Instant confirmedAt;

    public SchoolPaymentResponse(String id, String schoolId, String schoolName, BigDecimal amount,
                                  String currency, String status, Instant createdAt, Instant confirmedAt) {
        this.id = id;
        this.schoolId = schoolId;
        this.schoolName = schoolName;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
        this.confirmedAt = confirmedAt;
    }

    public String getId() { return id; }
    public String getSchoolId() { return schoolId; }
    public String getSchoolName() { return schoolName; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
}
