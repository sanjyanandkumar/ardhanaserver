package com.ardhana.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterPushTokenRequest {

    @NotBlank(message = "Token is required")
    @Size(max = 255)
    private String token;

    @NotBlank(message = "Platform is required")
    @Size(max = 20)
    private String platform;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
}
