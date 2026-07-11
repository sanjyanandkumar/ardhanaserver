package com.ardhana.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** Admin sets (or clears, by passing null) the amount currently owed by a school. */
public class UpdateSchoolFeeRequest {

    @NotNull(message = "Fee amount is required")
    @DecimalMin(value = "0.01", message = "Fee amount must be greater than zero")
    private BigDecimal feeAmount;

    private String currency; // optional — defaults to "INR" if omitted

    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
