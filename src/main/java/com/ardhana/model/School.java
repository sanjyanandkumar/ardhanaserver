package com.ardhana.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "schools")
public class School {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    // The current amount owed by this school (a one-off invoice the admin
    // sets, e.g. a licensing/subscription fee). Null means nothing is due.
    // Cleared back to null once SchoolPaymentService confirms a payment
    // covering it — admin sets a new one for the next billing cycle.
    @Column(name = "fee_amount")
    private BigDecimal feeAmount;

    @Column(name = "fee_currency", length = 3)
    private String feeCurrency = "INR";

    // References UserProfile.id for the login account created for this
    // school (role=school) via SchoolAuthService. Null until an admin
    // creates one.
    @Column(name = "login_user_id")
    private String loginUserId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        if (createdAt == null) createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() { updatedAt = Instant.now(); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
    public String getFeeCurrency() { return feeCurrency; }
    public void setFeeCurrency(String feeCurrency) { this.feeCurrency = feeCurrency; }
    public String getLoginUserId() { return loginUserId; }
    public void setLoginUserId(String loginUserId) { this.loginUserId = loginUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}