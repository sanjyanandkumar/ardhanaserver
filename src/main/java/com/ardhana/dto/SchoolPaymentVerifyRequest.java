package com.ardhana.dto;

import jakarta.validation.constraints.NotBlank;

/** Body posted by the frontend after Razorpay Checkout's handler callback fires with a successful payment. */
public class SchoolPaymentVerifyRequest {

    @NotBlank
    private String schoolPaymentId;

    @NotBlank
    private String razorpayOrderId;

    @NotBlank
    private String razorpayPaymentId;

    @NotBlank
    private String razorpaySignature;

    public String getSchoolPaymentId() { return schoolPaymentId; }
    public void setSchoolPaymentId(String schoolPaymentId) { this.schoolPaymentId = schoolPaymentId; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
    public String getRazorpaySignature() { return razorpaySignature; }
    public void setRazorpaySignature(String razorpaySignature) { this.razorpaySignature = razorpaySignature; }
}
