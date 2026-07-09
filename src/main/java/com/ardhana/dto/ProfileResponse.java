package com.ardhana.dto;

public class ProfileResponse {
    private String supabaseUid;
    private String fullName;
    private String email;
    private String mobile;
    private String role;
    private String country;

    public ProfileResponse(String supabaseUid, String fullName,
                           String email, String mobile, String role, String country) {
        this.supabaseUid = supabaseUid;
        this.fullName = fullName;
        this.email = email;
        this.mobile = mobile;
        this.role = role;
        this.country = country;
    }

    public String getSupabaseUid() { return supabaseUid; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getMobile() { return mobile; }
    public String getRole() { return role; }
    public String getCountry() { return country; }
}