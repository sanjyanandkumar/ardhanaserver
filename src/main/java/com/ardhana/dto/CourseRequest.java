package com.ardhana.dto;

import java.math.BigDecimal;

public class CourseRequest {
    private String title;
    private String description;
    private BigDecimal price;
    private Integer durationHours;
    private String level;
    private String thumbnailUrl;
    private String focuses; // comma-separated
    private String outcome;
    private Boolean active;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getDurationHours() { return durationHours; }
    public void setDurationHours(Integer durationHours) { this.durationHours = durationHours; }
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
}