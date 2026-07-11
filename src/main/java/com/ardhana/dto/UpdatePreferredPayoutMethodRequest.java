package com.ardhana.dto;

import com.ardhana.model.PayoutMethod;

import jakarta.validation.constraints.NotNull;

/** A teacher choosing which method (bank account or UPI) to pay them via, when both are on file. */
public class UpdatePreferredPayoutMethodRequest {

    @NotNull(message = "Payout method is required")
    private PayoutMethod method;

    public PayoutMethod getMethod() { return method; }
    public void setMethod(PayoutMethod method) { this.method = method; }
}
