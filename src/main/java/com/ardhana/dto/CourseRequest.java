package com.ardhana.dto;

import java.math.BigDecimal;

public class CourseRequest {
    private String title;
    private String name;
    private String description;
    private BigDecimal priceInr;
    private BigDecimal priceUsd;
    private BigDecimal priceEur;
    private BigDecimal priceGbp;
    private Integer sessions;
    private String level;
    private String thumbnailUrl;
    private String focuses; // comma-separated
    private String outcome;
    private Boolean active;

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
}