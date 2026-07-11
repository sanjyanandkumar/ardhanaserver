package com.ardhana.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** A teacher setting their own UPI VPA (e.g. "name@bank") for payouts. */
public class UpdateUpiDetailsRequest {

    @NotBlank(message = "UPI ID is required")
    @Pattern(regexp = "^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z][a-zA-Z0-9.\\-]{1,64}$",
             message = "Enter a valid UPI ID, e.g. name@bank")
    private String vpa;

    public String getVpa() { return vpa; }
    public void setVpa(String vpa) { this.vpa = vpa; }
}
