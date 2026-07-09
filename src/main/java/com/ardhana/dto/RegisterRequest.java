package com.ardhana.dto;

import jakarta.validation.constraints.*;

public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Mobile is required")
    @Pattern(
        regexp = "^\\+[1-9]\\d{7,14}$",
        message = "Mobile must include country code e.g. +91XXXXXXXXXX"
    )
    private String mobile;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 72, message = "Password must be 6-72 characters")
    private String password;

    @NotBlank(message = "Country is required")
    @Size(min = 2, max = 2, message = "Country must be a 2-letter code")
    private String country;

    // Getters and setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}
