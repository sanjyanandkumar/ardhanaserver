package com.ardhana.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Fields a signed-in user may edit about their own profile. Email and
 * mobile are intentionally excluded: email is the login identifier, and
 * mobile is OTP-verified at signup — changing either here would need its
 * own re-verification flow, which is out of scope for this simple update.
 */
public class UpdateProfileRequest {

    @NotBlank(message = "Name required")
    @Size(max = 100, message = "Name is too long")
    private String fullName;

    @Size(min = 2, max = 2, message = "Select a valid country")
    private String country;

    // Optional — null means "leave unchanged" (the frontend only sends this
    // for users with the teacher role; omit it entirely to avoid overwriting
    // an existing bio for anyone else).
    @Size(max = 280, message = "Bio is too long")
    private String bio;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
