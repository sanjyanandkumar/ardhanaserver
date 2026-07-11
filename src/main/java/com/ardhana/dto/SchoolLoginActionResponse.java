package com.ardhana.dto;

/** Confirms a school login was created/reset and credentials emailed — never carries the password itself. */
public class SchoolLoginActionResponse {
    private String email;

    public SchoolLoginActionResponse() {}
    public SchoolLoginActionResponse(String email) { this.email = email; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
