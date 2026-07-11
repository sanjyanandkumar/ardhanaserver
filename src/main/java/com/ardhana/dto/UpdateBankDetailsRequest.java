package com.ardhana.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** A teacher setting their own payout bank account. */
public class UpdateBankDetailsRequest {

    @NotBlank(message = "Account holder name is required")
    private String accountHolderName;

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^[0-9]{5,20}$", message = "Enter a valid account number")
    private String accountNumber;

    @NotBlank(message = "IFSC is required")
    @Pattern(regexp = "^[A-Za-z]{4}0[A-Za-z0-9]{6}$", message = "Enter a valid IFSC code")
    private String ifsc;

    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getIfsc() { return ifsc; }
    public void setIfsc(String ifsc) { this.ifsc = ifsc; }
}
