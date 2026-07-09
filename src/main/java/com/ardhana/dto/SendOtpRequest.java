package com.ardhana.dto;

import jakarta.validation.constraints.NotBlank;

public class SendOtpRequest {
    @NotBlank
    private String mobile;

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
}
