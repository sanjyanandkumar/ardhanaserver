package com.ardhana.dto;

import jakarta.validation.constraints.*;

/**
 * TEMPORARY/INTERIM design: resets the password directly with no identity
 * verification (no email link, no OTP) — anyone who knows an account's email
 * can change its password. This was an explicit short-term request; a
 * verification step (email link or OTP) is planned to gate this before it's
 * safe to leave in place long-term.
 */
public class ForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 72, message = "Password must be 6-72 characters")
    private String newPassword;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
