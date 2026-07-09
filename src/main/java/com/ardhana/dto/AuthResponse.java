package com.ardhana.dto;

public class AuthResponse {

    private String token;
    private ProfileResponse profile;

    public AuthResponse(String token, ProfileResponse profile) {
        this.token = token;
        this.profile = profile;
    }

    public String getToken() { return token; }
    public ProfileResponse getProfile() { return profile; }
}
