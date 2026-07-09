package com.ardhana.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    // The primary display name shown to students everywhere (admin table,
    // course cards, listings). "title" is kept as a secondary line shown
    // below the name — nullable since existing courses won't have one set
    // until an admin edits them.
    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // One price per supported currency — set whichever ones apply; a course
    // doesn't have to have a price in every currency. The old single
    // price/currency/duration_hours columns have been dropped from the DB.
    @Column(name = "price_inr")
    private BigDecimal priceInr;

    @Column(name = "price_usd")
    private BigDecimal priceUsd;

    @Column(name = "price_eur")
    private BigDecimal priceEur;

    @Column(name = "price_gbp")
    private BigDecimal priceGbp;

    @Column(name = "sessions")
    private Integer sessions;

    @Column(name = "level", length = 20)
    private String level = "BEGINNER";

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "focuses", columnDefinition = "TEXT")
    private String focuses; // comma-separated

    @Column(name = "outcome", columnDefinition = "TEXT")
    private String outcome;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

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
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPriceInr() { return priceInr; }
    public void setPriceInr(BigDecimal priceInr) { this.priceInr = priceInr; }
    public BigDecimal getPriceUsd() { return priceUsd; }
    public void setPriceUsd(BigDecimal priceUsd) { this.priceUsd = priceUsd; }
    public BigDecimal getPriceEur() { return priceEur; }
    public void setPriceEur(BigDecimal priceEur) { this.priceEur = priceEur; }
    public BigDecimal getPriceGbp() { return priceGbp; }
    public void setPriceGbp(BigDecimal priceGbp) { this.priceGbp = priceGbp; }

    /** Returns the price set for a given currency code, or null if this course has none set for it. */
    public BigDecimal getPriceFor(String currencyCode) {
        if (currencyCode == null) return null;
        return switch (currencyCode.toUpperCase()) {
            case "INR" -> priceInr;
            case "USD" -> priceUsd;
            case "EUR" -> priceEur;
            case "GBP" -> priceGbp;
            default -> null;
        };
    }

    /**
     * Resolves the best available {currency, price} for this course given a
     * preferred currency, falling back through INR -> USD -> EUR -> GBP (in
     * that order) if the course has no price set in the preferred currency.
     * Returns null if the course has no price set in any currency.
     */
    public ResolvedPrice resolvePrice(String preferredCurrency) {
        if (preferredCurrency != null) {
            BigDecimal preferred = getPriceFor(preferredCurrency);
            if (preferred != null) return new ResolvedPrice(preferredCurrency.toUpperCase(), preferred);
        }
        for (String fallback : new String[] { "INR", "USD", "EUR", "GBP" }) {
            BigDecimal p = getPriceFor(fallback);
            if (p != null) return new ResolvedPrice(fallback, p);
        }
        return null;
    }

    public record ResolvedPrice(String currency, BigDecimal price) {}
    public Integer getSessions() { return sessions; }
    public void setSessions(Integer sessions) { this.sessions = sessions; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public String getFocuses() { return focuses; }
    public void setFocuses(String focuses) { this.focuses = focuses; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}