package com.ardhana.dto;

public class ProfileResponse {
    private String supabaseUid;
    private String fullName;
    private String email;
    private String mobile;
    private String role;
    private String country;
    // Relative to the API base (e.g. "/auth/profile/photo/{id}"), or null if
    // the user hasn't uploaded a photo.
    private String avatarUrl;
    private String bio;

    public ProfileResponse(String supabaseUid, String fullName,
                           String email, String mobile, String role, String country,
                           String avatarUrl, String bio) {
        this.supabaseUid = supabaseUid;
        this.fullName = fullName;
        this.email = email;
        this.mobile = mobile;
        this.role = role;
        this.country = country;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
    }

    public String getSupabaseUid() { return supabaseUid; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getMobile() { return mobile; }
    public String getRole() { return role; }
    public String getCountry() { return country; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getBio() { return bio; }
}
