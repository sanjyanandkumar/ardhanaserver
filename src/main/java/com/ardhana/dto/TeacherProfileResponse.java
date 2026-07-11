package com.ardhana.dto;

/** Public-facing teacher card for the landing page — name, photo, bio only. */
public class TeacherProfileResponse {
    private String id;
    private String fullName;
    private String avatarUrl;
    private String bio;

    public TeacherProfileResponse(String id, String fullName, String avatarUrl, String bio) {
        this.id = id;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
    }

    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getBio() { return bio; }
}
