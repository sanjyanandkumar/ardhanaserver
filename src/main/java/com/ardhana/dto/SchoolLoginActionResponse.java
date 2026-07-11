package com.ardhana.dto;

/**
 * Confirms a school login was created/reset. Carries the freshly-generated
 * plaintext password ONCE, for the admin who just performed the action to
 * see/copy on screen — the same password is also emailed to the school.
 * This is the only response in the system that ever exposes a plaintext
 * password; it's never logged and never persisted (only its bcrypt hash is).
 */
public class SchoolLoginActionResponse {
    private String email;
    private String password;

    public SchoolLoginActionResponse() {}
    public SchoolLoginActionResponse(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
