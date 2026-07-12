package com.ardhana.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Submitted by anyone (no login required) before downloading the app. */
public class AppDownloadLeadRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String fullName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(
        regexp = "^[0-9+()\\-\\s]{7,20}$",
        message = "Enter a valid mobile number"
    )
    private String mobile;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
}
