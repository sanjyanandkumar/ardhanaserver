package com.ardhana.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** Admin sets the agreed per-session rate for a teacher. */
public class UpdateTeacherRateRequest {

    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.01", message = "Rate must be greater than zero")
    private BigDecimal ratePerSession;

    public BigDecimal getRatePerSession() { return ratePerSession; }
    public void setRatePerSession(BigDecimal ratePerSession) { this.ratePerSession = ratePerSession; }
}
