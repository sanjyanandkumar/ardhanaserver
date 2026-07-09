package com.ardhana.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Records a successful OTP verification for a mobile number, so
 * AuthService.register() can confirm "this mobile was actually verified"
 * without re-checking with the SMS provider (MSG91's own verify endpoint is
 * one-shot — you can't ask it "was this verified?" a second time).
 *
 * A row is inserted when /api/auth/verify-otp succeeds. AuthService.register()
 * looks for an unused row for the submitted mobile, verified within the last
 * OTP_VALID_MINUTES, and marks it used so it can't be replayed for a second
 * registration.
 */
@Entity
@Table(name = "phone_verifications")
public class PhoneVerification {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "mobile", nullable = false, length = 20)
    private String mobile;

    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;

    @Column(name = "used", nullable = false)
    private Boolean used = false;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (verifiedAt == null) verifiedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public Boolean getUsed() { return used; }
    public void setUsed(Boolean used) { this.used = used; }
}
