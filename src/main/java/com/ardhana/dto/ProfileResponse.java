package com.ardhana.dto;

public class ProfileResponse {
    private String supabaseUid;
    private String fullName;
    private String email;
    private String mobile;
    private String role;

    public ProfileResponse(String supabaseUid, String fullName,
                           String email, String mobile, String role) {
        this.supabaseUid = supabaseUid;
        this.fullName = fullName;
        this.email = email;
        this.mobile = mobile;
        this.role = role;
    }

    public String getSupabaseUid() { return supabaseUid; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getMobile() { return mobile; }
    public String getRole() { return role; }
}